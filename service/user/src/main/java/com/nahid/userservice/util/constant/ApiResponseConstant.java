package com.nahid.userservice.util.constant;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponseConstant {

    public static final String FETCH_SUCCESSFUL = "%s fetched successfully";
    public static final String CREATE_SUCCESSFUL = "%s created successfully";
    public static final String UPDATE_SUCCESSFUL = "%s updated successfully";
    public static final String DELETE_SUCCESSFUL = "%s deleted successfully";
    public static final String FETCH_ALL_SUCCESSFUL = "All %s retrieved successfully";
    public static final String STATUS_UPDATE_SUCCESSFUL = "%s status updated to %s successfully";

    public static final String SEARCH_RESULTS_FETCHED = "Search results fetched successfully";
    public static final String STATUS_FILTERED_RESULTS = "%s with status %s fetched successfully";

    public static final String USER_REGISTERED_SUCCESSFULLY = "User registered successfully";
    public static final String LOGIN_SUCCESSFUL = "Login successful";
    public static final String TOKEN_REFRESHED_SUCCESSFULLY = "Token refreshed successfully";
    public static final String LOGOUT_SUCCESSFUL = "Logout successful";
    public static final String USER_PROFILE_FETCHED = "User profile fetched successfully";

    public static final String CUSTOMER_VERIFICATION_SUCCESS = "Customer verified successfully";
    public static final String CUSTOMER_PROFILE_COMPLETE = "Customer profile is complete";
    public static final String CUSTOMER_ADDRESS_OPERATION = "Address %s successfully"; // Accepts: added, updated, deleted
}
