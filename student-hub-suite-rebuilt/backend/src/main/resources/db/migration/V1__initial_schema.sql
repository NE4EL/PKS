CREATE TABLE app_user (
                          id UUID PRIMARY KEY,
                          email VARCHAR(254) NOT NULL UNIQUE,
                          display_name VARCHAR(160) NOT NULL,
                          password_hash VARCHAR(100) NOT NULL,
                          enabled BOOLEAN NOT NULL DEFAULT TRUE,
                          password_change_required BOOLEAN NOT NULL DEFAULT TRUE,
                          failed_login_attempts INTEGER NOT NULL DEFAULT 0,
                          locked_until TIMESTAMPTZ,
                          created_at TIMESTAMPTZ NOT NULL,
                          updated_at TIMESTAMPTZ NOT NULL,
                          version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE app_user_role (
                               user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                               role VARCHAR(32) NOT NULL,
                               PRIMARY KEY (user_id, role)
);

CREATE TABLE refresh_session (
                                 id UUID PRIMARY KEY,
                                 user_id UUID NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,
                                 token_hash VARCHAR(64) NOT NULL UNIQUE,
                                 expires_at TIMESTAMPTZ NOT NULL,
                                 revoked_at TIMESTAMPTZ,
                                 replaced_by UUID,
                                 user_agent VARCHAR(500),
                                 ip_address VARCHAR(64),
                                 created_at TIMESTAMPTZ NOT NULL,
                                 last_used_at TIMESTAMPTZ
);

CREATE INDEX idx_refresh_user
    ON refresh_session(user_id);

CREATE INDEX idx_refresh_expires
    ON refresh_session(expires_at);


CREATE TABLE study_group (
                             id UUID PRIMARY KEY,
                             code VARCHAR(50) NOT NULL UNIQUE,
                             name VARCHAR(200),
                             admission_year INTEGER,
                             active BOOLEAN NOT NULL DEFAULT TRUE,
                             created_at TIMESTAMPTZ NOT NULL,
                             updated_at TIMESTAMPTZ NOT NULL,
                             version BIGINT NOT NULL DEFAULT 0
);


CREATE TABLE student (
                         id UUID PRIMARY KEY,
                         external_id VARCHAR(100) UNIQUE,
                         first_name VARCHAR(100) NOT NULL,
                         last_name VARCHAR(100) NOT NULL,
                         middle_name VARCHAR(100),
                         email VARCHAR(254),
                         phone VARCHAR(32),
                         birth_date DATE,
                         group_id UUID REFERENCES study_group(id),
                         status VARCHAR(32) NOT NULL,

    -- Средний балл по 100-балльной шкале.
                         average_grade NUMERIC(5,2),

                         has_debt BOOLEAN NOT NULL DEFAULT FALSE,
                         risk_level VARCHAR(32) NOT NULL,
                         archived BOOLEAN NOT NULL DEFAULT FALSE,
                         archived_at TIMESTAMPTZ,
                         archived_by UUID REFERENCES app_user(id),
                         created_by UUID REFERENCES app_user(id),
                         updated_by UUID REFERENCES app_user(id),
                         created_at TIMESTAMPTZ NOT NULL,
                         updated_at TIMESTAMPTZ NOT NULL,
                         version BIGINT NOT NULL DEFAULT 0,

                         CONSTRAINT chk_student_average_grade
                             CHECK (
                                 average_grade IS NULL
                                     OR (average_grade >= 0.00 AND average_grade <= 100.00)
                                 )
);

COMMENT ON COLUMN student.average_grade
    IS 'Средний балл студента по 100-балльной шкале (0..100)';

CREATE INDEX idx_student_name
    ON student(last_name, first_name, middle_name);

CREATE INDEX idx_student_group
    ON student(group_id);

CREATE INDEX idx_student_status
    ON student(status);

CREATE INDEX idx_student_archived
    ON student(archived);

CREATE INDEX idx_student_risk
    ON student(risk_level);


CREATE TABLE student_tag (
                             student_id UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
                             tag VARCHAR(50) NOT NULL,
                             PRIMARY KEY (student_id, tag)
);

CREATE INDEX idx_student_tag_value
    ON student_tag(tag);


CREATE TABLE student_note (
                              id UUID PRIMARY KEY,
                              student_id UUID NOT NULL REFERENCES student(id) ON DELETE CASCADE,
                              author_id UUID NOT NULL REFERENCES app_user(id),
                              body VARCHAR(4000) NOT NULL,
                              pinned BOOLEAN NOT NULL DEFAULT FALSE,
                              created_at TIMESTAMPTZ NOT NULL,
                              updated_at TIMESTAMPTZ NOT NULL,
                              version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_note_student
    ON student_note(student_id, pinned DESC, created_at DESC);


CREATE TABLE audit_event (
                             id UUID PRIMARY KEY,
                             actor_id UUID,
                             actor_email VARCHAR(254),
                             event_type VARCHAR(80) NOT NULL,
                             entity_type VARCHAR(80),
                             entity_id UUID,
                             summary VARCHAR(1000) NOT NULL,
                             correlation_id VARCHAR(100),
                             ip_address VARCHAR(64),
                             user_agent VARCHAR(500),
                             occurred_at TIMESTAMPTZ NOT NULL,
                             details_json TEXT
);

CREATE INDEX idx_audit_occurred
    ON audit_event(occurred_at DESC);

CREATE INDEX idx_audit_actor
    ON audit_event(actor_email);

CREATE INDEX idx_audit_entity
    ON audit_event(entity_type, entity_id);
