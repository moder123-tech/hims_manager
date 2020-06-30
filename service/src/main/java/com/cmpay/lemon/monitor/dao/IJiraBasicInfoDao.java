/*
 * @ClassName IJiraBasicInfoDao
 * @Description 
 * @version 1.0
 * @Date 2020-06-30 14:26:53
 */
package com.cmpay.lemon.monitor.dao;

import com.cmpay.lemon.framework.dao.BaseDao;
import com.cmpay.lemon.monitor.entity.JiraBasicInfoDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IJiraBasicInfoDao extends BaseDao<JiraBasicInfoDO, String> {
}