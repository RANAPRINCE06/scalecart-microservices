package com.nahid.gateway.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class CustomAuthContextDto {
    private String userName;
    private List<String> role;

}
