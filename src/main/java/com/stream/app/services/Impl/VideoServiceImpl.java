package com.stream.app.services.Impl;

import ch.qos.logback.core.util.StringUtil;
import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import com.stream.app.services.VideoService;
import jakarta.annotation.PostConstruct;
import org.springframework.aop.scope.ScopedProxyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

@Service
public class VideoServiceImpl implements VideoService {

    @Value("${files.video}")
    String DIR;

    @Value("${file.video.hsl}")
    String HSL_DIR;

    private VideoRepository videoRepository;

    public VideoServiceImpl(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    @PostConstruct
    public void init() {
        File file = new File(DIR);

        try {
            Files.createDirectories(Paths.get(HSL_DIR));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(!file.exists()) {
            file.mkdir();
            System.out.println("Folder created");
        } else {
            System.out.println("Folder already created");
        }
    }

    @Override
    public Video save(Video video, MultipartFile file) {
        try {
            String filename = file.getOriginalFilename();
            String contentType = file.getContentType();
            InputStream inputStream = file.getInputStream();

            assert filename != null;
            String cleanFileName = StringUtils.cleanPath(filename);
            String cleanFolder = StringUtils.cleanPath(DIR);
            Path path = Paths.get(cleanFolder, cleanFileName);

            System.out.println(path);

            Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);

            video.setContentType(contentType);
            video.setFilePath(path.toString());
            videoRepository.save(video);

            try {
                processVideo(video.getVideoId());
                return video;

            } catch (Exception e) {
                videoRepository.delete(video);
                System.out.println("Error in processing video");
                System.out.println(e);
            }

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
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
        Video video = this.get(videoId);
        String filePath = video.getFilePath();

        Path videoPath = Paths.get(filePath);
//        String output360p = HSL_DIR+videoId+"/360p";
//        String output720p = HSL_DIR+videoId+"/720p";
//        String output1080p = HSL_DIR+videoId+"/1080p";


        try {
//            Files.createDirectories(Paths.get(output360p));
//            Files.createDirectories(Paths.get(output720p));
//            Files.createDirectories(Paths.get(output1080p));

            Path outputPath = Paths.get(HSL_DIR, videoId);
            Files.createDirectories(outputPath);

            String ffmpegCmd = String.format(
                    "ffmpeg -i \"%s\" -c:v libx264 -c:a aac -strict -2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"%s/segment_%%3d.ts\"  \"%s/master.m3u8\" ",
                    videoPath, outputPath, outputPath
            );

            System.out.println(ffmpegCmd);
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", ffmpegCmd);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            int exit = process.waitFor();
            if (exit != 0) {
                throw new RuntimeException("video processing failed!!");
            }

            return videoId;
        } catch (IOException e) {
            throw new RuntimeException("Video processing Failed", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


    }

}
