ALTER TABLE sitter_profile ADD COLUMN admission_review_note varchar(1000);
COMMENT ON COLUMN sitter_profile.admission_review_note IS '最近一次喂养师入驻审核意见；驳回时供申请人修改后重新提交';
