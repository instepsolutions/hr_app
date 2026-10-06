-- Run from the repository root after 00_prepare_hrms_user.sql.
-- Development seed data is stored in Oracle, never in the React application.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

@database/01_create_sequences.sql
@database/02_create_tables.sql
@database/03_create_constraints.sql
@database/04_create_indexes.sql
@database/05_seed_master_data.sql
@database/06_seed_employee_data.sql
@database/07_seed_lifecycle_data.sql
@database/08_seed_bulk_action_data.sql
@database/09_seed_admin.sql
@database/13_create_dashboard_tables.sql
@database/14_seed_dashboard_widgets.sql
@database/15_create_pms_tables.sql
@database/16_seed_pms_data.sql
@database/18_create_pms_workspace.sql
@database/19_create_pms_reviews.sql
@database/20_create_pms_case_workflows.sql
@database/21_link_pip_objectives.sql

EXIT SUCCESS
