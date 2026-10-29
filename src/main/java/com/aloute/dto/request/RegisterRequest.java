package com.aloute.dto.request;

import lombok.Getter; import lombok.Setter;

@Getter @Setter
class RegisterRequest {
    private String email;
    private String password;
    private String fullname; // Tên hiển thị ban đầu
}