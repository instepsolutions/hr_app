-- Additive appraisal summary and PIP workflow storage on existing HRMS records.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

DECLARE
    v_count NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_count FROM user_constraints WHERE constraint_name = 'CHK_PMS_PIP_STATUS';
    IF v_count > 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE pms_pip DROP CONSTRAINT chk_pms_pip_status'; END IF;
    SELECT COUNT(*) INTO v_count FROM user_constraints WHERE constraint_name = 'CHK_PMS_PIP_OUTCOME';
    IF v_count > 0 THEN EXECUTE IMMEDIATE 'ALTER TABLE pms_pip DROP CONSTRAINT chk_pms_pip_outcome'; END IF;
END;
/
ALTER TABLE pms_pip MODIFY status VARCHAR2(24) DEFAULT 'ACTIVE';
ALTER TABLE pms_pip ADD (
    related_appraisal_id NUMBER(12),
    assigned_hr_reviewer_id NUMBER(10),
    reason_code VARCHAR2(40),
    current_performance CLOB,
    expected_performance CLOB,
    performance_gap CLOB,
    evidence CLOB,
    support_required CLOB,
    workflow_data CLOB,
    overall_progress NUMBER(5,2) DEFAULT 0 NOT NULL,
    review_frequency VARCHAR2(20) DEFAULT 'MONTHLY' NOT NULL,
    next_review_date DATE,
    extension_reason CLOB,
    outcome_reason CLOB,
    closed_at TIMESTAMP
);
ALTER TABLE pms_pip ADD CONSTRAINT chk_pms_pip_status
    CHECK (status IN ('DRAFT','ACTIVE','PENDING_REVIEW','COMPLETED','CANCELLED'));
ALTER TABLE pms_pip ADD CONSTRAINT chk_pms_pip_outcome
    CHECK (outcome IS NULL OR outcome IN ('IMPROVED','NOT_IMPROVED','SUCCESSFUL','EXTENDED','UNSUCCESSFUL','CLOSED'));
ALTER TABLE pms_pip ADD CONSTRAINT chk_pms_pip_progress
    CHECK (overall_progress BETWEEN 0 AND 100);
ALTER TABLE pms_pip ADD CONSTRAINT fk_pms_pip_appraisal
    FOREIGN KEY (related_appraisal_id) REFERENCES pms_appraisal(appraisal_id);
ALTER TABLE pms_pip ADD CONSTRAINT fk_pms_pip_hr_reviewer
    FOREIGN KEY (assigned_hr_reviewer_id) REFERENCES employee(employee_id);

CREATE SEQUENCE pms_pip_objective_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE TABLE pms_pip_objective (
    objective_id NUMBER(12) NOT NULL,
    pip_id NUMBER(10) NOT NULL,
    title VARCHAR2(300) NOT NULL,
    current_state CLOB,
    expected_state CLOB,
    measurement VARCHAR2(500),
    target_value NUMBER(12,2),
    actual_value NUMBER(12,2),
    weightage NUMBER(5,2) DEFAULT 0 NOT NULL,
    progress_percent NUMBER(5,2) DEFAULT 0 NOT NULL,
    deadline DATE,
    status VARCHAR2(24) DEFAULT 'OPEN' NOT NULL,
    manager_assessment CLOB,
    employee_comments CLOB,
    hr_comments CLOB,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_pip_objective PRIMARY KEY (objective_id),
    CONSTRAINT fk_pms_pip_objective_case FOREIGN KEY (pip_id) REFERENCES pms_pip(pip_id) ON DELETE CASCADE,
    CONSTRAINT chk_pms_pip_objective_weight CHECK (weightage BETWEEN 0 AND 100),
    CONSTRAINT chk_pms_pip_objective_progress CHECK (progress_percent BETWEEN 0 AND 100),
    CONSTRAINT chk_pms_pip_objective_status CHECK (status IN ('OPEN','ACHIEVED','NOT_ACHIEVED'))
);

CREATE SEQUENCE pms_pip_action_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE TABLE pms_pip_action_plan (
    action_id NUMBER(12) NOT NULL,
    pip_id NUMBER(10) NOT NULL,
    objective_id NUMBER(12),
    action_text VARCHAR2(1000) NOT NULL,
    owner_id NUMBER(10),
    owner_name VARCHAR2(100),
    due_date DATE,
    support_required VARCHAR2(1000),
    training_required VARCHAR2(1000),
    success_criteria VARCHAR2(1000),
    status VARCHAR2(24) DEFAULT 'OPEN' NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_pip_action_plan PRIMARY KEY (action_id),
    CONSTRAINT fk_pms_pip_action_case FOREIGN KEY (pip_id) REFERENCES pms_pip(pip_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_pip_action_objective FOREIGN KEY (objective_id) REFERENCES pms_pip_objective(objective_id) ON DELETE SET NULL,
    CONSTRAINT fk_pms_pip_action_owner FOREIGN KEY (owner_id) REFERENCES employee(employee_id),
    CONSTRAINT chk_pms_pip_action_status CHECK (status IN ('OPEN','IN_PROGRESS','COMPLETED','CANCELLED'))
);

CREATE SEQUENCE pms_pip_review_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE TABLE pms_pip_review (
    review_id NUMBER(12) NOT NULL,
    pip_id NUMBER(10) NOT NULL,
    reviewer_employee_id NUMBER(10),
    reviewer_name VARCHAR2(100) NOT NULL,
    reviewer_role VARCHAR2(24) NOT NULL,
    review_date DATE DEFAULT TRUNC(SYSDATE) NOT NULL,
    overall_progress NUMBER(5,2) NOT NULL,
    manager_assessment CLOB,
    employee_comments CLOB,
    hr_comments CLOB,
    submitted_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_pip_review PRIMARY KEY (review_id),
    CONSTRAINT fk_pms_pip_review_case FOREIGN KEY (pip_id) REFERENCES pms_pip(pip_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_pip_review_user FOREIGN KEY (reviewer_employee_id) REFERENCES employee(employee_id),
    CONSTRAINT chk_pms_pip_review_progress CHECK (overall_progress BETWEEN 0 AND 100),
    CONSTRAINT chk_pms_pip_review_role CHECK (reviewer_role IN ('MANAGER','HR','ADMIN','EMPLOYEE'))
);

CREATE SEQUENCE pms_pip_audit_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE TABLE pms_pip_audit (
    audit_id NUMBER(12) NOT NULL,
    pip_id NUMBER(10) NOT NULL,
    actor_employee_id NUMBER(10),
    actor_name VARCHAR2(100) NOT NULL,
    actor_role VARCHAR2(24) NOT NULL,
    action_type VARCHAR2(40) NOT NULL,
    old_value CLOB,
    new_value CLOB,
    changed_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_pip_audit PRIMARY KEY (audit_id),
    CONSTRAINT fk_pms_pip_audit_case FOREIGN KEY (pip_id) REFERENCES pms_pip(pip_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_pip_audit_user FOREIGN KEY (actor_employee_id) REFERENCES employee(employee_id)
);

CREATE SEQUENCE pms_pip_template_seq START WITH 1 INCREMENT BY 1 NOCACHE NOCYCLE;
CREATE TABLE pms_pip_template (
    template_id NUMBER(10) NOT NULL,
    template_name VARCHAR2(120) NOT NULL,
    duration_days NUMBER(4) NOT NULL,
    review_frequency VARCHAR2(20) NOT NULL,
    description VARCHAR2(1000),
    is_active NUMBER(1) DEFAULT 1 NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_pip_template PRIMARY KEY (template_id),
    CONSTRAINT uk_pms_pip_template_name UNIQUE (template_name),
    CONSTRAINT chk_pms_pip_template_duration CHECK (duration_days > 0),
    CONSTRAINT chk_pms_pip_template_active CHECK (is_active IN (0,1))
);

CREATE INDEX idx_pms_pip_cycle_status ON pms_pip (status, start_date, end_date);
CREATE INDEX idx_pms_pip_appraisal ON pms_pip (related_appraisal_id);
CREATE INDEX idx_pms_pip_objective_case ON pms_pip_objective (pip_id, status);
CREATE INDEX idx_pms_pip_action_case ON pms_pip_action_plan (pip_id, due_date);
CREATE INDEX idx_pms_pip_review_case ON pms_pip_review (pip_id, review_date);
CREATE INDEX idx_pms_pip_audit_case ON pms_pip_audit (pip_id, changed_at);

INSERT INTO pms_pip_template (template_id, template_name, duration_days, review_frequency, description)
VALUES (pms_pip_template_seq.NEXTVAL, '30-Day Improvement Plan', 30, 'WEEKLY', 'Focused short-term improvement with weekly check-ins.');
INSERT INTO pms_pip_template (template_id, template_name, duration_days, review_frequency, description)
VALUES (pms_pip_template_seq.NEXTVAL, '60-Day Improvement Plan', 60, 'BIWEEKLY', 'Structured improvement plan with bi-weekly check-ins.');
INSERT INTO pms_pip_template (template_id, template_name, duration_days, review_frequency, description)
VALUES (pms_pip_template_seq.NEXTVAL, '90-Day Improvement Plan', 90, 'MONTHLY', 'Sustained improvement plan with monthly check-ins.');

UPDATE pms_pip SET status = CASE WHEN status = 'COMPLETED' THEN 'COMPLETED' ELSE 'ACTIVE' END;
COMMIT;
PROMPT PMS case workflow schema created successfully.