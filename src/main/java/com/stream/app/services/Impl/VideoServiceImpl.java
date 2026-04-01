package com.stream.app.services.Impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.stream.app.entities.Video;
import com.stream.app.entities.VideoStatus;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.services.VideoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class VideoServiceImpl implements VideoService {

    private static final Logger log = LoggerFactory.getLogger(VideoServiceImpl.class);

    @Value("${files.video}")
    String DIR;

    @Value("${file.video.hsl}")
    String HSL_DIR;

    private final AmazonS3 s3Client;
    private final VideoRepository videoRepository;
    private final ExecutorService videoProcessingExecutor;

    @Value("${cloud.aws.bucket.name}")
    private String bucketName;

    @Value("${cloud.aws.cdn.url}")
    private String cdnUrl;

    public VideoServiceImpl(
            VideoRepository videoRepository,
            AmazonS3 s3Client,
            @Qualifier("videoProcessingExecutor") ExecutorService videoProcessingExecutor) {
        this.videoRepository = videoRepository;
        this.s3Client = s3Client;
        this.videoProcessingExecutor = videoProcessingExecutor;
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        String videoId = UUID.randomUUID().toString();
        video.setVideoId(videoId);

        // Save raw file to temp directory
        Path tempRawPath = Paths.get(System.getProperty("java.io.tmpdir"), videoId + "_raw.mp4");
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, tempRawPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not store raw file", e);
        }

        video.setContentType(file.getContentType());
        video.setFilePath(tempRawPath.toString());
        video.setStatus(VideoStatus.PROCESSING);

        // Persist immediately so frontend can poll status
        Video savedVideo = videoRepository.saveAndFlush(video);

        // Fire-and-forget: process in background thread pool
        videoProcessingExecutor.submit(() -> {
            try {
                log.info("Starting async processing for video: {}", savedVideo.getVideoId());
                processVideo(savedVideo.getVideoId());
                log.info("Processing complete for video: {}", savedVideo.getVideoId());
            } catch (Exception e) {
                log.error("Async processing failed for video: {}", savedVideo.getVideoId(), e);
                savedVideo.setStatus(VideoStatus.FAILED);
                videoRepository.save(savedVideo);
            }
        });

        // Return immediately — client gets a PROCESSING status
        return savedVideo;
    }

    @Override
    public Video get(String videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new RuntimeException("Video not found"));
    }

    @Override
    public Video getByTitle(String title) {
        return videoRepository.findByTitle(title)
                .orElseThrow(() -> new RuntimeException("Video not found with title: " + title));
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @Override
    public String processVideo(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow();
        Path rawVideoPath = Paths.get(video.getFilePath());
        Path hlsOutputDir = Paths.get(System.getProperty("java.io.tmpdir"), videoId);

        try {
            Files.createDirectories(hlsOutputDir);

            // Create subdirectories for each rendition
            Path dir360 = hlsOutputDir.resolve("360p");
            Path dir720 = hlsOutputDir.resolve("720p");
            Files.createDirectories(dir360);
            Files.createDirectories(dir720);

            // ABR: Two renditions — 360p (800kbps) and 720p (2500kbps)
            // Uses filter_complex with split to reliably produce both renditions.
            // The previous approach with two -vf flags silently dropped the second output.
            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" " +
                    "-filter_complex \"[v:0]split=2[v360][v720]; " +
                    "[v360]scale=-2:360[low]; [v720]scale=-2:720[high]\" " +
                    // 360p output
                    "-map \"[low]\" -map a:0 " +
                    "-c:v libx264 -preset veryfast -threads 2 -b:v 800k " +
                    "-c:a aac -b:a 96k -strict -2 " +
                    "-f hls -hls_time 10 -hls_list_size 0 " +
                    "-hls_segment_filename \"%s/segment_%%3d.ts\" \"%s/playlist.m3u8\" " +
                    // 720p output
                    "-map \"[high]\" -map a:0 " +
                    "-c:v libx264 -preset veryfast -threads 2 -b:v 2500k " +
                    "-c:a aac -b:a 128k -strict -2 " +
                    "-f hls -hls_time 10 -hls_list_size 0 " +
                    "-hls_segment_filename \"%s/segment_%%3d.ts\" \"%s/playlist.m3u8\"",
                    rawVideoPath,
                    dir360, dir360,
                    dir720, dir720
            );

            log.info("Running FFmpeg ABR for video {}", videoId);

            Process process = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd)
                    .redirectErrorStream(true)
                    .start();

            // Log FFmpeg output for debugging
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg [{}]: {}", videoId, line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
            }

            // Generate the master playlist that references both renditions
            String masterPlaylist = "#EXTM3U\n" +
                    "#EXT-X-VERSION:3\n" +
                    "#EXT-X-STREAM-INF:BANDWIDTH=896000,RESOLUTION=640x360\n" +
                    "360p/playlist.m3u8\n" +
                    "#EXT-X-STREAM-INF:BANDWIDTH=2628000,RESOLUTION=1280x720\n" +
                    "720p/playlist.m3u8\n";

            Path masterPath = hlsOutputDir.resolve("master.m3u8");
            Files.writeString(masterPath, masterPlaylist);

            log.info("FFmpeg ABR finished for video {}. Starting parallel S3 upload.", videoId);

            // IMPROVEMENT #6: Parallel S3 uploads
            List<Path> filesToUpload;
            try (Stream<Path> paths = Files.walk(hlsOutputDir)) {
                filesToUpload = paths.filter(Files::isRegularFile).collect(Collectors.toList());
            }

            List<CompletableFuture<Void>> uploadFutures = new ArrayList<>();
            for (Path filePath : filesToUpload) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    // Use relative path to preserve subdirectory structure (360p/, 720p/)
                    String relativePath = hlsOutputDir.relativize(filePath).toString();
                    String s3Key = "hls/" + videoId + "/" + relativePath;

                    ObjectMetadata metadata = new ObjectMetadata();
                    if (relativePath.endsWith(".m3u8")) {
                        metadata.setContentType("application/x-mpegURL");
                    } else if (relativePath.endsWith(".ts")) {
                        metadata.setContentType("video/MP2T");
                    }

                    try {
                        metadata.setContentLength(Files.size(filePath));
                    } catch (IOException e) {
                        log.warn("Could not set content length for {}", relativePath);
                    }

                    PutObjectRequest request = new PutObjectRequest(bucketName, s3Key, filePath.toFile())
                            .withMetadata(metadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead);

                    s3Client.putObject(request);
                    log.debug("Uploaded {} to S3", s3Key);
                }, videoProcessingExecutor);

                uploadFutures.add(future);
            }

            // Wait for all uploads to complete
            CompletableFuture.allOf(uploadFutures.toArray(new CompletableFuture[0])).join();

            log.info("All S3 uploads complete for video {}. {} files uploaded.",
                    videoId, filesToUpload.size());

            String finalHlsUrl = cdnUrl + "/hls/" + videoId + "/master.m3u8";
            video.setFilePath(finalHlsUrl);
            video.setStatus(VideoStatus.READY);
            videoRepository.save(video);

            return videoId;
        } catch (Exception e) {
            video.setStatus(VideoStatus.FAILED);
            videoRepository.save(video);
            throw new RuntimeException("Processing Failed for video: " + videoId, e);
        } finally {
            // Clean up local temp files
            FileSystemUtils.deleteRecursively(hlsOutputDir.toFile());
            try {
                Files.deleteIfExists(rawVideoPath);
            } catch (IOException ignored) {
            }
        }
    }

    @Override
    public List<Video> getVideosByUser(UUID userId) {
        return videoRepository.findByUserId(userId);
    }
}
