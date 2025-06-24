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
    private final ObjectMapper objectMapper;

    @Transactional(rollbackOn = Auth0Exception.class)
    public AppUser updateUser(String userId, UserUpdateDTO dto) {
        try {
            AppUser appUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            appUser.setName(dto.getName());
            appUser.setEmail(dto.getEmail());
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
            auth0User.setName(dto.getName());
            auth0User.setEmail(dto.getEmail());

            Map<String, Object> appMetadata = new HashMap<>();
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
            throw new RuntimeException("Erro ao atualizar usuário no Auth0: " + e.getMessage(), e);
        }
    }

    @Transactional
    public AppUser inactivateUser(String userId) {
        try {
            AppUser appUser = userRepository.findById(userId)
                    .orElseThrow(() -> new UserNotFoundException(userId));

            appUser.setStatus(UserStatus.INATIVO);

            log.info("Inativando usuário com ID: {}", userId);
            userRepository.save(appUser);

            User auth0User = new User();
            Map<String, Object> appMetadata = new HashMap<>();
            appMetadata.put("status", UserStatus.INATIVO.getCode());
            auth0User.setAppMetadata(appMetadata);

            managementAPI.users().update(userId, auth0User).execute();

            return appUser;
        } catch (Auth0Exception e) {
            log.error("Falha ao inativar usuário no Auth0 com ID: {}", userId, e);
            throw new RuntimeException("Erro ao inativar usuário no Auth0: " + e.getMessage(), e);
        }
    }

    private AppUser convertAuth0User(com.auth0.json.mgmt.users.User auth0User) {
        AppUser appUser = new AppUser();
        appUser.setId(auth0User.getId());
        appUser.setEmail(auth0User.getEmail() != null ? auth0User.getEmail() : "");
        appUser.setName(auth0User.getName());
        appUser.setStatus(UserStatus.ATIVO); // Valor padrão

        // Buscar roles do usuário
        try {
            List<Role> roles = managementAPI.users().listRoles(auth0User.getId(), null).execute().getBody().getItems();
            appUser.setRoles(roles.stream().map(Role::getName).collect(Collectors.toList()));

            // Buscar permissões associadas aos roles
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
            // Buscar usuários do Auth0
            UsersPage page = managementAPI.users().list(null).execute().getBody();
            List<com.auth0.json.mgmt.users.User> auth0Users = page.getItems();
            long auth0UserCount = auth0Users.size();
            long localUserCount = userRepository.count();

            // Comparar contagem de usuários
            if (auth0UserCount != localUserCount) {
                log.info("Diferença detectada: {} usuários no Auth0, {} no banco local. Sincronizando...", auth0UserCount, localUserCount);
                syncAllUsersFromAuth0Async().join();
            }

            // Retornar lista do banco local
            List<AppUser> appUsers = userRepository.findAll();
            log.info("Listando {} usuários do banco local", appUsers.size());
            return appUsers;
        } catch (Auth0Exception e) {
            log.error("Falha ao listar usuários do Auth0", e);
            throw new RuntimeException("Erro ao listar usuários do Auth0: " + e.getMessage(), e);
        }
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