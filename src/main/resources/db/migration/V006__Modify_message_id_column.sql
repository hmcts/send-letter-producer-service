ALTER TABLE letters
ADD CONSTRAINT unq_letters_message_id UNIQUE (message_id);
