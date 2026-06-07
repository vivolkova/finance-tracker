CREATE TABLE recurring_schedules
(
    id             BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT       NOT NULL UNIQUE,
    frequency      VARCHAR(20)  NOT NULL,
    day_of_month   INT,
    start_date     DATE         NOT NULL,
    end_date       DATE,
    last_run_date  DATE,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_schedule_transaction
        FOREIGN KEY (transaction_id) REFERENCES transactions (id) ON DELETE CASCADE
);