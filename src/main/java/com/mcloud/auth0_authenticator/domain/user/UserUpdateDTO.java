package com.mcloud.auth0_authenticator.domain.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {
    @NotBlank
    private String name;

    @NotBlank
    private String email;

    private UserType type;

    private UserDetails details;

    private List<String> roles;

    private List<String> permissions;

    private String phone;

    private String secondaryPhone;

    private UserStatus status;

    private String zipCode;

    private String state;

    private String city;

    private String neighborhood;

    private String street;

    private String number;

    private String complement;
}