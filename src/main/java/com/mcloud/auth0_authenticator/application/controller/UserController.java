package com.mcloud.auth0_authenticator.application.controller;

import com.mcloud.auth0_authenticator.domain.exception.ErrorResponse;
import com.mcloud.auth0_authenticator.domain.exception.ErrorCode;
import com.mcloud.auth0_authenticator.domain.exception.GoogleOAuth2UpdateNotAllowedException;
import com.mcloud.auth0_authenticator.domain.exception.UserNotFoundException;
import com.mcloud.auth0_authenticator.domain.user.AppUser;
import com.mcloud.auth0_authenticator.domain.user.UserService;
import com.mcloud.auth0_authenticator.domain.user.UserUpdateDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users, roles, and permissions")
public class UserController {

    private final UserService userService;
    private final MessageSource messageSource;

    @Operation(summary = "Obter usuário por ID", description = "Retorna um usuário específico identificado por `userId` do banco local. Se não encontrado, sincroniza com o Auth0. Exige permissão 'read:users' em produção (autenticação desativada em desenvolvimento).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário retornado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppUser.class))),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<AppUser> getUserById(@Parameter(description = "ID do usuário a buscar (ex.: `auth0|123456789`)") @PathVariable String userId) {
        AppUser user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    @Operation(summary = "Sincronizar usuário do Auth0", description = "Sincroniza um usuário do Auth0 para o banco local usando o ID fornecido (ex.: `auth0|xxx` ou `google-oauth2|xxx`). Apenas id, email e nome são obrigatórios; outros campos são opcionais e atualizados via PUT. Exige permissão 'write:users' em produção (autenticação desativada em desenvolvimento).")
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

    @Operation(summary = "Listar todos os usuários", description = "Retorna a lista de usuários no banco local. Se vazia, sincroniza com o Auth0 (apenas id, email, nome). Outros campos são atualizados via PUT. Exige permissão 'read:users' em produção (autenticação desativada em desenvolvimento).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de usuários retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppUser.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<AppUser>> listUsers() {
        return ResponseEntity.ok(userService.listUsers());
    }

    @Operation(summary = "Atualizar usuário", description = "Atualiza um usuário identificado por `userId` com os dados fornecidos, exigindo campos completos (nome, tipo, detalhes, telefone, endereço). Aplica no banco local e no Auth0. Exige permissão 'write:users' em produção (autenticação desativada em desenvolvimento). Para usuários autenticados via Google OAuth2, nome e email não podem ser alterados.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(implementation = AppUser.class))),
            @ApiResponse(responseCode = "400", description = "Corpo da requisição inválido, erros de validação ou tentativa de alterar nome/email de usuário Google OAuth2", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{userId}")
    public ResponseEntity<Object> updateUser(@Parameter(description = "ID do usuário a atualizar (ex.: `auth0|123456789`)") @PathVariable String userId, @Valid @RequestBody UserUpdateDTO dto) {
        try {
            AppUser updatedAppUser = userService.updateUser(userId, dto);
            return ResponseEntity.ok(updatedAppUser);
        } catch (GoogleOAuth2UpdateNotAllowedException ex) {
            String message;
            try {
                message = messageSource.getMessage("error.google.oauth2.update.not.allowed", new Object[]{ex.getUserId()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.google.oauth2.update.not.allowed' não encontrada, usando mensagem padrão");
                message = "Não é possível atualizar nome ou email para usuários autenticados via Google OAuth2: " + ex.getUserId();
            }
            ErrorResponse errorResponse = new ErrorResponse(ErrorCode.GOOGLE_OAUTH2_UPDATE_NOT_ALLOWED, message);
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (IllegalArgumentException ex) {
            ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        } catch (UserNotFoundException ex) {
            String message;
            try {
                message = messageSource.getMessage("error.user.not.found", new Object[]{ex.getUserId()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.user.not.found' não encontrada, usando mensagem padrão");
                message = "Usuário com ID " + ex.getUserId() + " não encontrado";
            }
            ErrorResponse errorResponse = new ErrorResponse(ErrorCode.USER_NOT_FOUND, message);
            return ResponseEntity.status(404).body(errorResponse);
        } catch (Exception ex) {
            String message;
            try {
                message = messageSource.getMessage("error.generic", new Object[]{ex.getMessage()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.generic' não encontrada, usando mensagem padrão");
                message = "Erro inesperado: " + ex.getMessage();
            }
            log.error("Erro inesperado no controlador: {}", ex.getMessage(), ex);
            ErrorResponse errorResponse = new ErrorResponse(ErrorCode.GENERIC_ERROR, message);
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(summary = "Listar papéis disponíveis", description = "Retorna a lista de papéis disponíveis no Auth0. Exige permissão 'read:users' em produção (autenticação desativada em desenvolvimento).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de papéis retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/roles")
    public ResponseEntity<List<String>> listRoles() {
        return ResponseEntity.ok(userService.listRoles());
    }

    @Operation(summary = "Listar permissões disponíveis", description = "Retorna a lista de permissões associadas aos papéis no Auth0. Exige permissão 'read:users' em produção (autenticação desativada em desenvolvimento).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lista de permissões retornada com sucesso", content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = String.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/permissions")
    public ResponseEntity<List<String>> listPermissions() {
        return ResponseEntity.ok(userService.listPermissions());
    }
}