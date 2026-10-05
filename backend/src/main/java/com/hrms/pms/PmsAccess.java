package com.hrms.pms;

import com.hrms.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

/** Resolves the signed-in user, their role and linked employee for PMS scoping. */
@Component
public class PmsAccess {

    private static final Set<String> MANAGE_ROLES = Set.of("ROLE_SUPER_ADMIN", "ROLE_HR_ADMIN", "ROLE_HR_MANAGER");

    private final JdbcTemplate jdbcTemplate;

    public PmsAccess(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String username() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
        return authentication.getName();
    }

    public boolean canManage() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (MANAGE_ROLES.contains(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }

    /** True when the login holds no HR/admin role, so it may only see and update its own goals. */
    public boolean employeeOnly() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return true;
        }
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            if (MANAGE_ROLES.contains(authority.getAuthority()) || "ROLE_HR_EXECUTIVE".equals(authority.getAuthority())) {
                return false;
            }
        }
        return true;
    }

    /** The employee linked to the signed-in login, or null when the login is not linked. */
    public Long employeeIdOrNull() {
        List<Long> ids = jdbcTemplate.query(
                "SELECT employee_id FROM app_user WHERE username = ? AND employee_id IS NOT NULL",
                (rs, rowNum) -> rs.getLong(1), username());
        return ids.isEmpty() ? null : ids.get(0);
    }

    public long requireEmployeeId() {
        Long id = employeeIdOrNull();
        if (id == null) {
            throw new ResourceNotFoundException("The signed-in user is not linked to an employee record");
        }
        return id;
    }
}
