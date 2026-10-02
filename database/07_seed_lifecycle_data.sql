-- =============================================================
-- Oracle HRMS lifecycle events seed
-- =============================================================

DECLARE
    v_status VARCHAR2(30);
BEGIN
    FOR emp IN (
        SELECT employee_id, date_of_joining, confirmation_date, employee_status
        FROM employee
    ) LOOP
        INSERT INTO employee_lifecycle_event (
            event_id,
            employee_id,
            event_type,
            event_date,
            description,
            reference_id,
            created_by,
            created_at
        ) VALUES (
            employee_lifecycle_event_seq.NEXTVAL,
            emp.employee_id,
            'JOINING',
            NVL(emp.date_of_joining, TRUNC(SYSDATE)),
            'Employee joined the organization.',
            'JOIN-' || emp.employee_id,
            'SYSTEM',
            SYSTIMESTAMP
        );

        IF emp.confirmation_date IS NOT NULL THEN
            INSERT INTO employee_lifecycle_event (
                event_id,
                employee_id,
                event_type,
                event_date,
                description,
                reference_id,
                created_by,
                created_at
            ) VALUES (
                employee_lifecycle_event_seq.NEXTVAL,
                emp.employee_id,
                'CONFIRMATION',
                emp.confirmation_date,
                'Employee successfully completed confirmation.',
                'CONF-' || emp.employee_id,
                'SYSTEM',
                SYSTIMESTAMP
            );
        END IF;

        IF emp.employee_status = 'ACTIVE' THEN
            INSERT INTO employee_lifecycle_event (
                event_id,
                employee_id,
                event_type,
                event_date,
                description,
                reference_id,
                created_by,
                created_at
            ) VALUES (
                employee_lifecycle_event_seq.NEXTVAL,
                emp.employee_id,
                'ONBOARDING',
                TRUNC(SYSDATE) - 30,
                'Onboarding and induction completed.',
                'ONB-' || emp.employee_id,
                'SYSTEM',
                SYSTIMESTAMP
            );
        ELSIF emp.employee_status = 'ON_LEAVE' THEN
            INSERT INTO employee_lifecycle_event (
                event_id,
                employee_id,
                event_type,
                event_date,
                description,
                reference_id,
                created_by,
                created_at
            ) VALUES (
                employee_lifecycle_event_seq.NEXTVAL,
                emp.employee_id,
                'NOTICE_PERIOD',
                TRUNC(SYSDATE) - 10,
                'Employee marked on leave.',
                'LV-' || emp.employee_id,
                'SYSTEM',
                SYSTIMESTAMP
            );
        ELSIF emp.employee_status = 'EXITED' THEN
            INSERT INTO employee_lifecycle_event (
                event_id,
                employee_id,
                event_type,
                event_date,
                description,
                reference_id,
                created_by,
                created_at
            ) VALUES (
                employee_lifecycle_event_seq.NEXTVAL,
                emp.employee_id,
                'EXIT',
                TRUNC(SYSDATE) - 5,
                'Exit formalities completed.',
                'EXIT-' || emp.employee_id,
                'SYSTEM',
                SYSTIMESTAMP
            );
        END IF;
    END LOOP;
END;
/

INSERT INTO employee_status_history (
    history_id,
    employee_id,
    old_status,
    new_status,
    effective_date,
    reason,
    remarks,
    changed_by,
    created_at
)
SELECT employee_status_history_seq.NEXTVAL,
       e.employee_id,
       'NEW',
       e.employee_status,
       NVL(e.date_of_joining, TRUNC(SYSDATE)),
       'Initial system assignment',
       'Employee status recorded at creation.',
       'SYSTEM',
       SYSTIMESTAMP
FROM employee e;

COMMIT;
PROMPT Lifecycle and status history seeded successfully.
