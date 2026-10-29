package com.aloute.model.document;

import com.aloute.model.enums.Privacy;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "posts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
    @Id
    private String id;

    @Indexed
    private Long userId;

    private String content;

    @Builder.Default
    private List<String> mediaUrls = new ArrayList<>();

    @Builder.Default
    private Privacy privacy = Privacy.PUBLIC;

    @Builder.Default
    private int likeCount = 0;

    @Builder.Default
    private int commentCount = 0;

    @Builder.Default
    private int shareCount = 0;

    @Indexed(direction = org.springframework.data.mongodb.core.index.IndexDirection.DESCENDING)
    private LocalDateTime createdAt;

    @Builder.Default
    private Boolean isDeleted = false;
    private LocalDateTime deletedAt;
}