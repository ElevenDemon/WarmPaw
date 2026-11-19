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
