package com.stream.app.repositories;

import com.stream.app.entities.Video;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, String> {
    Optional <Video> findByTitle(String title);
    List<Video> findByUserId(UUID userId);
}
