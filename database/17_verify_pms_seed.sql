-- Read-only reconciliation checks for the PMS seed.
SET PAGESIZE 100
SET LINESIZE 140
COLUMN grp FORMAT A20

SELECT 'goals_active' AS metric, COUNT(*) AS value FROM pms_goal WHERE is_archived = 0
UNION ALL SELECT 'goals_archived', COUNT(*) FROM pms_goal WHERE is_archived = 1
UNION ALL SELECT 'status_' || status, COUNT(*) FROM pms_goal WHERE is_archived = 0 GROUP BY status
UNION ALL SELECT 'type_' || goal_type, COUNT(*) FROM pms_goal WHERE is_archived = 0 GROUP BY goal_type
UNION ALL SELECT 'snapshots_today', COUNT(*) FROM pms_goal_progress WHERE recorded_on = TRUNC(SYSDATE)
UNION ALL SELECT 'snapshots_total', COUNT(*) FROM pms_goal_progress
UNION ALL SELECT 'appraisal_' || stage, COUNT(*) FROM pms_appraisal GROUP BY stage
UNION ALL SELECT 'pip_' || status || '_' || NVL(outcome, '-'), COUNT(*) FROM pms_pip GROUP BY status, outcome
UNION ALL SELECT 'admin_goals', COUNT(*) FROM pms_goal g JOIN app_user u ON u.employee_id = g.employee_id WHERE u.username = 'admin'
UNION ALL SELECT 'orphan_parent', COUNT(*) FROM pms_goal g WHERE g.alignment_goal_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM pms_goal p WHERE p.goal_id = g.alignment_goal_id);

SELECT COALESCE(dg.group_name, 'Others') AS grp, COUNT(*) AS total,
       SUM(CASE WHEN g.status = 'COMPLETED' THEN 1 ELSE 0 END) AS completed,
       SUM(CASE WHEN g.status = 'IN_PROGRESS' THEN 1 ELSE 0 END) AS in_progress,
       SUM(CASE WHEN g.status = 'BEHIND' THEN 1 ELSE 0 END) AS behind,
       SUM(CASE WHEN g.status = 'NOT_STARTED' THEN 1 ELSE 0 END) AS not_started,
       ROUND(AVG(g.progress_percent), 1) AS avg_progress
  FROM pms_goal g LEFT JOIN pms_department_group_v dg ON dg.department_id = g.department_id
 WHERE g.is_archived = 0
 GROUP BY COALESCE(dg.group_name, 'Others')
 ORDER BY 1;

SELECT ROUND(AVG(progress_percent), 1) AS overall_avg_progress FROM pms_goal WHERE is_archived = 0;
EXIT SUCCESS
