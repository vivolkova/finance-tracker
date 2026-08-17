alter TABLE budget add CONSTRAINT uq_budget_user_category_period UNIQUE (user_id, category_id, period);
