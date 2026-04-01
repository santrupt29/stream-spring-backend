-- 1. Create the users table first (Dependency)
CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       email VARCHAR(255) NOT NULL UNIQUE,
                       password VARCHAR(255),
                       created_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE video (
                       video_id VARCHAR(255) PRIMARY KEY,
                       title VARCHAR(255) NOT NULL,
                       description TEXT,
                       content_type VARCHAR(255),
                       file_path VARCHAR(255),
                       status VARCHAR(255) NOT NULL,
                       uploaded_at TIMESTAMPTZ NOT NULL,
                       user_id UUID,

                       CONSTRAINT status_check CHECK (status IN ('UPLOADING', 'PROCESSING', 'READY', 'FAILED')),

                       CONSTRAINT fk_video_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);