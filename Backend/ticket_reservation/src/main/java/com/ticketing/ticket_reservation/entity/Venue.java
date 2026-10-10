package com.ticketing.ticket_reservation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "venues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name; // e.g., "AMC Empire Screen 1"

    @NotBlank
    @Column(nullable = false)
    private String address; // e.g., "New York, NY"

    @Column(name = "pincode")
    private Integer pincode;

    @NotBlank
    @Column(nullable = false)
    private String city;
    
    @NotBlank
    @Column(nullable = false)
    private String state;

    @NotBlank
    @Column(nullable = false)
    private String venueType;

    @NotNull
    @Column(nullable = false)
    @Default
    private Boolean deleteFlag = false;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @JsonProperty("ownerId")
    public Long getOwnerId() {
        return owner == null ? null : owner.getId();
    }

    @JsonProperty("ownerName")
    public String getOwnerName() {
        return owner == null ? null : owner.getFullName();
    }
}