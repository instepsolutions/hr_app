-- Development dashboard seed data. Employee-derived counts are generated from Oracle employee rows.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

DECLARE
    v_existing_count NUMBER;
    v_approval_type VARCHAR2(40);
BEGIN
    FOR employee_row IN (
        SELECT employee_id,
               ROW_NUMBER() OVER (ORDER BY employee_id) AS row_number,
               COUNT(*) OVER () AS total_rows
          FROM employee e
         WHERE NOT EXISTS (
               SELECT 1
                 FROM dashboard_attendance a
                WHERE a.employee_id = e.employee_id
                  AND a.attendance_date = TRUNC(SYSDATE)
         )
    ) LOOP
        INSERT INTO dashboard_attendance (
            attendance_id, employee_id, attendance_date, attendance_status
        ) VALUES (
            dashboard_attendance_seq.NEXTVAL,
            employee_row.employee_id,
            TRUNC(SYSDATE),
            CASE
                WHEN employee_row.row_number <= FLOOR(employee_row.total_rows * 0.833) THEN 'PRESENT'
                WHEN employee_row.row_number <= FLOOR(employee_row.total_rows * 0.929) THEN 'LATE'
                ELSE 'ABSENT'
            END
        );
    END LOOP;

    SELECT COUNT(*) INTO v_existing_count FROM dashboard_approval;
    IF v_existing_count = 0 THEN
        FOR approval_number IN 1..36 LOOP
            v_approval_type := CASE
                WHEN approval_number <= 12 THEN 'LEAVE_REQUEST'
                WHEN approval_number <= 19 THEN 'EXPENSE_CLAIM'
                WHEN approval_number <= 24 THEN 'TIMESHEET'
                WHEN approval_number <= 32 THEN 'DOCUMENT'
                ELSE 'EXIT_CLEARANCE'
            END;
            INSERT INTO dashboard_approval (
                approval_id, approval_type, status, employee_id, requested_at, description
            ) VALUES (
                dashboard_approval_seq.NEXTVAL,
                v_approval_type,
                'PENDING',
                NULL,
                SYSTIMESTAMP - NUMTODSINTERVAL(approval_number, 'HOUR'),
                'Development approval queue seed'
            );
        END LOOP;
    END IF;

    SELECT COUNT(*) INTO v_existing_count FROM dashboard_announcement;
    IF v_existing_count = 0 THEN
        INSERT INTO dashboard_announcement (announcement_id, title, summary, published_at, is_new, is_active)
        VALUES (dashboard_announcement_seq.NEXTVAL, 'Employee engagement survey is open', 'Share feedback with the People team in the current employee engagement survey.', SYSTIMESTAMP - INTERVAL '1' DAY, 1, 1);
        INSERT INTO dashboard_announcement (announcement_id, title, summary, published_at, is_new, is_active)
        VALUES (dashboard_announcement_seq.NEXTVAL, 'Workplace policy update', 'Review the latest workplace policy information in your employee profile.', SYSTIMESTAMP - INTERVAL '3' DAY, 0, 1);
        INSERT INTO dashboard_announcement (announcement_id, title, summary, published_at, is_new, is_active)
        VALUES (dashboard_announcement_seq.NEXTVAL, 'Payroll processing schedule', 'The current month payroll is scheduled for processing on the final business day.', SYSTIMESTAMP - INTERVAL '5' DAY, 0, 1);
    END IF;

    SELECT COUNT(*) INTO v_existing_count FROM dashboard_event;
    IF v_existing_count = 0 THEN
        INSERT INTO dashboard_event (event_id, event_name, start_date, end_date, location, is_active)
        VALUES (dashboard_event_seq.NEXTVAL, 'Performance review cycle', TRUNC(SYSDATE) + 29, TRUNC(SYSDATE) + 43, 'People Operations', 1);
        INSERT INTO dashboard_event (event_id, event_name, start_date, end_date, location, is_active)
        VALUES (dashboard_event_seq.NEXTVAL, 'Leadership training program', TRUNC(SYSDATE) + 5, TRUNC(SYSDATE) + 5, 'Training room', 1);
        INSERT INTO dashboard_event (event_id, event_name, start_date, end_date, location, is_active)
        VALUES (dashboard_event_seq.NEXTVAL, 'HR townhall meeting', TRUNC(SYSDATE) + 10, TRUNC(SYSDATE) + 10, 'Main auditorium', 1);
    END IF;
END;
/

COMMIT;
PROMPT Dashboard development seed data prepared.
