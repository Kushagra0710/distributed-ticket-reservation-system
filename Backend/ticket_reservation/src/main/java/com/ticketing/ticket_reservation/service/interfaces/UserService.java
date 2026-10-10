package com.ticketing.ticket_reservation.service.interfaces;

import java.util.List;

import com.ticketing.ticket_reservation.dto.AdminList;
import com.ticketing.ticket_reservation.dto.AdminUserRequest;
import com.ticketing.ticket_reservation.dto.AdminUserResponse;
import com.ticketing.ticket_reservation.entity.StatusToggle;

public interface UserService {
    AdminUserResponse registerAdmin(AdminUserRequest request);
    List<AdminList> getAdminList();
    List<AdminList> getAdminsExcept(String loggedInEmail);
    List<AdminList> getOwners();
    AdminUserResponse editUser(Long id, AdminUserRequest request );
    AdminUserResponse toggleStatus(Long id, StatusToggle status);
}
