package com.nuanzhua.pet.mapper;
import com.nuanzhua.pet.entity.MembershipPurchase;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 业务SQL数据访问；实体CRUD继承MyBatis-Plus BaseMapper。 */
@Mapper
public interface MembershipPurchaseMapper extends BaseMapper<MembershipPurchase> {}
