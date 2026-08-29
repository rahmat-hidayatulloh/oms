package com.oms.admin.service;

import com.oms.admin.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface for admin user management operations.
 */
public interface AdminUserManagementServiceInterface {

    Page<UserResponse> getAllUsers(Pageable pageable);
}
