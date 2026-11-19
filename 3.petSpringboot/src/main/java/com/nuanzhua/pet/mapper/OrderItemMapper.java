package com.nuanzhua.pet.mapper;
import com.nuanzhua.pet.entity.OrderItem;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/** 业务SQL数据访问；实体CRUD继承MyBatis-Plus BaseMapper。 */
@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {}
