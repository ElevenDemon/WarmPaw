ALTER TABLE sitter_profile
    ADD COLUMN service_preferences varchar(1000) NOT NULL DEFAULT '';

COMMENT ON COLUMN sitter_profile.service_preferences IS '喂养师自述服务经验与偏好，仅用于订单推荐，不替代资质审核';
