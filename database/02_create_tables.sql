-- =============================================================
-- Oracle HRMS table creation
-- =============================================================

CREATE TABLE department (
    department_id NUMBER(10) NOT NULL,
    department_name VARCHAR2(120) NOT NULL,
    department_code VARCHAR2(30) NOT NULL,
    description VARCHAR2(500),
    head_employee_id NUMBER(10),
    parent_department_id NUMBER(10),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_department PRIMARY KEY (department_id)
);

CREATE TABLE location (
    location_id NUMBER(10) NOT NULL,
    location_name VARCHAR2(150) NOT NULL,
    city VARCHAR2(100) NOT NULL,
    state VARCHAR2(100),
    country VARCHAR2(100) DEFAULT 'India',
    postal_code VARCHAR2(20),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_location PRIMARY KEY (location_id)
);

CREATE TABLE designation (
    designation_id NUMBER(10) NOT NULL,
    designation_name VARCHAR2(120) NOT NULL,
    designation_code VARCHAR2(30),
    grade VARCHAR2(50),
    level_no NUMBER(3),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_designation PRIMARY KEY (designation_id)
);

CREATE TABLE employment_type (
    employment_type_id NUMBER(10) NOT NULL,
    employment_type_name VARCHAR2(80) NOT NULL,
    description VARCHAR2(250),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_employment_type PRIMARY KEY (employment_type_id)
);

CREATE TABLE employee (
    employee_id NUMBER(10) NOT NULL,
    employee_code VARCHAR2(30) NOT NULL,
    first_name VARCHAR2(100) NOT NULL,
    middle_name VARCHAR2(100),
    last_name VARCHAR2(100) NOT NULL,
    display_name VARCHAR2(150) NOT NULL,
    gender VARCHAR2(20),
    date_of_birth DATE,
    personal_email VARCHAR2(150),
    official_email VARCHAR2(150) NOT NULL,
    mobile_number VARCHAR2(30),
    alternate_mobile VARCHAR2(30),
    department_id NUMBER(10),
    designation_id NUMBER(10),
    location_id NUMBER(10),
    employment_type_id NUMBER(10),
    reporting_manager_id NUMBER(10),
    date_of_joining DATE,
    confirmation_date DATE,
    date_of_exit DATE,
    employee_status VARCHAR2(30) DEFAULT 'ACTIVE' NOT NULL,
    profile_photo_url VARCHAR2(500),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_employee PRIMARY KEY (employee_id)
);

CREATE TABLE employee_profile (
    profile_id NUMBER(10) NOT NULL,
    employee_id NUMBER(10) NOT NULL,
    blood_group VARCHAR2(10),
    marital_status VARCHAR2(30),
    nationality VARCHAR2(80),
    aadhaar_last_four VARCHAR2(10),
    pan_last_four VARCHAR2(10),
    emergency_contact_name VARCHAR2(120),
    emergency_contact_number VARCHAR2(30),
    emergency_contact_relation VARCHAR2(50),
    current_address VARCHAR2(500),
    permanent_address VARCHAR2(500),
    city VARCHAR2(100),
    state VARCHAR2(100),
    postal_code VARCHAR2(20),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_employee_profile PRIMARY KEY (profile_id)
);

CREATE TABLE employee_status_history (
    history_id NUMBER(10) NOT NULL,
    employee_id NUMBER(10) NOT NULL,
    old_status VARCHAR2(30),
    new_status VARCHAR2(30) NOT NULL,
    effective_date DATE NOT NULL,
    reason VARCHAR2(200),
    remarks VARCHAR2(500),
    changed_by VARCHAR2(100),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_employee_status_history PRIMARY KEY (history_id)
);

CREATE TABLE employee_lifecycle_event (
    event_id NUMBER(10) NOT NULL,
    employee_id NUMBER(10) NOT NULL,
    event_type VARCHAR2(50) NOT NULL,
    event_date DATE NOT NULL,
    description VARCHAR2(500),
    reference_id VARCHAR2(100),
    created_by VARCHAR2(100),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_employee_lifecycle_event PRIMARY KEY (event_id)
);

CREATE TABLE bulk_action (
    bulk_action_id NUMBER(10) NOT NULL,
    action_type VARCHAR2(80) NOT NULL,
    requested_by VARCHAR2(100),
    requested_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    total_employees NUMBER(10) DEFAULT 0,
    successful_count NUMBER(10) DEFAULT 0,
    failed_count NUMBER(10) DEFAULT 0,
    status VARCHAR2(30) DEFAULT 'PENDING' NOT NULL,
    remarks VARCHAR2(500),
    CONSTRAINT pk_bulk_action PRIMARY KEY (bulk_action_id)
);

CREATE TABLE bulk_action_employee (
    bulk_action_employee_id NUMBER(10) NOT NULL,
    bulk_action_id NUMBER(10) NOT NULL,
    employee_id NUMBER(10) NOT NULL,
    old_value VARCHAR2(200),
    new_value VARCHAR2(200),
    status VARCHAR2(30) DEFAULT 'PENDING' NOT NULL,
    error_message VARCHAR2(500),
    processed_at TIMESTAMP,
    CONSTRAINT pk_bulk_action_employee PRIMARY KEY (bulk_action_employee_id)
);

CREATE TABLE audit_log (
    audit_id NUMBER(10) NOT NULL,
    entity_name VARCHAR2(100) NOT NULL,
    entity_id NUMBER(20),
    action_type VARCHAR2(50) NOT NULL,
    old_data CLOB,
    new_data CLOB,
    performed_by VARCHAR2(100),
    performed_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    ip_address VARCHAR2(50),
    CONSTRAINT pk_audit_log PRIMARY KEY (audit_id)
);

CREATE TABLE app_user (
    user_id NUMBER(10) NOT NULL,
    username VARCHAR2(60) NOT NULL,
    password_hash VARCHAR2(255) NOT NULL,
    full_name VARCHAR2(150) NOT NULL,
    role VARCHAR2(50) NOT NULL,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    last_login_at TIMESTAMP,
    created_by VARCHAR2(100),
    CONSTRAINT pk_app_user PRIMARY KEY (user_id)
);

COMMIT;
PROMPT HRMS base tables created successfully.
