ALTER TABLE letters
  ADD COLUMN submit_index SMALLINT NOT NULL DEFAULT 1,
  ADD COLUMN resubmitted_at TIMESTAMP NULL,
  ADD CONSTRAINT unq_letters_message_id UNIQUE (message_id);
