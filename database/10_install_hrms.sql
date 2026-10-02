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

EXIT SUCCESS
