package com.mcloud.auth0_authenticator.domain.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserDetails {
    @JsonProperty("idNumber")
    private String idNumber;

    @JsonProperty("taxId")
    private String taxId;

    @JsonProperty("notes")
    private String notes;
}