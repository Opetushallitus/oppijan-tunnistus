UPDATE secure_link
SET email = :new_email
WHERE ((metadata ->> 'hakemusOid') = :hakemusOid) and valid_until > current_timestamp
RETURNING *;
