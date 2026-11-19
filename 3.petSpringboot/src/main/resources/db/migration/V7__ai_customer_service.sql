ALTER TABLE chat_message
    ADD COLUMN sender_type varchar(20) NOT NULL DEFAULT 'USER'
    CHECK (sender_type IN ('USER', 'HUMAN', 'AI'));

ALTER TABLE support_conversation
    DROP CONSTRAINT support_conversation_status_check;

ALTER TABLE support_conversation
    ADD CONSTRAINT support_conversation_status_check
    CHECK (status IN ('WAITING_HUMAN', 'HUMAN_REQUESTED', 'HUMAN', 'CLOSED'));
