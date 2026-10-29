package com.aloute.controller.api;

import com.aloute.dto.media.response.MediaUploadResponseDto;
import com.aloute.exception.BadRequestException;
import com.aloute.service.CloudinaryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/media")
public class MediaApiController {

    private static final long MAX_PROFILE_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_POST_MEDIA_SIZE = 25 * 1024 * 1024;
    private static final java.util.Set<String> PROFILE_IMAGE_TYPES =
            java.util.Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final java.util.Set<String> POST_VIDEO_TYPES =
            java.util.Set.of("video/mp4", "video/webm", "video/ogg");

    private final CloudinaryService cloudinaryService;

    public MediaApiController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/upload")
    public ResponseEntity<MediaUploadResponseDto> upload(@RequestParam("file") MultipartFile file) throws java.io.IOException {
        String url = cloudinaryService.uploadFile(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        return ResponseEntity.ok(MediaUploadResponseDto.builder().url(url).build());
    }

    @PostMapping("/post-media")
    public ResponseEntity<MediaUploadResponseDto> uploadPostMedia(
            @RequestParam("file") MultipartFile file) throws java.io.IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Choose a photo or video to upload");
        }
        if (file.getSize() > MAX_POST_MEDIA_SIZE) {
            throw new BadRequestException("Post photos and videos must be 25 MB or smaller");
        }

        String contentType = file.getContentType();
        if (contentType == null
                || (!PROFILE_IMAGE_TYPES.contains(contentType) && !POST_VIDEO_TYPES.contains(contentType))) {
            throw new BadRequestException("Use JPEG, PNG, GIF, or WebP photos, or MP4, WebM, or Ogg videos");
        }

        byte[] media = file.getBytes();
        boolean validSignature = PROFILE_IMAGE_TYPES.contains(contentType)
                ? hasValidImageSignature(contentType, media)
                : hasValidVideoSignature(contentType, media);
        if (!validSignature) {
            throw new BadRequestException("The uploaded file is not a valid supported photo or video");
        }

        String url = cloudinaryService.uploadFile(media, file.getOriginalFilename(), contentType);
        return ResponseEntity.ok(MediaUploadResponseDto.builder().url(url).build());
    }

    @PostMapping({"/profile-image", "/image"})
    public ResponseEntity<MediaUploadResponseDto> uploadProfileImage(
            @RequestParam("file") MultipartFile file) throws java.io.IOException {
        if (file.isEmpty()) {
            throw new BadRequestException("Choose an image to upload");
        }
        if (file.getSize() > MAX_PROFILE_IMAGE_SIZE) {
            throw new BadRequestException("Profile images must be 5 MB or smaller");
        }
        String contentType = file.getContentType();
        if (contentType == null || !PROFILE_IMAGE_TYPES.contains(contentType)) {
            throw new BadRequestException("Profile images must be JPEG, PNG, GIF, or WebP");
        }

        byte[] image = file.getBytes();
        if (!hasValidImageSignature(contentType, image)) {
            throw new BadRequestException("The uploaded file is not a valid supported image");
        }

        String url = cloudinaryService.uploadFile(image, file.getOriginalFilename(), contentType);
        return ResponseEntity.ok(MediaUploadResponseDto.builder().url(url).build());
    }

    private boolean hasValidImageSignature(String contentType, byte[] image) {
        return switch (contentType) {
            case "image/jpeg" -> image.length >= 3
                    && (image[0] & 0xFF) == 0xFF
                    && (image[1] & 0xFF) == 0xD8
                    && (image[2] & 0xFF) == 0xFF;
            case "image/png" -> image.length >= 8
                    && (image[0] & 0xFF) == 0x89
                    && image[1] == 'P' && image[2] == 'N' && image[3] == 'G'
                    && (image[4] & 0xFF) == 0x0D
                    && (image[5] & 0xFF) == 0x0A
                    && (image[6] & 0xFF) == 0x1A
                    && (image[7] & 0xFF) == 0x0A;
            case "image/gif" -> image.length >= 6
                    && image[0] == 'G' && image[1] == 'I' && image[2] == 'F'
                    && image[3] == '8' && (image[4] == '7' || image[4] == '9')
                    && image[5] == 'a';
            case "image/webp" -> image.length >= 12
                    && image[0] == 'R' && image[1] == 'I' && image[2] == 'F' && image[3] == 'F'
                    && image[8] == 'W' && image[9] == 'E' && image[10] == 'B' && image[11] == 'P';
            default -> false;
        };
    }

    private boolean hasValidVideoSignature(String contentType, byte[] video) {
        return switch (contentType) {
            case "video/mp4" -> video.length >= 12
                    && video[4] == 'f' && video[5] == 't' && video[6] == 'y' && video[7] == 'p';
            case "video/webm" -> video.length >= 4
                    && (video[0] & 0xFF) == 0x1A
                    && (video[1] & 0xFF) == 0x45
                    && (video[2] & 0xFF) == 0xDF
                    && (video[3] & 0xFF) == 0xA3;
            case "video/ogg" -> video.length >= 4
                    && video[0] == 'O' && video[1] == 'g'
                    && video[2] == 'g' && video[3] == 'S';
            default -> false;
        };
    }
}