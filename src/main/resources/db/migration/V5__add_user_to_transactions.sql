ALTER TABLE transactions
    ADD COLUMN user_id BIGINT;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_user
        FOREIGN KEY (user_id) REFERENCES users (id);
