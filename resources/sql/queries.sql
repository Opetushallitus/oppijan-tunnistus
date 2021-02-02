
-- name: get-secure-link
select * from secure_link where token = :token


-- name: add-secure-link<!
INSERT INTO secure_link (id, valid_until, email, token, callback_url, metadata, lang)
VALUES (nextval('secure_link_id_seq'), :valid_until::timestamptz, :email, :token, :callback_url, :metadata::json, :lang);


-- name: update-email-returning-secure-link!
UPDATE secure_link
SET email = :new_email
WHERE ((metadata ->> 'hakemusOid') = :hakemusOid) AND valid_until > current_timestamp
AND callback_url = :callbackUrl RETURNING *;
