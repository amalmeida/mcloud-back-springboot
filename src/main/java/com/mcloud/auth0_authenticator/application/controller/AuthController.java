package com.mcloud.auth0_authenticator.application.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getUserInfo(@AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("userId", jwt.getSubject());
        userInfo.put("email", jwt.getClaimAsString("email"));
        userInfo.put("name", jwt.getClaimAsString("name"));
        userInfo.put("roles", jwt.getClaimAsStringList("https://api.mcloud.com/roles"));
        userInfo.put("permissions", jwt.getClaimAsStringList("permissions"));
        return ResponseEntity.ok(userInfo);
    }

    @GetMapping("/roles")
    public ResponseEntity<List<String>> getUserRoles(@AuthenticationPrincipal Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("https://api.mcloud.com/roles");
        return ResponseEntity.ok(roles != null ? roles : List.of());
    }
}