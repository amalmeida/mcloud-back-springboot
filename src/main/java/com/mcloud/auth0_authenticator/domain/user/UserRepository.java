package com.mcloud.auth0_authenticator.domain.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepository extends JpaRepository<AppUser, String> {

    Page<AppUser> findAllByNameContainingIgnoreCaseAndEmailContainingIgnoreCase(
            String name, String email, Pageable pageable
    );

}