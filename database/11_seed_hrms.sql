-- Resume a fresh HRMS schema after 00_prepare_hrms_user.sql and 01-03.
-- Run from the repository root as HRMS_APP.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

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

EXIT SUCCESS
