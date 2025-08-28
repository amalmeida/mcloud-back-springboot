package com.mcloud.auth0_authenticator.application.dto;

import com.mcloud.auth0_authenticator.domain.user.UserType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserSyncDTO {
    @NotBlank
    private String userId;
    @NotBlank
    private String email;
    @NotBlank
    private String name;
    @NotNull
    private UserType type;
    @NotBlank
    private String details;
    private List<String> roles;
    private List<String> permissions;
}