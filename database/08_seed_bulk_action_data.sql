-- =============================================================
-- Oracle HRMS bulk action history seed
-- =============================================================
SET DEFINE OFF

INSERT INTO bulk_action (
    bulk_action_id,
    action_type,
    requested_by,
    requested_at,
    completed_at,
    total_employees,
    successful_count,
    failed_count,
    status,
    remarks
) VALUES (
    bulk_action_seq.NEXTVAL,
    'UPDATE_DEPARTMENT',
    'HR_ADMIN',
    SYSTIMESTAMP - 12,
    SYSTIMESTAMP - 10,
    15,
    14,
    1,
    'COMPLETED',
    'Department realignment after business restructure.'
);

INSERT INTO bulk_action (
    bulk_action_id,
    action_type,
    requested_by,
    requested_at,
    completed_at,
    total_employees,
    successful_count,
    failed_count,
    status,
    remarks
) VALUES (
    bulk_action_seq.NEXTVAL,
    'CHANGE_STATUS',
    'SUPER_ADMIN',
    SYSTIMESTAMP - 20,
    SYSTIMESTAMP - 18,
    8,
    8,
    0,
    'COMPLETED',
    'Change employee status for probation validation.'
);

INSERT INTO bulk_action (
    bulk_action_id,
    action_type,
    requested_by,
    requested_at,
    completed_at,
    total_employees,
    successful_count,
    failed_count,
    status,
    remarks
) VALUES (
    bulk_action_seq.NEXTVAL,
    'UPDATE_LOCATION',
    'HR_MANAGER',
    SYSTIMESTAMP - 40,
    SYSTIMESTAMP - 39,
    20,
    20,
    0,
    'COMPLETED',
    'Location updates for distributed teams.'
);

INSERT INTO bulk_action_employee (
    bulk_action_employee_id,
    bulk_action_id,
    employee_id,
    old_value,
    new_value,
    status,
    error_message,
    processed_at
)
SELECT bulk_action_employee_seq.NEXTVAL,
       a.bulk_action_id,
       e.employee_id,
       'Sales & Marketing',
       'Technology',
       'SUCCESS',
       NULL,
       SYSTIMESTAMP
FROM bulk_action a
JOIN employee e ON e.employee_id BETWEEN 1 AND 15
WHERE a.action_type = 'UPDATE_DEPARTMENT';

INSERT INTO bulk_action_employee (
    bulk_action_employee_id,
    bulk_action_id,
    employee_id,
    old_value,
    new_value,
    status,
    error_message,
    processed_at
)
SELECT bulk_action_employee_seq.NEXTVAL,
       a.bulk_action_id,
       e.employee_id,
       'ACTIVE',
       'ON_LEAVE',
       'SUCCESS',
       NULL,
       SYSTIMESTAMP
FROM bulk_action a
JOIN employee e ON e.employee_id BETWEEN 150 AND 157
WHERE a.action_type = 'CHANGE_STATUS';

COMMIT;
PROMPT Bulk action seed data inserted successfully.
