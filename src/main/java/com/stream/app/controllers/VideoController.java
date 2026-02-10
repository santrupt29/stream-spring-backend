package com.stream.app.controllers;

import com.stream.app.AppConstants;
import com.stream.app.entities.Users;
import com.stream.app.entities.Video;
import com.stream.app.payload.CustomMessage;
import com.stream.app.services.VideoService;
import org.apache.coyote.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.ranges.RangeException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
@CrossOrigin("*")
public class VideoController {



    private VideoService videoService;

    public VideoController(VideoService videoService) {
        this.videoService = videoService;
    }


    @PostMapping
    public ResponseEntity<?> create(
            @RequestParam("file")MultipartFile file,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            Authentication authentication) {

        Users currentUser = (Users) authentication.getPrincipal();
        Video video = new Video();
        video.setTitle(title);
        video.setDescription(description);
        video.setVideoId(UUID.randomUUID().toString());
        video.setUser(currentUser);
        String contentType = file.getContentType();
        if(contentType == null || !contentType.startsWith("video/")) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(CustomMessage.builder()
                            .message("only video files allowed")
                            .success(false)
                            .build()
                    );
        }
        Video savedVideo = videoService.save(video, file);
        if(savedVideo != null) {
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .body(video);
        } else {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CustomMessage.builder()
                            .message("video not uploaded")
                            .success(false)
                            .build()
                    );
        }
    }

    @GetMapping
    public ResponseEntity<List<Video>> getAllVideos() {
        return ResponseEntity.ok(videoService.getAll());
    }

    @GetMapping("/stream/{videoId}")
    public ResponseEntity<Resource> stream(@PathVariable String videoId) {
        Video video = videoService.get(videoId);
        String contentType = video.getContentType();
        String filePath = video.getFilePath();

        Resource resource = new FileSystemResource(filePath);
        if(contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity
                .ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @GetMapping("/stream/range/{videoId}")
    public ResponseEntity<Resource> streamVideoRange(
            @PathVariable String videoId,
            @RequestHeader (value = "Range", required = false) String range) {
        System.out.println(range);
        Video video = videoService.get(videoId);
        Path path = Paths.get(video.getFilePath());

        Resource resource = new FileSystemResource(path);

        String contentType = video.getContentType();
        if(contentType == null) contentType = "application/octet-stream";

        // File length
        long fileLength = path.toFile().length();
        if(range == null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(resource);
        }

        long rangeStart;
        long rangeEnd;
        String []ranges = range.replace("bytes=", "").split("-");
        rangeStart = Long.parseLong(ranges[0]);

        rangeEnd = rangeStart + AppConstants.CHUNK_SIZE-1;
//        if(ranges.length > 1) {
//            rangeEnd = Long.parseLong(ranges[1]);
//        } else {
//            rangeEnd = fileLength-1;
//        }

        if(rangeEnd > fileLength-1) rangeEnd = fileLength-1;

        InputStream inputStream;

        try {
            inputStream = Files.newInputStream(path);
            long skipped = inputStream.skip(rangeStart);
            long contentLength = rangeEnd-rangeStart+1;

            byte []data = new byte[(int) contentLength];
            int read = inputStream.read(data, 0, data.length);
            System.out.println("read(number of bytes) : " + read);
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.add("Content-Range", "bytes "+rangeStart+"-"+rangeEnd+"/"+fileLength);
            httpHeaders.add("Cache-Control", "no-cache, no-store, must-revalidate");
            httpHeaders.add("Pragma", "no-cache");
            httpHeaders.add("Expires", "0");
            httpHeaders.add("X-Content-Type-Options", "nosniff");
            httpHeaders.setContentLength(contentLength);

            return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                    .headers(httpHeaders)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new ByteArrayResource(data));


        } catch (IOException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Video Transcoding
    // Serve HLS playlist

    @Value("${file.video.hsl}")
    private String HSL_DIR;
    // master.m2u8 file
    @GetMapping("/{videoId}/master.m3u8")
    public ResponseEntity<Resource> serveMasterFile(@PathVariable String videoId) {

        Path path = Paths.get(HSL_DIR, videoId, "master.m3u8");
        System.out.println(path);

        if(!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);

        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "application/vnd.apple.mpegurl"
                ).body(resource);

    }

    @GetMapping("/{videoId}/{segment}.ts")
    public ResponseEntity<Resource> serveSegments(
            @PathVariable String videoId,
            @PathVariable String segment
    ) {
        Path path = Paths.get(HSL_DIR, videoId, segment+".ts");
        if(!Files.exists(path)) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Resource resource = new FileSystemResource(path);
        return ResponseEntity
                .ok()
                .header(
                        HttpHeaders.CONTENT_TYPE, "video/mp2t"
                ).body(resource);
    }

    @GetMapping("/my-videos")
    public ResponseEntity<List<Video>> getVideosPerUser(Authentication authentication) {
        Users currentUser = (Users) authentication.getPrincipal();
        List<Video> videos = videoService.getVideosByUser(currentUser.getId());
        return ResponseEntity.ok(videos);
    }

}


