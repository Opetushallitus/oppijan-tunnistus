CREATE TABLE securelink (
    id             serial PRIMARY KEY,
    created_at     timestamp with time zone default now(),
    valid_until    timestamp with time zone NOT NULL,
    email          varchar(128) NOT NULL,
    secure_link    varchar(256) NOT NULL
);
