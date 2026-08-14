CREATE TABLE budget
(
    id         BIGSERIAL PRIMARY KEY,
    category_id BIGINT          NOT NULL,
    period      VARCHAR(7)      NOT NULL,
    user_id     BIGINT          NOT NULL,
    limit_amount NUMERIC(19, 2)  NOT NULL,


    CONSTRAINT fk_budget_category
        FOREIGN KEY (category_id) REFERENCES categories (id),

    CONSTRAINT fk_budget_user
        FOREIGN KEY (user_id) REFERENCES users (id)
);
