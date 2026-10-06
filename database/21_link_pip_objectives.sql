-- Keep PIP improvement objectives linked to the existing PMS goal framework.
WHENEVER SQLERROR EXIT SQL.SQLCODE
SET DEFINE OFF

ALTER TABLE pms_pip_objective ADD (
    goal_id NUMBER(12),
    kra_id NUMBER(10),
    kpi_id NUMBER(10)
);

ALTER TABLE pms_pip_objective ADD CONSTRAINT fk_pms_pip_objective_goal
    FOREIGN KEY (goal_id) REFERENCES pms_goal(goal_id) ON DELETE SET NULL;
ALTER TABLE pms_pip_objective ADD CONSTRAINT fk_pms_pip_objective_kra
    FOREIGN KEY (kra_id) REFERENCES pms_kra(kra_id) ON DELETE SET NULL;
ALTER TABLE pms_pip_objective ADD CONSTRAINT fk_pms_pip_objective_kpi
    FOREIGN KEY (kpi_id) REFERENCES pms_kpi(kpi_id) ON DELETE SET NULL;

CREATE INDEX idx_pms_pip_objective_goal ON pms_pip_objective (goal_id);
CREATE INDEX idx_pms_pip_objective_kra ON pms_pip_objective (kra_id);
CREATE INDEX idx_pms_pip_objective_kpi ON pms_pip_objective (kpi_id);

COMMIT;
PROMPT PIP objectives linked to goals, KRAs, and KPIs.