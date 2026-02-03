package com.stream.app.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Video {
    @Id
    private String videoId;
    private String title;
    private String description;
    private String contentType;
    private String filePath;
}
