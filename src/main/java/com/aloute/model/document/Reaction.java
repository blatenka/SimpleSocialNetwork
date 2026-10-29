package com.aloute.model.document;

import com.aloute.model.enums.ReactionType;
import com.aloute.model.enums.TargetType;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "reactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@CompoundIndex(name = "user_target_idx", def = "{'userId': 1, 'targetId': 1, 'targetType': 1}", unique = true)
public class Reaction {
    @Id
    private String id;
    private Long userId;
    private String targetId;
    private TargetType targetType; 
    private ReactionType reactionType;
}