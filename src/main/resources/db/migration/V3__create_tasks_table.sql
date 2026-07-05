CREATE TABLE tasks
(
    id           BIGSERIAL PRIMARY KEY,
    title        VARCHAR(200)             NOT NULL,
    description  VARCHAR(5000),
    priority     VARCHAR(20)              NOT NULL,
    status       VARCHAR(20)              NOT NULL,
    project_id   BIGINT                   NOT NULL,
    creator_id   BIGINT                   NOT NULL,
    assignee_id  BIGINT,
    due_date     TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP                NOT NULL,
    updated_at   TIMESTAMP                NOT NULL,

    CONSTRAINT chk_tasks_priority
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_tasks_status
        CHECK (status IN ('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED')),
    CONSTRAINT fk_tasks_project
        FOREIGN KEY (project_id) REFERENCES projects (id) ON DELETE CASCADE,
    CONSTRAINT fk_tasks_creator
        FOREIGN KEY (creator_id) REFERENCES users (id),
    CONSTRAINT fk_tasks_assignee
        FOREIGN KEY (assignee_id) REFERENCES users (id)
);

CREATE INDEX idx_tasks_project_id ON tasks (project_id);
CREATE INDEX idx_tasks_creator_id ON tasks (creator_id);
CREATE INDEX idx_tasks_assignee_id ON tasks (assignee_id);
