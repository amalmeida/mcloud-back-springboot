package com.mcloud.auth0_authenticator.application.controller;

import com.mcloud.auth0_authenticator.application.dto.PageResponse;
import com.mcloud.auth0_authenticator.application.dto.UserListItemDTO;
import com.mcloud.auth0_authenticator.application.dto.UserResponseDTO;
import com.mcloud.auth0_authenticator.application.dto.UserUpdateDTO;
import com.mcloud.auth0_authenticator.application.mapper.UserMapper;
import com.mcloud.auth0_authenticator.domain.exception.ErrorCode;
import com.mcloud.auth0_authenticator.domain.exception.ErrorResponse;
import com.mcloud.auth0_authenticator.domain.exception.GoogleOAuth2UpdateNotAllowedException;
import com.mcloud.auth0_authenticator.domain.exception.UserNotFoundException;
import com.mcloud.auth0_authenticator.domain.user.AppUser;
import com.mcloud.auth0_authenticator.domain.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.context.MessageSource;
import org.springframework.context.NoSuchMessageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "APIs for managing users, roles, and permissions")
public class UserController {

    private final UserService userService;
    private final MessageSource messageSource;

    @Operation(summary = "Obter usuário por ID", description = "Retorna DTO do usuário identificado por `userId`.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário retornado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Formato de ID inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDTO> getUserById(
            @Parameter(description = "ID do usuário (ex.: `auth0|123456789`)") @PathVariable String userId) {
        return ResponseEntity.ok(userService.getUserDtoById(userId));
    }

    @Operation(summary = "Sincronizar usuário do Auth0",
            description = "Sincroniza um usuário do Auth0 para o banco local usando o ID fornecido.")
    @PostMapping("/sync")
    public ResponseEntity<String> syncUser(
            @Parameter(description = "ID do usuário (ex.: `auth0|123456789`)") @RequestParam String userId) {
        userService.syncUserFromAuth0(userId);
        return ResponseEntity.ok("Usuário sincronizado com sucesso");
    }

    @Operation(
            summary = "Listar usuários (paginado, enxuto)",
            description = "Filtra por `name` e `email` (contains, case-insensitive). Retorna um payload leve."
    )
    @GetMapping
    public ResponseEntity<PageResponse<UserListItemDTO>> listUsers(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String email,
            @ParameterObject Pageable pageable
    ) {
        var page = userService.searchUsersList(name, email, pageable);
        String sort = pageable.getSort().isSorted() ? pageable.getSort().toString() : null;

        var body = new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                sort
        );
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "Atualizar usuário",
            description = "Atualiza com base no DTO (inclui `address`) e retorna DTO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário atualizado com sucesso",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Request inválida ou tentativa de alterar nome/email de Google OAuth2",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/{userId}")
    public ResponseEntity<Object> updateUser(
            @Parameter(description = "ID do usuário (ex.: `auth0|123456789`)") @PathVariable String userId,
            @Valid @RequestBody UserUpdateDTO dto) {
        try {
            log.info("Controller: Editando usuário {} ", userId);
            UserResponseDTO updated = userService.updateUserReturningDto(userId, dto);
            return ResponseEntity.ok(updated);
        } catch (GoogleOAuth2UpdateNotAllowedException ex) {
            String message;
            try {
                message = messageSource.getMessage("error.google.oauth2.update.not.allowed", new Object[]{ex.getUserId()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.google.oauth2.update.not.allowed' não encontrada, usando mensagem padrão");
                message = "Não é possível atualizar nome ou email para usuários autenticados via Google OAuth2: " + ex.getUserId();
            }
            return ResponseEntity.badRequest().body(new ErrorResponse(ErrorCode.GOOGLE_OAUTH2_UPDATE_NOT_ALLOWED, message));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
        } catch (UserNotFoundException ex) {
            String message;
            try {
                message = messageSource.getMessage("error.user.not.found", new Object[]{ex.getUserId()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.user.not.found' não encontrada, usando mensagem padrão");
                message = "Usuário com ID " + ex.getUserId() + " não encontrado";
            }
            return ResponseEntity.status(404).body(new ErrorResponse(ErrorCode.USER_NOT_FOUND, message));
        } catch (Exception ex) {
            String message;
            try {
                message = messageSource.getMessage("error.generic", new Object[]{ex.getMessage()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.generic' não encontrada, usando mensagem padrão");
                message = "Erro inesperado: " + ex.getMessage();
            }
            log.error("Erro inesperado no controlador: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body(new ErrorResponse(ErrorCode.GENERIC_ERROR, message));
        }
    }

    @Operation(summary = "Listar papéis disponíveis")
    @GetMapping("/roles")
    public ResponseEntity<List<String>> listRoles() {
        return ResponseEntity.ok(userService.listRoles());
    }

    @Operation(summary = "Listar permissões disponíveis")
    @GetMapping("/permissions")
    public ResponseEntity<List<String>> listPermissions() {
        return ResponseEntity.ok(userService.listPermissions());
    }

    @Operation(summary = "Atualizar status do usuário", description = "Ativo (1) ou Inativo (0). Retorna DTO.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status atualizado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "ID ou status inválido",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Usuário não encontrado",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{userId}/status")
    public ResponseEntity<Object> updateUserStatus(
            @Parameter(description = "ID do usuário (ex.: `auth0|123456789`)") @PathVariable String userId,
            @RequestBody Map<String, Integer> request) {
        try {
            Integer status = request.get("status");
            if (status == null) {
                String message = messageSource.getMessage("error.invalid.status", new Object[]{"null"}, Locale.ROOT);
                return ResponseEntity.badRequest().body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, message));
            }
            AppUser updated = userService.updateUserStatus(userId, status);
            return ResponseEntity.ok(UserMapper.toResponse(updated));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ErrorResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
        } catch (UserNotFoundException ex) {
            String message;
            try {
                message = messageSource.getMessage("error.user.not.found", new Object[]{ex.getUserId()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.user.not.found' não encontrada, usando mensagem padrão");
                message = "Usuário com ID " + ex.getUserId() + " não encontrado";
            }
            return ResponseEntity.status(404).body(new ErrorResponse(ErrorCode.USER_NOT_FOUND, message));
        } catch (Exception ex) {
            String message;
            try {
                message = messageSource.getMessage("error.generic", new Object[]{ex.getMessage()}, Locale.ROOT);
            } catch (NoSuchMessageException e) {
                log.warn("Mensagem 'error.generic' não encontrada, usando mensagem padrão");
                message = "Erro inesperado: " + ex.getMessage();
            }
            log.error("Erro inesperado no controlador: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body(new ErrorResponse(ErrorCode.GENERIC_ERROR, message));
        }
    }
}
