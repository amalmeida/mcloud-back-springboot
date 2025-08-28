package com.mcloud.auth0_authenticator.domain.user;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "app_user", indexes = {
        @Index(name = "idx_app_user_id", columnList = "id"),
        @Index(name = "idx_app_user_email", columnList = "email"),
        @Index(name = "idx_app_user_status", columnList = "status")
})
@Getter
@Setter
public class AppUser {
    @Id
    private String id;

    private String email;

    private String name;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    private UserType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private UserDetails details;

    private String phone;

    private String secondaryPhone;

    @ElementCollection
    private List<String> roles;

    @ElementCollection
    private List<String> permissions;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "address_id")
    @JsonManagedReference
    private Address address;

    @Override
    public String toString() {
        return "AppUser{" +
                "id='" + id + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", type=" + type +
                ", phone='" + phone + '\'' +
                ", secondaryPhone='" + secondaryPhone + '\'' +
                ", roles=" + roles +
                ", permissions=" + permissions +
                ", addressId=" + (address != null ? address.getId() : null) +
                '}';
    }
}