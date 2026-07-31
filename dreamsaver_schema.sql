-- ================================================================
-- DreamSaver Personal Finance App — PostgreSQL Schema
-- Version: Final (9 Tables)
-- ================================================================

-- ==================================
-- TABLE 1: USERS
-- ==================================
CREATE TABLE IF NOT EXISTS users (
    id              TEXT         PRIMARY KEY,
    display_name    VARCHAR(255) NOT NULL DEFAULT 'User',
    email           VARCHAR(255) NOT NULL UNIQUE,
    phone           VARCHAR(20)  DEFAULT '',
    age             INTEGER      DEFAULT NULL
                                 CHECK (age IS NULL OR (age >= 13 AND age <= 120)),
    fcm_token       TEXT         DEFAULT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);


-- ==================================
-- TABLE 2: EXPENSES
-- ==================================
CREATE TABLE IF NOT EXISTS expenses (
    id              SERIAL        PRIMARY KEY,
    user_id         TEXT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category        VARCHAR(100)  NOT NULL DEFAULT 'Other',
    amount          DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    description     TEXT          DEFAULT '',
    date            BIGINT        NOT NULL,
    time            VARCHAR(10)   DEFAULT '00:00',
    category_icon   INTEGER       DEFAULT 0,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_expenses_user_date     ON expenses(user_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_expenses_user_category ON expenses(user_id, category);


-- ==================================
-- TABLE 3: INCOMES
-- ==================================
CREATE TABLE IF NOT EXISTS incomes (
    id              SERIAL        PRIMARY KEY,
    user_id         TEXT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    source          VARCHAR(100)  NOT NULL DEFAULT '',
    amount          DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    note            TEXT          DEFAULT '',
    date            BIGINT        NOT NULL,
    time            VARCHAR(10)   DEFAULT '00:00',
    source_icon     INTEGER       DEFAULT 0,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_incomes_user_date ON incomes(user_id, date DESC);


-- ==================================
-- TABLE 4: BILLS
-- ==================================
CREATE TABLE IF NOT EXISTS bills (
    id              SERIAL        PRIMARY KEY,
    user_id         TEXT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name            VARCHAR(255)  NOT NULL DEFAULT '',
    description     TEXT          DEFAULT '',
    amount          DECIMAL(12,2) NOT NULL CHECK (amount >= 0),
    due_date        BIGINT        NOT NULL,
    category        VARCHAR(100)  NOT NULL DEFAULT '',
    category_icon   INTEGER       DEFAULT 0,
    status          VARCHAR(50)   NOT NULL DEFAULT 'pending'
                                  CHECK (status IN ('urgent','due_soon','pending','paid')),
    indicator_color INTEGER       DEFAULT 0,
    is_recurring    BOOLEAN       NOT NULL DEFAULT FALSE,
    is_deleted      BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bills_user_due    ON bills(user_id, due_date ASC);
CREATE INDEX IF NOT EXISTS idx_bills_user_status ON bills(user_id, status);


-- ==================================
-- TABLE 5: GOALS
-- ==================================
CREATE TABLE IF NOT EXISTS goals (
    id                   SERIAL        PRIMARY KEY,
    user_id              TEXT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                 VARCHAR(255)  NOT NULL DEFAULT '',
    description          TEXT          DEFAULT '',
    target_amount        DECIMAL(12,2) NOT NULL CHECK (target_amount > 0),
    current_amount       DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (current_amount >= 0),
    added_savings_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    target_date          BIGINT        NOT NULL,
    category             VARCHAR(100)  NOT NULL DEFAULT '',
    category_icon        INTEGER       DEFAULT 0,
    progress_circle_bg   INTEGER       DEFAULT 0,
    is_deleted           BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_goals_user_target ON goals(user_id, target_date ASC);


-- ==================================
-- TABLE 6: NOTIFICATIONS
-- ==================================
CREATE TABLE IF NOT EXISTS notifications (
    id              SERIAL        PRIMARY KEY,
    user_id         TEXT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title           VARCHAR(255)  NOT NULL,
    message         TEXT          NOT NULL,
    type            VARCHAR(50)   NOT NULL DEFAULT 'general'
                                  CHECK (type IN (
                                      'bill_reminder',
                                      'goal_reminder',
                                      'goal_progress_nudge',
                                      'budget_alert',
                                      'general'
                                  )),
    reference_type  VARCHAR(20)   DEFAULT NULL,
    reference_id    INTEGER       DEFAULT NULL,
    is_read         BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_read ON notifications(user_id, is_read, created_at DESC);


-- ==================================
-- TABLE 7: BUDGET_LIMITS
-- ==================================
CREATE TABLE IF NOT EXISTS budget_limits (
    id              SERIAL        PRIMARY KEY,
    user_id         TEXT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    category        VARCHAR(100)  NOT NULL,
    limit_amount    DECIMAL(12,2) NOT NULL CHECK (limit_amount > 0),
    month_year      VARCHAR(7)    NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, category, month_year)
);

CREATE INDEX IF NOT EXISTS idx_budget_limits_user_month ON budget_limits(user_id, month_year);


-- ==================================
-- TABLE 8: FORECASTS
-- ==================================
CREATE TABLE IF NOT EXISTS forecasts (
    id                  SERIAL        PRIMARY KEY,
    user_id             TEXT          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    forecast_month      VARCHAR(7)    NOT NULL,
    predicted_expense   DECIMAL(12,2) DEFAULT 0,
    predicted_income    DECIMAL(12,2) DEFAULT 0,
    predicted_bills     DECIMAL(12,2) DEFAULT 0,
    net_cash_flow       DECIMAL(12,2) DEFAULT 0,
    category_breakdown  JSONB         DEFAULT '{}',
    model_version       VARCHAR(100)  DEFAULT 'chronos-t5-small',
    generated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, forecast_month)
);

CREATE INDEX IF NOT EXISTS idx_forecasts_user_month ON forecasts(user_id, forecast_month DESC);


-- ==================================
-- AUTO-UPDATE TRIGGER (updated_at)
-- ==================================
CREATE OR REPLACE FUNCTION trigger_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS trg_expenses_updated_at ON expenses;
CREATE TRIGGER trg_expenses_updated_at
    BEFORE UPDATE ON expenses FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS trg_incomes_updated_at ON incomes;
CREATE TRIGGER trg_incomes_updated_at
    BEFORE UPDATE ON incomes FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS trg_bills_updated_at ON bills;
CREATE TRIGGER trg_bills_updated_at
    BEFORE UPDATE ON bills FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS trg_goals_updated_at ON goals;
CREATE TRIGGER trg_goals_updated_at
    BEFORE UPDATE ON goals FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();

DROP TRIGGER IF EXISTS trg_budget_limits_updated_at ON budget_limits;
CREATE TRIGGER trg_budget_limits_updated_at
    BEFORE UPDATE ON budget_limits FOR EACH ROW EXECUTE FUNCTION trigger_set_updated_at();


-- ==================================
-- USEFUL VIEWS
-- ==================================
CREATE OR REPLACE VIEW v_monthly_expense_summary AS
SELECT user_id,
       TO_CHAR(TO_TIMESTAMP(date / 1000.0), 'YYYY-MM') AS month,
       category,
       COUNT(*)    AS transaction_count,
       SUM(amount) AS total_amount
FROM expenses
WHERE is_deleted = FALSE
GROUP BY user_id, TO_CHAR(TO_TIMESTAMP(date / 1000.0), 'YYYY-MM'), category;

CREATE OR REPLACE VIEW v_goal_progress AS
SELECT id, user_id, name, target_amount, current_amount,
       ROUND((current_amount / NULLIF(target_amount, 0)) * 100, 1) AS progress_percent
FROM goals
WHERE is_deleted = FALSE;

CREATE OR REPLACE VIEW v_budget_vs_actual AS
SELECT bl.user_id, bl.month_year, bl.category,
       bl.limit_amount,
       COALESCE(SUM(e.amount), 0) AS actual_spent,
       bl.limit_amount - COALESCE(SUM(e.amount), 0) AS remaining,
       CASE
           WHEN COALESCE(SUM(e.amount), 0) >= bl.limit_amount THEN 'exceeded'
           WHEN COALESCE(SUM(e.amount), 0) >= bl.limit_amount * 0.8 THEN 'warning'
           ELSE 'ok'
       END AS status
FROM budget_limits bl
LEFT JOIN expenses e ON e.user_id = bl.user_id
    AND e.category = bl.category
    AND TO_CHAR(TO_TIMESTAMP(e.date / 1000.0), 'YYYY-MM') = bl.month_year
    AND e.is_deleted = FALSE
GROUP BY bl.id, bl.user_id, bl.month_year, bl.category, bl.limit_amount;


-- ==================================
-- TABLE 9: AUDIT LOGS
-- ==================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          SERIAL       PRIMARY KEY,
    user_id     TEXT         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    entity_type VARCHAR(50)  NOT NULL,
    action      VARCHAR(50)  NOT NULL,
    entity_id   VARCHAR(255),
    details     TEXT,
    timestamp   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user ON audit_logs(user_id, timestamp DESC);

-- Enable RLS to secure the table from public API access
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Deny All" ON audit_logs FOR ALL TO public USING (false);
