ALTER TABLE accounts
    ADD COLUMN previous_display_name TEXT;

ALTER TABLE accounts
    ADD COLUMN display_name_changed_at TIMESTAMP;
