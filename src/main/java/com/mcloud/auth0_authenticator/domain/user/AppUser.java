package com.mcloud.auth0_authenticator.domain.user;

import com.mcloud.auth0_authenticator.domain.user.Address;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Data
public class AppUser {
    @Id
    private String id;

    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    private UserType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private String details;

    @ElementCollection
    private List<String> roles;

    @ElementCollection
    private List<String> permissions;

    private String passwordHash;

    private String phone;

    private String secondaryPhone;

    @OneToOne(cascade = CascadeType.ALL, optional = true)
    @JoinColumn(name = "address_id", referencedColumnName = "id")
    private Address address;
}