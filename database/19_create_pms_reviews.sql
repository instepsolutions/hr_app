-- Additive Manager Review and HR Review workflow on existing appraisal records.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

ALTER TABLE pms_appraisal ADD (
    manager_status VARCHAR2(24) DEFAULT 'NOT_READY' NOT NULL,
    manager_rating NUMBER(3,2),
    manager_data CLOB,
    manager_submitted_at TIMESTAMP,
    hr_status VARCHAR2(24) DEFAULT 'NOT_READY' NOT NULL,
    hr_rating NUMBER(3,2),
    hr_data CLOB,
    hr_submitted_at TIMESTAMP,
    assigned_hr_reviewer_id NUMBER(10),
    final_recommendation VARCHAR2(30)
);

ALTER TABLE pms_appraisal ADD CONSTRAINT chk_pms_appraisal_manager_status
    CHECK (manager_status IN ('NOT_READY','PENDING','IN_PROGRESS','SUBMITTED','SENT_BACK','COMPLETED'));
ALTER TABLE pms_appraisal ADD CONSTRAINT chk_pms_appraisal_manager_rating
    CHECK (manager_rating IS NULL OR manager_rating BETWEEN 1 AND 5);
ALTER TABLE pms_appraisal ADD CONSTRAINT chk_pms_appraisal_hr_status
    CHECK (hr_status IN ('NOT_READY','PENDING','IN_PROGRESS','UNDER_CALIBRATION','COMPLETED','SENT_BACK','FINALIZED'));
ALTER TABLE pms_appraisal ADD CONSTRAINT chk_pms_appraisal_hr_rating
    CHECK (hr_rating IS NULL OR hr_rating BETWEEN 1 AND 5);
ALTER TABLE pms_appraisal ADD CONSTRAINT chk_pms_appraisal_final_recommendation
    CHECK (final_recommendation IS NULL OR final_recommendation IN ('PROMOTION','INCREMENT','PIP','NO_CHANGE'));
ALTER TABLE pms_appraisal ADD CONSTRAINT fk_pms_appraisal_hr_reviewer
    FOREIGN KEY (assigned_hr_reviewer_id) REFERENCES employee(employee_id);

CREATE SEQUENCE pms_review_history_seq START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE;
CREATE TABLE pms_review_history (
    history_id NUMBER(12) NOT NULL,
    appraisal_id NUMBER(12) NOT NULL,
    reviewer_employee_id NUMBER(10),
    reviewer_name VARCHAR2(100) NOT NULL,
    reviewer_role VARCHAR2(20) NOT NULL,
    action_type VARCHAR2(40) NOT NULL,
    old_value VARCHAR2(1000),
    new_value VARCHAR2(1000),
    reason VARCHAR2(1000),
    changed_at TIMESTAMP DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT pk_pms_review_history PRIMARY KEY (history_id),
    CONSTRAINT fk_pms_review_history_appraisal FOREIGN KEY (appraisal_id) REFERENCES pms_appraisal(appraisal_id) ON DELETE CASCADE,
    CONSTRAINT fk_pms_review_history_employee FOREIGN KEY (reviewer_employee_id) REFERENCES employee(employee_id),
    CONSTRAINT chk_pms_review_history_role CHECK (reviewer_role IN ('MANAGER','HR','ADMIN'))
);

CREATE INDEX idx_pms_appraisal_manager_status ON pms_appraisal (manager_status, self_status);
CREATE INDEX idx_pms_appraisal_hr_status ON pms_appraisal (hr_status, manager_status);
CREATE INDEX idx_pms_appraisal_hr_reviewer ON pms_appraisal (assigned_hr_reviewer_id, hr_status);
CREATE INDEX idx_pms_review_history_appraisal ON pms_review_history (appraisal_id, changed_at);

UPDATE pms_appraisal
   SET manager_status = CASE
           WHEN stage = 'COMPLETED' THEN 'COMPLETED'
           WHEN stage IN ('HR_REVIEW','MANAGER_REVIEW') AND self_status = 'SUBMITTED' THEN 'SUBMITTED'
           WHEN self_status = 'SUBMITTED' THEN 'PENDING'
           ELSE 'NOT_READY'
       END,
       manager_rating = CASE WHEN stage = 'COMPLETED' THEN final_rating END,
       hr_status = CASE
           WHEN stage = 'COMPLETED' THEN 'FINALIZED'
           WHEN stage = 'HR_REVIEW' THEN 'PENDING'
           ELSE 'NOT_READY'
       END,
       hr_rating = CASE WHEN stage = 'COMPLETED' THEN final_rating END,
       assigned_hr_reviewer_id = (SELECT MIN(u.employee_id) FROM app_user u
                                   WHERE u.employee_id IS NOT NULL AND u.role IN ('SUPER_ADMIN','HR_ADMIN','HR_MANAGER')),
       final_recommendation = CASE WHEN stage = 'COMPLETED' THEN 'NO_CHANGE' END
 WHERE manager_status = 'NOT_READY' AND hr_status = 'NOT_READY';

INSERT INTO pms_review_history (history_id, appraisal_id, reviewer_name, reviewer_role, action_type, new_value, reason)
SELECT pms_review_history_seq.NEXTVAL, appraisal_id, 'SYSTEM', 'ADMIN', 'MIGRATED', stage,
       'Backfilled existing appraisal stage into the review workflow'
  FROM pms_appraisal
 WHERE stage IN ('MANAGER_REVIEW','HR_REVIEW','COMPLETED');

COMMIT;
PROMPT PMS review workflow schema created successfully.