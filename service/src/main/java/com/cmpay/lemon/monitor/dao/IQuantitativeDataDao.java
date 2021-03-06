/*
 * @ClassName IQuantitativeDataDao
 * @Description 
 * @version 1.0
 * @Date 2020-11-11 14:28:53
 */
package com.cmpay.lemon.monitor.dao;

import com.cmpay.lemon.framework.dao.BaseDao;
import com.cmpay.lemon.monitor.entity.QuantitativeDataDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IQuantitativeDataDao extends BaseDao<QuantitativeDataDO, Long> {
}