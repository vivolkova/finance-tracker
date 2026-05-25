CREATE TABLE transactions
(
    id          BIGSERIAL PRIMARY KEY,
    amount      NUMERIC(19, 2)  NOT NULL,
    description VARCHAR(255),
    date        DATE            NOT NULL,
    type        VARCHAR(20)     NOT NULL,
    category_id BIGINT          NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_transactions_category
        FOREIGN KEY (category_id) REFERENCES categories (id)
);