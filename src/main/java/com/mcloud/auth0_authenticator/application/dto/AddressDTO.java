package com.mcloud.auth0_authenticator.application.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AddressDTO {
    private Long id; // opcional no response

    private String zipCode;
    private String state;
    private String city;
    private String neighborhood;
    private String street;
    private String number;
    private String complement;

}
