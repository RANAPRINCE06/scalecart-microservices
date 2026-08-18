package com.nahid.order.entity;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingAddress {

    @Column(name = "shipping_first_name", nullable = false)
    String firstName;

    @Column(name = "shipping_last_name", nullable = false)
    String lastName;

    @Column(name = "shipping_street_address", nullable = false)
    String streetAddress;

    @Column(name = "shipping_city", nullable = false)
    String city;

    @Column(name = "shipping_state")
    String state;

    @Column(name = "shipping_postal_code", nullable = false)
    String postalCode;

    @Column(name = "shipping_country", nullable = false)
    String country;

    @Column(name = "shipping_phone")
    String phone;
}