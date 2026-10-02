-- =============================================================
-- Oracle HRMS master data seed
-- =============================================================
SET DEFINE OFF

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Executive', 'EXEC', 'Executive leadership and company vision.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Sales & Marketing', 'SALES', 'Revenue generation and brand marketing.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Technology', 'TECH', 'Product, engineering and platform operations.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Finance', 'FIN', 'Finance and accounting operations.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'HR', 'HR', 'People operations and employee lifecycle.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Operations', 'OPS', 'Daily operational execution and support.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Customer Success', 'CS', 'Customer onboarding and support.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Quality', 'QUAL', 'Quality assurance and controls.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Legal', 'LEGAL', 'Legal and compliance.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Procurement', 'PROC', 'Vendor and procurement management.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Administration', 'ADMIN', 'Office and administrative support.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Product', 'PROD', 'Product strategy and roadmap.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Infrastructure', 'INFRA', 'IT infrastructure and security.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO department (department_id, department_name, department_code, description, head_employee_id, parent_department_id, status, created_at, updated_at, created_by, updated_by)
VALUES (department_seq.NEXTVAL, 'Research & Development', 'RND', 'Research, innovation and experimentation.', NULL, NULL, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Mumbai Office', 'Mumbai', 'Maharashtra', 'India', '400001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Bengaluru Office', 'Bengaluru', 'Karnataka', 'India', '560001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Pune Office', 'Pune', 'Maharashtra', 'India', '411001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Hyderabad Office', 'Hyderabad', 'Telangana', 'India', '500001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Delhi Office', 'Delhi', 'Delhi', 'India', '110001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Chennai Office', 'Chennai', 'Tamil Nadu', 'India', '600001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Ahmedabad Office', 'Ahmedabad', 'Gujarat', 'India', '380001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Kolkata Office', 'Kolkata', 'West Bengal', 'India', '700001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Jaipur Office', 'Jaipur', 'Rajasthan', 'India', '302001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Kochi Office', 'Kochi', 'Kerala', 'India', '682001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Noida Office', 'Noida', 'Uttar Pradesh', 'India', '201301', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO location (location_id, location_name, city, state, country, postal_code, status, created_at, updated_at, created_by, updated_by)
VALUES (location_seq.NEXTVAL, 'Gurugram Office', 'Gurugram', 'Haryana', 'India', '122001', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Chief Executive Officer', 'CEO', 'L1', 1, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Department Head', 'DEPT_HEAD', 'L2', 2, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Manager - Sales', 'MGR_SALES', 'L3', 3, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Software Engineer', 'SW_ENGINEER', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'HR Executive', 'HR_EXEC', 'L3', 3, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Business Analyst', 'BA', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Executive - Finance', 'FIN_EXEC', 'L3', 3, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Operations Lead', 'OPS_LEAD', 'L3', 3, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'UI/UX Designer', 'UX_DESIGNER', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Sales Executive', 'SALES_EXEC', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Associate', 'ASSOCIATE', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'HR Associate', 'HR_ASSOC', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Executive', 'EXEC', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Senior Manager', 'SR_MANAGER', 'L3', 3, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO designation (designation_id, designation_name, designation_code, grade, level_no, status, created_at, updated_at, created_by, updated_by)
VALUES (designation_seq.NEXTVAL, 'Team Lead', 'TEAM_LEAD', 'L4', 4, 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

INSERT INTO employment_type (employment_type_id, employment_type_name, description, status, created_at, updated_at, created_by, updated_by)
VALUES (employment_type_seq.NEXTVAL, 'Permanent', 'Full-time permanent employment.', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO employment_type (employment_type_id, employment_type_name, description, status, created_at, updated_at, created_by, updated_by)
VALUES (employment_type_seq.NEXTVAL, 'Contract', 'Fixed-term contract staff.', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO employment_type (employment_type_id, employment_type_name, description, status, created_at, updated_at, created_by, updated_by)
VALUES (employment_type_seq.NEXTVAL, 'Intern', 'Short-term internship arrangement.', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
INSERT INTO employment_type (employment_type_id, employment_type_name, description, status, created_at, updated_at, created_by, updated_by)
VALUES (employment_type_seq.NEXTVAL, 'Trainee', 'Training and learning contract.', 'ACTIVE', SYSTIMESTAMP, SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');

COMMIT;
PROMPT Master data seeded successfully.
