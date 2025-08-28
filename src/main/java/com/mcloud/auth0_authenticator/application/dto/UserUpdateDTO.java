package com.mcloud.auth0_authenticator.application.dto;

import com.mcloud.auth0_authenticator.domain.user.UserDetails;
import com.mcloud.auth0_authenticator.domain.user.UserStatus;
import com.mcloud.auth0_authenticator.domain.user.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {
    @NotBlank private String name;
    @NotBlank @Email private String email;

    private UserType type;
    private UserDetails details;

    private List<String> roles;
    private List<String> permissions;

    private String phone;
    private String secondaryPhone;

    private UserStatus status;

    private AddressDTO address;
}
