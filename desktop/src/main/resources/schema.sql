-- ============================================================
-- Владелец: A (Team/Tech Lead)
-- СУБД: PostgreSQL
-- Схема БД: абоненты (1) --- (N) заявки
-- ============================================================

-- Таблица абонентов
CREATE TABLE IF NOT EXISTS subscribers (
    id                BIGSERIAL     PRIMARY KEY,
    full_name         VARCHAR(255)  NOT NULL,
    phone             VARCHAR(20)   NOT NULL UNIQUE,
    email             VARCHAR(255)  UNIQUE,
    address           VARCHAR(500)  NOT NULL,
    registration_date DATE          NOT NULL DEFAULT CURRENT_DATE
);

-- Таблица заявок
CREATE TABLE IF NOT EXISTS connection_requests (
    id            BIGSERIAL     PRIMARY KEY,
    subscriber_id BIGINT        NOT NULL,
    type          VARCHAR(30)   NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'NEW',
    tariff_plan   VARCHAR(100),
    description   VARCHAR(1000) NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_request_subscriber
        FOREIGN KEY (subscriber_id) REFERENCES subscribers (id),

    CONSTRAINT chk_request_type
        CHECK (type IN ('NEW_CONNECTION', 'TARIFF_CHANGE', 'TECHNICAL_SUPPORT',
                        'DISCONNECTION', 'EQUIPMENT_REPLACEMENT')),

    CONSTRAINT chk_request_status
        CHECK (status IN ('NEW', 'IN_PROGRESS', 'APPROVED', 'COMPLETED',
                         'REJECTED', 'CANCELLED'))
);

-- Индекс для быстрого поиска заявок абонента (FK)
CREATE INDEX IF NOT EXISTS idx_requests_subscriber_id
    ON connection_requests (subscriber_id);
