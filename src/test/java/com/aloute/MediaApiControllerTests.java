package com.aloute;

import com.aloute.controller.api.MediaApiController;
import com.aloute.exception.BadRequestException;
import com.aloute.service.CloudinaryService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MediaApiControllerTests {

    private final CloudinaryService cloudinaryService = mock(CloudinaryService.class);
    private final MediaApiController controller = new MediaApiController(cloudinaryService);

    @Test
    void rejectsUnsupportedProfileImageTypes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.txt", "text/plain", "not an image".getBytes());

        assertThrows(BadRequestException.class, () -> controller.uploadProfileImage(file));
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void rejectsImageMimeTypeWhenFileSignatureDoesNotMatch() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.png", "image/png", "<html>not an image</html>".getBytes());

        assertThrows(BadRequestException.class, () -> controller.uploadProfileImage(file));
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void uploadsValidPngImage() throws Exception {
        byte[] image = Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/rWQAAAAASUVORK5CYII=");
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", image);
        when(cloudinaryService.uploadFile(image, "avatar.png", "image/png"))
                .thenReturn("https://images.example/avatar.png");

        var response = controller.uploadProfileImage(file);

        assertEquals("https://images.example/avatar.png", response.getBody().getUrl());
        verify(cloudinaryService).uploadFile(image, "avatar.png", "image/png");
    }

    @Test
    void uploadsValidMp4ForPostMedia() throws Exception {
        byte[] video = new byte[]{0, 0, 0, 24, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'};
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", video);
        when(cloudinaryService.uploadFile(video, "clip.mp4", "video/mp4"))
                .thenReturn("https://media.example/clip.mp4");

        var response = controller.uploadPostMedia(file);

        assertEquals("https://media.example/clip.mp4", response.getBody().getUrl());
        verify(cloudinaryService).uploadFile(video, "clip.mp4", "video/mp4");
    }

    @Test
    void rejectsVideoWithInvalidFileSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "clip.mp4", "video/mp4", "not a real video".getBytes());

        assertThrows(BadRequestException.class, () -> controller.uploadPostMedia(file));
        verifyNoInteractions(cloudinaryService);
    }

    @Test
    void rejectsUnsupportedPostMediaTypes() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "document.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(BadRequestException.class, () -> controller.uploadPostMedia(file));
        verifyNoInteractions(cloudinaryService);
    }
}
