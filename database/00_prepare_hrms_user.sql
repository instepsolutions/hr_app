-- Development-only HRMS application account for Oracle AI Database Free.
-- Run this script as SYS or another account with CREATE USER and GRANT privileges.
-- The password must match backend/src/main/resources/application.properties.

WHENEVER SQLERROR EXIT SQL.SQLCODE
SET SERVEROUTPUT ON

ALTER SESSION SET CONTAINER = FREEPDB1;

DECLARE
    v_user_count NUMBER;
BEGIN
    SELECT COUNT(*)
      INTO v_user_count
      FROM dba_users
     WHERE username = 'HRMS_APP';

    IF v_user_count = 0 THEN
        EXECUTE IMMEDIATE 'CREATE USER HRMS_APP IDENTIFIED BY "HrmsApp#2025" DEFAULT TABLESPACE USERS TEMPORARY TABLESPACE TEMP QUOTA UNLIMITED ON USERS';
        DBMS_OUTPUT.PUT_LINE('Created HRMS_APP.');
    ELSE
        EXECUTE IMMEDIATE 'ALTER USER HRMS_APP IDENTIFIED BY "HrmsApp#2025" ACCOUNT UNLOCK';
        EXECUTE IMMEDIATE 'ALTER USER HRMS_APP QUOTA UNLIMITED ON USERS';
        DBMS_OUTPUT.PUT_LINE('Unlocked HRMS_APP and aligned its development password with Spring configuration.');
    END IF;
END;
/

GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE, CREATE VIEW TO HRMS_APP;

SELECT username, account_status, default_tablespace
  FROM dba_users
 WHERE username = 'HRMS_APP';

SELECT COUNT(*) AS existing_hrms_tables
  FROM dba_tables
 WHERE owner = 'HRMS_APP';

EXIT SUCCESS
