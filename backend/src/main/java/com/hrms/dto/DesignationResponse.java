package com.hrms.dto;

import lombok.Data;

@Data
public class DesignationResponse {
    private Long designationId;
    private String designationName;
    private String designationCode;
    private String grade;
    private Integer levelNo;
    private String status;
}
