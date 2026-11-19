ALTER TABLE auth_session
    ADD COLUMN active_role varchar(20)
    CHECK (active_role IS NULL OR active_role IN ('OWNER', 'SITTER'));
