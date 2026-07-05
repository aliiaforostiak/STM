CREATE TABLE projects
(
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100)  NOT NULL,
    description VARCHAR(2000),
    owner_id    BIGINT        NOT NULL,
    created_at  TIMESTAMP     NOT NULL,
    updated_at  TIMESTAMP     NOT NULL,

    CONSTRAINT uq_projects_owner_name UNIQUE (owner_id, name),
    CONSTRAINT fk_projects_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);
