INSERT INTO secure_link (id, valid_until, email, token, callback_url, metadata)
VALUES (nextval('secure_link_id_seq'), :valid_until::timestamptz, :email, :token, :callback_url, :metadata::json);
