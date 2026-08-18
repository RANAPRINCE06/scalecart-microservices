package com.nahid.userservice.mapper;

import com.nahid.userservice.dto.response.UserResponse;
import com.nahid.userservice.dto.response.UserPublicResponse;
import com.nahid.userservice.dto.response.UserAddressResponse;
import com.nahid.userservice.entity.User;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class UserMapper {

    public UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }

    public UserPublicResponse toUserPublicResponse(User user) {
        List<UserAddressResponse> addressResponses = user.getAddresses() != null ?
            user.getAddresses().stream()
                .map(address -> UserAddressResponse.builder()
                    .id(address.getId())
                    .street(address.getStreet())
                    .city(address.getCity())
                    .state(address.getState())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .isDefault(address.isDefault())
                    .build())
                .toList() : List.of();

        return UserPublicResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .addresses(addressResponses)
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
