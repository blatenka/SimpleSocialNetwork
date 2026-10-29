package com.aloute.dto.post.request;

import com.aloute.model.enums.Privacy;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdatePostRequestDto {
    @Size(max = 5000)
    private String content;

    @Size(max = 10)
    private List<@NotBlank @Size(max = 2048) String> mediaUrls;

    private Privacy privacy;
}
