package com.mcloud.auth0_authenticator.domain.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateDTO {
    @NotBlank
    private String name;

    @NotNull
    private UserType type;

    @NotBlank
    private String details;

    private List<String> roles;

    private List<String> permissions;

    @NotBlank
    private String phone;

    private String secondaryPhone;

    @NotBlank
    private String zipCode;

    @NotBlank
    private String state;

    @NotBlank
    private String city;

    @NotBlank
    private String neighborhood;

    @NotBlank
    private String street;

    @NotBlank
    private String number;

    private String complement;
}