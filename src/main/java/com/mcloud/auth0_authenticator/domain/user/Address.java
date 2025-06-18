package com.mcloud.auth0_authenticator.domain.user;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.mcloud.auth0_authenticator.domain.user.AppUser;
import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class Address {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String zipCode;

    private String state;

    private String city;

    private String neighborhood;

    private String street;

    private String number;

    private String complement;

    @OneToOne(mappedBy = "address")
    @JsonBackReference
    private AppUser user;
}