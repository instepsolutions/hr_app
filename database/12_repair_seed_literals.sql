-- Repair the two development seed values affected by SQL*Plus ampersand substitution.
SET DEFINE OFF
WHENEVER SQLERROR EXIT SQL.SQLCODE

UPDATE department
   SET department_name = 'Sales & Marketing'
 WHERE department_code = 'SALES';

UPDATE department
   SET department_name = 'Research & Development'
 WHERE department_code = 'RND';

UPDATE bulk_action_employee
   SET old_value = 'Sales & Marketing'
 WHERE old_value IN ('Sales &', 'Sales Marketing');

UPDATE bulk_action_employee
   SET new_value = 'Sales & Marketing'
 WHERE new_value IN ('Sales &', 'Sales Marketing');

COMMIT;

SELECT department_code, department_name
  FROM department
 WHERE department_code IN ('SALES', 'RND')
 ORDER BY department_code;

EXIT SUCCESS
