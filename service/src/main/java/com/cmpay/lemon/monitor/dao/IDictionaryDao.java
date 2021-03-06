/*
 * @ClassName ILogTypeDao
 * @Description 
 * @version 1.0
 * @Date 2019-07-03 11:31:38
 */
package com.cmpay.lemon.monitor.dao;

import com.cmpay.lemon.framework.dao.BaseDao;
import com.cmpay.lemon.monitor.entity.DictionaryDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IDictionaryDao extends BaseDao<DictionaryDO, Long> {
}