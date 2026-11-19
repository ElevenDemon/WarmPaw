-- 仅用于空数据库，连接pet_demo后执行。应用推荐使用Flyway自动初始化，不要重复执行本脚本。
-- PostgreSQL；账号postgres/root。此脚本不创建或删除数据库。
BEGIN;
-- V1__schema.sql
-- 暖爪 PostgreSQL 业务库；时间均使用 timestamptz，金额单位人民币元。
CREATE TABLE app_user (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 username varchar(64) NOT NULL UNIQUE, password_hash varchar(100) NOT NULL,
 name varchar(32) NOT NULL, phone varchar(11) UNIQUE,
 role varchar(20) NOT NULL CHECK (role IN ('ADMIN','SUPPORT','FINANCE','OWNER','SITTER')),
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BLOCKED')),
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE auth_session (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 user_id bigint NOT NULL REFERENCES app_user(id), token_hash varchar(64) NOT NULL UNIQUE,
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE role_permission (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 role varchar(20) NOT NULL, permission varchar(80) NOT NULL, UNIQUE(role,permission)
);
CREATE TABLE pet (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 owner_id bigint NOT NULL REFERENCES app_user(id), name varchar(32) NOT NULL,
 category varchar(20) NOT NULL CHECK (category IN ('CAT','DOG','SMALL','OTHER')),
 breed varchar(60), birthday date, weight numeric(6,2) CHECK (weight>0),
 temperament varchar(500), dietary_notes text, medical_history text, photo_url text,
 deleted boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE owner_address (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 owner_id bigint NOT NULL REFERENCES app_user(id), contact_name varchar(32) NOT NULL,
 phone varchar(11) NOT NULL, district varchar(60) NOT NULL, address varchar(300) NOT NULL,
 longitude numeric(10,6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
 latitude numeric(10,6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE sitter_grade (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 code varchar(20) NOT NULL UNIQUE, name varchar(32) NOT NULL,
 min_orders int NOT NULL CHECK (min_orders>=0), min_rating numeric(3,2) NOT NULL CHECK (min_rating BETWEEN 0 AND 5),
 max_complaints int NOT NULL CHECK (max_complaints>=0),
 commission_rate numeric(5,2) NOT NULL CHECK (commission_rate BETWEEN 0 AND 50),
 dispatch_weight numeric(5,2) NOT NULL CHECK (dispatch_weight BETWEEN 0 AND 100),
 high_risk_enabled boolean NOT NULL DEFAULT false, active boolean NOT NULL DEFAULT true,
 effective_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE sitter_profile (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 user_id bigint NOT NULL UNIQUE REFERENCES app_user(id), code varchar(32) NOT NULL UNIQUE,
 grade_id bigint NOT NULL REFERENCES sitter_grade(id),
 status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','ACTIVE','REST','DISABLED','REJECTED')),
 emergency_contact varchar(80), avatar_url text, notes varchar(60),
 district varchar(60) NOT NULL, address varchar(300) NOT NULL,
 longitude numeric(10,6) NOT NULL CHECK (longitude BETWEEN 113.7 AND 114.7),
 latitude numeric(10,6) NOT NULL CHECK (latitude BETWEEN 22.4 AND 22.9),
 radius_km numeric(5,2) NOT NULL DEFAULT 5 CHECK (radius_km BETWEEN 0.5 AND 50),
 transport varchar(30) NOT NULL DEFAULT 'E_BIKE', settlement_method varchar(30) NOT NULL DEFAULT 'BANK',
 categories jsonb NOT NULL DEFAULT '[]', rating numeric(3,2) NOT NULL DEFAULT 5 CHECK (rating BETWEEN 0 AND 5),
 completed_orders int NOT NULL DEFAULT 0, major_complaints int NOT NULL DEFAULT 0,
 created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE sitter_schedule (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 sitter_id bigint NOT NULL REFERENCES sitter_profile(id), weekday int NOT NULL CHECK (weekday BETWEEN 1 AND 7),
 rest boolean NOT NULL DEFAULT false, start_time time NOT NULL, end_time time NOT NULL,
 capacity int NOT NULL CHECK (capacity BETWEEN 0 AND 20), district varchar(60), address varchar(300),
 longitude numeric(10,6), latitude numeric(10,6),
 CHECK (start_time<end_time), CHECK ((longitude IS NULL)=(latitude IS NULL)),
 CHECK (longitude IS NULL OR longitude BETWEEN 113.7 AND 114.7),
 CHECK (latitude IS NULL OR latitude BETWEEN 22.4 AND 22.9), UNIQUE(sitter_id,weekday)
);
CREATE TABLE sitter_qualification (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 sitter_id bigint NOT NULL REFERENCES sitter_profile(id), category varchar(32) NOT NULL,
 real_verified boolean NOT NULL DEFAULT false, training_passed boolean NOT NULL DEFAULT false,
 practice_passed boolean NOT NULL DEFAULT false, status varchar(20) NOT NULL DEFAULT 'PENDING'
 CHECK (status IN ('PENDING','APPROVED','REJECTED')), expires_at timestamptz NOT NULL,
 materials jsonb NOT NULL DEFAULT '[]', reviewer_id bigint REFERENCES app_user(id), review_note text,
 updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(sitter_id,category)
);
CREATE TABLE service_item (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 code varchar(32) NOT NULL UNIQUE, name varchar(60) NOT NULL, description varchar(500),
 price numeric(10,2) NOT NULL CHECK (price BETWEEN 0 AND 9999),
 required_category varchar(32), active boolean NOT NULL DEFAULT true,
 updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE membership_plan (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(60) NOT NULL, display_name varchar(60) NOT NULL,
 months int NOT NULL CHECK (months BETWEEN 1 AND 120),
 price numeric(10,2) NOT NULL CHECK (price BETWEEN 0.01 AND 9999),
 active boolean NOT NULL DEFAULT true, updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE membership_benefit (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 code varchar(32) NOT NULL UNIQUE, name varchar(60) NOT NULL, enabled boolean NOT NULL DEFAULT true,
 discount_rate numeric(4,3) NOT NULL DEFAULT 1 CHECK (discount_rate>0 AND discount_rate<=1),
 updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE membership (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 owner_id bigint NOT NULL UNIQUE REFERENCES app_user(id), plan_id bigint NOT NULL REFERENCES membership_plan(id),
 starts_at timestamptz NOT NULL, expires_at timestamptz NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DISABLED')),
 saved_amount numeric(12,2) NOT NULL DEFAULT 0,
 updated_at timestamptz NOT NULL DEFAULT now(), CHECK (starts_at<expires_at)
);
CREATE TABLE membership_purchase (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 owner_id bigint NOT NULL REFERENCES app_user(id), plan_id bigint NOT NULL REFERENCES membership_plan(id),
 plan_name varchar(60) NOT NULL, months int NOT NULL,
 amount numeric(10,2) NOT NULL, status varchar(20) NOT NULL DEFAULT 'PENDING'
 CHECK (status IN ('PENDING','PAID')), created_at timestamptz NOT NULL DEFAULT now(), paid_at timestamptz
);
CREATE TABLE coupon_template (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(60) NOT NULL, issue_type varchar(10) NOT NULL CHECK (issue_type IN ('push','claim')),
 amount numeric(10,2) NOT NULL CHECK (amount>0), threshold numeric(10,2) NOT NULL DEFAULT 0 CHECK (threshold>=0),
 service_codes jsonb NOT NULL DEFAULT '[]', valid_days int NOT NULL CHECK (valid_days BETWEEN 1 AND 3650),
 active boolean NOT NULL DEFAULT true, audience varchar(20) NOT NULL DEFAULT 'ALL'
 CHECK (audience IN ('ALL','NEW','NORMAL','MEMBER','SMALL')),
 per_user_limit int NOT NULL DEFAULT 1 CHECK (per_user_limit BETWEEN 1 AND 100),
 stock int NOT NULL DEFAULT 0 CHECK (stock>=0), issued int NOT NULL DEFAULT 0 CHECK (issued>=0),
 claim_starts_at timestamptz NOT NULL, claim_ends_at timestamptz NOT NULL,
 redeem_code varchar(32) UNIQUE, limit_device boolean NOT NULL DEFAULT false,
 limit_phone boolean NOT NULL DEFAULT false, limit_ip boolean NOT NULL DEFAULT false,
 created_at timestamptz NOT NULL DEFAULT now(), CHECK (claim_starts_at<claim_ends_at)
);
CREATE TABLE user_coupon (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 template_id bigint NOT NULL REFERENCES coupon_template(id), owner_id bigint NOT NULL REFERENCES app_user(id),
 source varchar(10) NOT NULL CHECK (source IN ('push','claim')), amount numeric(10,2) NOT NULL,
 threshold numeric(10,2) NOT NULL, service_codes jsonb NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'AVAILABLE' CHECK (status IN ('AVAILABLE','LOCKED','USED','EXPIRED')),
 expires_at timestamptz NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE coupon_claim_log (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 template_id bigint NOT NULL REFERENCES coupon_template(id), owner_id bigint NOT NULL REFERENCES app_user(id),
 source varchar(10) NOT NULL, success boolean NOT NULL, failed_step int,
 reason varchar(300) NOT NULL, chain jsonb NOT NULL, device_hash varchar(64), phone_hash varchar(64), ip_hash varchar(64),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE pet_order (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_no varchar(40) NOT NULL UNIQUE, request_key varchar(80) NOT NULL,
 owner_id bigint NOT NULL REFERENCES app_user(id), sitter_id bigint REFERENCES sitter_profile(id),
 status varchar(30) NOT NULL DEFAULT 'PENDING_PAYMENT'
 CHECK (status IN ('PENDING_PAYMENT','DISPATCHING','PENDING_ACCEPT','ACCEPTED','SERVICING','COMPLETED','CANCELLED','REFUNDED')),
 service_start timestamptz NOT NULL, service_end timestamptz NOT NULL,
 address_snapshot jsonb NOT NULL, pet_snapshot jsonb NOT NULL, notes varchar(1000),
 total_amount numeric(12,2) NOT NULL CHECK (total_amount>=0), member_discount numeric(12,2) NOT NULL DEFAULT 0,
 coupon_discount numeric(12,2) NOT NULL DEFAULT 0, payable_amount numeric(12,2) NOT NULL CHECK (payable_amount>=0),
 coupon_id bigint UNIQUE REFERENCES user_coupon(id), commission_rate numeric(5,2) CHECK (commission_rate BETWEEN 0 AND 50),
 signed_agreement jsonb NOT NULL, dispatch_reason jsonb, accept_deadline timestamptz,
 created_at timestamptz NOT NULL DEFAULT now(), paid_at timestamptz, checkin_at timestamptz, checkout_at timestamptz,
 UNIQUE(owner_id,request_key), CHECK (service_start<service_end),
 CHECK (payable_amount=total_amount-member_discount-coupon_discount)
);
CREATE TABLE order_item (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES pet_order(id), service_id bigint NOT NULL REFERENCES service_item(id),
 code varchar(32) NOT NULL, name varchar(60) NOT NULL,
 unit_price numeric(10,2) NOT NULL, quantity int NOT NULL CHECK (quantity>0),
 required_category varchar(32), UNIQUE(order_id,code)
);
CREATE TABLE order_event (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES pet_order(id), actor_id bigint REFERENCES app_user(id),
 event varchar(40) NOT NULL, detail jsonb NOT NULL DEFAULT '{}', created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE dispatch_record (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES pet_order(id), sitter_id bigint NOT NULL REFERENCES sitter_profile(id),
 operator_id bigint REFERENCES app_user(id), mode varchar(20) NOT NULL DEFAULT 'MANUAL',
 reason text NOT NULL, status varchar(20) NOT NULL DEFAULT 'PENDING',
 created_at timestamptz NOT NULL DEFAULT now(), responded_at timestamptz
);
CREATE TABLE payment_record (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint REFERENCES pet_order(id), purchase_id bigint REFERENCES membership_purchase(id),
 trade_no varchar(80) NOT NULL UNIQUE, provider varchar(20) NOT NULL,
 amount numeric(12,2) NOT NULL CHECK (amount>=0), status varchar(20) NOT NULL DEFAULT 'PAID',
 paid_at timestamptz NOT NULL DEFAULT now(),
 CHECK ((order_id IS NULL)<>(purchase_id IS NULL)), UNIQUE(order_id), UNIQUE(purchase_id)
);
CREATE TABLE refund_record (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL UNIQUE REFERENCES pet_order(id), amount numeric(12,2) NOT NULL,
 reason text NOT NULL, operator_id bigint NOT NULL REFERENCES app_user(id),
 provider varchar(20) NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE service_checklist (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES pet_order(id), code varchar(32) NOT NULL,
 completed boolean NOT NULL DEFAULT false, quantity numeric(10,2), unit varchar(20), notes varchar(1000),
 updated_at timestamptz NOT NULL DEFAULT now(), UNIQUE(order_id,code)
);
CREATE TABLE service_media (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES pet_order(id), uploader_id bigint NOT NULL REFERENCES app_user(id),
 kind varchar(20) NOT NULL CHECK (kind IN ('PHOTO','VIDEO','LIVE')),
 object_key varchar(500) NOT NULL, expires_at timestamptz NOT NULL,
 deleted boolean NOT NULL DEFAULT false, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE feeding_report (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL UNIQUE REFERENCES pet_order(id),
 status varchar(20) NOT NULL DEFAULT 'PENDING_AI' CHECK (status IN ('PENDING_AI','MANUAL','READY')),
 source_snapshot jsonb NOT NULL, content jsonb, updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE review (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL UNIQUE REFERENCES pet_order(id), owner_id bigint NOT NULL REFERENCES app_user(id),
 sitter_id bigint NOT NULL REFERENCES sitter_profile(id), score int NOT NULL CHECK (score BETWEEN 1 AND 5),
 tags jsonb NOT NULL DEFAULT '[]', content varchar(1000) NOT NULL, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE complaint (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES pet_order(id), owner_id bigint NOT NULL REFERENCES app_user(id),
 content varchar(2000) NOT NULL, status varchar(20) NOT NULL DEFAULT 'OPEN'
 CHECK (status IN ('OPEN','PROCESSING','RESOLVED')), response_due_at timestamptz NOT NULL DEFAULT now()+interval '24 hours',
 resolution text, created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE hospital (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 name varchar(100) NOT NULL, phone varchar(32) NOT NULL, address varchar(300) NOT NULL,
 longitude numeric(10,6) NOT NULL CHECK (longitude BETWEEN -180 AND 180),
 latitude numeric(10,6) NOT NULL CHECK (latitude BETWEEN -90 AND 90),
 emergency_24h boolean NOT NULL DEFAULT false, active boolean NOT NULL DEFAULT true
);
CREATE TABLE emergency_ticket (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL REFERENCES pet_order(id), reporter_id bigint NOT NULL REFERENCES app_user(id),
 description varchar(2000) NOT NULL, status varchar(20) NOT NULL DEFAULT 'OPEN'
 CHECK (status IN ('OPEN','ACCEPTED','TRANSPORTING','RESOLVED')),
 sitter_id bigint REFERENCES sitter_profile(id), hospital_id bigint REFERENCES hospital(id),
 owner_consent boolean NOT NULL DEFAULT false, response_due_at timestamptz NOT NULL DEFAULT now()+interval '1 hour',
 resolution text, created_at timestamptz NOT NULL DEFAULT now(), updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE settlement (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 order_id bigint NOT NULL UNIQUE REFERENCES pet_order(id), sitter_id bigint NOT NULL REFERENCES sitter_profile(id),
 gross_amount numeric(12,2) NOT NULL, commission_rate numeric(5,2) NOT NULL,
 platform_amount numeric(12,2) NOT NULL, sitter_amount numeric(12,2) NOT NULL,
 status varchar(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING','SETTLED','REVERSED')),
 settled_at timestamptz, created_at timestamptz NOT NULL DEFAULT now(), CHECK (gross_amount=platform_amount+sitter_amount)
);
CREATE TABLE notification_template (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 code varchar(32) NOT NULL UNIQUE, name varchar(100) NOT NULL,
 enabled boolean NOT NULL DEFAULT true, channels jsonb NOT NULL DEFAULT '["IN_APP"]',
 updated_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE notification (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 user_id bigint NOT NULL REFERENCES app_user(id), order_id bigint REFERENCES pet_order(id),
 code varchar(32) NOT NULL, title varchar(100) NOT NULL, content text NOT NULL,
 read_at timestamptz, delivery_status varchar(20) NOT NULL DEFAULT 'IN_APP',
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE support_conversation (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 owner_id bigint NOT NULL REFERENCES app_user(id), order_id bigint REFERENCES pet_order(id),
 agent_id bigint REFERENCES app_user(id), status varchar(20) NOT NULL DEFAULT 'WAITING_HUMAN'
 CHECK (status IN ('WAITING_HUMAN','HUMAN','CLOSED')), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE chat_message (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 conversation_id bigint REFERENCES support_conversation(id), order_id bigint REFERENCES pet_order(id),
 sender_id bigint NOT NULL REFERENCES app_user(id), content varchar(2000) NOT NULL,
 media_keys jsonb NOT NULL DEFAULT '[]', created_at timestamptz NOT NULL DEFAULT now(),
 CHECK ((conversation_id IS NULL)<>(order_id IS NULL))
);
CREATE TABLE operation_log (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 actor_id bigint REFERENCES app_user(id), action varchar(100) NOT NULL,
 resource varchar(80) NOT NULL, resource_id bigint, before_data jsonb, after_data jsonb,
 result varchar(20) NOT NULL DEFAULT 'SUCCESS', created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_order_owner ON pet_order(owner_id,created_at DESC);
CREATE INDEX idx_order_sitter_time ON pet_order(sitter_id,service_start) WHERE status IN ('PENDING_ACCEPT','ACCEPTED','SERVICING');
CREATE INDEX idx_order_status ON pet_order(status,created_at);
CREATE INDEX idx_coupon_owner ON user_coupon(owner_id,status,expires_at);
CREATE INDEX idx_claim_risk ON coupon_claim_log(template_id,success,device_hash,phone_hash,ip_hash);
CREATE INDEX idx_pet_owner ON pet(owner_id) WHERE NOT deleted;
CREATE INDEX idx_event_order ON order_event(order_id,created_at);
CREATE INDEX idx_notice_user ON notification(user_id,created_at DESC);
CREATE INDEX idx_session_expiry ON auth_session(expires_at);
CREATE INDEX idx_chat_order ON chat_message(order_id,created_at);
CREATE INDEX idx_operation_time ON operation_log(created_at DESC);


-- V2__reference_data.sql
INSERT INTO role_permission(role,permission) VALUES
 ('ADMIN','*'),
 ('SUPPORT','order:read'),('SUPPORT','order:update'),('SUPPORT','dispatch:update'),
 ('SUPPORT','sitter:read'),('SUPPORT','audit:read'),('SUPPORT','audit:update'),
 ('SUPPORT','service:read'),('SUPPORT','service:update'),('SUPPORT','emergency:read'),('SUPPORT','emergency:update'),
 ('SUPPORT','review:read'),('SUPPORT','review:update'),('SUPPORT','dash:read'),('SUPPORT','user:read'),
 ('FINANCE','finance:read'),('FINANCE','finance:settle'),('FINANCE','finance:refund'),
 ('FINANCE','coupon:read'),('FINANCE','coupon:update'),('FINANCE','dash:read'),('FINANCE','order:read');
INSERT INTO sitter_grade(code,name,min_orders,min_rating,max_complaints,commission_rate,dispatch_weight,high_risk_enabled) VALUES
 ('L1','见习喂养师',0,4.50,0,20,40,false),('L2','认证喂养师',100,4.80,0,18,70,true),('L3','金牌喂养师',500,4.90,0,15,100,true);
INSERT INTO service_item(code,name,description,price,required_category) VALUES
 ('BASE','基础上门喂养','一只宠物：喂食、换水、清洁、陪玩至少10分钟',35,null),
 ('EXTRA_PET','增加一只宠物','同地址同一时段',15,null),('WALK','遛狗服务','户外遛行30分钟',30,'WALK'),
 ('MEDICINE','喂药护理','按医嘱口服或外用',20,'MEDICINE'),('GROOM','梳毛 / 剪指甲','基础清洁护理',10,null),
 ('DEWORM','驱虫','按既定方案执行',15,'DEWORM'),('LIVE','服务直播','仅记录直播资源，推流平台待接入',10,null);
INSERT INTO membership_plan(name,display_name,months,price) VALUES ('月度会员','月度会员',1,29),('年度会员','年度会员',12,199);
INSERT INTO membership_benefit(code,name,discount_rate) VALUES
 ('SERVICE_DISCOUNT','服务费9折',0.9),('PRIORITY','优先派单',1),('SUPPORT','专属客服',1),('BIRTHDAY','生日礼遇',1);
INSERT INTO notification_template(code,name) VALUES
 ('CHECKIN','进门实时提醒'),('CHECKOUT','离门实时提醒'),('REPORT','喂养报告推送'),
 ('EMERGENCY','异常主动预警'),('MEMBERSHIP','会员开通与续费提醒'),('DISPATCH','派单提醒'),('COUPON','优惠券到账');


-- V3__scheduled_rules_and_member_gifts.sql
CREATE TABLE sitter_grade_revision (
 id bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 grade_id bigint NOT NULL REFERENCES sitter_grade(id), rule_data jsonb NOT NULL,
 effective_at timestamptz NOT NULL, applied_at timestamptz,
 operator_id bigint NOT NULL REFERENCES app_user(id), created_at timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_grade_revision_due ON sitter_grade_revision(effective_at) WHERE applied_at IS NULL;
ALTER TABLE membership_plan ADD COLUMN gift_coupon_template_id bigint REFERENCES coupon_template(id);
ALTER TABLE membership_plan ADD COLUMN gift_coupon_quantity int NOT NULL DEFAULT 0 CHECK (gift_coupon_quantity BETWEEN 0 AND 20);
ALTER TABLE membership_purchase ADD COLUMN gift_coupon_template_id bigint REFERENCES coupon_template(id);
ALTER TABLE membership_purchase ADD COLUMN gift_coupon_quantity int NOT NULL DEFAULT 0;
INSERT INTO coupon_template(name,issue_type,amount,threshold,valid_days,audience,claim_starts_at,claim_ends_at)
 VALUES ('年度会员赠券','push',20,60,365,'MEMBER',now(),now()+interval '10 years');
UPDATE membership_plan SET gift_coupon_template_id=(SELECT id FROM coupon_template WHERE name='年度会员赠券'),gift_coupon_quantity=2 WHERE months=12;


-- V4__table_and_column_comments.sql
-- 数据库中文表注释与字段注释；仅更新元数据，不改变结构和业务数据。
-- 不限定schema，以兼容默认业务库和隔离测试schema。

COMMENT ON TABLE "app_user" IS '统一账号';
COMMENT ON COLUMN "app_user"."id" IS '自增主键';
COMMENT ON COLUMN "app_user"."username" IS '登录账号';
COMMENT ON COLUMN "app_user"."password_hash" IS 'BCrypt密码摘要，不返回前端';
COMMENT ON COLUMN "app_user"."name" IS '名称';
COMMENT ON COLUMN "app_user"."phone" IS '手机号码';
COMMENT ON COLUMN "app_user"."role" IS '账号角色';
COMMENT ON COLUMN "app_user"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "app_user"."created_at" IS '创建时间';
COMMENT ON COLUMN "app_user"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "auth_session" IS '登录会话';
COMMENT ON COLUMN "auth_session"."id" IS '自增主键';
COMMENT ON COLUMN "auth_session"."user_id" IS '关联统一账号';
COMMENT ON COLUMN "auth_session"."token_hash" IS '随机Bearer令牌的SHA-256摘要';
COMMENT ON COLUMN "auth_session"."expires_at" IS '到期时间';
COMMENT ON COLUMN "auth_session"."created_at" IS '创建时间';

COMMENT ON TABLE "chat_message" IS '聊天消息';
COMMENT ON COLUMN "chat_message"."id" IS '自增主键';
COMMENT ON COLUMN "chat_message"."conversation_id" IS '客服会话ID';
COMMENT ON COLUMN "chat_message"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "chat_message"."sender_id" IS '发送人账号';
COMMENT ON COLUMN "chat_message"."content" IS '文本内容';
COMMENT ON COLUMN "chat_message"."media_keys" IS '媒体对象键JSON数组';
COMMENT ON COLUMN "chat_message"."created_at" IS '创建时间';

COMMENT ON TABLE "complaint" IS '投诉工单';
COMMENT ON COLUMN "complaint"."id" IS '自增主键';
COMMENT ON COLUMN "complaint"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "complaint"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "complaint"."content" IS '文本内容';
COMMENT ON COLUMN "complaint"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "complaint"."response_due_at" IS '响应截止时间';
COMMENT ON COLUMN "complaint"."resolution" IS '处理结果';
COMMENT ON COLUMN "complaint"."created_at" IS '创建时间';

COMMENT ON TABLE "coupon_claim_log" IS '发券及领券校验日志';
COMMENT ON COLUMN "coupon_claim_log"."id" IS '自增主键';
COMMENT ON COLUMN "coupon_claim_log"."template_id" IS '券模板ID';
COMMENT ON COLUMN "coupon_claim_log"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "coupon_claim_log"."source" IS '来源claim主动领取或push后台直发';
COMMENT ON COLUMN "coupon_claim_log"."success" IS '是否成功';
COMMENT ON COLUMN "coupon_claim_log"."failed_step" IS '失败步骤1至6';
COMMENT ON COLUMN "coupon_claim_log"."reason" IS '业务说明或失败理由';
COMMENT ON COLUMN "coupon_claim_log"."chain" IS '校验链JSON，成功为7步';
COMMENT ON COLUMN "coupon_claim_log"."device_hash" IS '设备标识摘要';
COMMENT ON COLUMN "coupon_claim_log"."phone_hash" IS '手机号摘要';
COMMENT ON COLUMN "coupon_claim_log"."ip_hash" IS '来源IP摘要';
COMMENT ON COLUMN "coupon_claim_log"."created_at" IS '创建时间';

COMMENT ON TABLE "coupon_template" IS '优惠券模板';
COMMENT ON COLUMN "coupon_template"."id" IS '自增主键';
COMMENT ON COLUMN "coupon_template"."name" IS '名称';
COMMENT ON COLUMN "coupon_template"."issue_type" IS 'push直发或claim主动领取';
COMMENT ON COLUMN "coupon_template"."amount" IS '金额，人民币元';
COMMENT ON COLUMN "coupon_template"."threshold" IS '最低消费门槛，元';
COMMENT ON COLUMN "coupon_template"."service_codes" IS '可用服务代码JSON数组，空表示不限';
COMMENT ON COLUMN "coupon_template"."valid_days" IS '有效天数';
COMMENT ON COLUMN "coupon_template"."active" IS '是否启用';
COMMENT ON COLUMN "coupon_template"."audience" IS '适用人群ALL/NEW/NORMAL/MEMBER/SMALL';
COMMENT ON COLUMN "coupon_template"."per_user_limit" IS '每人主动领取上限';
COMMENT ON COLUMN "coupon_template"."stock" IS '主动领取剩余库存';
COMMENT ON COLUMN "coupon_template"."issued" IS '累计发放数量';
COMMENT ON COLUMN "coupon_template"."claim_starts_at" IS '领取开始时间';
COMMENT ON COLUMN "coupon_template"."claim_ends_at" IS '领取截止时间';
COMMENT ON COLUMN "coupon_template"."redeem_code" IS '兑换码，唯一';
COMMENT ON COLUMN "coupon_template"."limit_device" IS '同设备限制开关';
COMMENT ON COLUMN "coupon_template"."limit_phone" IS '同手机号限制开关';
COMMENT ON COLUMN "coupon_template"."limit_ip" IS '同IP限制开关';
COMMENT ON COLUMN "coupon_template"."created_at" IS '创建时间';

COMMENT ON TABLE "dispatch_record" IS '人工派单记录';
COMMENT ON COLUMN "dispatch_record"."id" IS '自增主键';
COMMENT ON COLUMN "dispatch_record"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "dispatch_record"."sitter_id" IS '喂养师档案ID';
COMMENT ON COLUMN "dispatch_record"."operator_id" IS '操作人账号ID';
COMMENT ON COLUMN "dispatch_record"."mode" IS '派单模式，当前MANUAL';
COMMENT ON COLUMN "dispatch_record"."reason" IS '业务说明或失败理由';
COMMENT ON COLUMN "dispatch_record"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "dispatch_record"."created_at" IS '创建时间';
COMMENT ON COLUMN "dispatch_record"."responded_at" IS '响应时间';

COMMENT ON TABLE "emergency_ticket" IS '紧急送医工单';
COMMENT ON COLUMN "emergency_ticket"."id" IS '自增主键';
COMMENT ON COLUMN "emergency_ticket"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "emergency_ticket"."reporter_id" IS '上报人账号ID';
COMMENT ON COLUMN "emergency_ticket"."description" IS '说明';
COMMENT ON COLUMN "emergency_ticket"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "emergency_ticket"."sitter_id" IS '喂养师档案ID';
COMMENT ON COLUMN "emergency_ticket"."hospital_id" IS '合作医院ID';
COMMENT ON COLUMN "emergency_ticket"."owner_consent" IS '宠主是否明确同意送医';
COMMENT ON COLUMN "emergency_ticket"."response_due_at" IS '响应截止时间';
COMMENT ON COLUMN "emergency_ticket"."resolution" IS '处理结果';
COMMENT ON COLUMN "emergency_ticket"."created_at" IS '创建时间';
COMMENT ON COLUMN "emergency_ticket"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "feeding_report" IS '喂养报告与待处理原始数据';
COMMENT ON COLUMN "feeding_report"."id" IS '自增主键';
COMMENT ON COLUMN "feeding_report"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "feeding_report"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "feeding_report"."source_snapshot" IS '检查清单、媒体等原始快照，供后续Python读取';
COMMENT ON COLUMN "feeding_report"."content" IS '文本内容';
COMMENT ON COLUMN "feeding_report"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "hospital" IS '合作医院';
COMMENT ON COLUMN "hospital"."id" IS '自增主键';
COMMENT ON COLUMN "hospital"."name" IS '名称';
COMMENT ON COLUMN "hospital"."phone" IS '手机号码';
COMMENT ON COLUMN "hospital"."address" IS '详细地址';
COMMENT ON COLUMN "hospital"."longitude" IS '经度';
COMMENT ON COLUMN "hospital"."latitude" IS '纬度';
COMMENT ON COLUMN "hospital"."emergency_24h" IS '是否24小时急诊';
COMMENT ON COLUMN "hospital"."active" IS '是否启用';

COMMENT ON TABLE "membership" IS '宠主会员账户';
COMMENT ON COLUMN "membership"."id" IS '自增主键';
COMMENT ON COLUMN "membership"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "membership"."plan_id" IS '会员套餐ID';
COMMENT ON COLUMN "membership"."starts_at" IS '开始时间';
COMMENT ON COLUMN "membership"."expires_at" IS '到期时间';
COMMENT ON COLUMN "membership"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "membership"."saved_amount" IS '累计会员优惠金额';
COMMENT ON COLUMN "membership"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "membership_benefit" IS '会员权益';
COMMENT ON COLUMN "membership_benefit"."id" IS '自增主键';
COMMENT ON COLUMN "membership_benefit"."code" IS '稳定业务代码';
COMMENT ON COLUMN "membership_benefit"."name" IS '名称';
COMMENT ON COLUMN "membership_benefit"."enabled" IS '开关';
COMMENT ON COLUMN "membership_benefit"."discount_rate" IS '优惠乘数，例如0.900为九折';
COMMENT ON COLUMN "membership_benefit"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "membership_plan" IS '会员套餐';
COMMENT ON COLUMN "membership_plan"."id" IS '自增主键';
COMMENT ON COLUMN "membership_plan"."name" IS '名称';
COMMENT ON COLUMN "membership_plan"."display_name" IS '展示名称';
COMMENT ON COLUMN "membership_plan"."months" IS '套餐月数';
COMMENT ON COLUMN "membership_plan"."price" IS '单价，人民币元';
COMMENT ON COLUMN "membership_plan"."active" IS '是否启用';
COMMENT ON COLUMN "membership_plan"."updated_at" IS '最后更新时间（由业务显式更新）';
COMMENT ON COLUMN "membership_plan"."gift_coupon_template_id" IS '会员赠券模板ID';
COMMENT ON COLUMN "membership_plan"."gift_coupon_quantity" IS '赠券张数';

COMMENT ON TABLE "membership_purchase" IS '会员购买单';
COMMENT ON COLUMN "membership_purchase"."id" IS '自增主键';
COMMENT ON COLUMN "membership_purchase"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "membership_purchase"."plan_id" IS '会员套餐ID';
COMMENT ON COLUMN "membership_purchase"."plan_name" IS '购买时套餐名快照';
COMMENT ON COLUMN "membership_purchase"."months" IS '套餐月数';
COMMENT ON COLUMN "membership_purchase"."amount" IS '金额，人民币元';
COMMENT ON COLUMN "membership_purchase"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "membership_purchase"."created_at" IS '创建时间';
COMMENT ON COLUMN "membership_purchase"."paid_at" IS '支付时间';
COMMENT ON COLUMN "membership_purchase"."gift_coupon_template_id" IS '会员赠券模板ID';
COMMENT ON COLUMN "membership_purchase"."gift_coupon_quantity" IS '赠券张数';

COMMENT ON TABLE "notification" IS '站内通知';
COMMENT ON COLUMN "notification"."id" IS '自增主键';
COMMENT ON COLUMN "notification"."user_id" IS '关联统一账号';
COMMENT ON COLUMN "notification"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "notification"."code" IS '稳定业务代码';
COMMENT ON COLUMN "notification"."title" IS '标题';
COMMENT ON COLUMN "notification"."content" IS '文本内容';
COMMENT ON COLUMN "notification"."read_at" IS '已读时间，空为未读';
COMMENT ON COLUMN "notification"."delivery_status" IS '站内投递状态';
COMMENT ON COLUMN "notification"."created_at" IS '创建时间';

COMMENT ON TABLE "notification_template" IS '消息模板';
COMMENT ON COLUMN "notification_template"."id" IS '自增主键';
COMMENT ON COLUMN "notification_template"."code" IS '稳定业务代码';
COMMENT ON COLUMN "notification_template"."name" IS '名称';
COMMENT ON COLUMN "notification_template"."enabled" IS '开关';
COMMENT ON COLUMN "notification_template"."channels" IS '配置投递渠道JSON，目前仅站内落地';
COMMENT ON COLUMN "notification_template"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "operation_log" IS '后台操作审计';
COMMENT ON COLUMN "operation_log"."id" IS '自增主键';
COMMENT ON COLUMN "operation_log"."actor_id" IS '操作人账号，定时任务可空';
COMMENT ON COLUMN "operation_log"."action" IS '操作名称';
COMMENT ON COLUMN "operation_log"."resource" IS '业务资源类型';
COMMENT ON COLUMN "operation_log"."resource_id" IS '业务资源ID';
COMMENT ON COLUMN "operation_log"."before_data" IS '变更前JSON快照';
COMMENT ON COLUMN "operation_log"."after_data" IS '变更后JSON快照';
COMMENT ON COLUMN "operation_log"."result" IS '结果JSON';
COMMENT ON COLUMN "operation_log"."created_at" IS '创建时间';

COMMENT ON TABLE "order_event" IS '订单事件流水';
COMMENT ON COLUMN "order_event"."id" IS '自增主键';
COMMENT ON COLUMN "order_event"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "order_event"."actor_id" IS '操作人账号，定时任务可空';
COMMENT ON COLUMN "order_event"."event" IS '事件代码';
COMMENT ON COLUMN "order_event"."detail" IS '事件详情JSON';
COMMENT ON COLUMN "order_event"."created_at" IS '创建时间';

COMMENT ON TABLE "order_item" IS '订单服务明细快照';
COMMENT ON COLUMN "order_item"."id" IS '自增主键';
COMMENT ON COLUMN "order_item"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "order_item"."service_id" IS '价格配置ID';
COMMENT ON COLUMN "order_item"."code" IS '稳定业务代码';
COMMENT ON COLUMN "order_item"."name" IS '名称';
COMMENT ON COLUMN "order_item"."unit_price" IS '下单时单价快照';
COMMENT ON COLUMN "order_item"."quantity" IS '数量';
COMMENT ON COLUMN "order_item"."required_category" IS '服务要求的资质类别';

COMMENT ON TABLE "owner_address" IS '宠主地址';
COMMENT ON COLUMN "owner_address"."id" IS '自增主键';
COMMENT ON COLUMN "owner_address"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "owner_address"."contact_name" IS '联系人';
COMMENT ON COLUMN "owner_address"."phone" IS '手机号码';
COMMENT ON COLUMN "owner_address"."district" IS '行政区';
COMMENT ON COLUMN "owner_address"."address" IS '详细地址';
COMMENT ON COLUMN "owner_address"."longitude" IS '经度';
COMMENT ON COLUMN "owner_address"."latitude" IS '纬度';
COMMENT ON COLUMN "owner_address"."created_at" IS '创建时间';

COMMENT ON TABLE "payment_record" IS '支付记录';
COMMENT ON COLUMN "payment_record"."id" IS '自增主键';
COMMENT ON COLUMN "payment_record"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "payment_record"."purchase_id" IS '会员购买单ID';
COMMENT ON COLUMN "payment_record"."trade_no" IS '交易流水号';
COMMENT ON COLUMN "payment_record"."provider" IS '支付渠道，当前DEMO';
COMMENT ON COLUMN "payment_record"."amount" IS '金额，人民币元';
COMMENT ON COLUMN "payment_record"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "payment_record"."paid_at" IS '支付时间';

COMMENT ON TABLE "pet" IS '宠物档案';
COMMENT ON COLUMN "pet"."id" IS '自增主键';
COMMENT ON COLUMN "pet"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "pet"."name" IS '名称';
COMMENT ON COLUMN "pet"."category" IS '宠物或认证类别';
COMMENT ON COLUMN "pet"."breed" IS '品种';
COMMENT ON COLUMN "pet"."birthday" IS '出生日期';
COMMENT ON COLUMN "pet"."weight" IS '体重kg';
COMMENT ON COLUMN "pet"."temperament" IS '性格与注意事项';
COMMENT ON COLUMN "pet"."dietary_notes" IS '饮食说明';
COMMENT ON COLUMN "pet"."medical_history" IS '病史说明';
COMMENT ON COLUMN "pet"."photo_url" IS '照片地址';
COMMENT ON COLUMN "pet"."deleted" IS '软删除标志';
COMMENT ON COLUMN "pet"."created_at" IS '创建时间';
COMMENT ON COLUMN "pet"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "pet_order" IS '服务订单';
COMMENT ON COLUMN "pet_order"."id" IS '自增主键';
COMMENT ON COLUMN "pet_order"."order_no" IS '业务订单号';
COMMENT ON COLUMN "pet_order"."request_key" IS '同宠主下单幂等键';
COMMENT ON COLUMN "pet_order"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "pet_order"."sitter_id" IS '喂养师档案ID';
COMMENT ON COLUMN "pet_order"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "pet_order"."service_start" IS '预约服务开始';
COMMENT ON COLUMN "pet_order"."service_end" IS '预约服务结束';
COMMENT ON COLUMN "pet_order"."address_snapshot" IS '下单地址JSON快照';
COMMENT ON COLUMN "pet_order"."pet_snapshot" IS '下单宠物JSON快照';
COMMENT ON COLUMN "pet_order"."notes" IS '备注';
COMMENT ON COLUMN "pet_order"."total_amount" IS '原价总额';
COMMENT ON COLUMN "pet_order"."member_discount" IS '会员减免额';
COMMENT ON COLUMN "pet_order"."coupon_discount" IS '优惠券减免额';
COMMENT ON COLUMN "pet_order"."payable_amount" IS '实际应付金额';
COMMENT ON COLUMN "pet_order"."coupon_id" IS '本次锁定或核销的用户券ID';
COMMENT ON COLUMN "pet_order"."commission_rate" IS '平台抽成百分数，18表示18%';
COMMENT ON COLUMN "pet_order"."signed_agreement" IS '服务协议版本及授权范围JSON快照';
COMMENT ON COLUMN "pet_order"."dispatch_reason" IS '人工派单原因JSON';
COMMENT ON COLUMN "pet_order"."accept_deadline" IS '接单截止时间';
COMMENT ON COLUMN "pet_order"."created_at" IS '创建时间';
COMMENT ON COLUMN "pet_order"."paid_at" IS '支付时间';
COMMENT ON COLUMN "pet_order"."checkin_at" IS '进门打卡时间';
COMMENT ON COLUMN "pet_order"."checkout_at" IS '服务完成离门时间';

COMMENT ON TABLE "refund_record" IS '退款记录';
COMMENT ON COLUMN "refund_record"."id" IS '自增主键';
COMMENT ON COLUMN "refund_record"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "refund_record"."amount" IS '金额，人民币元';
COMMENT ON COLUMN "refund_record"."reason" IS '业务说明或失败理由';
COMMENT ON COLUMN "refund_record"."operator_id" IS '操作人账号ID';
COMMENT ON COLUMN "refund_record"."provider" IS '支付渠道，当前DEMO';
COMMENT ON COLUMN "refund_record"."created_at" IS '创建时间';

COMMENT ON TABLE "review" IS '订单评价';
COMMENT ON COLUMN "review"."id" IS '自增主键';
COMMENT ON COLUMN "review"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "review"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "review"."sitter_id" IS '喂养师档案ID';
COMMENT ON COLUMN "review"."score" IS '评分1至5';
COMMENT ON COLUMN "review"."tags" IS '评价标签JSON数组';
COMMENT ON COLUMN "review"."content" IS '文本内容';
COMMENT ON COLUMN "review"."created_at" IS '创建时间';

COMMENT ON TABLE "role_permission" IS '角色权限';
COMMENT ON COLUMN "role_permission"."id" IS '自增主键';
COMMENT ON COLUMN "role_permission"."role" IS '账号角色';
COMMENT ON COLUMN "role_permission"."permission" IS '权限代码，管理员为*';

COMMENT ON TABLE "service_checklist" IS '服务检查清单';
COMMENT ON COLUMN "service_checklist"."id" IS '自增主键';
COMMENT ON COLUMN "service_checklist"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "service_checklist"."code" IS '稳定业务代码';
COMMENT ON COLUMN "service_checklist"."completed" IS '是否完成';
COMMENT ON COLUMN "service_checklist"."quantity" IS '数量';
COMMENT ON COLUMN "service_checklist"."unit" IS '计量单位，陪玩必须MINUTE';
COMMENT ON COLUMN "service_checklist"."notes" IS '备注';
COMMENT ON COLUMN "service_checklist"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "service_item" IS '服务价格配置';
COMMENT ON COLUMN "service_item"."id" IS '自增主键';
COMMENT ON COLUMN "service_item"."code" IS '稳定业务代码';
COMMENT ON COLUMN "service_item"."name" IS '名称';
COMMENT ON COLUMN "service_item"."description" IS '说明';
COMMENT ON COLUMN "service_item"."price" IS '单价，人民币元';
COMMENT ON COLUMN "service_item"."required_category" IS '服务要求的资质类别';
COMMENT ON COLUMN "service_item"."active" IS '是否启用';
COMMENT ON COLUMN "service_item"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "service_media" IS '服务媒体元数据';
COMMENT ON COLUMN "service_media"."id" IS '自增主键';
COMMENT ON COLUMN "service_media"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "service_media"."uploader_id" IS '上传者账号ID';
COMMENT ON COLUMN "service_media"."kind" IS '媒体类型PHOTO/VIDEO/LIVE';
COMMENT ON COLUMN "service_media"."object_key" IS '相对存储对象键，不是公网播放地址';
COMMENT ON COLUMN "service_media"."expires_at" IS '到期时间';
COMMENT ON COLUMN "service_media"."deleted" IS '软删除标志';
COMMENT ON COLUMN "service_media"."created_at" IS '创建时间';

COMMENT ON TABLE "settlement" IS '喂养师结算';
COMMENT ON COLUMN "settlement"."id" IS '自增主键';
COMMENT ON COLUMN "settlement"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "settlement"."sitter_id" IS '喂养师档案ID';
COMMENT ON COLUMN "settlement"."gross_amount" IS '计费基数，按订单实付金额';
COMMENT ON COLUMN "settlement"."commission_rate" IS '平台抽成百分数，18表示18%';
COMMENT ON COLUMN "settlement"."platform_amount" IS '平台抽成金额';
COMMENT ON COLUMN "settlement"."sitter_amount" IS '喂养师应得金额';
COMMENT ON COLUMN "settlement"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "settlement"."settled_at" IS '财务确认结算时间';
COMMENT ON COLUMN "settlement"."created_at" IS '创建时间';

COMMENT ON TABLE "sitter_grade" IS '当前等级规则';
COMMENT ON COLUMN "sitter_grade"."id" IS '自增主键';
COMMENT ON COLUMN "sitter_grade"."code" IS '稳定业务代码';
COMMENT ON COLUMN "sitter_grade"."name" IS '名称';
COMMENT ON COLUMN "sitter_grade"."min_orders" IS '等级最低完成单量';
COMMENT ON COLUMN "sitter_grade"."min_rating" IS '等级最低平均评分';
COMMENT ON COLUMN "sitter_grade"."max_complaints" IS '最大重大投诉数';
COMMENT ON COLUMN "sitter_grade"."commission_rate" IS '平台抽成百分数，18表示18%';
COMMENT ON COLUMN "sitter_grade"."dispatch_weight" IS '预留派单权重，不执行AI计算';
COMMENT ON COLUMN "sitter_grade"."high_risk_enabled" IS '能否承接遛狗/用药/驱虫高风险服务';
COMMENT ON COLUMN "sitter_grade"."active" IS '是否启用';
COMMENT ON COLUMN "sitter_grade"."effective_at" IS '规则生效时间';
COMMENT ON COLUMN "sitter_grade"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "sitter_grade_revision" IS '等级规则待生效版本';
COMMENT ON COLUMN "sitter_grade_revision"."id" IS '自增主键';
COMMENT ON COLUMN "sitter_grade_revision"."grade_id" IS '等级ID';
COMMENT ON COLUMN "sitter_grade_revision"."rule_data" IS '待生效规则完整JSON';
COMMENT ON COLUMN "sitter_grade_revision"."effective_at" IS '规则生效时间';
COMMENT ON COLUMN "sitter_grade_revision"."applied_at" IS '实际生效时间，空表示待执行';
COMMENT ON COLUMN "sitter_grade_revision"."operator_id" IS '操作人账号ID';
COMMENT ON COLUMN "sitter_grade_revision"."created_at" IS '创建时间';

COMMENT ON TABLE "sitter_profile" IS '喂养师档案';
COMMENT ON COLUMN "sitter_profile"."id" IS '自增主键';
COMMENT ON COLUMN "sitter_profile"."user_id" IS '关联统一账号';
COMMENT ON COLUMN "sitter_profile"."code" IS '稳定业务代码';
COMMENT ON COLUMN "sitter_profile"."grade_id" IS '等级ID';
COMMENT ON COLUMN "sitter_profile"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "sitter_profile"."emergency_contact" IS '紧急联系人';
COMMENT ON COLUMN "sitter_profile"."avatar_url" IS '头像地址';
COMMENT ON COLUMN "sitter_profile"."notes" IS '备注';
COMMENT ON COLUMN "sitter_profile"."district" IS '行政区';
COMMENT ON COLUMN "sitter_profile"."address" IS '详细地址';
COMMENT ON COLUMN "sitter_profile"."longitude" IS '经度';
COMMENT ON COLUMN "sitter_profile"."latitude" IS '纬度';
COMMENT ON COLUMN "sitter_profile"."radius_km" IS '服务半径0.5至50km';
COMMENT ON COLUMN "sitter_profile"."transport" IS '交通方式';
COMMENT ON COLUMN "sitter_profile"."settlement_method" IS '结算方式';
COMMENT ON COLUMN "sitter_profile"."categories" IS '擅长宠物类别JSON数组';
COMMENT ON COLUMN "sitter_profile"."rating" IS '综合评分';
COMMENT ON COLUMN "sitter_profile"."completed_orders" IS '已完成订单数';
COMMENT ON COLUMN "sitter_profile"."major_complaints" IS '重大投诉数';
COMMENT ON COLUMN "sitter_profile"."created_at" IS '创建时间';
COMMENT ON COLUMN "sitter_profile"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "sitter_qualification" IS '喂养师分类资质';
COMMENT ON COLUMN "sitter_qualification"."id" IS '自增主键';
COMMENT ON COLUMN "sitter_qualification"."sitter_id" IS '喂养师档案ID';
COMMENT ON COLUMN "sitter_qualification"."category" IS '宠物或认证类别';
COMMENT ON COLUMN "sitter_qualification"."real_verified" IS '实名认证是否通过';
COMMENT ON COLUMN "sitter_qualification"."training_passed" IS '培训是否通过';
COMMENT ON COLUMN "sitter_qualification"."practice_passed" IS '实操考核是否通过';
COMMENT ON COLUMN "sitter_qualification"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "sitter_qualification"."expires_at" IS '到期时间';
COMMENT ON COLUMN "sitter_qualification"."materials" IS '证明材料对象键JSON数组';
COMMENT ON COLUMN "sitter_qualification"."reviewer_id" IS '审核人账号ID';
COMMENT ON COLUMN "sitter_qualification"."review_note" IS '审核意见';
COMMENT ON COLUMN "sitter_qualification"."updated_at" IS '最后更新时间（由业务显式更新）';

COMMENT ON TABLE "sitter_schedule" IS '每周七天档期';
COMMENT ON COLUMN "sitter_schedule"."id" IS '自增主键';
COMMENT ON COLUMN "sitter_schedule"."sitter_id" IS '喂养师档案ID';
COMMENT ON COLUMN "sitter_schedule"."weekday" IS '星期1至7，周一为1';
COMMENT ON COLUMN "sitter_schedule"."rest" IS '是否休息';
COMMENT ON COLUMN "sitter_schedule"."start_time" IS '当日接单开始时间';
COMMENT ON COLUMN "sitter_schedule"."end_time" IS '当日接单结束时间';
COMMENT ON COLUMN "sitter_schedule"."capacity" IS '当日容量0至20';
COMMENT ON COLUMN "sitter_schedule"."district" IS '行政区';
COMMENT ON COLUMN "sitter_schedule"."address" IS '详细地址';
COMMENT ON COLUMN "sitter_schedule"."longitude" IS '经度';
COMMENT ON COLUMN "sitter_schedule"."latitude" IS '纬度';

COMMENT ON TABLE "support_conversation" IS '人工客服会话';
COMMENT ON COLUMN "support_conversation"."id" IS '自增主键';
COMMENT ON COLUMN "support_conversation"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "support_conversation"."order_id" IS '服务订单ID';
COMMENT ON COLUMN "support_conversation"."agent_id" IS '接管人工客服账号ID';
COMMENT ON COLUMN "support_conversation"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "support_conversation"."created_at" IS '创建时间';

COMMENT ON TABLE "user_coupon" IS '宠主持券快照';
COMMENT ON COLUMN "user_coupon"."id" IS '自增主键';
COMMENT ON COLUMN "user_coupon"."template_id" IS '券模板ID';
COMMENT ON COLUMN "user_coupon"."owner_id" IS '宠主账号ID';
COMMENT ON COLUMN "user_coupon"."source" IS '来源claim主动领取或push后台直发';
COMMENT ON COLUMN "user_coupon"."amount" IS '金额，人民币元';
COMMENT ON COLUMN "user_coupon"."threshold" IS '最低消费门槛，元';
COMMENT ON COLUMN "user_coupon"."service_codes" IS '可用服务代码JSON数组，空表示不限';
COMMENT ON COLUMN "user_coupon"."status" IS '业务状态，枚举见本表CHECK约束';
COMMENT ON COLUMN "user_coupon"."expires_at" IS '到期时间';
COMMENT ON COLUMN "user_coupon"."created_at" IS '创建时间';

-- V5__sitter_service_preferences.sql
ALTER TABLE sitter_profile ADD COLUMN service_preferences varchar(1000) NOT NULL DEFAULT '';
COMMENT ON COLUMN sitter_profile.service_preferences IS '喂养师自述服务经验与偏好，仅用于订单推荐，不替代资质审核';

-- V6__user_avatar.sql
ALTER TABLE app_user ADD COLUMN avatar_key text;

-- V7__ai_customer_service.sql
ALTER TABLE chat_message ADD COLUMN sender_type varchar(20) NOT NULL DEFAULT 'USER'
    CHECK (sender_type IN ('USER', 'HUMAN', 'AI'));
ALTER TABLE support_conversation DROP CONSTRAINT support_conversation_status_check;
ALTER TABLE support_conversation ADD CONSTRAINT support_conversation_status_check
    CHECK (status IN ('WAITING_HUMAN', 'HUMAN_REQUESTED', 'HUMAN', 'CLOSED'));

-- V8__sitter_admission_feedback.sql
ALTER TABLE sitter_profile ADD COLUMN admission_review_note varchar(1000);
COMMENT ON COLUMN sitter_profile.admission_review_note IS '最近一次喂养师入驻审核意见；驳回时供申请人修改后重新提交';

COMMIT;
