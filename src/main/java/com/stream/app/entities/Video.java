package com.stream.app.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Video {
    @Id
    private String videoId;
    @Column(nullable = false)
    private String title;
    private String description;
    private String contentType;
    private String filePath;
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant uploadedAt;
    @ManyToOne(fetch = FetchType.LAZY) // Crucial for performance,
                                       // When fetching a Video,
                                       // Spring won't waste resources
                                       // fetching the entire User object unless
                                       // explicitly called getVideo().getUser().
    @JoinColumn(name = "user_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Users user;
}
