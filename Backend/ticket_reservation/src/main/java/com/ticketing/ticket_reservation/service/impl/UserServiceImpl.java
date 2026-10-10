package com.ticketing.ticket_reservation.service.impl;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.ticketing.ticket_reservation.dto.AdminList;
import com.ticketing.ticket_reservation.dto.AdminUserRequest;
import com.ticketing.ticket_reservation.dto.AdminUserResponse;
import com.ticketing.ticket_reservation.entity.AuthProvider;
import com.ticketing.ticket_reservation.entity.Role;
import com.ticketing.ticket_reservation.entity.StatusToggle;
import com.ticketing.ticket_reservation.entity.User;
import com.ticketing.ticket_reservation.exception.EmailAlreadyRegisteredException;
import com.ticketing.ticket_reservation.repository.UserRepository;
import com.ticketing.ticket_reservation.service.interfaces.UserService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class UserServiceImpl implements UserService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SPECIAL_CHARACTERS = "!@#$%^&*()-_=+[]{};:,.?";
    private static final String ALL_CHARACTERS = UPPERCASE + LOWERCASE + DIGITS + SPECIAL_CHARACTERS;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepo;

    private String generateRandomPassword() {
        StringBuilder password = new StringBuilder(12);
        password.append(randomCharacter(UPPERCASE));
        password.append(randomCharacter(LOWERCASE));
        password.append(randomCharacter(DIGITS));
        password.append(randomCharacter(SPECIAL_CHARACTERS));

        while (password.length() < 12) {
            password.append(randomCharacter(ALL_CHARACTERS));
        }

        for (int index = password.length() - 1; index > 0; index--) {
            int swapIndex = SECURE_RANDOM.nextInt(index + 1);
            char current = password.charAt(index);
            password.setCharAt(index, password.charAt(swapIndex));
            password.setCharAt(swapIndex, current);
        }

        return password.toString();
    }

    private char randomCharacter(String characters) {
        return characters.charAt(SECURE_RANDOM.nextInt(characters.length()));
    }

    @Override
    public List<AdminList> getAdminList() {
        return userRepo.findAllByRole(Role.ADMIN);
    }

    @Override
    public AdminUserResponse registerAdmin(AdminUserRequest request) {
        String email = request.getEmail().trim();
        if (userRepo.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException("Email is already registered");
        }

        Role role = request.getRole() == null ? Role.ADMIN : request.getRole();
        if (role == Role.USER) {
            throw new IllegalArgumentException("Only ADMIN and OWNER accounts can be created here.");
        }
        String pass = generateRandomPassword();
        String password = passwordEncoder.encode(pass);
        User adminUser = User.builder()
            .fullName(request.getFullName().trim())
            .email(email)
            .password(password)
            .role(role)
            .provider(AuthProvider.LOCAL)
            .phoneNumber(request.getPhoneNumber().trim())
            .build();
        
        userRepo.save(adminUser);
        return new AdminUserResponse(true, role.name() + " account created successfully.");
    }


    @Override
    public List<AdminList> getAdminsExcept(String loggedInEmail) {
        return userRepo.findAllByRoleInAndEmailNot(List.of(Role.ADMIN, Role.OWNER), loggedInEmail.trim());
    }

    @Override
    public List<AdminList> getOwners() {
        return userRepo.findAllByRoleAndActiveTrueOrderByFullNameAsc(Role.OWNER);
    }

    @Override 
    public AdminUserResponse editUser(Long id, AdminUserRequest request){
            User adminUser = userRepo.findById(id)
                            .orElseThrow(()-> new IllegalArgumentException("Admin not found."));

            adminUser.setFullName(request.getFullName().trim());
            adminUser.setPhoneNumber(request.getPhoneNumber().trim());
        userRepo.save(adminUser);
        return new AdminUserResponse(true, "Admin info edited successfully.");
    }

    @Override 
    public AdminUserResponse toggleStatus(Long id, StatusToggle request){
        User user = userRepo.findById(id)
                    .orElseThrow(()-> new IllegalArgumentException("User not found"));

        if(user.isActive()==request.isActive())
            throw new IllegalArgumentException("Status is already set as " + request.isActive());

        user.setActive(request.isActive());
        userRepo.save(user);
        return new AdminUserResponse(true,"Admin status toggled successfully.");
    }
}
