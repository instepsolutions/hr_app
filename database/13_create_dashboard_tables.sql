-- Additive dashboard data tables. Existing Employee Management tables are unchanged.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

CREATE SEQUENCE dashboard_attendance_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE dashboard_approval_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE dashboard_announcement_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE dashboard_event_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE dashboard_attendance (
    attendance_id NUMBER(10) NOT NULL,
    employee_id NUMBER(10) NOT NULL,
    attendance_date DATE DEFAULT TRUNC(SYSDATE) NOT NULL,
    attendance_status VARCHAR2(20) NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_dashboard_attendance PRIMARY KEY (attendance_id),
    CONSTRAINT uk_dashboard_attendance_day UNIQUE (employee_id, attendance_date),
    CONSTRAINT chk_dashboard_attendance_status CHECK (attendance_status IN ('PRESENT','LATE','ABSENT')),
    CONSTRAINT fk_dashboard_attendance_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

CREATE TABLE dashboard_approval (
    approval_id NUMBER(10) NOT NULL,
    approval_type VARCHAR2(40) NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING' NOT NULL,
    employee_id NUMBER(10),
    requested_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    description VARCHAR2(300),
    CONSTRAINT pk_dashboard_approval PRIMARY KEY (approval_id),
    CONSTRAINT chk_dashboard_approval_type CHECK (approval_type IN ('LEAVE_REQUEST','EXPENSE_CLAIM','TIMESHEET','DOCUMENT','EXIT_CLEARANCE')),
    CONSTRAINT chk_dashboard_approval_status CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    CONSTRAINT fk_dashboard_approval_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id)
);

CREATE TABLE dashboard_announcement (
    announcement_id NUMBER(10) NOT NULL,
    title VARCHAR2(200) NOT NULL,
    summary VARCHAR2(1000) NOT NULL,
    published_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    is_new NUMBER(1) DEFAULT 0 NOT NULL,
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    CONSTRAINT pk_dashboard_announcement PRIMARY KEY (announcement_id),
    CONSTRAINT chk_dashboard_announcement_new CHECK (is_new IN (0,1)),
    CONSTRAINT chk_dashboard_announcement_active CHECK (is_active IN (0,1))
);

CREATE TABLE dashboard_event (
    event_id NUMBER(10) NOT NULL,
    event_name VARCHAR2(200) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    location VARCHAR2(200),
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_dashboard_event PRIMARY KEY (event_id),
    CONSTRAINT chk_dashboard_event_dates CHECK (end_date >= start_date),
    CONSTRAINT chk_dashboard_event_active CHECK (is_active IN (0,1))
);

CREATE INDEX idx_dashboard_attendance_day ON dashboard_attendance (attendance_date, attendance_status);
CREATE INDEX idx_dashboard_approval_pending ON dashboard_approval (status, approval_type);
CREATE INDEX idx_dashboard_announcement_feed ON dashboard_announcement (is_active, published_at);
CREATE INDEX idx_dashboard_event_dates ON dashboard_event (is_active, start_date, end_date);

COMMIT;
PROMPT Dashboard tables created successfully.
