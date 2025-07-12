package com.mcloud.auth0_authenticator.domain.user;

import com.auth0.client.mgmt.ManagementAPI;
import com.auth0.client.mgmt.filter.RolesFilter;
import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.json.mgmt.users.User;
import com.auth0.json.mgmt.roles.Role;
import com.auth0.json.mgmt.users.UsersPage;
import com.auth0.json.mgmt.permissions.Permission;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mcloud.auth0_authenticator.domain.exception.GoogleOAuth2UpdateNotAllowedException;
import com.mcloud.auth0_authenticator.domain.exception.UserNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
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
    private final ObjectMapper objectMapper;

    @Transactional(rollbackOn = Exception.class)
    public AppUser updateUser(String userId, UserUpdateDTO dto) {
        log.info("Service: Editando usuário {} ", userId);

        if (userId == null || userId.trim().isEmpty() || !userId.matches("^(auth0|google-oauth2)\\|.+")) {
            log.error("Formato de ID de usuário inválido: {}", userId);
            throw new IllegalArgumentException(messageSource.getMessage("error.invalid.user.id", new Object[]{userId}, Locale.getDefault()));
        }

        AppUser appUser = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        boolean isGoogleUser = userId.startsWith("google-oauth2|");
        if (isGoogleUser) {
            String existingName = appUser.getName() != null ? appUser.getName().trim() : "";
            String existingEmail = appUser.getEmail() != null ? appUser.getEmail().trim() : "";
            String newName = dto.getName() != null ? dto.getName().trim() : "";
            String newEmail = dto.getEmail() != null ? dto.getEmail().trim() : "";

            if ((!newName.isEmpty() && !newName.equals(existingName)) || (!newEmail.isEmpty() && !newEmail.equals(existingEmail))) {
                log.warn("Tentativa de alterar nome ou email para usuário Google OAuth2 com ID: {}", userId);
                throw new GoogleOAuth2UpdateNotAllowedException(userId);
            }
        }

        if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
            appUser.setName(dto.getName().trim());
        }
        if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
            appUser.setEmail(dto.getEmail().trim());
        }
        if (dto.getType() != null) {
            appUser.setType(dto.getType());
        }
        if (dto.getDetails() != null) {
            appUser.setDetails(dto.getDetails());
        }
        if (dto.getRoles() != null) {
            appUser.setRoles(dto.getRoles());
        }
        if (dto.getPermissions() != null) {
            appUser.setPermissions(dto.getPermissions());
        }
        if (dto.getPhone() != null) {
            appUser.setPhone(dto.getPhone());
        }
        if (dto.getSecondaryPhone() != null) {
            appUser.setSecondaryPhone(dto.getSecondaryPhone());
        }
        if (dto.getStatus() != null) {
            appUser.setStatus(dto.getStatus());
        }

        Address address = appUser.getAddress() != null ? appUser.getAddress() : new Address();
        boolean hasAddressData = false;
        if (dto.getZipCode() != null && !dto.getZipCode().isBlank()) {
            address.setZipCode(dto.getZipCode());
            hasAddressData = true;
        }
        if (dto.getState() != null && !dto.getState().isBlank()) {
            address.setState(dto.getState());
            hasAddressData = true;
        }
        if (dto.getCity() != null && !dto.getCity().isBlank()) {
            address.setCity(dto.getCity());
            hasAddressData = true;
        }
        if (dto.getNeighborhood() != null && !dto.getNeighborhood().isBlank()) {
            address.setNeighborhood(dto.getNeighborhood());
            hasAddressData = true;
        }
        if (dto.getStreet() != null && !dto.getStreet().isBlank()) {
            address.setStreet(dto.getStreet());
            hasAddressData = true;
        }
        if (dto.getNumber() != null && !dto.getNumber().isBlank()) {
            address.setNumber(dto.getNumber());
            hasAddressData = true;
        }
        if (dto.getComplement() != null && !dto.getComplement().isBlank()) {
            address.setComplement(dto.getComplement());
            hasAddressData = true;
        }

        if (hasAddressData) {
            address.setUser(appUser);
            appUser.setAddress(address);
        } else {
            appUser.setAddress(null);
        }

        log.info("Salvando AppUser com endereço: {}", appUser.getAddress());
        userRepository.save(appUser);

        User auth0User = new User();
        Map<String, Object> appMetadata = new HashMap<>();

        if (!isGoogleUser) {
            if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                auth0User.setName(dto.getName().trim());
            }
            if (dto.getEmail() != null && !dto.getEmail().trim().isEmpty()) {
                auth0User.setEmail(dto.getEmail().trim());
            }
        }

        if (dto.getType() != null) {
            appMetadata.put("type", dto.getType().getCode());
        }
        if (dto.getDetails() != null) {
            appMetadata.put("details", dto.getDetails());
        }
        if (dto.getPhone() != null) {
            appMetadata.put("phone", dto.getPhone());
        }
        if (dto.getSecondaryPhone() != null) {
            appMetadata.put("secondaryPhone", dto.getSecondaryPhone());
        }
        if (dto.getStatus() != null) {
            appMetadata.put("status", dto.getStatus().getCode());
        }
        if (hasAddressData) {
            appMetadata.put("address", Map.of(
                    "zipCode", address.getZipCode() != null ? address.getZipCode() : "",
                    "state", address.getState() != null ? address.getState() : "",
                    "city", address.getCity() != null ? address.getCity() : "",
                    "neighborhood", address.getNeighborhood() != null ? address.getNeighborhood() : "",
                    "street", address.getStreet() != null ? address.getStreet() : "",
                    "number", address.getNumber() != null ? address.getNumber() : "",
                    "complement", address.getComplement() != null ? address.getComplement() : ""
            ));
        }

        auth0User.setAppMetadata(appMetadata);

        try {
            log.info("Editando na AWS o usuário {} ", userId);

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

            syncUserFromAuth0(userId);
        } catch (Auth0Exception e) {
            log.error("Falha ao atualizar usuário no Auth0 com ID: {}", userId, e);
            e.printStackTrace(); // Adiciona stack trace completo no log
            throw new RuntimeException(messageSource.getMessage("error.auth0.update.failed", new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }

        return appUser;
    }

    @Transactional
    public AppUser getUserById(String userId) {
        if (userId == null || userId.trim().isEmpty() || !userId.matches("^(auth0|google-oauth2)\\|.+")) {
            log.error("Formato de ID de usuário inválido: {}", userId);
            throw new IllegalArgumentException(messageSource.getMessage("error.invalid.user.id", new Object[]{userId}, Locale.getDefault()));
        }

        Optional<AppUser> appUserOptional = userRepository.findById(userId);
        if (appUserOptional.isPresent()) {
            log.info("Usuário com ID {} encontrado no banco local", userId);
            return appUserOptional.get();
        }

        log.info("Usuário com ID {} não encontrado no banco local, sincronizando com o Auth0", userId);
        syncUserFromAuth0(userId);

        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public AppUser updateUserStatus(String userId, int status) {
        if (userId == null || userId.trim().isEmpty() || !userId.matches("^(auth0|google-oauth2)\\|.+")) {
            log.error("Formato de ID de usuário inválido: {}", userId);
            throw new IllegalArgumentException(messageSource.getMessage("error.invalid.user.id", new Object[]{userId}, Locale.getDefault()));
        }

        String statusCode;
        switch (status) {
            case 0:
                statusCode = "inativo";
                break;
            case 1:
                statusCode = "ativo";
                break;
            default:
                log.error("Valor de status inválido: {}", status);
                throw new IllegalArgumentException(messageSource.getMessage("error.invalid.status", new Object[]{status}, Locale.getDefault()));
        }

        UserStatus userStatus;
        try {
            userStatus = UserStatus.fromCode(statusCode);
        } catch (IllegalArgumentException e) {
            log.error("Erro ao converter status: {}", statusCode);
            throw new IllegalArgumentException(messageSource.getMessage("error.invalid.status", new Object[]{status}, Locale.getDefault()));
        }

        AppUser appUser = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.error("Usuário com ID {} não encontrado no banco local", userId);
                    return new UserNotFoundException(userId);
                });

        appUser.setStatus(userStatus);
        log.info("Atualizando status do usuário com ID {} para {}", userId, userStatus.getCode());
        userRepository.save(appUser);

        User auth0User = new User();
        Map<String, Object> appMetadata = new HashMap<>();
        appMetadata.put("status", userStatus.getCode());
        auth0User.setAppMetadata(appMetadata);

        try {
            managementAPI.users().update(userId, auth0User).execute();
            log.info("Status do usuário com ID {} atualizado com sucesso no Auth0", userId);
        } catch (Auth0Exception e) {
            log.error("Falha ao atualizar status do usuário no Auth0 com ID: {}, erro: {}", userId, e.getMessage());
            throw new RuntimeException(messageSource.getMessage("error.auth0.update.failed", new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }

        return appUser;
    }

    private AppUser convertAuth0User(com.auth0.json.mgmt.users.User auth0User) {
        AppUser appUser = new AppUser();
        appUser.setId(auth0User.getId());
        appUser.setEmail(auth0User.getEmail() != null ? auth0User.getEmail() : "");
        appUser.setName(auth0User.getName());
        appUser.setStatus(UserStatus.ATIVO);

        try {
            List<Role> roles = managementAPI.users().listRoles(auth0User.getId(), null).execute().getBody().getItems();
            appUser.setRoles(roles.stream().map(Role::getName).collect(Collectors.toList()));

            Set<String> permissions = new HashSet<>();
            for (Role role : roles) {
                List<Permission> permissionItems = managementAPI.roles().listPermissions(role.getId(), null)
                        .execute().getBody().getItems();
                permissions.addAll(permissionItems.stream()
                        .map(Permission::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }
            appUser.setPermissions(new ArrayList<>(permissions));
        } catch (Auth0Exception e) {
            log.error("Falha ao buscar roles ou permissões para usuário: {}", auth0User.getId(), e);
        }

        if (auth0User.getAppMetadata() != null) {
            Object type = auth0User.getAppMetadata().get("type");
            Object details = auth0User.getAppMetadata().get("details");
            Object phone = auth0User.getAppMetadata().get("phone");
            Object secondaryPhone = auth0User.getAppMetadata().get("secondaryPhone");
            Object status = auth0User.getAppMetadata().get("status");
            Object addressObj = auth0User.getAppMetadata().get("address");

            if (type != null) {
                try {
                    appUser.setType(UserType.fromCode(type.toString()));
                } catch (IllegalArgumentException e) {
                    log.warn("Tipo de usuário inválido: {}", type);
                }
            }
            if (details != null) {
                try {
                    appUser.setDetails(objectMapper.convertValue(details, UserDetails.class));
                } catch (Exception e) {
                    log.warn("Falha ao desserializar details: {}", details, e);
                }
            }
            if (phone != null) {
                appUser.setPhone(phone.toString());
            }
            if (secondaryPhone != null) {
                appUser.setSecondaryPhone(secondaryPhone.toString());
            }
            if (status != null) {
                try {
                    appUser.setStatus(UserStatus.fromCode(status.toString()));
                } catch (IllegalArgumentException e) {
                    log.warn("Status de usuário inválido: {}", status);
                }
            }
            if (addressObj != null) {
                try {
                    Map<String, String> addressMap = objectMapper.convertValue(addressObj, Map.class);
                    Address address = new Address();
                    address.setZipCode(addressMap.getOrDefault("zipCode", ""));
                    address.setState(addressMap.getOrDefault("state", ""));
                    address.setCity(addressMap.getOrDefault("city", ""));
                    address.setNeighborhood(addressMap.getOrDefault("neighborhood", ""));
                    address.setStreet(addressMap.getOrDefault("street", ""));
                    address.setNumber(addressMap.getOrDefault("number", ""));
                    address.setComplement(addressMap.getOrDefault("complement", ""));
                    address.setUser(appUser);
                    appUser.setAddress(address);
                    log.info("Endereço recuperado do Auth0 para usuário {}: {}", auth0User.getId(), address);
                } catch (Exception e) {
                    log.warn("Falha ao desserializar endereço: {}", addressObj, e);
                }
            }
        }

        return appUser;
    }

    public List<AppUser> listUsers() {
        try {
            List<AppUser> appUsers = userRepository.findAll();
            log.info("Listando {} usuários do banco local", appUsers.size());
            return appUsers;
        } catch (Exception e) {
            log.error("Falha ao listar usuários do banco local", e);
            throw new RuntimeException(messageSource.getMessage("error.generic", new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }
    }

    @Async
    @Scheduled(fixedRate = 5 * 60 * 1000)
    public CompletableFuture<Void> syncAllUsersFromAuth0Async() {
        try {
            log.info("Iniciando sincronização periódica com o Auth0");
            UsersPage page = managementAPI.users().list(null).execute().getBody();
            List<com.auth0.json.mgmt.users.User> auth0Users = page.getItems();

            List<AppUser> domainAppUsers = auth0Users.stream()
                    .map(this::convertAuth0User)
                    .collect(Collectors.toList());

            List<AppUser> existingUsers = userRepository.findAll();
            Map<String, AppUser> existingUsersMap = existingUsers.stream()
                    .collect(Collectors.toMap(AppUser::getId, user -> user));

            List<AppUser> usersToSave = new ArrayList<>();
            for (AppUser auth0User : domainAppUsers) {
                AppUser existingUser = existingUsersMap.get(auth0User.getId());
                if (existingUser == null || !existingUser.equals(auth0User)) {
                    usersToSave.add(auth0User);
                }
            }

            if (!usersToSave.isEmpty()) {
                userRepository.saveAll(usersToSave);
                log.info("Sincronizados {} usuários do Auth0 com sucesso", usersToSave.size());
            } else {
                log.info("Nenhum usuário novo ou alterado para sincronizar do Auth0");
            }

        } catch (Auth0Exception e) {
            log.error("Falha ao sincronizar todos os usuários do Auth0", e);
        }

        return CompletableFuture.completedFuture(null);
    }

    @Transactional
    public void syncUserFromAuth0(String userId) {
        if (userId == null || userId.trim().isEmpty() || !userId.matches("^(auth0|google-oauth2)\\|.+")) {
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
                List<Permission> permissionItems = managementAPI.roles().listPermissions(role.getId(), null)
                        .execute().getBody().getItems();
                permissions.addAll(permissionItems.stream()
                        .map(Permission::getName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()));
            }

            return new ArrayList<>(permissions);
        } catch (Auth0Exception e) {
            log.error("Falha ao listar permissões do Auth0", e);
            throw new RuntimeException(messageSource.getMessage("error.auth0.permissions.failed", new Object[]{e.getMessage()}, Locale.getDefault()), e);
        }
    }
}