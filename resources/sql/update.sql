UPDATE secure_link
SET email = :new_email
WHERE token = :token;