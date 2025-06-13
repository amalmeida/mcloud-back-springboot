package com.mcloud.auth0_authenticator.application.controller;

import com.mcloud.auth0_authenticator.domain.user.AppUser;
import com.mcloud.auth0_authenticator.domain.user.UserService;
import com.mcloud.auth0_authenticator.domain.user.UserUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;


    @PostMapping("/sync")
    @PreAuthorize("hasAuthority('write:users')")
    public ResponseEntity<String> syncUser(@RequestParam String userId) {
        userService.syncUserFromAuth0(userId);
        return ResponseEntity.ok("Usuário sincronizado com sucesso");
    }


    @GetMapping
  //  @PreAuthorize("hasAuthority('read:users')")
    public ResponseEntity<List<AppUser>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAuthority('write:users')")
    public ResponseEntity<AppUser> updateUser(@PathVariable String userId, @Valid @RequestBody UserUpdateDTO dto) {
        AppUser updatedAppUser = userService.updateUser(userId, dto);
        return ResponseEntity.ok(updatedAppUser);
    }

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('read:users')")
    public ResponseEntity<List<String>> listRoles() {

        return ResponseEntity.ok(userService.listRoles());
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('read:users')")
    public ResponseEntity<List<String>> listPermissions() {
        return ResponseEntity.ok(userService.listPermissions());
    }


}
