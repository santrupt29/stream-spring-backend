package com.stream.app.services.Impl;

import ch.qos.logback.core.util.StringUtil;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.services.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    String DIR;

    @Value("${file.video.hsl}")
    String HSL_DIR;

    @Autowired
    private AmazonS3 s3Client;

    @Value("${cloud.aws.bucket.name}") private String bucketName;
    @Value("${cloud.aws.cdn.url}") private String cdnUrl;

    private VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        String videoId = UUID.randomUUID().toString();
        video.setVideoId(videoId);

        Path tempRawPath = Paths.get(System.getProperty("java.io.tmpdir"), videoId + "_raw.mp4");
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, tempRawPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not store raw file", e);
        }

        video.setContentType(file.getContentType());
        video.setFilePath(tempRawPath.toString());

        // saveAndFlush to ensure the ID is in the DB before FFmpeg starts
        Video savedVideo = videoRepository.saveAndFlush(video);

        try {
            processVideo(savedVideo.getVideoId());
            return savedVideo;
        } catch (Exception e) {
            videoRepository.delete(savedVideo);
            try { Files.deleteIfExists(tempRawPath); } catch (IOException ignored) {}
            throw new RuntimeException("Video processing failed: " + e.getMessage());
        }
    }

    @Override
    public Video get(String videoId) {
        return videoRepository.findById(videoId).orElseThrow(() -> new RuntimeException("Video not found"));

    }

    @Override
    public Video getByTitle(String title) {
        return null;
    }

    @Override
    public List<Video> getAll() {
        return videoRepository.findAll();
    }

    @CrossOrigin("*")
    @Override
    public String processVideo(String videoId) {
        Video video = videoRepository.findById(videoId).orElseThrow();
        Path rawVideoPath = Paths.get(video.getFilePath());
        Path hlsOutputDir = Paths.get(System.getProperty("java.io.tmpdir"), videoId);

        try {
            Files.createDirectories(hlsOutputDir);

            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    rawVideoPath, hlsOutputDir, hlsOutputDir
            );

            Process process = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd).inheritIO().start();
            if (process.waitFor() != 0) throw new RuntimeException("FFmpeg failed");

            try (Stream<Path> paths = Files.walk(hlsOutputDir)) {
                paths.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString();
                    String s3Key = "hls/" + videoId + "/" + fileName;

                    com.amazonaws.services.s3.model.ObjectMetadata metadata = new com.amazonaws.services.s3.model.ObjectMetadata();

                    if (fileName.endsWith(".m3u8")) {
                        metadata.setContentType("application/x-mpegURL");
                    } else if (fileName.endsWith(".ts")) {
                        metadata.setContentType("video/MP2T");
                    }

                    PutObjectRequest request = new PutObjectRequest(bucketName, s3Key, path.toFile())
                            .withMetadata(metadata)
                            .withCannedAcl(CannedAccessControlList.PublicRead);

                    s3Client.putObject(request);
                });
            }

            String finalHlsUrl = cdnUrl + "/hls/" + videoId + "/master.m3u8";
            video.setFilePath(finalHlsUrl);
            videoRepository.save(video);

            return videoId;
        } catch (Exception e) {
            throw new RuntimeException("Processing Failed", e);
        } finally {
            // clean up local files
            FileSystemUtils.deleteRecursively(hlsOutputDir.toFile());
            try { Files.deleteIfExists(rawVideoPath); } catch (IOException ignored) {}
        }
    }

    @Override
    public List<Video> getVideosByUser(UUID userId) {
        return videoRepository.findByUserId(userId);
    }

}
