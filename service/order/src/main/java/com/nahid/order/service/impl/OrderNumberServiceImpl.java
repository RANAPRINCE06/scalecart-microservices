package com.nahid.order.service.impl;

import com.nahid.order.service.OrderNumberService;
import org.springframework.stereotype.Service;

@Service
public class OrderNumberServiceImpl implements OrderNumberService {

    @Override
    public String generateOrderNumber() {
        String prefix = "ORD";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + "-" + timestamp;
    }
}

