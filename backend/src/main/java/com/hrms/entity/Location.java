package com.hrms.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "LOCATION")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "location_seq")
    @SequenceGenerator(name = "location_seq", sequenceName = "LOCATION_SEQ", allocationSize = 1)
    @Column(name = "LOCATION_ID")
    private Long locationId;

    @Column(name = "LOCATION_NAME", nullable = false, length = 150)
    private String locationName;

    @Column(name = "CITY", nullable = false, length = 100)
    private String city;

    @Column(name = "STATE", length = 100)
    private String state;

    @Column(name = "COUNTRY", length = 100)
    private String country = "India";

    @Column(name = "POSTAL_CODE", length = 20)
    private String postalCode;

    @Column(name = "STATUS", nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(name = "CREATED_AT", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "CREATED_BY", length = 100)
    private String createdBy;

    @Column(name = "UPDATED_BY", length = 100)
    private String updatedBy;
}
