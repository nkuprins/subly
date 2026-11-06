-- Needs MySQL 8.0.16 or newer. Older versions ignore CHECK constraints.

CREATE TABLE IF NOT EXISTS users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    time_zone VARCHAR(64) NOT NULL DEFAULT 'Europe/Riga',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_users_time_zone (time_zone),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_categories_name UNIQUE (name)
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS subscription_providers (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    logo_path VARCHAR(500) NULL,
    created_by BIGINT NULL,
    category_id BIGINT NOT NULL,
    provider_type ENUM('CATALOG', 'CUSTOM') NOT NULL DEFAULT 'CATALOG',
    PRIMARY KEY (id),
    INDEX idx_subscription_providers_created_by (created_by),
    INDEX idx_subscription_providers_category_id (category_id),
    -- Catalog providers are shared by all users, so they have no creator.
    -- A custom provider always has the user who created it.
    CONSTRAINT chk_subscription_providers_created_by CHECK (
        (provider_type = 'CATALOG' AND created_by IS NULL)
        OR (provider_type = 'CUSTOM' AND created_by IS NOT NULL)
    ),
    -- Must not cascade: MySQL does not allow the CHECK above on a cascading column.
    CONSTRAINT fk_subscription_providers_created_by
        FOREIGN KEY (created_by) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_subscription_providers_category_id
        FOREIGN KEY (category_id) REFERENCES categories (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- The owner of a subscription is its OWNER row in subscription_membership.
CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    provider_id BIGINT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    billing_cycle ENUM('MONTHLY', 'YEARLY') NOT NULL,
    next_payment_date DATE NOT NULL,
    billing_anchor_date DATE NOT NULL,
    free_trial BOOLEAN NOT NULL DEFAULT FALSE,
    status ENUM('ACTIVE', 'CANCELLED') NOT NULL DEFAULT 'ACTIVE',
    cancelled_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    currency ENUM('EUR', 'USD', 'GBP') NOT NULL DEFAULT 'EUR',
    is_sharable BOOLEAN NOT NULL DEFAULT FALSE,
    max_participants INT NOT NULL DEFAULT 1,
    PRIMARY KEY (id),
    INDEX idx_subscriptions_provider_id (provider_id),
    INDEX idx_subscriptions_status_next_payment_date (status, next_payment_date),
    CONSTRAINT chk_subscriptions_price CHECK (price > 0),
    CONSTRAINT chk_subscriptions_max_participants CHECK (max_participants >= 1),
    CONSTRAINT chk_subscriptions_cancelled_at CHECK ((status = 'CANCELLED') = (cancelled_at IS NOT NULL)),
    CONSTRAINT chk_subscriptions_sharable CHECK (is_sharable OR max_participants = 1),
    CONSTRAINT chk_subscriptions_payment_date CHECK (next_payment_date >= billing_anchor_date),
    CONSTRAINT fk_subscriptions_provider_id
        FOREIGN KEY (provider_id) REFERENCES subscription_providers (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS subscription_membership (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    role ENUM('OWNER', 'MEMBER') NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Allows only one OWNER per subscription (see uq_subscription_membership_owner).
    -- It is 1 for the owner and NULL for members. A unique key lets NULLs repeat.
    -- MySQL fills this column. Do not map it in JPA.
    owner_marker TINYINT GENERATED ALWAYS AS (IF(role = 'OWNER', 1, NULL)) STORED,
    PRIMARY KEY (id),
    CONSTRAINT uq_subscription_membership_pair UNIQUE (subscription_id, user_id),
    CONSTRAINT uq_subscription_membership_owner UNIQUE (subscription_id, owner_marker),
    INDEX idx_subscription_membership_user_id (user_id),
    CONSTRAINT fk_subscription_membership_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_subscription_membership_subscription_id
        FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

-- Invitations that the user has not accepted yet.
CREATE TABLE IF NOT EXISTS subscription_pending (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    subscription_id BIGINT NOT NULL,
    invited_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT uq_subscription_pending_pair UNIQUE (subscription_id, user_id),
    INDEX idx_subscription_pending_user_id (user_id),
    CONSTRAINT fk_subscription_pending_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_subscription_pending_subscription_id
        FOREIGN KEY (subscription_id) REFERENCES subscriptions (id)
        ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE TABLE IF NOT EXISTS subscription_payments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    membership_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    paid_on DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    INDEX idx_subscription_payments_membership_id (membership_id),
    CONSTRAINT chk_subscription_payments_amount CHECK (amount > 0),
    CONSTRAINT fk_subscription_payments_membership_id
        FOREIGN KEY (membership_id) REFERENCES subscription_membership (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;
