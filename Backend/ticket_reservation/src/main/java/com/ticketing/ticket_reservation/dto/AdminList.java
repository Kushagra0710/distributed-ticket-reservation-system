package com.ticketing.ticket_reservation.dto;

import org.springframework.beans.factory.annotation.Value;
import com.ticketing.ticket_reservation.entity.Role;

public interface AdminList {

    Long getId();
    String getFullName();
    String getEmail();
    String getPhoneNumber();
    Role getRole();

    @Value("#{target.active}")
    boolean getIsActive();
}
