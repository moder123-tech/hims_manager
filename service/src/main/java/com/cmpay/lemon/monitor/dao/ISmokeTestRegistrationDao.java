/*
 * @ClassName ISmokeTestRegistrationDao
 * @Description 
 * @version 1.0
 * @Date 2020-06-18 14:35:53
 */
package com.cmpay.lemon.monitor.dao;

import com.cmpay.lemon.framework.dao.BaseDao;
import com.cmpay.lemon.monitor.entity.SmokeTestRegistrationDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ISmokeTestRegistrationDao extends BaseDao<SmokeTestRegistrationDO, Integer> {
}