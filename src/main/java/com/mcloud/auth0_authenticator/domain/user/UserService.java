package com.mcloud.auth0_authenticator.domain.user;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.RolesFilter;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.roles.Role;
import com.auth0.json.mgmt.users.UsersPage;
import com.mcloud.auth0_authenticator.domain.exception.ErrorCode;
import com.mcloud.auth0_authenticator.domain.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ManagementAPI managementAPI;
    private final MessageSource messageSource;

    public List<AppUser> listUsers() {
        List<AppUser> appUsers = userRepository.findAll();

        if (appUsers.isEmpty()) {
            log.warn("Nenhum usuário encontrado localmente. Disparando sincronização com Auth0.");
            syncAllUsersFromAuth0Async();
        }

        return appUsers;
    }

    @Async
    public CompletableFuture<Void> syncAllUsersFromAuth0Async() {
        try {
            UsersPage page = managementAPI.users().list(null).execute().getBody();
            List<com.auth0.json.mgmt.users.User> auth0Users = page.getItems();

            List<AppUser> domainAppUsers = auth0Users.stream()
                    .map(this::convertAuth0User)
                    .collect(Collectors.toList());

            userRepository.saveAll(domainAppUsers);

            log.info("Sincronização de {} usuários do Auth0 concluída com sucesso", domainAppUsers.size());

        } catch (Auth0Exception e) {
            log.error("Erro ao sincronizar todos os usuários do Auth0", e);
            // opcional: lançar exceção monitorada ou notificação interna
        }

        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void syncUserFromAuth0(String userId) {
        if (userId == null || !userId.matches("^(auth0|google-oauth2)\\|.+")) {
            throw new IllegalArgumentException("Invalid user ID format: " + userId);
        }
        try {
            log.info("Sincronizando usuário com ID: {} do Auth0", userId);
            com.auth0.json.mgmt.users.User auth0User = managementAPI.users().get(userId, null).execute().getBody();

            if (auth0User == null) {
                throw new UserNotFoundException(userId);
            }

            AppUser appUser = convertAuth0User(auth0User);
            userRepository.save(appUser);
            log.info("Usuário sincronizado com sucesso: {}", appUser.getEmail());

        } catch (Auth0Exception e) {
            log.error("Erro ao sincronizar usuário do Auth0 com ID: {}", userId, e);
            if (e instanceof APIException && ((APIException) e).getStatusCode() == 404) {
                throw new UserNotFoundException(userId);
            }
            throw new RuntimeException(messageSource.getMessage(ErrorCode.AUTH0_UPDATE_FAILED.getMessageKey(), new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }
    }

    @Transactional
    public AppUser updateUser(String userId, UserUpdateDTO dto) {
        try {
            AppUser appUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            appUser.setName(dto.getName());
            appUser.setType(dto.getType());
            appUser.setDetails(dto.getDetails());
            appUser.setRoles(dto.getRoles());
            appUser.setPermissions(dto.getPermissions());
            userRepository.save(appUser);

            com.auth0.json.mgmt.users.User auth0User = new com.auth0.json.mgmt.users.User();
            auth0User.setName(dto.getName());

            Map<String, Object> appMetadata = new HashMap<>();
            appMetadata.put("type", dto.getType().name());
            appMetadata.put("details", dto.getDetails());
            auth0User.setAppMetadata(appMetadata);

            managementAPI.users().update(userId, auth0User).execute();

            if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
                List<Role> allRoles = managementAPI.roles().list(new RolesFilter()).execute().getBody().getItems();
                List<String> roleIds = allRoles.stream()
                        .filter(role -> dto.getRoles().contains(role.getName()))
                        .map(Role::getId)
                        .collect(Collectors.toList());

                if (!roleIds.isEmpty()) {
                    managementAPI.users().addRoles(userId, roleIds).execute();
                }
            }

            return appUser;
        } catch (Auth0Exception e) {
            log.error("Erro ao atualizar usuário no Auth0 com ID: {}", userId, e);
            throw new RuntimeException(messageSource.getMessage(ErrorCode.AUTH0_UPDATE_FAILED.getMessageKey(), null, Locale.getDefault()), e);
        }
    }

    private AppUser convertAuth0User(com.auth0.json.mgmt.users.User auth0User) {
        AppUser appUser = new AppUser();
        appUser.setUserId(auth0User.getId());
        appUser.setEmail(auth0User.getEmail());
        appUser.setName(auth0User.getName());

        if (auth0User.getAppMetadata() != null) {
            Object type = auth0User.getAppMetadata().get("type");
            Object details = auth0User.getAppMetadata().get("details");

            if (type != null) {
                appUser.setType(UserType.valueOf(type.toString()));
            }
            if (details != null) {
                appUser.setDetails(details.toString());
            }
        }

        return appUser;
    }

    public List<String> listRoles() {
        try {
            return managementAPI.roles().list(new RolesFilter()).execute().getBody().getItems()
                    .stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());
        } catch (Auth0Exception e) {
            throw new RuntimeException(messageSource.getMessage(ErrorCode.AUTH0_ROLES_FAILED.getMessageKey(), null, Locale.getDefault()), e);
        }
    }

    public List<String> listPermissions() {
        try {
            List<Role> roles = managementAPI.roles().list(new RolesFilter()).execute().getBody().getItems();
            Set<String> permissions = new HashSet<>();

            for (Role role : roles) {
                try {
                    var response = managementAPI.roles().listPermissions(role.getId(), null).execute();
                    response.getBody().getItems().forEach(p -> permissions.add(p.getName()));
                } catch (Auth0Exception e) {
                    throw new RuntimeException("Erro ao buscar permissões do papel " + role.getName(), e);
                }
            }

            return new ArrayList<>(permissions);
        } catch (Auth0Exception e) {
            throw new RuntimeException(messageSource.getMessage(ErrorCode.AUTH0_PERMISSIONS_FAILED.getMessageKey(), null, Locale.getDefault()), e);
        }
    }
}
