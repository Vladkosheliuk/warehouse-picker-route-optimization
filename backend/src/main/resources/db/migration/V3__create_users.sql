-- V3: Create application users
-- Warehouse Picker Route Optimization Platform

CREATE TABLE public.users
(
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    username      VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(150) NOT NULL,
    role          VARCHAR(30)  NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_username
        UNIQUE (username),

    CONSTRAINT chk_user_role
        CHECK (role IN ('MANAGER', 'PICKER')),

    CONSTRAINT chk_username_not_empty
        CHECK (LENGTH(TRIM(username)) > 0),

    CONSTRAINT chk_full_name_not_empty
        CHECK (LENGTH(TRIM(full_name)) > 0),

    CONSTRAINT chk_password_hash_not_empty
        CHECK (LENGTH(TRIM(password_hash)) > 0)
);
