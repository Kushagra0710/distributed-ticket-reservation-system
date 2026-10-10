package com.ticketing.ticket_reservation.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ticketing.ticket_reservation.constants.ApplicationConstants;
import com.ticketing.ticket_reservation.dto.AdminList;
import com.ticketing.ticket_reservation.dto.AdminUserRequest;
import com.ticketing.ticket_reservation.dto.AdminUserResponse;
import com.ticketing.ticket_reservation.entity.StatusToggle;
import com.ticketing.ticket_reservation.service.interfaces.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping (ApplicationConstants.ADMIN_URL)
@RequiredArgsConstructor 
public class UserController {
    private final UserService userService;
    
    @PostMapping(ApplicationConstants.ADMIN_USER)
    public ResponseEntity<?> addAdmin(@Valid @RequestBody AdminUserRequest request){
            AdminUserResponse response = userService.registerAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);  
    }

    @GetMapping(ApplicationConstants.ADMIN_USER)
    public ResponseEntity<?> getAllAdmin(){
        List<AdminList> addAdmin=userService.getAdminList();
        return ResponseEntity.status(HttpStatus.CREATED).body(addAdmin);
    }

    @GetMapping(ApplicationConstants.ADMIN_USER + "/except-current")
    public ResponseEntity<List<AdminList>> getLoggedUser(Authentication authentication){
        String loggedInEmail = authentication.getName();
        return ResponseEntity.ok(userService.getAdminsExcept(loggedInEmail));
    }

    @GetMapping(ApplicationConstants.ADMIN_USER + "/owners")
    public ResponseEntity<List<AdminList>> getOwners() {
        return ResponseEntity.ok(userService.getOwners());
    }

    @PutMapping (ApplicationConstants.ADMIN_USER + "/{id}")
    public ResponseEntity<?> editUser(@Valid @PathVariable Long id, @RequestBody AdminUserRequest request){
        AdminUserResponse response = userService.editUser(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping (ApplicationConstants.ADMIN_USER + "/{id}" + ApplicationConstants.STATUS)
    public ResponseEntity<AdminUserResponse> toggleStatus(@Valid @PathVariable Long id, @RequestBody StatusToggle status){
        AdminUserResponse response = userService.toggleStatus(id,status); 
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
