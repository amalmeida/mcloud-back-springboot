package com.mcloud.auth0_authenticator.application.dto;

import com.mcloud.auth0_authenticator.domain.user.UserDetails;
import com.mcloud.auth0_authenticator.domain.user.UserStatus;
import com.mcloud.auth0_authenticator.domain.user.UserType;
import lombok.*;

import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponseDTO {
    private String id;
    private String email;
    private String name;

    private UserStatus status;
    private UserType type;

    private UserDetails details;

    private String phone;
    private String secondaryPhone;

    private List<String> roles;
    private List<String> permissions;

    private AddressDTO address; // mesmo formato do update
}
