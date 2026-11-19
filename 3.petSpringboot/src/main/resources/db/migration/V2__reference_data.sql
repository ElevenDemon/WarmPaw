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
