-- =============================================================
-- Oracle HRMS indexes
-- =============================================================

CREATE INDEX idx_employee_status ON employee (employee_status);
CREATE INDEX idx_employee_department ON employee (department_id);
CREATE INDEX idx_employee_designation ON employee (designation_id);
CREATE INDEX idx_employee_location ON employee (location_id);
CREATE INDEX idx_employee_employment_type ON employee (employment_type_id);
CREATE INDEX idx_employee_manager ON employee (reporting_manager_id);
CREATE INDEX idx_employee_joining_date ON employee (date_of_joining);
CREATE INDEX idx_employee_name ON employee (last_name, first_name);

CREATE INDEX idx_employee_status_history_employee ON employee_status_history (employee_id);
CREATE INDEX idx_employee_status_history_date ON employee_status_history (effective_date);
CREATE INDEX idx_employee_lifecycle_employee ON employee_lifecycle_event (employee_id);
CREATE INDEX idx_employee_lifecycle_type ON employee_lifecycle_event (event_type);

CREATE INDEX idx_bulk_action_status ON bulk_action (status);
CREATE INDEX idx_bulk_action_requested_by ON bulk_action (requested_by);
CREATE INDEX idx_bulk_action_employee_bulk ON bulk_action_employee (bulk_action_id);
CREATE INDEX idx_bulk_action_employee_emp ON bulk_action_employee (employee_id);

COMMIT;
PROMPT HRMS indexes created successfully.
