UPDATE secure_link
SET email = :new_email
WHERE ((metadata ->> 'hakemusOid') = :hakemusOid) AND valid_until > current_timestamp
AND callback_url = :callbackUrl
RETURNING *;
