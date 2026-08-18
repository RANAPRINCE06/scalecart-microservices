package com.nahid.order.service.impl;

import com.nahid.order.client.UserClient;
import com.nahid.order.dto.response.ApiResponse;
import com.nahid.order.dto.response.UserResponseDto;
import com.nahid.order.exception.OrderProcessingException;
import com.nahid.order.exception.UserValidationException;
import com.nahid.order.service.UserValidationService;
import com.nahid.order.util.constant.ExceptionMessageConstant;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserValidationServiceImpl implements UserValidationService {

    private final UserClient userClient;

    @Override
    public void validateUserForOrder(Long userId) {
        try {
            ResponseEntity<ApiResponse<UserResponseDto>> userResponseDto = userClient.getUserById(userId);
            UserResponseDto user = Optional.ofNullable(userResponseDto)
                    .filter(response -> response.getStatusCode().is2xxSuccessful())
                    .map(ResponseEntity::getBody)
                    .filter(ApiResponse::isSuccess)
                    .map(ApiResponse::getData)
                    .orElseThrow(() -> new OrderProcessingException(String.format(ExceptionMessageConstant.USER_NOT_FOUND, userId)));
            validateUserStatus(user, userId);

        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException(String.format(ExceptionMessageConstant.USER_NOT_FOUND, userId));

        } catch (OrderProcessingException e) {
            throw new OrderProcessingException(e.getMessage());

        } catch (Exception e) {
            throw new UserValidationException(String.format(ExceptionMessageConstant.USER_VALIDATION_FAILED, userId), e);
        }
    }
    private void validateUserStatus(UserResponseDto user, Long userId) {
        if (user.getStatus() == null) {
            throw new OrderProcessingException(String.format(ExceptionMessageConstant.USER_VALIDATION_FAILED, userId));
        }

        switch (user.getStatus()) {
            case SUSPENDED:
                throw new OrderProcessingException(ExceptionMessageConstant.USER_SUSPENDED);
            case INACTIVE:
                throw new OrderProcessingException(ExceptionMessageConstant.USER_INACTIVE);
            case BLOCKED:
                throw new OrderProcessingException(ExceptionMessageConstant.USER_BLOCKED);
            case ACTIVE:
                break;
            default:
                throw new OrderProcessingException(String.format(ExceptionMessageConstant.USER_VALIDATION_FAILED, userId));
        }
    }
}
