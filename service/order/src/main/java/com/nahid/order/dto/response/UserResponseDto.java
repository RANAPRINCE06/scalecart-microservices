package com.nahid.order.dto.response;

import com.nahid.order.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;
    private List<UserAddressResponse> addresses;
    private UserStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}