package com.mcloud.auth0_authenticator.application.mapper;

import com.mcloud.auth0_authenticator.application.dto.AddressDTO;
import com.mcloud.auth0_authenticator.application.dto.UserListItemDTO;
import com.mcloud.auth0_authenticator.application.dto.UserResponseDTO;
import com.mcloud.auth0_authenticator.application.dto.UserUpdateDTO;
import com.mcloud.auth0_authenticator.domain.user.AppUser;
import com.mcloud.auth0_authenticator.domain.user.Address;

public final class UserMapper {

    private UserMapper() {}

    public static UserResponseDTO toResponse(AppUser u) {
        if (u == null) return null;
        return UserResponseDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .status(u.getStatus())
                .type(u.getType())
                .details(u.getDetails())
                .phone(u.getPhone())
                .secondaryPhone(u.getSecondaryPhone())
                .roles(u.getRoles())
                .permissions(u.getPermissions())
                .address(toAddressDTO(u.getAddress()))
                .build();
    }

    public static AddressDTO toAddressDTO(Address a) {
        if (a == null) return null;
        AddressDTO dto = new AddressDTO();
        dto.setId(a.getId());
        dto.setZipCode(a.getZipCode());
        dto.setState(a.getState());
        dto.setCity(a.getCity());
        dto.setNeighborhood(a.getNeighborhood());
        dto.setStreet(a.getStreet());
        dto.setNumber(a.getNumber());
        dto.setComplement(a.getComplement());
        return dto;
    }

    // Aplica UPDATE do DTO na entidade existente
    public static void applyUpdate(AppUser entity, UserUpdateDTO dto) {
        entity.setName(dto.getName());
        entity.setEmail(dto.getEmail());
        entity.setType(dto.getType());
        entity.setDetails(dto.getDetails());
        entity.setPhone(dto.getPhone());
        entity.setSecondaryPhone(dto.getSecondaryPhone());
        entity.setStatus(dto.getStatus());
        entity.setRoles(dto.getRoles());
        entity.setPermissions(dto.getPermissions());

        if (dto.getAddress() != null) {
            if (entity.getAddress() == null) {
                entity.setAddress(new Address());
            }
            applyAddress(entity.getAddress(), dto.getAddress());
        }
    }

    private static void applyAddress(Address target, AddressDTO src) {
        target.setZipCode(src.getZipCode());
        target.setState(src.getState());
        target.setCity(src.getCity());
        target.setNeighborhood(src.getNeighborhood());
        target.setStreet(src.getStreet());
        target.setNumber(src.getNumber());
        target.setComplement(src.getComplement());
    }

    public static UserListItemDTO toListItem(AppUser u) {
        if (u == null) return null;
        return UserListItemDTO.builder()
                .id(u.getId())
                .email(u.getEmail())
                .name(u.getName())
                .status(u.getStatus())
                .type(u.getType())
                .phone(u.getPhone())
                .build();
    }


}
