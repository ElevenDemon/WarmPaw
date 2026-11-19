package com.nuanzhua.pet.service;
import com.nuanzhua.pet.dto.ConfigRequests;
import com.nuanzhua.pet.vo.Actor;

import java.time.*;
import java.util.*;

/** 业务服务契约；调用方通过接口依赖具体实现。 */
public interface GradeRuleService {
    /** 计算上海时区下一个半月结算期：每月1日或16日00:00。 */
    public static OffsetDateTime nextPeriod() {
        var now=ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return (now.getDayOfMonth()<16?now.withDayOfMonth(16):now.plusMonths(1).withDayOfMonth(1)).toLocalDate().atStartOfDay(now.getZone()).toOffsetDateTime();
    }
    long save(Actor actor,Long id,ConfigRequests.Grade r);
    void apply();
}
