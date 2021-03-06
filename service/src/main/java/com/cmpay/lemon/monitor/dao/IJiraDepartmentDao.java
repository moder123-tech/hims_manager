/*
 * @ClassName IJiraDepartmentDao
 * @Description 
 * @version 1.0
 * @Date 2019-09-29 10:11:21
 */
package com.cmpay.lemon.monitor.dao;

import com.cmpay.lemon.framework.dao.BaseDao;
import com.cmpay.lemon.monitor.entity.JiraDepartmentDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IJiraDepartmentDao extends BaseDao<JiraDepartmentDO, String> {
}