const action = (label, schema, path, permission, method='PUT', defaults={}) => ({ label,schema,path,permission,method,defaults })
export const edits = {
 price:action('编辑价格','Price','/admin/prices/:id','price:update'),
 plans:action('编辑套餐','Plan','/admin/plans/:id','member:update'),
 benefits:action('编辑权益','Benefit','/admin/benefits/:id','member:update'),
 grades:action('编辑等级规则','Grade','/admin/grades/:id','sitter:rule'),
 coupon:action('配置优惠券','Coupon','/admin/coupons/:id','coupon:update'),
 msg:action('编辑通知模板','Notice','/admin/notification-templates/:id','msg:update'),
 user:action('设置账号状态','Status','/admin/owners/:id/status','user:risk'),
 member:action('设置会员状态','MemberStatus','/admin/memberships/:id/status','member:update'),
 sitter:action('编辑完整档案','Profile','/admin/sitters/:id','sitter:update'),
 audit:action('审核入驻申请','AdmissionAudit','/admin/sitters/:id/audit','audit:update','POST'),
 qualifications:action('审核资质','Audit','/admin/qualifications/:id/audit','audit:update','POST'),
 emergency:action('处理应急工单','EmergencyUpdate','/admin/emergencies/:id','emergency:update'),
 complaints:action('处理投诉','ComplaintUpdate','/admin/complaints/:id','review:update'),
 roles:action('配置角色完整权限','Permissions','/admin/roles/:role/permissions','system:update'),
}
export const creates = {
 plans:action('新增套餐','Plan','/admin/plans','member:update','POST',{ active:true, months:1 }),
 grades:action('新增等级','Grade','/admin/grades','sitter:rule','POST',{ active:true }),
 coupon:action('新增优惠券','Coupon','/admin/coupons','coupon:update','POST',{ active:true, issueType:'claim', audience:'ALL', validDays:30, perUserLimit:1, stock:0, serviceCodes:[] }),
 hospitals:action('新增合作医院','Hospital','/admin/hospitals','emergency:update','POST',{ active:true }),
 staff:action('新增后台账号','Staff','/admin/staff','system:update','POST'),
}
export const assignAction = action('人工派单','Assign','/admin/orders/:id/assign','dispatch:update','POST')
export const pushAction = action('直发优惠券','Push','/admin/coupons/:id/push','coupon:update','POST',{ quantity:1 })
export const cancelAction = action('取消订单','Reason','/orders/:id/cancel','order:update','POST')
export const refundAction = action('演示退款','Reason','/admin/orders/:id/demo-refund','finance:refund','POST')
