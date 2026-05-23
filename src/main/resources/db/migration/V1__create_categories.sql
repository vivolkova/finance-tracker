CREATE TABLE categories
(
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

INSERT INTO categories (name, type) VALUES ('Зарплата', 'INCOME');
INSERT INTO categories (name, type) VALUES ('Фриланс', 'INCOME');
INSERT INTO categories (name, type) VALUES ('Продукты', 'EXPENSE');
INSERT INTO categories (name, type) VALUES ('Транспорт', 'EXPENSE');