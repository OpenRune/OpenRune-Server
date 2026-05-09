CREATE INDEX IF NOT EXISTS idx_accounts_login_username_nocase
    ON accounts(login_username COLLATE NOCASE);

CREATE INDEX IF NOT EXISTS idx_accounts_display_name_nocase
    ON accounts(display_name COLLATE NOCASE);

CREATE INDEX IF NOT EXISTS idx_accounts_previous_display_name_nocase
    ON accounts(previous_display_name COLLATE NOCASE);
