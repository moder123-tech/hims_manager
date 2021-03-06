/*
 * @ClassName ISmokeTestFailedCountDao
 * @Description 
 * @version 1.0
 * @Date 2020-09-16 16:08:58
 */
package com.cmpay.lemon.monitor.dao;

import com.cmpay.lemon.framework.dao.BaseDao;
import com.cmpay.lemon.monitor.entity.SmokeTestFailedCountDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ISmokeTestFailedCountDao extends BaseDao<SmokeTestFailedCountDO, Integer> {
}