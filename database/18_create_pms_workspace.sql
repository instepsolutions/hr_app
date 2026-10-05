-- Additive PMS workspace schema. Existing employee, department and PMS goal data is reused.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

ALTER TABLE pms_kra ADD (
    department_id NUMBER(10),
    owner_type VARCHAR2(20) DEFAULT 'SHARED' NOT NULL,
    owner_employee_id NUMBER(10),
    weightage NUMBER(5,2) DEFAULT 0 NOT NULL,
    effective_date DATE DEFAULT TRUNC(SYSDATE) NOT NULL
);
ALTER TABLE pms_kra DROP CONSTRAINT chk_pms_kra_status;
ALTER TABLE pms_kra ADD CONSTRAINT chk_pms_kra_status
    CHECK (status IN ('ACTIVE','INACTIVE','DRAFT','UNDER_REVIEW'));
ALTER TABLE pms_kra ADD CONSTRAINT fk_pms_kra_department
    FOREIGN KEY (department_id) REFERENCES department(department_id);
ALTER TABLE pms_kra ADD CONSTRAINT fk_pms_kra_owner
    FOREIGN KEY (owner_employee_id) REFERENCES employee(employee_id);
ALTER TABLE pms_kra ADD CONSTRAINT chk_pms_kra_weightage CHECK (weightage BETWEEN 0 AND 100);
ALTER TABLE pms_kra ADD CONSTRAINT chk_pms_kra_owner
    CHECK (owner_type IN ('MANAGER','EMPLOYEE','SHARED','HR'));

ALTER TABLE pms_kpi ADD (
    department_id NUMBER(10),
    owner_type VARCHAR2(20) DEFAULT 'SHARED' NOT NULL,
    owner_employee_id NUMBER(10),
    measurement_type VARCHAR2(20) DEFAULT 'NUMBER' NOT NULL,
    frequency VARCHAR2(20) DEFAULT 'QUARTERLY' NOT NULL,
    weightage NUMBER(5,2) DEFAULT 0 NOT NULL
);
ALTER TABLE pms_kpi DROP CONSTRAINT chk_pms_kpi_status;
ALTER TABLE pms_kpi ADD CONSTRAINT chk_pms_kpi_status
    CHECK (status IN ('ACTIVE','INACTIVE','DRAFT','UNDER_REVIEW'));
ALTER TABLE pms_kpi ADD CONSTRAINT fk_pms_kpi_department
    FOREIGN KEY (department_id) REFERENCES department(department_id);
ALTER TABLE pms_kpi ADD CONSTRAINT fk_pms_kpi_owner
    FOREIGN KEY (owner_employee_id) REFERENCES employee(employee_id);
ALTER TABLE pms_kpi ADD CONSTRAINT chk_pms_kpi_weightage CHECK (weightage BETWEEN 0 AND 100);
ALTER TABLE pms_kpi ADD CONSTRAINT chk_pms_kpi_owner
    CHECK (owner_type IN ('MANAGER','EMPLOYEE','SHARED','HR'));
ALTER TABLE pms_kpi ADD CONSTRAINT chk_pms_kpi_measurement
    CHECK (measurement_type IN ('NUMBER','PERCENTAGE','CURRENCY','RATING','BOOLEAN','RATIO'));
ALTER TABLE pms_kpi ADD CONSTRAINT chk_pms_kpi_frequency
    CHECK (frequency IN ('DAILY','WEEKLY','MONTHLY','QUARTERLY','HALF_YEARLY','YEARLY'));

CREATE TABLE pms_kra_department (
    kra_id NUMBER(10) NOT NULL,
    department_id NUMBER(10) NOT NULL,
    mapped_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    mapped_by VARCHAR2(100),
    CONSTRAINT pk_pms_kra_department PRIMARY KEY (kra_id, department_id),
    CONSTRAINT fk_pms_kra_dept_kra FOREIGN KEY (kra_id) REFERENCES pms_kra(kra_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_kra_dept_department FOREIGN KEY (department_id) REFERENCES department(department_id)
);

CREATE TABLE pms_kpi_department (
    kpi_id NUMBER(10) NOT NULL,
    department_id NUMBER(10) NOT NULL,
    mapped_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    mapped_by VARCHAR2(100),
    CONSTRAINT pk_pms_kpi_department PRIMARY KEY (kpi_id, department_id),
    CONSTRAINT fk_pms_kpi_dept_kpi FOREIGN KEY (kpi_id) REFERENCES pms_kpi(kpi_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_kpi_dept_department FOREIGN KEY (department_id) REFERENCES department(department_id)
);

CREATE TABLE pms_employee_kra (
    employee_id NUMBER(10) NOT NULL,
    kra_id NUMBER(10) NOT NULL,
    weightage NUMBER(5,2) DEFAULT 0 NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    mapped_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    mapped_by VARCHAR2(100),
    CONSTRAINT pk_pms_employee_kra PRIMARY KEY (employee_id, kra_id),
    CONSTRAINT fk_pms_employee_kra_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_employee_kra_kra FOREIGN KEY (kra_id) REFERENCES pms_kra(kra_id) ON DELETE CASCADE,
    CONSTRAINT chk_pms_employee_kra_weight CHECK (weightage BETWEEN 0 AND 100),
    CONSTRAINT chk_pms_employee_kra_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE TABLE pms_employee_kpi (
    employee_id NUMBER(10) NOT NULL,
    kpi_id NUMBER(10) NOT NULL,
    weightage NUMBER(5,2) DEFAULT 0 NOT NULL,
    status VARCHAR2(20) DEFAULT 'ACTIVE' NOT NULL,
    mapped_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    mapped_by VARCHAR2(100),
    CONSTRAINT pk_pms_employee_kpi PRIMARY KEY (employee_id, kpi_id),
    CONSTRAINT fk_pms_employee_kpi_employee FOREIGN KEY (employee_id) REFERENCES employee(employee_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_employee_kpi_kpi FOREIGN KEY (kpi_id) REFERENCES pms_kpi(kpi_id) ON DELETE CASCADE,
    CONSTRAINT chk_pms_employee_kpi_weight CHECK (weightage BETWEEN 0 AND 100),
    CONSTRAINT chk_pms_employee_kpi_status CHECK (status IN ('ACTIVE','INACTIVE'))
);

CREATE SEQUENCE pms_setup_history_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE TABLE pms_setup_history (
    history_id NUMBER(12) NOT NULL,
    action_type VARCHAR2(40) NOT NULL,
    module_name VARCHAR2(20) NOT NULL,
    item_id NUMBER(10),
    item_title VARCHAR2(200),
    old_value VARCHAR2(1000),
    new_value VARCHAR2(1000),
    changed_by VARCHAR2(100),
    changed_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_setup_history PRIMARY KEY (history_id)
);

ALTER TABLE pms_appraisal ADD (
    self_status VARCHAR2(20) DEFAULT 'NOT_STARTED' NOT NULL,
    self_rating NUMBER(3,2),
    self_data CLOB,
    self_submitted_at TIMESTAMP,
    self_updated_at TIMESTAMP
);
ALTER TABLE pms_appraisal ADD CONSTRAINT chk_pms_appraisal_self_status
    CHECK (self_status IN ('NOT_STARTED','DRAFT','SUBMITTED'));
ALTER TABLE pms_appraisal ADD CONSTRAINT chk_pms_appraisal_self_rating
    CHECK (self_rating IS NULL OR self_rating BETWEEN 1 AND 5);

CREATE SEQUENCE pms_appraisal_comment_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE TABLE pms_appraisal_comment (
    comment_id NUMBER(12) NOT NULL,
    appraisal_id NUMBER(12) NOT NULL,
    author_name VARCHAR2(100) NOT NULL,
    author_role VARCHAR2(20) NOT NULL,
    comment_text VARCHAR2(2000) NOT NULL,
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_appraisal_comment PRIMARY KEY (comment_id),
    CONSTRAINT fk_pms_appraisal_comment FOREIGN KEY (appraisal_id) REFERENCES pms_appraisal(appraisal_id) ON DELETE CASCADE,
    CONSTRAINT chk_pms_appraisal_comment_role CHECK (author_role IN ('EMPLOYEE','MANAGER','HR'))
);

CREATE TABLE pms_appraisal_timeline (
    cycle_id NUMBER(10) NOT NULL,
    stage_code VARCHAR2(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    updated_by VARCHAR2(100),
    updated_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_appraisal_timeline PRIMARY KEY (cycle_id, stage_code),
    CONSTRAINT fk_pms_appraisal_timeline_cycle FOREIGN KEY (cycle_id) REFERENCES pms_appraisal_cycle(cycle_id) ON DELETE CASCADE,
    CONSTRAINT chk_pms_appraisal_timeline_stage CHECK (stage_code IN ('SELF_APPRAISAL','MANAGER_REVIEW','HR_REVIEW','FINALIZATION')),
    CONSTRAINT chk_pms_appraisal_timeline_dates CHECK (end_date >= start_date)
);

CREATE INDEX idx_pms_kra_department ON pms_kra_department (department_id, kra_id);
CREATE INDEX idx_pms_kpi_department ON pms_kpi_department (department_id, kpi_id);
CREATE INDEX idx_pms_employee_kra ON pms_employee_kra (kra_id, employee_id);
CREATE INDEX idx_pms_employee_kpi ON pms_employee_kpi (kpi_id, employee_id);
CREATE INDEX idx_pms_setup_history_date ON pms_setup_history (changed_at);
CREATE INDEX idx_pms_appraisal_self_status ON pms_appraisal (self_status, self_rating);
CREATE INDEX idx_pms_appraisal_comment ON pms_appraisal_comment (appraisal_id, created_at);

UPDATE pms_kra k SET k.department_id = (SELECT MIN(d.department_id) FROM department d)
 WHERE k.department_id IS NULL;
UPDATE pms_kra SET effective_date = TRUNC(created_at), weightage = 100 / GREATEST(1, (SELECT COUNT(*) FROM pms_kra))
 WHERE weightage = 0;
UPDATE pms_kpi k SET k.department_id = (SELECT kra.department_id FROM pms_kra kra WHERE kra.kra_id = k.kra_id)
 WHERE k.department_id IS NULL;
UPDATE pms_appraisal
   SET self_status = CASE WHEN stage IN ('MANAGER_REVIEW','HR_REVIEW','COMPLETED') THEN 'SUBMITTED'
                          WHEN stage = 'SELF_APPRAISAL' THEN 'DRAFT' ELSE 'NOT_STARTED' END,
       self_rating = CASE WHEN stage = 'COMPLETED' THEN final_rating ELSE NULL END
 WHERE self_status = 'NOT_STARTED';

INSERT INTO pms_kra_department (kra_id, department_id, mapped_by)
SELECT k.kra_id, d.department_id, 'SYSTEM'
  FROM pms_kra k CROSS JOIN department d
 WHERE MOD(k.kra_id * 17 + d.department_id * 11, 5) < 2;
INSERT INTO pms_kpi_department (kpi_id, department_id, mapped_by)
SELECT kp.kpi_id, kd.department_id, 'SYSTEM'
  FROM pms_kpi kp JOIN pms_kra_department kd ON kd.kra_id = kp.kra_id;
UPDATE pms_kra k SET k.department_id = (SELECT MIN(m.department_id) FROM pms_kra_department m WHERE m.kra_id = k.kra_id);
UPDATE pms_kpi kp SET kp.department_id = (SELECT MIN(m.department_id) FROM pms_kpi_department m WHERE m.kpi_id = kp.kpi_id);

INSERT INTO pms_employee_kra (employee_id, kra_id, weightage, mapped_by)
SELECT e.employee_id, k.kra_id, k.weightage, 'SYSTEM'
  FROM employee e CROSS JOIN pms_kra k
 WHERE e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')
   AND MOD(e.employee_id + k.kra_id, 7) = 0;
INSERT INTO pms_employee_kpi (employee_id, kpi_id, weightage, mapped_by)
SELECT m.employee_id, kp.kpi_id, kp.weightage, 'SYSTEM'
  FROM pms_employee_kra m JOIN pms_kpi kp ON kp.kra_id = m.kra_id
 WHERE MOD(m.employee_id + kp.kpi_id, 2) = 0;

INSERT INTO pms_appraisal_timeline (cycle_id, stage_code, start_date, end_date, updated_by)
SELECT cycle_id, 'SELF_APPRAISAL', start_date, start_date + GREATEST(1, FLOOR((due_date - start_date) * 0.45)), 'SYSTEM'
  FROM pms_appraisal_cycle;
INSERT INTO pms_appraisal_timeline (cycle_id, stage_code, start_date, end_date, updated_by)
SELECT cycle_id, 'MANAGER_REVIEW', start_date + GREATEST(1, FLOOR((due_date - start_date) * 0.45)) + 1,
       start_date + GREATEST(2, FLOOR((due_date - start_date) * 0.72)), 'SYSTEM'
  FROM pms_appraisal_cycle;
INSERT INTO pms_appraisal_timeline (cycle_id, stage_code, start_date, end_date, updated_by)
SELECT cycle_id, 'HR_REVIEW', start_date + GREATEST(2, FLOOR((due_date - start_date) * 0.72)) + 1,
       start_date + GREATEST(3, FLOOR((due_date - start_date) * 0.9)), 'SYSTEM'
  FROM pms_appraisal_cycle;
INSERT INTO pms_appraisal_timeline (cycle_id, stage_code, start_date, end_date, updated_by)
SELECT cycle_id, 'FINALIZATION', start_date + GREATEST(3, FLOOR((due_date - start_date) * 0.9)) + 1,
       due_date, 'SYSTEM'
  FROM pms_appraisal_cycle;

COMMIT;
PROMPT PMS workspace schema created successfully.