package com.hrms.service;

import com.hrms.dto.EmployeeProfileRequest;
import com.hrms.dto.EmployeeProfileResponse;
import com.hrms.dto.EmployeeRequest;
import com.hrms.dto.EmployeeResponse;
import com.hrms.entity.Employee;
import com.hrms.entity.EmployeeProfile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class EmployeeMapper {

    public EmployeeResponse toResponse(Employee employee) {
        if (employee == null) return null;
        EmployeeResponse response = new EmployeeResponse();
        response.setEmployeeId(employee.getEmployeeId());
        response.setEmployeeCode(employee.getEmployeeCode());
        response.setFirstName(employee.getFirstName());
        response.setMiddleName(employee.getMiddleName());
        response.setLastName(employee.getLastName());
        response.setDisplayName(employee.getDisplayName());
        response.setGender(employee.getGender());
        response.setDateOfBirth(employee.getDateOfBirth());
        response.setPersonalEmail(employee.getPersonalEmail());
        response.setOfficialEmail(employee.getOfficialEmail());
        response.setMobileNumber(employee.getMobileNumber());
        response.setAlternateMobile(employee.getAlternateMobile());
        response.setDepartmentId(employee.getDepartmentId());
        response.setDesignationId(employee.getDesignationId());
        response.setLocationId(employee.getLocationId());
        response.setEmploymentTypeId(employee.getEmploymentTypeId());
        response.setReportingManagerId(employee.getReportingManagerId());
        response.setDateOfJoining(employee.getDateOfJoining());
        response.setConfirmationDate(employee.getConfirmationDate());
        response.setDateOfExit(employee.getDateOfExit());
        response.setEmployeeStatus(employee.getEmployeeStatus());
        response.setProfilePhotoUrl(employee.getProfilePhotoUrl());
        response.setCreatedAt(employee.getCreatedAt());
        response.setUpdatedAt(employee.getUpdatedAt());
        response.setCreatedBy(employee.getCreatedBy());
        response.setUpdatedBy(employee.getUpdatedBy());
        return response;
    }

    public Employee toEntity(EmployeeRequest request, Employee existing) {
        if (existing == null) {
            existing = new Employee();
            existing.setCreatedAt(LocalDateTime.now());
        }

        existing.setEmployeeCode(request.getEmployeeCode());
        existing.setFirstName(request.getFirstName());
        existing.setMiddleName(request.getMiddleName());
        existing.setLastName(request.getLastName());
        existing.setDisplayName(request.getFirstName() + " " + request.getLastName());
        existing.setGender(request.getGender());
        existing.setDateOfBirth(request.getDateOfBirth());
        existing.setPersonalEmail(request.getPersonalEmail());
        existing.setOfficialEmail(request.getOfficialEmail());
        existing.setMobileNumber(request.getMobileNumber());
        existing.setAlternateMobile(request.getAlternateMobile());
        existing.setDepartmentId(request.getDepartmentId());
        existing.setDesignationId(request.getDesignationId());
        existing.setLocationId(request.getLocationId());
        existing.setEmploymentTypeId(request.getEmploymentTypeId());
        existing.setReportingManagerId(request.getReportingManagerId());
        existing.setDateOfJoining(request.getDateOfJoining());
        existing.setConfirmationDate(request.getConfirmationDate());
        existing.setDateOfExit(request.getDateOfExit());
        existing.setEmployeeStatus(request.getEmployeeStatus() != null ? request.getEmployeeStatus() : "ACTIVE");
        existing.setProfilePhotoUrl(request.getProfilePhotoUrl());
        existing.setUpdatedAt(LocalDateTime.now());
        existing.setUpdatedBy("SYSTEM");
        return existing;
    }

    public EmployeeProfileResponse toProfileResponse(EmployeeProfile profile) {
        if (profile == null) return null;
        EmployeeProfileResponse response = new EmployeeProfileResponse();
        response.setProfileId(profile.getProfileId());
        response.setEmployeeId(profile.getEmployeeId());
        response.setBloodGroup(profile.getBloodGroup());
        response.setMaritalStatus(profile.getMaritalStatus());
        response.setNationality(profile.getNationality());
        response.setAadhaarLastFour(profile.getAadhaarLastFour());
        response.setPanLastFour(profile.getPanLastFour());
        response.setEmergencyContactName(profile.getEmergencyContactName());
        response.setEmergencyContactNumber(profile.getEmergencyContactNumber());
        response.setEmergencyContactRelation(profile.getEmergencyContactRelation());
        response.setCurrentAddress(profile.getCurrentAddress());
        response.setPermanentAddress(profile.getPermanentAddress());
        response.setCity(profile.getCity());
        response.setState(profile.getState());
        response.setPostalCode(profile.getPostalCode());
        response.setCreatedAt(profile.getCreatedAt());
        response.setUpdatedAt(profile.getUpdatedAt());
        return response;
    }

    public EmployeeProfile toProfileEntity(EmployeeProfileRequest request, EmployeeProfile existing) {
        if (existing == null) {
            existing = new EmployeeProfile();
            existing.setCreatedAt(LocalDateTime.now());
        }
        existing.setEmployeeId(request.getEmployeeId());
        existing.setBloodGroup(request.getBloodGroup());
        existing.setMaritalStatus(request.getMaritalStatus());
        existing.setNationality(request.getNationality());
        existing.setAadhaarLastFour(request.getAadhaarLastFour());
        existing.setPanLastFour(request.getPanLastFour());
        existing.setEmergencyContactName(request.getEmergencyContactName());
        existing.setEmergencyContactNumber(request.getEmergencyContactNumber());
        existing.setEmergencyContactRelation(request.getEmergencyContactRelation());
        existing.setCurrentAddress(request.getCurrentAddress());
        existing.setPermanentAddress(request.getPermanentAddress());
        existing.setCity(request.getCity());
        existing.setState(request.getState());
        existing.setPostalCode(request.getPostalCode());
        existing.setUpdatedAt(LocalDateTime.now());
        return existing;
    }
}
