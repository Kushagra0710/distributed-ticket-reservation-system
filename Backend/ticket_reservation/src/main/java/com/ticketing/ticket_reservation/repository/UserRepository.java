package com.ticketing.ticket_reservation.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ticketing.ticket_reservation.dto.AdminList;
import com.ticketing.ticket_reservation.entity.Role;
import com.ticketing.ticket_reservation.entity.User;

@Repository 
public interface UserRepository extends JpaRepository<User,Long>{
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    List<AdminList> findAllByRole(Role role);
    List<AdminList> findAllByRoleAndEmailNot(Role role, String email);
    List<AdminList> findAllByRoleInAndEmailNot(List<Role> roles, String email);
    List<AdminList> findAllByRoleAndActiveTrueOrderByFullNameAsc(Role role);
    Optional<User> findById(Long id);
}
