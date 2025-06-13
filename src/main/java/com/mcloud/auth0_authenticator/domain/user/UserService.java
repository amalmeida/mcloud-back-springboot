package com.mcloud.auth0_authenticator.domain.user;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.RolesFilter;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.roles.Role;
import com.auth0.json.mgmt.users.UsersPage;
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
import java.util.stream.Stream;

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
            appUsers = userRepository.findAll();
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
            log.info("Sincronizados {} usuários do Auth0 com sucesso", domainAppUsers.size());

        } catch (Auth0Exception e) {
            log.error("Falha ao sincronizar todos os usuários do Auth0", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void syncUserFromAuth0(String userId) {
        if (userId == null || !userId.matches("^(auth0|google-oauth2)\\|.+")) {
            log.error("Formato de ID de usuário inválido: {}", userId);
            throw new IllegalArgumentException(messageSource.getMessage("error.invalid.user.id", new Object[]{userId}, Locale.getDefault()));
        }
        try {
            log.info("Sincronizando usuário com ID: {} do Auth0", userId);
            com.auth0.json.mgmt.users.User auth0User = managementAPI.users().get(userId, null).execute().getBody();

            if (auth0User == null) {
                log.warn("Usuário não encontrado no Auth0: {}", userId);
                throw new UserNotFoundException(userId);
            }

            AppUser appUser = convertAuth0User(auth0User);
            userRepository.save(appUser);
            log.info("Usuário sincronizado com sucesso: {}", appUser.getEmail());

        } catch (Auth0Exception e) {
            log.error("Falha ao sincronizar usuário do Auth0 com ID: {}", userId, e);
            if (e instanceof APIException apiEx && apiEx.getStatusCode() == 404) {
                throw new UserNotFoundException(userId);
            }
            throw new RuntimeException(messageSource.getMessage("error.auth0.sync.failed", new Object[]{e.getMessage()}, Locale.getDefault()), e);
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
                    managementAPI.users().removeRoles(userId, managementAPI.users().listRoles(userId, null).execute().getBody().getItems().stream().map(Role::getId).collect(Collectors.toList())).execute();
                    managementAPI.users().addRoles(userId, roleIds).execute();
                }
            }

            return appUser;
        } catch (Auth0Exception e) {
            log.error("Falha ao atualizar usuário no Auth0 com ID: {}", userId, e);
            throw new RuntimeException(messageSource.getMessage("error.auth0.update.failed", new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }
    }

    private AppUser convertAuth0User(com.auth0.json.mgmt.users.User auth0User) {
        AppUser appUser = new AppUser();
        appUser.setId(auth0User.getId());
        appUser.setEmail(auth0User.getEmail());
        appUser.setName(auth0User.getName());

        if (auth0User.getAppMetadata() != null) {
            Object type = auth0User.getAppMetadata().get("type");
            Object details = auth0User.getAppMetadata().get("details");

            if (type != null) {
                try {
                    appUser.setType(UserType.valueOf(type.toString()));
                } catch (IllegalArgumentException e) {
                    log.warn("Tipo de usuário inválido: {}", type);
                }
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
            log.error("Falha ao listar papéis do Auth0", e);
            throw new RuntimeException(messageSource.getMessage("error.auth0.roles.failed", new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }
    }

    public List<String> listPermissions() {
        try {
            List<Role> roles = managementAPI.roles().list(new RolesFilter()).execute().getBody().getItems();
            Set<String> permissions = new HashSet<>();

            for (Role role : roles) {
                managementAPI.roles().listPermissions(role.getId(), null).execute().getBody()
                        .getItems().forEach(p -> permissions.add(p.getName()));
            }

            return new ArrayList<>(permissions);
        } catch (Auth0Exception e) {
            log.error("Falha ao listar permissões do Auth0", e);
            throw new RuntimeException(messageSource.getMessage("error.auth0.permissions.failed", new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }
    }
}