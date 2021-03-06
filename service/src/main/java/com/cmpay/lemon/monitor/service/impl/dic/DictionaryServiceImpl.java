package com.cmpay.lemon.monitor.service.impl.dic;

import com.cmpay.lemon.monitor.bo.DictionaryBO;
import com.cmpay.lemon.monitor.dao.IDictionaryExtDao;
import com.cmpay.lemon.monitor.entity.DictionaryDO;
import com.cmpay.lemon.monitor.service.dic.DictionaryService;
import com.cmpay.lemon.monitor.utils.BeanConvertUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author zhou_xiong
 */
@Service
@Transactional(propagation = Propagation.SUPPORTS,rollbackFor = RuntimeException.class)
public class DictionaryServiceImpl implements DictionaryService {
    static final String  WORKLOADLOCKSTATUS="WORKLOAD_LOCK_STATUS";
    static final String  PRDLINE="PRD_LINE";
    @Autowired
    private IDictionaryExtDao dictionaryDao;

    @Override
    public DictionaryBO getDicByDicId(DictionaryDO dictionaryDO) {

        List<DictionaryDO> dictionaryDOList = dictionaryDao.getDicByDicId(dictionaryDO);
        // 产品线只展示最新数据
        if(PRDLINE.equals(dictionaryDO.getDicId())){
            dictionaryDOList = dictionaryDOList.subList(0,44);
        }

        List<DictionaryBO> dictionaryBOList = BeanConvertUtils.convertList(dictionaryDOList, DictionaryBO.class);
        DictionaryBO dictionaryBO = new DictionaryBO();
        dictionaryBO.setDictionaryBOList(dictionaryBOList);
        return dictionaryBO;
    }

    @Override
    public DictionaryBO getJdInfo(DictionaryDO dictionaryDO) {

        List<DictionaryDO> dictionaryDOList = dictionaryDao.getJdInfo(dictionaryDO);

        List<DictionaryBO> dictionaryBOList = BeanConvertUtils.convertList(dictionaryDOList, DictionaryBO.class);
        DictionaryBO dictionaryBO = new DictionaryBO();
        dictionaryBO.setDictionaryBOList(dictionaryBOList);
        return dictionaryBO;
    }

    @Override
    public String findFieldName(String fieldId, String fieldValue)  {
        String fieldName = "";
        Map<String, String> map = new HashMap<String, String>();
        map.put("fieldId", fieldId);
        map.put("fieldValue", fieldValue);
        if ("DEPART_NAME".equals(fieldId)) {
            fieldName = dictionaryDao.findDepartId(fieldValue);
        } else {
            fieldName = dictionaryDao.findFieldName(map);
        }
        return (null == fieldName || "".equals(fieldName)) ? "" : fieldName;
    }

    @Override
    public List<DictionaryBO> findUploadPeriod(String reqPeriod){
        List<DictionaryDO> lst =  dictionaryDao.findUploadPeriod(reqPeriod);
        List<DictionaryBO> dictionaryBOList = BeanConvertUtils.convertList(lst, DictionaryBO.class);
        return dictionaryBOList;
    }

    @Override
    public List<DictionaryBO> getcpInfo(DictionaryDO dictionaryDO) {
        List<DictionaryDO> lst =  dictionaryDao.findProManager();
        List<DictionaryBO> dictionaryBOList = BeanConvertUtils.convertList(lst, DictionaryBO.class);
        return dictionaryBOList;
    }

    @Override
    public List<DictionaryBO> getPeople(DictionaryDO dictionaryDO) {
        List<DictionaryDO> lst =  dictionaryDao.findPeople();
        List<DictionaryBO> dictionaryBOList = BeanConvertUtils.convertList(lst, DictionaryBO.class);
        return dictionaryBOList;
    }

    @Override
    public DictionaryBO findPordmod() {

        List<DictionaryDO> dictionaryDOList = dictionaryDao.findPordmod();

        List<DictionaryBO> dictionaryBOList = BeanConvertUtils.convertList(dictionaryDOList, DictionaryBO.class);
        DictionaryBO dictionaryBO = new DictionaryBO();
        dictionaryBO.setDictionaryBOList(dictionaryBOList);
        return dictionaryBO;
    }
    @Override
    public DictionaryBO findDictionary() {

        List<DictionaryDO> dictionaryDOList = dictionaryDao.findDictionary();

        List<DictionaryBO> dictionaryBOList = BeanConvertUtils.convertList(dictionaryDOList, DictionaryBO.class);
        DictionaryBO dictionaryBO = new DictionaryBO();
        dictionaryBO.setDictionaryBOList(dictionaryBOList);
        return dictionaryBO;
    }

    @Override
    public void workloadLockStatus() {
        DictionaryDO dictionaryDO = new DictionaryDO();
        dictionaryDO.setDicId(WORKLOADLOCKSTATUS);
        List<DictionaryDO> dictionaryDOList = dictionaryDao.getDicByDicId(dictionaryDO);
        String workloadLockStatus= dictionaryDOList.get(0).getValue();
        if(workloadLockStatus.equals("off")){
            //变为开启
            workloadLockStatus="no";
        }else{
            //变为关闭
            workloadLockStatus="off";
        }
        dictionaryDao.updateWorkloadLockStatus(workloadLockStatus);
    }

    @Override
    public String findFieldValue(String req_peroid, String preCurPeriod) {
        String value ="";
        value= dictionaryDao.getValue(req_peroid, preCurPeriod);
        return  value;
    }

}
