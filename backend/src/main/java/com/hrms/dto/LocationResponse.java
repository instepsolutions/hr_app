package com.hrms.dto;

import lombok.Data;

@Data
public class LocationResponse {
    private Long locationId;
    private String locationName;
    private String city;
    private String state;
    private String country;
    private String postalCode;
    private String status;
}
