package com.stream.app.services;

import com.stream.app.entities.Video;
import com.stream.app.repositories.VideoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface VideoService {

    Video save(Video video, MultipartFile file);

    Video get(String videoId);

    Video getByTitle(String title);

    List<Video> getAll();
}
