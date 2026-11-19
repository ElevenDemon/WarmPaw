-- 沿用 owner_id 存储客服会话发起人的 app_user.id，兼容现有接口及管理端。
COMMENT ON TABLE support_conversation IS '宠主及喂养师的AI与人工客服会话';
COMMENT ON COLUMN support_conversation.owner_id IS '会话发起人账号ID（宠主或喂养师，关联app_user）';
