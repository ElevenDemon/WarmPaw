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
