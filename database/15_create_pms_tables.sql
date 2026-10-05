-- PMS tables. Additive only: employee/department/designation master data is reused, never copied.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

CREATE SEQUENCE pms_goal_category_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE pms_kra_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE pms_kpi_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE pms_goal_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE SEQUENCE pms_goal_progress_seq START WITH 1 INCREMENT BY 1 CACHE 1000 NOCYCLE;
CREATE SEQUENCE pms_goal_history_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE SEQUENCE pms_appraisal_cycle_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE SEQUENCE pms_appraisal_seq START WITH 1 INCREMENT BY 1 CACHE 1000 NOCYCLE;
CREATE SEQUENCE pms_pip_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;

CREATE TABLE pms_goal_category (
    category_id NUMBER(10) NOT NULL,
    category_name VARCHAR2(120) NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_goal_category PRIMARY KEY (category_id),
    CONSTRAINT uk_pms_goal_category_name UNIQUE (category_name),
    CONSTRAINT chk_pms_goal_category_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE pms_kra (
    kra_id NUMBER(10) NOT NULL,
    kra_name VARCHAR2(160) NOT NULL,
    category_id NUMBER(10),
    description VARCHAR2(500),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_kra PRIMARY KEY (kra_id),
    CONSTRAINT uk_pms_kra_name UNIQUE (kra_name),
    CONSTRAINT fk_pms_kra_category FOREIGN KEY (category_id) REFERENCES pms_goal_category(category_id),
    CONSTRAINT chk_pms_kra_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE pms_kpi (
    kpi_id NUMBER(10) NOT NULL,
    kra_id NUMBER(10) NOT NULL,
    kpi_name VARCHAR2(160) NOT NULL,
    unit VARCHAR2(30),
    target_value NUMBER(14,2),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_kpi PRIMARY KEY (kpi_id),
    CONSTRAINT fk_pms_kpi_kra FOREIGN KEY (kra_id) REFERENCES pms_kra(kra_id),
    CONSTRAINT chk_pms_kpi_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE pms_goal (
    goal_id NUMBER(10) NOT NULL,
    title VARCHAR2(200) NOT NULL,
    description VARCHAR2(2000),
    employee_id NUMBER(10) NOT NULL,
    department_id NUMBER(10),
    category_id NUMBER(10),
    goal_type VARCHAR2(20) NOT NULL,
    kra_id NUMBER(10),
    kpi_id NUMBER(10),
    weightage NUMBER(5,2) DEFAULT 0 NOT NULL,
    target_value NUMBER(14,2) NOT NULL,
    unit VARCHAR2(30) NOT NULL,
    progress_percent NUMBER(5,2) DEFAULT 0 NOT NULL,
    priority VARCHAR2(20) DEFAULT 'MEDIUM' NOT NULL,
    alignment_goal_id NUMBER(10),
    manager_id NUMBER(10),
    status VARCHAR2(20) DEFAULT 'NOT_STARTED' NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    completed_at TIMESTAMP,
    is_archived NUMBER(1) DEFAULT 0 NOT NULL,
    archived_at TIMESTAMP,
    archived_by VARCHAR2(100),
    archive_reason VARCHAR2(500),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_goal PRIMARY KEY (goal_id),
    CONSTRAINT fk_pms_goal_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_pms_goal_department FOREIGN KEY (department_id) REFERENCES department(department_id),
    CONSTRAINT fk_pms_goal_category FOREIGN KEY (category_id) REFERENCES pms_goal_category(category_id),
    CONSTRAINT fk_pms_goal_kra FOREIGN KEY (kra_id) REFERENCES pms_kra(kra_id),
    CONSTRAINT fk_pms_goal_kpi FOREIGN KEY (kpi_id) REFERENCES pms_kpi(kpi_id),
    CONSTRAINT fk_pms_goal_parent FOREIGN KEY (alignment_goal_id) REFERENCES pms_goal(goal_id) ON DELETE SET NULL,
    CONSTRAINT fk_pms_goal_manager FOREIGN KEY (manager_id) REFERENCES employee(employee_id),
    CONSTRAINT chk_pms_goal_type CHECK (goal_type IN ('COMPANY','DEPARTMENT','TEAM','INDIVIDUAL')),
    CONSTRAINT chk_pms_goal_status CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED','BEHIND')),
    CONSTRAINT chk_pms_goal_priority CHECK (priority IN ('LOW','MEDIUM','HIGH','CRITICAL')),
    CONSTRAINT chk_pms_goal_progress CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_pms_goal_weightage CHECK (weightage BETWEEN 0 AND 100),
    CONSTRAINT chk_pms_goal_dates CHECK (due_date >= start_date),
    CONSTRAINT chk_pms_goal_archived CHECK (is_archived IN (0,1))
);

CREATE TABLE pms_goal_progress (
    progress_id NUMBER(12) NOT NULL,
    goal_id NUMBER(10) NOT NULL,
    recorded_on DATE NOT NULL,
    progress_percent NUMBER(5,2) NOT NULL,
    status VARCHAR2(20) NOT NULL,
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_goal_progress PRIMARY KEY (progress_id),
    CONSTRAINT uk_pms_goal_progress_day UNIQUE (goal_id, recorded_on),
    CONSTRAINT fk_pms_goal_progress_goal FOREIGN KEY (goal_id) REFERENCES pms_goal(goal_id) ON DELETE CASCADE,
    CONSTRAINT chk_pms_goal_progress_status CHECK (status IN ('NOT_STARTED','IN_PROGRESS','COMPLETED','BEHIND'))
);

CREATE TABLE pms_goal_history (
    history_id NUMBER(12) NOT NULL,
    goal_id NUMBER(10) NOT NULL,
    action_type VARCHAR2(30) NOT NULL,
    old_value VARCHAR2(500),
    new_value VARCHAR2(500),
    changed_by VARCHAR2(100),
    changed_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_goal_history PRIMARY KEY (history_id),
    CONSTRAINT fk_pms_goal_history_goal FOREIGN KEY (goal_id) REFERENCES pms_goal(goal_id) ON DELETE CASCADE
);

CREATE TABLE pms_appraisal_cycle (
    cycle_id NUMBER(10) NOT NULL,
    cycle_name VARCHAR2(200) NOT NULL,
    dept_group VARCHAR2(60) NOT NULL,
    start_date DATE NOT NULL,
    due_date DATE NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_appraisal_cycle PRIMARY KEY (cycle_id),
    CONSTRAINT chk_pms_cycle_status CHECK (status IN ('DRAFT','ACTIVE','CLOSED')),
    CONSTRAINT chk_pms_cycle_dates CHECK (due_date >= start_date)
);

CREATE TABLE pms_appraisal (
    appraisal_id NUMBER(12) NOT NULL,
    cycle_id NUMBER(10) NOT NULL,
    employee_id NUMBER(10) NOT NULL,
    stage VARCHAR2(20) DEFAULT 'YET_TO_START' NOT NULL,
    final_rating NUMBER(3,2),
    completed_at DATE,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_appraisal PRIMARY KEY (appraisal_id),
    CONSTRAINT uk_pms_appraisal_employee UNIQUE (cycle_id, employee_id),
    CONSTRAINT fk_pms_appraisal_cycle FOREIGN KEY (cycle_id) REFERENCES pms_appraisal_cycle(cycle_id),
    CONSTRAINT fk_pms_appraisal_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT chk_pms_appraisal_stage CHECK (stage IN ('YET_TO_START','SELF_APPRAISAL','MANAGER_REVIEW','HR_REVIEW','COMPLETED')),
    CONSTRAINT chk_pms_appraisal_rating CHECK (final_rating IS NULL OR final_rating BETWEEN 1 AND 5)
);

CREATE TABLE pms_pip (
    pip_id NUMBER(10) NOT NULL,
    employee_id NUMBER(10) NOT NULL,
    manager_id NUMBER(10),
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    outcome VARCHAR2(20),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    reason VARCHAR2(500),
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    created_by VARCHAR2(100),
    updated_by VARCHAR2(100),
    CONSTRAINT pk_pms_pip PRIMARY KEY (pip_id),
    CONSTRAINT fk_pms_pip_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id),
    CONSTRAINT fk_pms_pip_manager FOREIGN KEY (manager_id) REFERENCES employee(employee_id),
    CONSTRAINT chk_pms_pip_status CHECK (status IN ('ACTIVE','COMPLETED')),
    CONSTRAINT chk_pms_pip_outcome CHECK (outcome IS NULL OR outcome IN ('IMPROVED','NOT_IMPROVED')),
    CONSTRAINT chk_pms_pip_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_pms_goal_employee ON pms_goal (employee_id);
CREATE INDEX idx_pms_goal_department ON pms_goal (department_id);
CREATE INDEX idx_pms_goal_manager ON pms_goal (manager_id);
CREATE INDEX idx_pms_goal_status ON pms_goal (is_archived, status);
CREATE INDEX idx_pms_goal_due ON pms_goal (due_date);
CREATE INDEX idx_pms_goal_category ON pms_goal (category_id);
CREATE INDEX idx_pms_goal_parent ON pms_goal (alignment_goal_id);
CREATE INDEX idx_pms_goal_progress_day ON pms_goal_progress (recorded_on, status);
CREATE INDEX idx_pms_goal_history_goal ON pms_goal_history (goal_id);
CREATE INDEX idx_pms_appraisal_employee ON pms_appraisal (employee_id);
CREATE INDEX idx_pms_appraisal_stage ON pms_appraisal (stage, completed_at);
CREATE INDEX idx_pms_pip_employee ON pms_pip (employee_id);

CREATE OR REPLACE VIEW pms_department_group_v AS
SELECT department_id,
       CASE department_name
            WHEN 'Technology' THEN 'IT'
            WHEN 'Infrastructure' THEN 'IT'
            WHEN 'Product' THEN 'IT'
            WHEN 'Research & Development' THEN 'IT'
            WHEN 'HR' THEN 'HR'
            WHEN 'Sales & Marketing' THEN 'Sales & Marketing'
            WHEN 'Operations' THEN 'Operations'
            WHEN 'Finance' THEN 'Finance'
            ELSE 'Others'
       END AS group_name
  FROM department;

-- Lets a login resolve to its employee for "My goals" and "Team goals".
ALTER TABLE app_user ADD (employee_id NUMBER(10));
ALTER TABLE app_user ADD CONSTRAINT fk_app_user_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id);

COMMIT;
PROMPT PMS tables created successfully.
