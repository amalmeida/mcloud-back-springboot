package com.mcloud.auth0_authenticator.application.dto;

import com.mcloud.auth0_authenticator.domain.user.UserStatus;
import com.mcloud.auth0_authenticator.domain.user.UserType;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UserListItemDTO {
    private String id;
    private String email;
    private String name;
    private UserStatus status;
    private UserType type;
    private String phone;

}
