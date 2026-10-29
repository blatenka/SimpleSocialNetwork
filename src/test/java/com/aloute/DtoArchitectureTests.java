package com.aloute;

import com.aloute.dto.auth.request.RegisterRequestDto;
import com.aloute.dto.common.PageResponseDto;
import com.aloute.dto.user.response.UserProfileResponseDto;
import com.aloute.mapper.UserMapper;
import com.aloute.model.entity.User;
import com.aloute.model.entity.UserInfo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DtoArchitectureTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registrationRequestRejectsInvalidEmailAndMissingRequiredFields() {
        RegisterRequestDto request = new RegisterRequestDto();
        request.setEmail("invalid");
        request.setPassword("short");
        request.setConfirmPassword("different");
        request.setDob(LocalDate.now().plusDays(1));

        Set<String> invalidFields = validator.validate(request).stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(Collectors.toSet());

        assertTrue(invalidFields.contains("email"));
        assertTrue(invalidFields.contains("fullname"));
        assertTrue(invalidFields.contains("dob"));
    }

    @Test
    void userProfileResponseIsBoundedAndDoesNotExposeAccountData() throws Exception {
        User user = User.builder()
                .id(7L)
                .email("private@example.com")
                .password("hashed-password")
                .build();
        UserInfo info = UserInfo.builder()
                .id(7L)
                .user(user)
                .fullname("Test User")
                .build();
        user.setUserInfo(info);

        UserProfileResponseDto response = UserMapper.toProfileResponse(user, info);
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertTrue(json.has("id"));
        assertTrue(json.has("fullname"));
        assertFalse(json.has("email"));
        assertFalse(json.has("password"));
        assertFalse(json.has("user"));
    }

    @Test
    void pageResponseContainsOnlyStablePaginationFields() {
        PageResponseDto<String> response = PageResponseDto.from(
                new PageImpl<>(List.of("item"), PageRequest.of(1, 1), 2));

        assertTrue(response.getContent().contains("item"));
        assertTrue(response.isLast());
        assertFalse(response.isFirst());
        assertEquals(1, response.getPage());
        assertEquals(2, response.getTotalElements());
    }
}
