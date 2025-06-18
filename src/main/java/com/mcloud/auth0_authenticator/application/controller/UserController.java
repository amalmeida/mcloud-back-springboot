package com.mcloud.auth0_authenticator.application.controller;

import com.mcloud.auth0_authenticator.domain.exception.ErrorResponse;
import com.mcloud.auth0_authenticator.domain.user.AppUser;
import com.mcloud.auth0_authenticator.domain.user.UserService;
import com.mcloud.auth0_authenticator.domain.user.UserUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "Sincronizar usuário do Auth0", description = "Sincroniza um usuário do Auth0 para o banco local usando o ID fornecido. O ID deve seguir o formato `auth0|xxx` ou `google-oauth2|xxx`. Apenas campos básicos (id, email, nome) são obrigatórios; outros são opcionais e atualizados via PUT. Autenticação desativada, mas exige permissão 'write:users' em produção.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário sincronizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado no Auth0", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(@Parameter(description = "ID do usuário a sincronizar (ex.: `auth0|123456789`)") @RequestParam String userId) {
        userService.syncUserFromAuth0(userId);
        return ResponseEntity.ok("Usuário sincronizado com sucesso");
    }

    @Operation(summary = "Listar todos os usuários", description = "Retorna a lista de usuários no banco local. Se não houver usuários, sincroniza com o Auth0, importando apenas campos básicos (id, email, nome). Campos adicionais são opcionais e atualizados via PUT. Autenticação desativada, mas exige permissão 'read:users' em produção.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppUser.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<AppUser>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza um usuário existente identificado por `userId` com os dados fornecidos, exigindo campos completos (nome, tipo, detalhes, telefone, endereço). A atualização é aplicada ao banco local (PostgreSQL) e ao Auth0. Autenticação desativada, mas exige permissão 'write:users' em produção.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppUser.class))),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido ou erros de validação", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{userId}")
    public ResponseEntity<AppUser> updateUser(@Parameter(description = "ID do usuário a atualizar (ex.: `auth0|123456789`)") @PathVariable String userId, @Valid @RequestBody UserUpdateDTO dto) {
        AppUser updatedAppUser = userService.updateUser(userId, dto);
        return ResponseEntity.ok(updatedAppUser);
    }

    @Operation(summary = "Listar papéis disponíveis", description = "Retorna a lista de papéis disponíveis no Auth0. Autenticação desativada, mas exige permissão 'read:users' em produção.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de papéis retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/roles")
    public ResponseEntity<List<String>> listRoles() {
        return ResponseEntity.ok(userService.listRoles());
    }

    @Operation(summary = "Listar permissões disponíveis", description = "Retorna a lista de permissões associadas aos papéis no Auth0. Autenticação desativada, mas exige permissão 'read:users' em produção.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de permissões retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/permissions")
    public ResponseEntity<List<String>> listPermissions() {
        return ResponseEntity.ok(userService.listPermissions());
    }
}
