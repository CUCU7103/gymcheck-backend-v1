CREATE TABLE notification_settings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    notify_time TIME,
    timezone VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
