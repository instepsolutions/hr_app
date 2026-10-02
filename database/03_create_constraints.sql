-- =============================================================
-- Oracle HRMS constraints
-- =============================================================

ALTER TABLE department ADD (
    CONSTRAINT uk_department_code UNIQUE (department_code),
    CONSTRAINT chk_department_status CHECK (status IN ('ACTIVE','INACTIVE')),
    CONSTRAINT fk_department_parent FOREIGN KEY (parent_department_id) REFERENCES department(department_id),
    CONSTRAINT fk_department_head_employee FOREIGN KEY (head_employee_id) REFERENCES employee(employee_id)
);

ALTER TABLE location ADD (
    CONSTRAINT uk_location_name UNIQUE (location_name, city),
    CONSTRAINT chk_location_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

ALTER TABLE designation ADD (
    CONSTRAINT uk_designation_name UNIQUE (designation_name),
    CONSTRAINT chk_designation_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

ALTER TABLE employment_type ADD (
    CONSTRAINT uk_employment_type_name UNIQUE (employment_type_name),
    CONSTRAINT chk_employment_type_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

ALTER TABLE employee ADD (
    CONSTRAINT uk_employee_code UNIQUE (employee_code),
    CONSTRAINT uk_employee_official_email UNIQUE (official_email),
    CONSTRAINT uk_employee_personal_email UNIQUE (personal_email),
    CONSTRAINT chk_employee_gender CHECK (gender IN ('MALE','FEMALE','OTHER','PREFER_NOT_TO_SAY')),
    CONSTRAINT chk_employee_status CHECK (employee_status IN ('ACTIVE','ON_LEAVE','PROBATION','NOTICE_PERIOD','RESIGNED','EXITED','INACTIVE')),
    CONSTRAINT fk_employee_department FOREIGN KEY (department_id) REFERENCES department(department_id),
    CONSTRAINT fk_employee_designation FOREIGN KEY (designation_id) REFERENCES designation(designation_id),
    CONSTRAINT fk_employee_location FOREIGN KEY (location_id) REFERENCES location(location_id),
    CONSTRAINT fk_employee_employment_type FOREIGN KEY (employment_type_id) REFERENCES employment_type(employment_type_id),
    CONSTRAINT fk_employee_reporting_manager FOREIGN KEY (reporting_manager_id) REFERENCES employee(employee_id)
);

ALTER TABLE employee_profile ADD (
    CONSTRAINT uk_employee_profile UNIQUE (employee_id),
    CONSTRAINT fk_employee_profile_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

ALTER TABLE employee_status_history ADD (
    CONSTRAINT chk_status_history_new_status CHECK (new_status IN ('ACTIVE','ON_LEAVE','PROBATION','NOTICE_PERIOD','RESIGNED','EXITED','INACTIVE')),
    CONSTRAINT fk_employee_status_history_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

ALTER TABLE employee_lifecycle_event ADD (
    CONSTRAINT chk_lifecycle_event_type CHECK (event_type IN ('RECRUITMENT','OFFER','JOINING','ONBOARDING','PROBATION','CONFIRMATION','PROMOTION','TRANSFER','TRAINING','PERFORMANCE','REWARD','NOTICE_PERIOD','EXIT','EXIT_INTERVIEW','FULL_AND_FINAL')),
    CONSTRAINT fk_employee_lifecycle_event_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

ALTER TABLE bulk_action ADD (
    CONSTRAINT chk_bulk_action_status CHECK (status IN ('PENDING','IN_PROGRESS','COMPLETED','FAILED','CANCELLED'))
);

ALTER TABLE bulk_action_employee ADD (
    CONSTRAINT chk_bulk_action_employee_status CHECK (status IN ('PENDING','SUCCESS','FAILED')),
    CONSTRAINT fk_bulk_action_employee_bulk_action FOREIGN KEY (bulk_action_id) REFERENCES bulk_action(bulk_action_id),
    CONSTRAINT fk_bulk_action_employee_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

ALTER TABLE app_user ADD (
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT chk_app_user_role CHECK (role IN ('SUPER_ADMIN','HR_ADMIN','HR_MANAGER','HR_EXECUTIVE','EMPLOYEE')),
    CONSTRAINT chk_app_user_active CHECK (is_active IN (0,1))
);

COMMIT;
PROMPT HRMS constraints created successfully.
