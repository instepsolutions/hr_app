-- =============================================================
-- Oracle HRMS employee seed generation (approx. 1,248 employees)
-- =============================================================

DECLARE
    v_employee_id NUMBER;
    v_department_id NUMBER;
    v_designation_id NUMBER;
    v_location_id NUMBER;
    v_employment_type_id NUMBER;
    v_manager_id NUMBER;
    v_first_name VARCHAR2(100);
    v_last_name VARCHAR2(100);
    v_gender VARCHAR2(20);
    v_status VARCHAR2(30);
    v_email_base VARCHAR2(150);
BEGIN
    FOR i IN 1..1248 LOOP
        v_department_id := MOD(i, 12) + 1;
        v_designation_id := CASE MOD(i, 12)
            WHEN 0 THEN 15
            WHEN 1 THEN 13
            WHEN 2 THEN 14
            WHEN 3 THEN 4
            WHEN 4 THEN 5
            WHEN 5 THEN 7
            WHEN 6 THEN 8
            WHEN 7 THEN 9
            WHEN 8 THEN 12
            WHEN 9 THEN 10
            WHEN 10 THEN 11
            ELSE 3
        END;

        v_location_id := MOD(i, 12) + 1;
        v_employment_type_id := CASE MOD(i, 4)
            WHEN 0 THEN 1
            WHEN 1 THEN 2
            WHEN 2 THEN 3
            ELSE 4
        END;

        v_manager_id := CASE
            WHEN i <= 2 THEN NULL
            ELSE GREATEST(1, FLOOR((i - 1) / 8))
        END;

        IF MOD(i, 2) = 0 THEN
            v_gender := 'MALE';
        ELSE
            v_gender := 'FEMALE';
        END IF;

        IF i <= 1180 THEN
            v_status := 'ACTIVE';
        ELSIF i <= 1236 THEN
            v_status := 'ON_LEAVE';
        ELSE
            v_status := 'EXITED';
        END IF;

        v_first_name := CASE MOD(i, 13)
            WHEN 0 THEN 'Aarav'
            WHEN 1 THEN 'Ananya'
            WHEN 2 THEN 'Rohan'
            WHEN 3 THEN 'Sneha'
            WHEN 4 THEN 'Karan'
            WHEN 5 THEN 'Priya'
            WHEN 6 THEN 'Amit'
            WHEN 7 THEN 'Neha'
            WHEN 8 THEN 'Vikram'
            WHEN 9 THEN 'Anjali'
            WHEN 10 THEN 'Rahul'
            WHEN 11 THEN 'Meera'
            ELSE 'Ishita'
        END;

        v_last_name := CASE MOD(i, 11)
            WHEN 0 THEN 'Sharma'
            WHEN 1 THEN 'Mehta'
            WHEN 2 THEN 'Patil'
            WHEN 3 THEN 'Verma'
            WHEN 4 THEN 'Kapoor'
            WHEN 5 THEN 'Joshi'
            WHEN 6 THEN 'Deshmukh'
            WHEN 7 THEN 'Singh'
            WHEN 8 THEN 'Malhotra'
            WHEN 9 THEN 'Iyer'
            ELSE 'Nair'
        END;

        INSERT INTO employee (
            employee_id,
            employee_code,
            first_name,
            middle_name,
            last_name,
            display_name,
            gender,
            date_of_birth,
            personal_email,
            official_email,
            mobile_number,
            alternate_mobile,
            department_id,
            designation_id,
            location_id,
            employment_type_id,
            reporting_manager_id,
            date_of_joining,
            confirmation_date,
            date_of_exit,
            employee_status,
            profile_photo_url,
            created_at,
            updated_at,
            created_by,
            updated_by
        ) VALUES (
            employee_seq.NEXTVAL,
            'EMP' || LPAD(TO_CHAR(i), 5, '0'),
            v_first_name,
            NULL,
            v_last_name,
            v_first_name || ' ' || v_last_name,
            v_gender,
            ADD_MONTHS(TRUNC(SYSDATE), -(18 + MOD(i, 35) * 12)),
            LOWER(v_first_name || '.' || v_last_name || '.' || i || '@gmail.com'),
            LOWER('emp' || LPAD(TO_CHAR(i), 5, '0') || '@company.com'),
            '+91 ' || LPAD(TO_CHAR(7000000000 + i), 10, '0'),
            NULL,
            v_department_id,
            v_designation_id,
            v_location_id,
            v_employment_type_id,
            v_manager_id,
            TRUNC(SYSDATE) - MOD(i, 800),
            TRUNC(SYSDATE) - MOD(i, 800) + 90,
            CASE WHEN v_status = 'EXITED' THEN TRUNC(SYSDATE) - 10 ELSE NULL END,
            v_status,
            NULL,
            SYSTIMESTAMP,
            SYSTIMESTAMP,
            'SYSTEM',
            'SYSTEM'
        );
    END LOOP;
END;
/

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'SALES'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'Manager - Sales'),
    location_id = (SELECT location_id FROM location WHERE city = 'Mumbai'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Permanent'),
    employee_status = 'ACTIVE',
    display_name = 'Priya Sharma',
    first_name = 'Priya',
    middle_name = NULL,
    last_name = 'Sharma',
    gender = 'FEMALE',
    date_of_birth = DATE '1993-02-14',
    personal_email = 'priya.sharma@gmail.com',
    official_email = 'priya.sharma@company.com',
    mobile_number = '+91 98765 43210',
    reporting_manager_id = NULL,
    date_of_joining = DATE '2022-05-26',
    confirmation_date = DATE '2023-05-26'
WHERE employee_code = 'EMP00125';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'TECH'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'Software Engineer'),
    location_id = (SELECT location_id FROM location WHERE city = 'Bengaluru'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Permanent'),
    employee_status = 'ACTIVE',
    display_name = 'Rohan Mehta',
    first_name = 'Rohan',
    middle_name = NULL,
    last_name = 'Mehta',
    gender = 'MALE',
    date_of_birth = DATE '1995-08-10',
    personal_email = 'rohan.mehta@gmail.com',
    official_email = 'rohan.mehta@company.com',
    mobile_number = '+91 99887 76543',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00125'),
    date_of_joining = DATE '2022-05-25',
    confirmation_date = DATE '2023-05-25'
WHERE employee_code = 'EMP00124';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'HR'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'HR Executive'),
    location_id = (SELECT location_id FROM location WHERE city = 'Pune'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Permanent'),
    employee_status = 'ACTIVE',
    display_name = 'Sneha Patil',
    first_name = 'Sneha',
    middle_name = NULL,
    last_name = 'Patil',
    gender = 'FEMALE',
    date_of_birth = DATE '1994-03-19',
    personal_email = 'sneha.patil@gmail.com',
    official_email = 'sneha.patil@company.com',
    mobile_number = '+91 97654 11223',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00125'),
    date_of_joining = DATE '2022-05-24',
    confirmation_date = DATE '2023-05-24'
WHERE employee_code = 'EMP00123';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'FIN'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'Business Analyst'),
    location_id = (SELECT location_id FROM location WHERE city = 'Mumbai'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Permanent'),
    employee_status = 'ACTIVE',
    display_name = 'Amit Verma',
    first_name = 'Amit',
    middle_name = NULL,
    last_name = 'Verma',
    gender = 'MALE',
    date_of_birth = DATE '1992-09-12',
    personal_email = 'amit.verma@gmail.com',
    official_email = 'amit.verma@company.com',
    mobile_number = '+91 98678 33445',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00125'),
    date_of_joining = DATE '2022-05-23',
    confirmation_date = DATE '2023-05-23'
WHERE employee_code = 'EMP00122';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'FIN'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'Executive - Finance'),
    location_id = (SELECT location_id FROM location WHERE city = 'Delhi'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Permanent'),
    employee_status = 'ACTIVE',
    display_name = 'Neha Kapoor',
    first_name = 'Neha',
    middle_name = NULL,
    last_name = 'Kapoor',
    gender = 'FEMALE',
    date_of_birth = DATE '1996-11-08',
    personal_email = 'neha.kapoor@gmail.com',
    official_email = 'neha.kapoor@company.com',
    mobile_number = '+91 97111 45678',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00125'),
    date_of_joining = DATE '2022-05-22',
    confirmation_date = DATE '2023-05-22'
WHERE employee_code = 'EMP00121';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'OPS'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'Operations Lead'),
    location_id = (SELECT location_id FROM location WHERE city = 'Hyderabad'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Permanent'),
    employee_status = 'ACTIVE',
    display_name = 'Vikram Joshi',
    first_name = 'Vikram',
    middle_name = NULL,
    last_name = 'Joshi',
    gender = 'MALE',
    date_of_birth = DATE '1990-07-20',
    personal_email = 'vikram.joshi@gmail.com',
    official_email = 'vikram.joshi@company.com',
    mobile_number = '+91 98900 11234',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00125'),
    date_of_joining = DATE '2022-05-21',
    confirmation_date = DATE '2023-05-21'
WHERE employee_code = 'EMP00120';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'TECH'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'UI/UX Designer'),
    location_id = (SELECT location_id FROM location WHERE city = 'Pune'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Contract'),
    employee_status = 'ACTIVE',
    display_name = 'Anjali Deshmukh',
    first_name = 'Anjali',
    middle_name = NULL,
    last_name = 'Deshmukh',
    gender = 'FEMALE',
    date_of_birth = DATE '1993-05-19',
    personal_email = 'anjali.deshmukh@gmail.com',
    official_email = 'anjali.deshmukh@company.com',
    mobile_number = '+91 97222 33221',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00125'),
    date_of_joining = DATE '2022-05-20',
    confirmation_date = DATE '2023-05-20'
WHERE employee_code = 'EMP00119';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'SALES'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'Sales Executive'),
    location_id = (SELECT location_id FROM location WHERE city = 'Mumbai'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Contract'),
    employee_status = 'ACTIVE',
    display_name = 'Rahul Singh',
    first_name = 'Rahul',
    middle_name = NULL,
    last_name = 'Singh',
    gender = 'MALE',
    date_of_birth = DATE '1991-02-21',
    personal_email = 'rahul.singh@gmail.com',
    official_email = 'rahul.singh@company.com',
    mobile_number = '+91 98988 10022',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00125'),
    date_of_joining = DATE '2022-05-19',
    confirmation_date = DATE '2023-05-19'
WHERE employee_code = 'EMP00118';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'OPS'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'Associate'),
    location_id = (SELECT location_id FROM location WHERE city = 'Bengaluru'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Permanent'),
    employee_status = 'ACTIVE',
    display_name = 'Karan Malhotra',
    first_name = 'Karan',
    middle_name = NULL,
    last_name = 'Malhotra',
    gender = 'MALE',
    date_of_birth = DATE '1998-08-26',
    personal_email = 'karan.malhotra@gmail.com',
    official_email = 'karan.malhotra@company.com',
    mobile_number = '+91 97888 99876',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00120'),
    date_of_joining = DATE '2022-05-18',
    confirmation_date = DATE '2023-05-18'
WHERE employee_code = 'EMP00117';

UPDATE employee
SET department_id = (SELECT department_id FROM department WHERE department_code = 'HR'),
    designation_id = (SELECT designation_id FROM designation WHERE designation_name = 'HR Associate'),
    location_id = (SELECT location_id FROM location WHERE city = 'Mumbai'),
    employment_type_id = (SELECT employment_type_id FROM employment_type WHERE employment_type_name = 'Trainee'),
    employee_status = 'ACTIVE',
    display_name = 'Meera Iyer',
    first_name = 'Meera',
    middle_name = NULL,
    last_name = 'Iyer',
    gender = 'FEMALE',
    date_of_birth = DATE '1997-01-16',
    personal_email = 'meera.iyer@gmail.com',
    official_email = 'meera.iyer@company.com',
    mobile_number = '+91 97543 66990',
    reporting_manager_id = (SELECT employee_id FROM employee WHERE employee_code = 'EMP00123'),
    date_of_joining = DATE '2022-05-17',
    confirmation_date = DATE '2023-05-17'
WHERE employee_code = 'EMP00116';

COMMIT;
PROMPT Employee seed generation completed with 1,248 rows.
