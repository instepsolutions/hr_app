-- PMS development seed. Built from existing employees; every total below reconciles by construction.
-- Goals: 1,512 = 876 completed + 432 in progress + 156 behind + 48 not started.
-- Alignment 512/476/312/212 and categories 432/314/266/214/162/124 are exact permutations of 1..1512.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

DECLARE
    v_cnt NUMBER;
    TYPE t_names IS VARRAY(6) OF VARCHAR2(120);
    v_cat t_names := t_names('Revenue Growth','Customer Satisfaction','Operational Excellence','People Development','Innovation & Technology','Compliance');
    v_kra t_names := t_names('Revenue & Business Growth','Customer Experience','Operational Efficiency','Talent & Capability','Innovation & Digital','Risk & Compliance');
    v_kpi t_names := t_names('Net new revenue|Win rate','Customer satisfaction score|Retention rate','Cost reduction|Turnaround time','Training hours per employee|Engagement score','Projects delivered|System adoption rate','Audit findings closed|Policy training coverage');
    v_unit t_names := t_names('INR Lakh|%','%|%','%|Days','Hours|Score','Count|%','Count|%');
    v_kra_id NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM pms_goal_category;
    IF v_cnt = 0 THEN
        FOR i IN 1..6 LOOP
            INSERT INTO pms_goal_category (category_id, category_name, created_by, updated_by)
            VALUES (pms_goal_category_seq.NEXTVAL, v_cat(i), 'SYSTEM', 'SYSTEM');
            INSERT INTO pms_kra (kra_id, kra_name, category_id, created_by, updated_by)
            VALUES (pms_kra_seq.NEXTVAL, v_kra(i), (SELECT category_id FROM pms_goal_category WHERE category_name = v_cat(i)), 'SYSTEM', 'SYSTEM')
            RETURNING kra_id INTO v_kra_id;
            FOR j IN 1..2 LOOP
                INSERT INTO pms_kpi (kpi_id, kra_id, kpi_name, unit, target_value, created_by, updated_by)
                VALUES (pms_kpi_seq.NEXTVAL, v_kra_id, REGEXP_SUBSTR(v_kpi(i), '[^|]+', 1, j), REGEXP_SUBSTR(v_unit(i), '[^|]+', 1, j), 100, 'SYSTEM', 'SYSTEM');
            END LOOP;
        END LOOP;
    END IF;
END;
/

DECLARE
    v_cnt NUMBER;
    TYPE t_num IS TABLE OF NUMBER INDEX BY PLS_INTEGER;
    TYPE t_str IS TABLE OF VARCHAR2(60) INDEX BY PLS_INTEGER;
    TYPE t_groups IS TABLE OF t_num INDEX BY PLS_INTEGER;
    TYPE t_ints IS VARRAY(6) OF PLS_INTEGER;
    TYPE t_titles IS VARRAY(6) OF VARCHAR2(400);
    v_emps t_groups;
    v_empty t_num;
    v_cat_id t_num;
    v_kra_id t_num;
    v_kpi_id t_num;
    v_kpi_unit t_str;
    v_cat_name t_str;
    v_total t_ints := t_ints(312, 204, 276, 298, 198, 224);
    v_done t_ints := t_ints(186, 122, 162, 172, 108, 126);
    v_prog t_ints := t_ints(92, 58, 78, 88, 68, 48);
    v_behind t_ints := t_ints(26, 18, 28, 30, 18, 36);
    v_titles t_titles := t_titles(
        'Increase revenue from new customers|Expand enterprise account portfolio|Grow recurring revenue by 15%|Launch regional sales campaign',
        'Increase customer retention rate|Improve customer satisfaction score|Reduce average support resolution time|Launch customer feedback program',
        'Reduce operational costs by 10%|Complete process automation rollout|Improve service delivery turnaround|Standardize operating procedures',
        'Enhance employee training program|Improve employee engagement score|Complete leadership development track|Increase internal mobility rate',
        'Implement new HRMS module|Complete system migration project|Deliver analytics platform upgrade|Pilot AI-assisted workflow tool',
        'Achieve audit readiness certification|Complete policy compliance training|Close open compliance findings|Update data privacy controls');
    v_n PLS_INTEGER := 0;
    v_gi PLS_INTEGER;
    v_emp NUMBER;
    v_status VARCHAR2(20);
    v_type VARCHAR2(20);
    v_rank PLS_INTEGER;
    v_ci PLS_INTEGER;
    v_ki PLS_INTEGER;
    v_c PLS_INTEGER;
    v_start DATE;
    v_due DATE;
    v_progress NUMBER;
    v_completed TIMESTAMP;
    v_created TIMESTAMP;
    v_title VARCHAR2(200);
    v_priority VARCHAR2(20);
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM pms_goal;
    IF v_cnt = 0 THEN
        FOR i IN 1..6 LOOP
            v_cat_name(i) := CASE i WHEN 1 THEN 'Revenue Growth' WHEN 2 THEN 'Customer Satisfaction' WHEN 3 THEN 'Operational Excellence' WHEN 4 THEN 'People Development' WHEN 5 THEN 'Innovation & Technology' ELSE 'Compliance' END;
            SELECT category_id INTO v_cat_id(i) FROM pms_goal_category WHERE category_name = v_cat_name(i);
            SELECT kra_id INTO v_kra_id(i) FROM pms_kra WHERE category_id = v_cat_id(i);
            FOR j IN 1..2 LOOP
                SELECT kpi_id, unit INTO v_kpi_id((i - 1) * 2 + j), v_kpi_unit((i - 1) * 2 + j)
                  FROM (SELECT kpi_id, unit, ROW_NUMBER() OVER (ORDER BY kpi_id) rn FROM pms_kpi WHERE kra_id = v_kra_id(i))
                 WHERE rn = j;
            END LOOP;
        END LOOP;

        FOR i IN 1..6 LOOP v_emps(i) := v_empty; END LOOP;
        FOR r IN (
            SELECT e.employee_id,
                   CASE COALESCE(dg.group_name, 'Others') WHEN 'IT' THEN 1 WHEN 'HR' THEN 2 WHEN 'Sales & Marketing' THEN 3 WHEN 'Operations' THEN 4 WHEN 'Finance' THEN 5 ELSE 6 END gi
              FROM employee e LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id
             WHERE e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED') AND e.employee_code <> 'EMP00001'
             ORDER BY e.employee_id
        ) LOOP
            v_emps(r.gi)(v_emps(r.gi).COUNT + 1) := r.employee_id;
        END LOOP;

        FOR g IN 1..6 LOOP
            FOR k IN 1..v_total(g) LOOP
                v_n := v_n + 1;
                v_status := CASE
                    WHEN k <= v_done(g) THEN 'COMPLETED'
                    WHEN k <= v_done(g) + v_prog(g) THEN 'IN_PROGRESS'
                    WHEN k <= v_done(g) + v_prog(g) + v_behind(g) THEN 'BEHIND'
                    ELSE 'NOT_STARTED' END;
                v_emp := v_emps(g)(MOD(k * 13 + g, v_emps(g).COUNT) + 1);
                v_rank := MOD(v_n * 7919, 1512);
                v_type := CASE WHEN v_rank < 512 THEN 'COMPANY' WHEN v_rank < 988 THEN 'DEPARTMENT' WHEN v_rank < 1300 THEN 'TEAM' ELSE 'INDIVIDUAL' END;
                v_rank := MOD(v_n * 6271, 1512);
                v_ci := CASE WHEN v_rank < 432 THEN 1 WHEN v_rank < 746 THEN 2 WHEN v_rank < 1012 THEN 3 WHEN v_rank < 1226 THEN 4 WHEN v_rank < 1388 THEN 5 ELSE 6 END;
                v_ki := (v_ci - 1) * 2 + 1 + MOD(v_n, 2);
                v_c := MOD(v_n * 53, 150);
                v_start := TRUNC(SYSDATE) - v_c;
                v_due := v_start + 45 + MOD(v_n * 11, 120);
                v_progress := CASE v_status WHEN 'COMPLETED' THEN 100 WHEN 'NOT_STARTED' THEN 0 WHEN 'IN_PROGRESS' THEN 5 + MOD(v_n * 17, 60) ELSE 5 + MOD(v_n * 19, 45) END;
                v_created := CAST(v_start + 9 / 24 AS TIMESTAMP);
                v_completed := CASE WHEN v_status = 'COMPLETED' THEN CAST(TRUNC(SYSDATE) - MOD(v_n * 37, v_c + 1) + 15 / 24 AS TIMESTAMP) END;
                v_title := REGEXP_SUBSTR(v_titles(v_ci), '[^|]+', 1, 1 + MOD(v_n, 4));
                v_priority := CASE MOD(v_n * 5, 4) WHEN 0 THEN 'LOW' WHEN 1 THEN 'MEDIUM' WHEN 2 THEN 'HIGH' ELSE 'CRITICAL' END;

                INSERT INTO pms_goal (goal_id, title, description, employee_id, department_id, category_id, goal_type, kra_id, kpi_id,
                                      weightage, target_value, unit, progress_percent, priority, manager_id, status, start_date, due_date,
                                      completed_at, is_archived, created_at, updated_at, created_by, updated_by)
                VALUES (pms_goal_seq.NEXTVAL, v_title, v_title || ' for the current performance cycle.', v_emp,
                        (SELECT department_id FROM employee WHERE employee_id = v_emp), v_cat_id(v_ci), v_type, v_kra_id(v_ci), v_kpi_id(v_ki),
                        5 + MOD(v_n * 3, 26), 10 + MOD(v_n * 7, 90), v_kpi_unit(v_ki), v_progress, v_priority,
                        (SELECT reporting_manager_id FROM employee WHERE employee_id = v_emp), v_status, v_start, v_due,
                        v_completed, 0, v_created, v_created, 'SYSTEM', 'SYSTEM');
            END LOOP;
        END LOOP;

        -- Archived goals sit outside the 1,512 active total.
        FOR a IN 1..24 LOOP
            v_n := 1512 + a;
            v_gi := MOD(a, 6) + 1;
            v_emp := v_emps(v_gi)(MOD(a * 7, v_emps(v_gi).COUNT) + 1);
            v_ci := MOD(a, 6) + 1;
            v_ki := (v_ci - 1) * 2 + 1 + MOD(a, 2);
            v_start := TRUNC(SYSDATE) - (150 + MOD(a, 15));
            v_title := REGEXP_SUBSTR(v_titles(v_ci), '[^|]+', 1, 1 + MOD(a, 4));
            INSERT INTO pms_goal (goal_id, title, description, employee_id, department_id, category_id, goal_type, kra_id, kpi_id,
                                  weightage, target_value, unit, progress_percent, priority, manager_id, status, start_date, due_date,
                                  completed_at, is_archived, archived_at, archived_by, archive_reason, created_at, updated_at, created_by, updated_by)
            VALUES (pms_goal_seq.NEXTVAL, v_title, v_title || ' (previous cycle).', v_emp,
                    (SELECT department_id FROM employee WHERE employee_id = v_emp), v_cat_id(v_ci), 'INDIVIDUAL', v_kra_id(v_ci), v_kpi_id(v_ki),
                    10, 50, v_kpi_unit(v_ki), 100, 'MEDIUM', (SELECT reporting_manager_id FROM employee WHERE employee_id = v_emp),
                    'COMPLETED', v_start, v_start + 60, CAST(v_start + 40 AS TIMESTAMP), 1,
                    SYSTIMESTAMP - NUMTODSINTERVAL(a, 'DAY'), 'admin', 'Performance cycle closed',
                    CAST(v_start + 9 / 24 AS TIMESTAMP), SYSTIMESTAMP, 'SYSTEM', 'SYSTEM');
        END LOOP;
    END IF;
END;
/

-- The development admin login owns four goals so "My goals" has real content.
DECLARE
    v_emp NUMBER;
    v_mgr NUMBER;
    v_dept NUMBER;
    v_owned NUMBER;
    PROCEDURE assign_goal(p_title VARCHAR2, p_category VARCHAR2, p_status VARCHAR2, p_progress NUMBER, p_due_offset NUMBER) IS
        v_goal NUMBER;
    BEGIN
        SELECT MIN(g.goal_id) INTO v_goal
          FROM pms_goal g JOIN pms_goal_category c ON c.category_id = g.category_id
         WHERE g.is_archived = 0 AND g.employee_id <> v_emp AND g.department_id = v_dept
           AND g.status = p_status AND c.category_name = p_category;
        IF v_goal IS NULL THEN
            RAISE_APPLICATION_ERROR(-20010, 'No seed goal available for ' || p_title);
        END IF;
        UPDATE pms_goal
           SET employee_id = v_emp, manager_id = v_mgr, title = p_title,
               description = p_title || ' for the current performance cycle.',
               progress_percent = p_progress, due_date = TRUNC(SYSDATE) + p_due_offset,
               updated_at = SYSTIMESTAMP + NUMTODSINTERVAL(v_goal, 'SECOND')
         WHERE goal_id = v_goal;
    END;
BEGIN
    SELECT employee_id, reporting_manager_id, department_id INTO v_emp, v_mgr, v_dept FROM employee WHERE employee_code = 'EMP00001';
    SELECT COUNT(*) INTO v_owned FROM pms_goal WHERE employee_id = v_emp;
    IF v_owned = 0 THEN
        assign_goal('Increase Customer Retention Rate', 'Customer Satisfaction', 'IN_PROGRESS', 75, 26);
        assign_goal('Reduce Operational Costs by 10%', 'Operational Excellence', 'COMPLETED', 100, 26);
        assign_goal('Implement New HRMS Module', 'Innovation & Technology', 'IN_PROGRESS', 60, 40);
        assign_goal('Improve Employee Engagement Score', 'People Development', 'BEHIND', 30, 26);
    END IF;
    UPDATE app_user SET employee_id = v_emp WHERE username = 'admin' AND employee_id IS NULL;
END;
/

-- Each goal aligns to a goal one level above it (Company > Department > Team > Individual).
UPDATE pms_goal g
   SET alignment_goal_id = (SELECT MIN(p.goal_id) FROM pms_goal p
                             WHERE p.is_archived = 0 AND p.category_id = g.category_id
                               AND p.goal_type = CASE g.goal_type WHEN 'DEPARTMENT' THEN 'COMPANY' WHEN 'TEAM' THEN 'DEPARTMENT' ELSE 'TEAM' END
                               AND MOD(p.goal_id, 13) = MOD(g.goal_id, 13))
 WHERE g.is_archived = 0 AND g.goal_type <> 'COMPANY' AND g.alignment_goal_id IS NULL;

-- Daily snapshots (121 days) drive trends and period-over-period deltas from one source.
DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM pms_goal_progress;
    IF v_cnt = 0 THEN
        INSERT INTO pms_goal_progress (progress_id, goal_id, recorded_on, progress_percent, status, updated_by)
        SELECT pms_goal_progress_seq.NEXTVAL, x.goal_id, x.day_date, x.p,
               CASE WHEN x.p >= 100 THEN 'COMPLETED' WHEN x.p <= 0 THEN 'NOT_STARTED' WHEN x.status = 'BEHIND' THEN 'BEHIND' ELSE 'IN_PROGRESS' END,
               'SYSTEM'
          FROM (
                SELECT g.goal_id, g.status, TRUNC(SYSDATE) - d.n AS day_date,
                       CASE WHEN d.n = 0 THEN g.progress_percent
                            WHEN g.status = 'COMPLETED' THEN
                                 CASE WHEN TRUNC(SYSDATE) - d.n >= TRUNC(g.completed_at) THEN 100
                                      ELSE ROUND(95 * (TRUNC(SYSDATE) - d.n - TRUNC(g.created_at)) / GREATEST(1, TRUNC(g.completed_at) - TRUNC(g.created_at))) END
                            ELSE ROUND(g.progress_percent * (TRUNC(SYSDATE) - d.n - TRUNC(g.created_at)) / GREATEST(1, TRUNC(SYSDATE) - TRUNC(g.created_at))) END AS p
                  FROM pms_goal g
                  JOIN (SELECT LEVEL - 1 AS n FROM dual CONNECT BY LEVEL <= 121) d ON TRUNC(SYSDATE) - d.n >= TRUNC(g.created_at)
                 WHERE g.is_archived = 0
          ) x;
    END IF;
END;
/

DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM pms_appraisal_cycle;
    IF v_cnt = 0 THEN
        INSERT INTO pms_appraisal_cycle (cycle_id, cycle_name, dept_group, start_date, due_date, created_by, updated_by) VALUES (pms_appraisal_cycle_seq.NEXTVAL, 'Sales & Marketing Appraisal', 'Sales & Marketing', TRUNC(SYSDATE) - 60, TRUNC(SYSDATE) + 27, 'SYSTEM', 'SYSTEM');
        INSERT INTO pms_appraisal_cycle (cycle_id, cycle_name, dept_group, start_date, due_date, created_by, updated_by) VALUES (pms_appraisal_cycle_seq.NEXTVAL, 'IT Department Appraisal', 'IT', TRUNC(SYSDATE) - 60, TRUNC(SYSDATE) + 32, 'SYSTEM', 'SYSTEM');
        INSERT INTO pms_appraisal_cycle (cycle_id, cycle_name, dept_group, start_date, due_date, created_by, updated_by) VALUES (pms_appraisal_cycle_seq.NEXTVAL, 'Operations Appraisal', 'Operations', TRUNC(SYSDATE) - 60, TRUNC(SYSDATE) + 37, 'SYSTEM', 'SYSTEM');
        INSERT INTO pms_appraisal_cycle (cycle_id, cycle_name, dept_group, start_date, due_date, created_by, updated_by) VALUES (pms_appraisal_cycle_seq.NEXTVAL, 'Finance Appraisal', 'Finance', TRUNC(SYSDATE) - 60, TRUNC(SYSDATE) + 43, 'SYSTEM', 'SYSTEM');
        INSERT INTO pms_appraisal_cycle (cycle_id, cycle_name, dept_group, start_date, due_date, created_by, updated_by) VALUES (pms_appraisal_cycle_seq.NEXTVAL, 'HR Appraisal', 'HR', TRUNC(SYSDATE) - 60, TRUNC(SYSDATE) + 48, 'SYSTEM', 'SYSTEM');
        INSERT INTO pms_appraisal_cycle (cycle_id, cycle_name, dept_group, start_date, due_date, created_by, updated_by) VALUES (pms_appraisal_cycle_seq.NEXTVAL, 'Corporate Functions Appraisal', 'Others', TRUNC(SYSDATE) - 60, TRUNC(SYSDATE) + 55, 'SYSTEM', 'SYSTEM');

        INSERT INTO pms_appraisal (appraisal_id, cycle_id, employee_id, stage, final_rating, completed_at, created_by, updated_by)
        SELECT pms_appraisal_seq.NEXTVAL, c.cycle_id, x.employee_id, x.stage,
               CASE WHEN x.stage = 'COMPLETED' THEN x.rating END,
               CASE WHEN x.stage = 'COMPLETED' THEN TRUNC(SYSDATE) - MOD(x.employee_id * 13, 90) END,
               'SYSTEM', 'SYSTEM'
          FROM (
                SELECT e.employee_id, COALESCE(dg.group_name, 'Others') grp,
                       CASE WHEN MOD(e.employee_id * 7919, 1000) / 10 < 25 THEN 'SELF_APPRAISAL'
                            WHEN MOD(e.employee_id * 7919, 1000) / 10 < 47 THEN 'MANAGER_REVIEW'
                            WHEN MOD(e.employee_id * 7919, 1000) / 10 < 61 THEN 'HR_REVIEW'
                            WHEN MOD(e.employee_id * 7919, 1000) / 10 < 88 THEN 'COMPLETED'
                            ELSE 'YET_TO_START' END stage,
                       LEAST(5, GREATEST(1, ROUND(
                           CASE WHEN MOD(e.employee_id * 104729, 1000) / 10 < 2.4 THEN 1 + MOD(e.employee_id * 31, 100) / 100 * 0.49
                                WHEN MOD(e.employee_id * 104729, 1000) / 10 < 9.3 THEN 1.5 + MOD(e.employee_id * 31, 100) / 100 * 0.99
                                WHEN MOD(e.employee_id * 104729, 1000) / 10 < 31.8 THEN 2.5 + MOD(e.employee_id * 31, 100) / 100 * 0.99
                                WHEN MOD(e.employee_id * 104729, 1000) / 10 < 71.9 THEN 3.5 + MOD(e.employee_id * 31, 100) / 100 * 0.99
                                ELSE 4.5 + MOD(e.employee_id * 31, 100) / 100 * 0.5 END
                           + CASE COALESCE(dg.group_name, 'Others') WHEN 'IT' THEN 0.2 WHEN 'HR' THEN 0.1 WHEN 'Sales & Marketing' THEN 0.05 WHEN 'Finance' THEN -0.05 WHEN 'Others' THEN -0.15 ELSE 0 END, 2))) rating
                  FROM employee e LEFT JOIN pms_department_group_v dg ON dg.department_id = e.department_id
                 WHERE e.employee_status NOT IN ('EXITED','INACTIVE','RESIGNED')
          ) x
          JOIN pms_appraisal_cycle c ON c.dept_group = x.grp;
    END IF;
END;
/

DECLARE
    v_cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO v_cnt FROM pms_pip;
    IF v_cnt = 0 THEN
        INSERT INTO pms_pip (pip_id, employee_id, manager_id, status, outcome, start_date, end_date, reason, created_by, updated_by)
        SELECT pms_pip_seq.NEXTVAL, y.employee_id, y.reporting_manager_id,
               CASE WHEN y.rn <= 22 THEN 'ACTIVE' ELSE 'COMPLETED' END,
               CASE WHEN y.rn <= 22 THEN NULL WHEN y.rn <= 32 THEN 'IMPROVED' ELSE 'NOT_IMPROVED' END,
               y.start_date, y.start_date + 90, 'Sustained gap against agreed performance goals.', 'SYSTEM', 'SYSTEM'
          FROM (
                SELECT x.employee_id, x.reporting_manager_id, x.rn,
                       CASE WHEN x.rn <= 22 THEN TRUNC(SYSDATE) - 20 - x.rn ELSE TRUNC(SYSDATE) - 120 - x.rn END start_date
                  FROM (SELECT employee_id, reporting_manager_id,
                               ROW_NUMBER() OVER (ORDER BY MOD(employee_id * 7919, 100003)) rn
                          FROM employee WHERE employee_status = 'ACTIVE') x
                 WHERE x.rn <= 37
          ) y;
    END IF;
END;
/

COMMIT;
PROMPT PMS seed data prepared.
