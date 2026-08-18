package com.nahid.payment.util.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiResponseConstant {
    public static final String FETCH_SUCCESSFUL = "%s fetched successfully";
    public static final String FETCH_ALL_SUCCESSFUL = "All %s retrieved successfully";
    public static final String CREATE_SUCCESSFUL = "%s created successfully";
    public static final String UPDATE_SUCCESSFUL = "%s updated successfully";
    public static final String DELETE_SUCCESSFUL = "%s deleted successfully";
    public static final String STATUS_UPDATE_SUCCESSFUL = "%s status updated to %s successfully";
    public static final String ACTION_SUCCESSFUL = "%s %s successfully";
    public static final String EVENT_PUBLISHED = "%s event published successfully";
}
