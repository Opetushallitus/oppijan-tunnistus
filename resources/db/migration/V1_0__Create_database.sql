CREATE TABLE secure_link (
    id             serial PRIMARY KEY,
    created_at     timestamp with time zone default now(),
    valid_until    timestamp with time zone NOT NULL,
    email          varchar(128) NOT NULL,
    token          varchar(256) NOT NULL,
    callback_url   varchar(128) NOT NULL
);

CREATE INDEX ON secure_link (token);
