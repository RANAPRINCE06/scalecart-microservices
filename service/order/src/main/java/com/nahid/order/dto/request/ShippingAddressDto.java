package com.nahid.order.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShippingAddressDto {
    private String firstName;
    private String lastName;
    private String streetAddress;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phone;
}