package com.cmpay.lemon.monitor.service.impl.reportForm;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.BeanUtils;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.framework.page.PageInfo;
import com.cmpay.lemon.framework.utils.PageUtils;
import com.cmpay.lemon.monitor.bo.*;
import com.cmpay.lemon.monitor.dao.*;
import com.cmpay.lemon.monitor.entity.*;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import com.cmpay.lemon.monitor.service.demand.ReqPlanService;
import com.cmpay.lemon.monitor.service.demand.ReqTaskService;
import com.cmpay.lemon.monitor.service.impl.demand.ReqPlanServiceImpl;
import com.cmpay.lemon.monitor.service.production.OperationProductionService;
import com.cmpay.lemon.monitor.service.reportForm.ReqDataCountService;
import com.cmpay.lemon.monitor.service.workload.ReqWorkLoadService;
import com.cmpay.lemon.monitor.utils.BeanConvertUtils;
import com.cmpay.lemon.monitor.utils.DateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.xmlbeans.impl.xb.xsdschema.Public;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@Transactional(rollbackFor = Exception.class)
@Service
public class ReqDataCountServiceImpl implements ReqDataCountService {
    //30 ?????????????????????
    private static final String REQSUSPEND = "30";
    //40 ?????????????????????
    private static final String REQCANCEL = "40";
    // 30 ????????????
    private static final int REQCONFIRM = 30;
    // 50 ??????????????????
    private static final int TECHDOCCONFIRM = 50;
    // 70 ??????????????????
    private static final int TESTCASECONFIRM = 70;
    // 110 ??????SIT??????
    private static final int FINISHSITTEST = 110;
    // 120 UAT????????????
    private static final int UPDATEUAT = 120;
    // 140 ??????UAT??????
    private static final int FINISHUATTEST = 140;
    // 160 ???????????????
    private static final int FINISHPRETEST = 160;
    // 180 ??????????????????
    private static final int FINISHPRD = 180;

    // 30 ????????????
    private static final String REQCONFIRM1 = "30";
    // 50 ??????????????????
    private static final String TECHDOCCONFIRM1 = "50";
    // 110 ??????SIT??????
    private static final String FINISHSITTEST1 = "110";
    // 140 ??????UAT??????
    private static final String FINISHUATTEST1 = "140";
    // 160 ???????????????
    private static final String FINISHPRETEST1 = "160";
    // 180 ??????????????????
    private static final String FINISHPRD1 = "180";

    private static final String XINGQISAN = "?????????";

    private static final Logger LOGGER = LoggerFactory.getLogger(ReqPlanServiceImpl.class);
    @Autowired
    private IDemandExtDao demandDao;
    @Autowired
    private IReqDataCountDao reqDataCountDao;
    @Autowired
    private IWorkingHoursExtDao iWorkingHoursDao;
    @Autowired
    private IDemandQualityExtDao iDemandQualityDao;
    @Autowired
    private ReqPlanService reqPlanService;
    @Autowired
    private IDemandResourceInvestedDao iDemandResourceInvestedDao;
    @Autowired
    IDemandJiraDao demandJiraDao;
    @Autowired
    IJiraBasicInfoDao jiraBasicInfoDao;
    @Autowired
    private OperationProductionService operationProductionService;
    @Autowired
    private IProblemStatisticDao problemStatisticDao;
    @Autowired
    private IDefectDetailsExtDao defectDetailsExtDao;
    @Autowired
    private IIssueDetailsExtDao issueDetailsExtDao;
    @Autowired
    private IProductionDefectsExtDao productionDefectsExtDao;
    @Autowired
    private ReqWorkLoadService reqWorkLoadService;
    @Autowired
    private IProCheckTimeOutStatisticsExtDao proCheckTimeOutStatisticsExtDao;
    @Autowired
    private IProUnhandledIssuesExtDao proUnhandledIssuesExtDao;
    @Autowired
    private IOperationProductionDao iOperationProductionDao;
    @Autowired
    private IBuildFailedCountDao iBuildFailedCountDao;
    @Autowired
    private ISmokeTestFailedCountExtDao iSmokeTestFailedCountDao;
    @Autowired
    private IOrganizationStructureDao iOrganizationStructureDao;
    @Autowired
    private IDictionaryExtDao dictionaryDao;
    @Autowired
    private IUserExtDao iUserDao;
    @Autowired
    private ITestProgressDetailExtDao testProgressDetailExtDao;
    @Autowired
    private IMonthWorkdayDao monthWorkdayDao;
    @Autowired
    private  IProductionVerificationIsNotTimelyExtDao iProductionVerificationIsNotTimelyExtDao;
    @Resource
    private IDemandEaseDevelopmentExtDao easeDevelopmentExtDao;
    @Autowired
    private IProblemExtDao iProblemDao;
    @Autowired
    private ReqTaskService reqTaskService;
    @Autowired
    private IOnlineDefectExtDao onlineDefectExtDao;
    @Autowired
    private IQuantitativeDataExtDao quantitativeDataExtDao;
    @Autowired
    private IGitlabDataExtDao gitlabDataDao;


    /**
     * ?????????????????????????????????????????????????????????
     */
    @Override
    public Map getProg(String report_start_mon, String report_end_mon) {
        Map DataMap = new TreeMap();

        ReqDataCountDO reqDataCountDO = reqDataCountDao.getProg(report_start_mon, report_end_mon);

        if ((reqDataCountDO == null)) {
            return DataMap;
        }

        DataMap.put("?????????", reqDataCountDO.getReqOper());
        DataMap.put("?????????", reqDataCountDO.getReqIng());

        return DataMap;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    @Override
    public Map getProgDetl(String report_start_mon, String report_end_mon) {
        Map DataMap = new TreeMap();

        ReqDataCountDO reqDataCountD0 = reqDataCountDao.getProgDetl(report_start_mon, report_end_mon);

        if ((reqDataCountD0 == null)) {
            return DataMap;
        }

        DataMap.put("????????????", reqDataCountD0.getReqPrd());
        DataMap.put("????????????", reqDataCountD0.getReqDevp());
        DataMap.put("????????????", reqDataCountD0.getReqTest());
        DataMap.put("???????????????", reqDataCountD0.getReqPre());
        DataMap.put("?????????", reqDataCountD0.getReqOper());

        return DataMap;
    }

    /**
     * ???????????????????????????????????????
     */
    @Override
    public Map getAbnoByDept(String report_start_mon, String report_end_mon) {
        Map DataMap = new TreeMap();

        List<ReqDataCountDO> list = reqDataCountDao.getAbnoByDept(report_start_mon, report_end_mon);

        if ((list == null)) {
            return DataMap;
        }

        for (int i = 0; i < list.size(); i++) {
            DataMap.put(list.get(i).getDevpLeadDept(), list.get(i).getReqUnusual());
        }

        return DataMap;
    }

    /**
     * ??????????????????????????????????????????
     */
    @Override
    public Map getAbnoByLine(String report_start_mon, String report_end_mon) {
        Map DataMap = new TreeMap();

        List<ReqDataCountDO> list = reqDataCountDao.getAbnoByLine(report_start_mon, report_end_mon);

        if ((list == null)) {
            return DataMap;
        }

        for (int i = 0; i < list.size(); i++) {
            DataMap.put(list.get(i).getReqPrdLine(), list.get(i).getReqUnusual());
        }

        return DataMap;
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     */
    @Override
    public Map getProgByDept(String report_start_mon, String report_end_mon) {
        Map DataMap = new TreeMap();

        List<ReqDataCountDO> list = reqDataCountDao.getProgByDept(report_start_mon, report_end_mon);

        if ((list == null)) {
            return DataMap;
        }

        for (int i = 0; i < list.size(); i++) {
            Map Data = new TreeMap();
            Data.put("?????????", list.get(i).getReqOper());
            Data.put("?????????", list.get(i).getReqIng());

            DataMap.put(list.get(i).getDevpLeadDept(), Data);
        }

        return DataMap;
    }

    /**
     * ???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
     */
    @Override
    public Map getProgDetlByDept(String report_start_mon, String report_end_mon) {
        Map DataMap = new TreeMap();

        List<ReqDataCountDO> list = reqDataCountDao.getProgDetlByDept(report_start_mon, report_end_mon);

        if ((list == null)) {
            return DataMap;
        }

        for (int i = 0; i < list.size(); i++) {
            Map Data = new TreeMap();
            Data.put("????????????", list.get(i).getReqPrd());
            Data.put("????????????", list.get(i).getReqDevp());
            Data.put("????????????", list.get(i).getReqTest());
            Data.put("???????????????", list.get(i).getReqPre());
            Data.put("?????????", list.get(i).getReqOper());

            DataMap.put(list.get(i).getReqPrdLine(), Data);
        }


        return DataMap;
    }


    /**
     * map???json
     *
     * @param DataMap
     * @return jsonStr
     */
    public String strToJson(Map DataMap) {
        String bigJsonStr = "";

        Set treeSet = new TreeSet();
        treeSet = DataMap.keySet();

        String smallJson = "";

        StringBuilder bigJson = new StringBuilder();

        for (Iterator localIterator = treeSet.iterator(); localIterator.hasNext(); ) {
            String amtSrc = (String) localIterator.next();

            smallJson = new StringBuilder().append("{\"value\":\"").append((String) DataMap.get(amtSrc)).append("\",\"name\":\"").append(amtSrc).append("\"},").toString();
            bigJson.append(smallJson);
        }

        bigJsonStr = bigJson.toString();

        bigJsonStr = new StringBuilder().append("{\"LineData\":[").append(bigJsonStr.substring(0, bigJsonStr.length() - 1)).append("]}").toString();

        return bigJsonStr;

    }

    @Override
    public List<ReqDataCountBO> getImpl(String ReqImplMon) {
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        List<ReqDataCountDO> impl = reqDataCountDao.getImpl(ReqImplMon);
        impl.forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );

        return reqDataCountBOS;
    }

    @Override
    public List<ReqDataCountBO> getImplByDept(String reqImplMon) {
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        reqDataCountDao.getImplByDept(reqImplMon).forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );
        return reqDataCountBOS;
    }

    @Override
    public List<ReqDataCountBO> getComp(String reqImplMon) {
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        List<ReqDataCountDO> comp = reqDataCountDao.getComp(reqImplMon);
        comp.forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );
        return reqDataCountBOS;
    }

    @Override
    public List<ReqDataCountBO> getCompByDept(String reqImplMon) {
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        reqDataCountDao.getCompByDept(reqImplMon).forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );

        return reqDataCountBOS;
    }

    public List<ReqDataCountBO> getCompByDept2(String reqImplMon) {
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        reqDataCountDao.getCompByDept2(reqImplMon).forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );

        return reqDataCountBOS;
    }

    @Override
    public List<ReqDataCountBO> getReqSts(String reqImplMon) {
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        List<ReqDataCountDO> impl = reqDataCountDao.getReqSts(reqImplMon);
        impl.forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );
        return reqDataCountBOS;
    }

    @Override
    public List<ScheduleBO> getProduction(String reqImplMon) {
        List<ScheduleBO> reqDataCountBOS = new LinkedList<>();
        List<ScheduleDO> impl = reqDataCountDao.getProduction(reqImplMon);
        impl.forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ScheduleBO(), m))
        );
        return reqDataCountBOS;
    }
    @Override
    public List<DemandBO> getReportForm6(String reqImplMon, String devpLeadDept, String productMng, String firstLevelOrganization) {
        List<DemandBO> reqDataCountBOS = new LinkedList<>();
        ReqDateCountDO reqDateCountDO = new ReqDateCountDO();
        reqDateCountDO.setReqImplMon(reqImplMon);
        reqDateCountDO.setDevpLeadDept(devpLeadDept);
        reqDateCountDO.setProductMng(productMng);
        reqDateCountDO.setFirstLevelOrganization(firstLevelOrganization);
        List<DemandDO> impl = reqDataCountDao.getReportForm6(reqDateCountDO);
        impl.forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new DemandBO(), m))
        );
        for (int i = 0; i < reqDataCountBOS.size(); i++) {
            int sum = 0;
            //????????????????????????????????????????????????6?????????
            if (FINISHPRD == Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())) {
                if (reqDataCountBOS.get(i).getActPrdUploadTm() == null || reqDataCountBOS.get(i).getActPrdUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActWorkloadUploadTm() == null || reqDataCountBOS.get(i).getActWorkloadUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActSitUploadTm() == null || reqDataCountBOS.get(i).getActSitUploadTm() == "") {
                    sum = sum + 1;
                }
//				if(reqDataCountBOS.get(i).getActUatUploadTm()==null || reqDataCountBOS.get(i).getActUatUploadTm()==""){
//					sum=sum+1;
//				}
                if (reqDataCountBOS.get(i).getActPreUploadTm() == null || reqDataCountBOS.get(i).getActPreUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActProductionUploadTm() == null || reqDataCountBOS.get(i).getActProductionUploadTm() == "") {
                    sum = sum + 1;
                }
            }
            //?????????
            else if (FINISHPRETEST <= Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())  && FINISHPRD > Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())) {
                if (reqDataCountBOS.get(i).getActPrdUploadTm() == null || reqDataCountBOS.get(i).getActPrdUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActWorkloadUploadTm() == null || reqDataCountBOS.get(i).getActWorkloadUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActSitUploadTm() == null || reqDataCountBOS.get(i).getActSitUploadTm() == "") {
                    sum = sum + 1;
                }
//				if(reqDataCountBOS.get(i).getActUatUploadTm()==null || reqDataCountBOS.get(i).getActUatUploadTm()==""){
//                    sum=sum+1;
//				}
                if (reqDataCountBOS.get(i).getActPreUploadTm() == null || reqDataCountBOS.get(i).getActPreUploadTm() == "") {
                    sum = sum + 1;
                }
            }
            // uat
            else if (FINISHUATTEST <= Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())&& FINISHPRETEST > Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())) {
                if (reqDataCountBOS.get(i).getActPrdUploadTm() == null || reqDataCountBOS.get(i).getActPrdUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActWorkloadUploadTm() == null || reqDataCountBOS.get(i).getActWorkloadUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActSitUploadTm() == null || reqDataCountBOS.get(i).getActSitUploadTm() == "") {
                    sum = sum + 1;
                }
//				if(reqDataCountBOS.get(i).getActUatUploadTm()==null || reqDataCountBOS.get(i).getActUatUploadTm()==""){
//                    sum=sum+1;
//				}
            }
            // sit
            else if (FINISHSITTEST <= Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2()) && FINISHUATTEST > Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())) {
                if (reqDataCountBOS.get(i).getActPrdUploadTm() == null || reqDataCountBOS.get(i).getActPrdUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActWorkloadUploadTm() == null || reqDataCountBOS.get(i).getActWorkloadUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActSitUploadTm() == null || reqDataCountBOS.get(i).getActSitUploadTm() == "") {
                    sum = sum + 1;
                }
            }
            // ????????????
            else if (TECHDOCCONFIRM <= Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2()) && FINISHSITTEST > Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())) {
                if (reqDataCountBOS.get(i).getActPrdUploadTm() == null || reqDataCountBOS.get(i).getActPrdUploadTm() == "") {
                    sum = sum + 1;
                }
                if (reqDataCountBOS.get(i).getActWorkloadUploadTm() == null || reqDataCountBOS.get(i).getActWorkloadUploadTm() == "") {
                    sum = sum + 1;
                }
            }
            // ????????????
            else if (REQCONFIRM <= Integer.parseInt(reqDataCountBOS.get(i).getPreCurPeriod2())) {
                if (reqDataCountBOS.get(i).getActPrdUploadTm() == null || reqDataCountBOS.get(i).getActPrdUploadTm() == "") {
                    sum = sum + 1;
                }
            }
            reqDataCountBOS.get(i).setNoUpload(sum);
        }
        return reqDataCountBOS;
    }

    @Override
    public List<WorkingHoursBO> getReportForm7(String devpLeadDept, String date, String date1, String date2) {
        List<WorkingHoursBO> workingHoursBOS = new LinkedList<>();
        List<WorkingHoursDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        if (StringUtils.isBlank(date) && StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("???????????????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (StringUtils.isNotBlank(date) && StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date);
            impl = iWorkingHoursDao.findSum(workingHoursDO);
        }
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekSum(workingHoursDO);
        }
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthSum(workingHoursDO);
            // ???????????????????????????
            int day = DateUtil.getDaysByYearMonth(date2);
            for(int j=0;j<impl.size();j++){
                List<String> listDay = new LinkedList<>();
                for(int i=1;i<=day;i++){
                    WorkingHoursDO workingHoursDO1 = new WorkingHoursDO();
                    if(i<10){
                        String day1 = date2 +"-0"+i ;
                        workingHoursDO1.setSelectTime(day1);
                    }
                    if(i>=10){
                        String day1 = date2 +"-"+ i;
                        workingHoursDO1.setSelectTime(day1);
                    }
                    workingHoursDO1.setDisplayname(impl.get(j).getDisplayname());
                    workingHoursDO1.setDevpLeadDept(devpLeadDept);
                    List<WorkingHoursDO> impl1 = iWorkingHoursDao.findSum(workingHoursDO1);
                    if(impl1.isEmpty()){
                        listDay.add("0");
                    }else{
                        listDay.add(getWorkHours(impl1.get(0).getSumTime())+"");
                    }

                }
                impl.get(j).setListDay(listDay);
                impl.get(j).setSumTime(getWorkHours(impl.get(j).getSumTime())+"");
            }


        }
        impl.forEach(m ->
                workingHoursBOS.add(BeanUtils.copyPropertiesReturnDest(new WorkingHoursBO(), m))
        );
        return workingHoursBOS;
    }
    public Double getWorkHours(String value){
        Long time = Long.parseLong(value);
        return (double) (Math.round(time* 100 /  3600)/ 100.0);
    }
    @Override
    public List<WorkingHoursBO> getReportForm7B(String devpLeadDept, String date, String date1, String date2) {
        List<WorkingHoursBO> workingHoursBOS = new LinkedList<>();
        List<WorkingHoursDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        if (StringUtils.isBlank(date) && StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("???????????????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (StringUtils.isNotBlank(date) && StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date);
            impl = iWorkingHoursDao.findSumB(workingHoursDO);
        }
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekSumB(workingHoursDO);
        }
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthSumB(workingHoursDO);
        }

        impl.forEach(m ->
                workingHoursBOS.add(BeanUtils.copyPropertiesReturnDest(new WorkingHoursBO(), m))
        );
        return workingHoursBOS;
    }

    @Override
    public List<String> getReportForm8(String date) {
        List<String> workingHoursBOS = new LinkedList<>();
        List<DepartmentWorkDO> impl = iWorkingHoursDao.findDeptHours(date);
        impl.forEach(m ->
                workingHoursBOS.add(m.getWorkHoursToString())
        );
        return workingHoursBOS;
    }

    ;

    @Override
    public List<WorkingHoursBO> findEpicKeyHours(String epic) {
        List<WorkingHoursBO> workingHoursBOS = new LinkedList<>();
        System.err.println(epic);
        List<WorkingHoursDO> impl = iWorkingHoursDao.findEpicKeyHours(epic);
        System.err.println(impl);
        impl.forEach(m ->
                workingHoursBOS.add(BeanUtils.copyPropertiesReturnDest(new WorkingHoursBO(), m))
        );
        System.err.println(workingHoursBOS);
        return workingHoursBOS;
    }

    @Override
    public DemandQualityBO findDemandQuality(String epic) {
        DemandQualityBO demandQualityBO = new DemandQualityBO();
        DemandQualityDO demandQualityDO = iDemandQualityDao.get(epic);
        if (demandQualityDO == null || "".equals(demandQualityBO)) {
            return demandQualityBO;
        }
        BeanUtils.copyPropertiesReturnDest(demandQualityBO, demandQualityDO);
        return demandQualityBO;
    }

    @Override
    public WorkingHoursBO getReportForm10(String devpLeadDept, String date1, String date2) {
        OrganizationStructureDO organizationStructureDO = new OrganizationStructureDO();
        organizationStructureDO.setSecondlevelorganization(devpLeadDept);
        List<OrganizationStructureDO> organizationStructureDOList = iOrganizationStructureDao.find(organizationStructureDO);
        WorkingHoursBO workingHoursBO = new WorkingHoursBO();
        List<WorkingHoursDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        impl = iWorkingHoursDao.findDeptView(workingHoursDO);
        if (impl != null && impl.size() >= 0) {
            BeanUtils.copyPropertiesReturnDest(workingHoursBO, impl.get(0));
            DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                defectDetailsDO.setRegistrationDate(date1.substring(0,7));
            }
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                defectDetailsDO.setRegistrationDate(date2);
            }
            List<DefectDetailsDO> list = defectDetailsExtDao.findList(defectDetailsDO);
            if(list == null ||list.size()<0){
                workingHoursBO.setMeanDefect("0");
            }else{
                //??????????????????
                Date date = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
                String month = simpleDateFormat.format(date);
                int day = 1;
                //?????????????????????????????????
                if(month.equals(defectDetailsDO.getRegistrationDate())){
                    day = DateUtil.getDaysByMonth();
                }else{
                    day = DateUtil.getDaysByYearMonth(defectDetailsDO.getRegistrationDate());
                }
                // ???????????????????????????

                int a = list.size();
                int b = Integer.parseInt(workingHoursBO.getSumDept()) * day;
                //??????????????????
                DecimalFormat df = new DecimalFormat("0.##");
                String meanDefect = df.format((float)a/b);
                workingHoursBO.setMeanDefect(meanDefect);
            }
        }
        workingHoursBO.setComment(organizationStructureDOList.get(0).getFirstlevelorganization());
        return workingHoursBO;
    }

    /**
     * ???????????? ?????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptWorkHours(String devpLeadDept, String date1, String date2) {
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        List<WorkingHoursDO> impl = null;
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekSum(workingHoursDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthSum(workingHoursDO);
            System.err.println(impl);
        }
        System.err.println(impl);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        double sumx = 0;
        DecimalFormat df = new DecimalFormat("0.##");
        if (impl != null && impl.size() >= 0) {
            for (int i = 0; i < impl.size(); i++) {
                SumBos.add(impl.get(i).getDisplayname());
                sumx = sumx + getWorkHoursTime(Integer.parseInt(impl.get(i).getSumTime()));
                workingHoursBOS.add(df.format(getWorkHoursTime(Integer.parseInt(impl.get(i).getSumTime()))));
            }
            sum = df.format(sumx);
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setSum(sum);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setStringList(workingHoursBOS);
        System.err.println(demandHoursRspBO);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getDeptWorkHoursAndPoint(String devpLeadDept, String date1, String date2) {
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        DecimalFormat df = new DecimalFormat("0");
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        List<WorkingHoursDO> impl = null;
        DemandBO demandBO = new DemandBO();
        demandBO.setDevpLeadDept(devpLeadDept);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekSumB(workingHoursDO);
            //demandBO.setReqImplMon(date1.substring(0,7));
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    SumBos.add(df.format(getWorkHours(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }
            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
//            // ????????????????????????
//            System.err.println(demandBO);
//            List<Double> dept =reqWorkLoadService.getExportCountForDevp(demandBO);
//            if(dept.get(0) != null){
//                workingHoursBOS.add("?????????");
//                SumBos.add(Double.toString(dept.get(0)));
//            }else {
            workingHoursBOS.add("?????????");
            SumBos.add("0");
//            }
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthSumB(workingHoursDO);
            demandBO.setReqImplMon(date2);

            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    SumBos.add(df.format(getWorkHours(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }
            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
            // ????????????????????????
            List<Double> dept = reqWorkLoadService.getExportCountForDevp(demandBO);
            if (dept.get(0) != null) {
                workingHoursBOS.add("?????????");
                SumBos.add(df.format(dept.get(0)));
            } else {
                workingHoursBOS.add("?????????");
                SumBos.add("0");
            }
        }

        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    public DemandHoursRspBO getDeptWorkHoursAndPoint2(String devpLeadDept, String date1, String date2) {
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        List<WorkingHoursDO> impl = null;
        System.err.println(date1 + "=====" + date2);
        DemandBO demandBO = new DemandBO();
        demandBO.setDevpLeadDept(devpLeadDept);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekSumB(workingHoursDO);
            //demandBO.setReqImplMon(date1.substring(0,7));
            System.err.println(impl);
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    DecimalFormat df = new DecimalFormat("0");
                    SumBos.add(df.format(getWorkHoursTime(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }
            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
            workingHoursBOS.add("?????????");
            SumBos.add("0");
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthSumB(workingHoursDO);
            demandBO.setReqImplMon(date2);

            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    DecimalFormat df = new DecimalFormat("0.##");
                    SumBos.add(df.format(getWorkHours(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }
            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
            // ????????????????????????
            System.err.println(demandBO);
            List<Double> dept = reqWorkLoadService.getExportCountForDevp(demandBO);
            if (dept.get(0) != null) {
                workingHoursBOS.add("?????????");
                DecimalFormat df = new DecimalFormat("0.##");
                SumBos.add(df.format(dept.get(0)));
            } else {
                workingHoursBOS.add("?????????");
                SumBos.add("0");
            }
        }

        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    public DemandHoursRspBO getDeptWorkHoursAndPoint3(String devpLeadDept, String date1, String date2) {
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        DecimalFormat df = new DecimalFormat("0.###");
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        List<WorkingHoursDO> impl = null;
        DemandBO demandBO = new DemandBO();
        demandBO.setDevpLeadDept(devpLeadDept);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekSumB(workingHoursDO);
            //demandBO.setReqImplMon(date1.substring(0,7));
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    SumBos.add(df.format(getWorkHours(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }
            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
//            // ????????????????????????
//            System.err.println(demandBO);
//            List<Double> dept =reqWorkLoadService.getExportCountForDevp(demandBO);
//            if(dept.get(0) != null){
//                workingHoursBOS.add("?????????");
//                SumBos.add(Double.toString(dept.get(0)));
//            }else {
            workingHoursBOS.add("?????????");
            SumBos.add("0");
//            }
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthSumB(workingHoursDO);
            demandBO.setReqImplMon(date2);

            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    SumBos.add(df.format(getWorkHours(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }
            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
            // ????????????????????????
            List<Double> dept = reqWorkLoadService.getExportCountForDevp(demandBO);
            if (dept.get(0) != null) {
                workingHoursBOS.add("?????????");
                SumBos.add(df.format(dept.get(0)));
            } else {
                workingHoursBOS.add("?????????");
                SumBos.add("0");
            }
        }

        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getDeptProduction(String devpLeadDept, String date1, String date2) {
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        List<ProductionDO> impl = null;
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findListDeptWeek(workingHoursDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findListDeptMonth(workingHoursDO);
            System.err.println(impl);
        }
        System.err.println(impl);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            System.err.println(impl.size());
            sum = impl.size() + "";
            for (int i = 0; i < impl.size(); i++) {
                if ("????????????".equals(impl.get(i).getProType()) && "???".equals(impl.get(i).getIsOperationProduction())) {
                    a = a + 1;
                }
                if ("????????????".equals(impl.get(i).getProType()) && "???".equals(impl.get(i).getIsOperationProduction())) {
                    b = b + 1;
                }
                if ("????????????".equals(impl.get(i).getProType())) {
                    c = c + 1;
                }
            }
            if (a > 0) {
                String a1 = "{'value': '" + a + "', 'name': '????????????'}";
                workingHoursBOS.add(a1);
                SumBos.add("????????????");
            }
            if (b > 0) {
                String b1 = "{'value': '" + b + "', 'name': '????????????????????????'}";
                workingHoursBOS.add(b1);
                SumBos.add("????????????????????????");
            }
            if (c > 0) {
                String c1 = "{'value': '" + c + "', 'name': '????????????'}";
                workingHoursBOS.add(c1);
                SumBos.add("????????????");
            }
        } else {
            sum = "0";
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum);
        return demandHoursRspBO;
    }

    public static String testDate(String newtime) {
        String dayNames[] = {"?????????", "?????????", "?????????", "?????????", "?????????", "?????????", "?????????"};
        Calendar c = Calendar.getInstance();// ?????????????????????zhi??????
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            c.setTime(sdf.parse(newtime));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return dayNames[c.get(Calendar.DAY_OF_WEEK) - 1];
    }

    /**
     * ????????????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDemandDispose(String devpLeadDept, String date1, String date2) {
        List<DemandDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        workingHoursDO.setSelectTime(date2);
        impl = demandDao.findListDeptDemand(workingHoursDO);
        System.err.println(impl);

        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            System.err.println(impl.size());
            sum = impl.size() + "";
            for (int i = 0; i < impl.size(); i++) {
                if (!impl.get(i).getReqSts().equals("30") && !impl.get(i).getReqSts().equals("40")) {
                    a = a + 1;
                }
                if (impl.get(i).getReqSts().equals("30")) {
                    b = b + 1;
                }
                if (impl.get(i).getReqSts().equals("40")) {
                    c = c + 1;
                }
            }
            if (a > 0) {
                String a1 = "{'value': '" + a + "', 'name': '????????????'}";
                workingHoursBOS.add(a1);
                SumBos.add("????????????");
            }
            if (b > 0) {
                String b1 = "{'value': '" + b + "', 'name': '????????????'}";
                workingHoursBOS.add(b1);
                SumBos.add("????????????");
            }
            if (c > 0) {
                String c1 = "{'value': '" + c + "', 'name': '????????????'}";
                workingHoursBOS.add(c1);
                SumBos.add("????????????");
            }
        } else {
            sum = "0";
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum);
        return demandHoursRspBO;
    }

    /**
     * ????????????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDemandCoordinate(String devpLeadDept, String date1, String date2) {
        List<DemandDO> impl = null;
        List<DemandDO> impl2 = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        workingHoursDO.setSelectTime(date2);
        impl = demandDao.findListDeptDemand(workingHoursDO);
        impl2 = demandDao.findListDevpCoorDeptDemand(workingHoursDO);
        System.err.println(impl);
        System.err.println(impl2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        int sum = 0;
        if (impl != null && impl.size() >= 0) {
            workingHoursBOS.add(impl.size() + "");
            SumBos.add("????????????");
            sum = sum + impl.size();
        } else {
            workingHoursBOS.add("0");
            SumBos.add("????????????");
        }
        if (impl2 != null && impl2.size() != 0) {
            workingHoursBOS.add(impl2.size() + "");
            SumBos.add("????????????");
            sum = sum + impl2.size();
        } else {
            workingHoursBOS.add("0");
            SumBos.add("????????????");
        }
        String simx = sum + "";
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(simx);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptFlawNumber(String devpLeadDept, String date1, String date2) {
        List<DefectDetailsDO> impl = null;
        DefectDetailsDO workingHoursDO = new DefectDetailsDO();
        if(!"??????????????????".equals(devpLeadDept)){
            workingHoursDO.setProblemHandlerDepartment(devpLeadDept);
        }
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setRegistrationDate(list.get(i));
            impl = defectDetailsExtDao.findList(workingHoursDO);
            SumBos.add(list.get(i));
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }

        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptIssueNumber(String devpLeadDept, String date1, String date2) {
        List<IssueDetailsDO> impl = null;
        IssueDetailsDO workingHoursDO = new IssueDetailsDO();
        workingHoursDO.setIssueDepartment(devpLeadDept);
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setRegistrationDate(list.get(i));
            impl = issueDetailsExtDao.findList(workingHoursDO);
            SumBos.add(list.get(i));
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptProductionDefects(String devpLeadDept, String date1, String date2) {
        OrganizationStructureDO  organizationStructureDO = new OrganizationStructureDO();
        organizationStructureDO.setSecondlevelorganization(devpLeadDept);
        List<OrganizationStructureDO> organizationStructureDOList = iOrganizationStructureDao.find(organizationStructureDO);

        List<ProductionDefectsDO> impl = null;
        List<OnlineDefectDO> impl2 = null;
        ProductionDefectsDO workingHoursDO = new ProductionDefectsDO();
        OnlineDefectDO onlineDefectDO = new OnlineDefectDO();
        onlineDefectDO.setFirstlevelorganization(organizationStructureDOList.get(0).getFirstlevelorganization());
        workingHoursDO.setProblemattributiondept(devpLeadDept);
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            // 2020-09 ????????????????????????
            if(list.get(i).compareTo("2020-09")<0){
                workingHoursDO.setProcessstartdate(list.get(i));
                impl = productionDefectsExtDao.findList(workingHoursDO);
                SumBos.add(list.get(i));
                if (impl != null && impl.size() >= 0) {
                    workingHoursBOS.add(impl.size() + "");
                } else {
                    workingHoursBOS.add("0");
                }
            }else{
                // 2020-09????????????????????????????????????
                onlineDefectDO.setProcessStartDate(list.get(i));
                impl2 = onlineDefectExtDao.findList(onlineDefectDO);
                SumBos.add(list.get(i));
                if (impl2 != null && impl2.size() >= 0) {
                    workingHoursBOS.add(impl2.size() + "");
                } else {
                    workingHoursBOS.add("0");
                }
            }

        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptProCheckTimeOutStatistics(String devpLeadDept, String date1, String date2) {
        List<ProCheckTimeOutStatisticsDO> impl = null;
        ProCheckTimeOutStatisticsDO workingHoursDO = new ProCheckTimeOutStatisticsDO();
        workingHoursDO.setDepartment(devpLeadDept);
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setRegistrationdate(list.get(i));
            impl = proCheckTimeOutStatisticsExtDao.findMonth(workingHoursDO);
            SumBos.add(list.get(i));
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    sum = sum + impl.get(j).getCount();
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ?????????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptProUnhandledIssues1(String devpLeadDept, String date1, String date2) {
        List<ProUnhandledIssuesDO> impl = null;
        ProUnhandledIssuesDO workingHoursDO = new ProUnhandledIssuesDO();
        workingHoursDO.setDepartment(devpLeadDept);
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setProductionDate(list.get(i));
            impl = proUnhandledIssuesExtDao.findMonth(workingHoursDO);
            SumBos.add(list.get(i));
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    if(impl.get(j).getDefectsNumber()>0){
                        sum = sum + 1;
                    }
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ?????????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptProUnhandledIssues2(String devpLeadDept, String date1, String date2) {
        List<ProUnhandledIssuesDO> impl = null;
        ProUnhandledIssuesDO workingHoursDO = new ProUnhandledIssuesDO();
        workingHoursDO.setDepartment(devpLeadDept);
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setProductionDate(list.get(i));
            impl = proUnhandledIssuesExtDao.findMonth(workingHoursDO);
            SumBos.add(list.get(i));
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    if(impl.get(j).getProblemNumber()>0){
                        sum = sum + 1;
                    }
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    public static java.sql.Date strToDate(String strDate) {
        String str = strDate;
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM");
        java.util.Date d = null;
        try {
            d = format.parse(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        java.sql.Date date = new java.sql.Date(d.getTime());
        return date;
    }

    /**
     * ????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptProductionBack(String devpLeadDept, String date1, String date2) {
        List<ProductionDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);

        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setSelectTime(list.get(i));
            impl = iOperationProductionDao.findMonth(workingHoursDO);
            SumBos.add(list.get(i));
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    if ("????????????".equals(impl.get(j).getProStatus())) {
                        sum = sum + 1;
                    }
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDeptSum(String devpLeadDept, String date1, String date2) {
        List<ProductionDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        // ????????????
        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
        defectDetailsDO.setProblemHandlerDepartment(devpLeadDept);
        defectDetailsDO.setRegistrationDate(date1);
        List<DefectDetailsDO> impl1 = null;
        impl1 = defectDetailsExtDao.findWeekList(defectDetailsDO);
        SumBos.add("????????????");
        if (impl1 != null && impl1.size() != 0) {
            workingHoursBOS.add(impl1.size() + "");
        } else {
            workingHoursBOS.add("0");
        }
        // ????????????
        IssueDetailsDO issueDetailsDO = new IssueDetailsDO();
        issueDetailsDO.setIssueDepartment(devpLeadDept);
        issueDetailsDO.setRegistrationDate(date1);
        List<IssueDetailsDO> impl2 = null;
        impl2 = issueDetailsExtDao.findWeekList(issueDetailsDO);
        SumBos.add("????????????");
        if (impl2 != null && impl2.size() != 0) {
            workingHoursBOS.add(impl2.size() + "");
        } else {
            workingHoursBOS.add("0");
        }
        // ????????????
        List<ProductionDefectsDO> impl3 = null;
        ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
        productionDefectsDO.setProblemattributiondept(devpLeadDept);
        productionDefectsDO.setProcessstartdate(date1);
        impl3 = productionDefectsExtDao.findWeekList(productionDefectsDO);
        SumBos.add("????????????");
        if (impl3 != null && impl3.size() != 0) {
            workingHoursBOS.add(impl3.size() + "");
        } else {
            workingHoursBOS.add("0");
        }
        //???????????????
        List<ProCheckTimeOutStatisticsDO> impl4 = null;
        ProCheckTimeOutStatisticsDO proCheckTimeOutStatisticsDO = new ProCheckTimeOutStatisticsDO();
        proCheckTimeOutStatisticsDO.setDepartment(devpLeadDept);
        proCheckTimeOutStatisticsDO.setRegistrationdate(date1);
        impl4 = proCheckTimeOutStatisticsExtDao.findWeek(proCheckTimeOutStatisticsDO);
        SumBos.add("?????????????????????");
        int sum = 0;
        if (impl4 != null && impl4.size() != 0) {
            for (int j = 0; j < impl4.size(); j++) {
                sum = sum + impl4.get(j).getCount();
            }
            workingHoursBOS.add(sum + "");
        } else {
            workingHoursBOS.add("0");
        }

        //???????????????
        List<ProUnhandledIssuesDO> impl5 = null;
        ProUnhandledIssuesDO proUnhandledIssuesDO = new ProUnhandledIssuesDO();
        proUnhandledIssuesDO.setDepartment(devpLeadDept);
        proUnhandledIssuesDO.setProductionDate(date1);
        impl5 = proUnhandledIssuesExtDao.findMonth(proUnhandledIssuesDO);
        SumBos.add("?????????????????????");
        int sumx = 0;
        if (impl5 != null && impl5.size() != 0) {
            for (int j = 0; j < impl5.size(); j++) {
                if(impl5.get(j).getDefectsNumber()>0){
                    sumx = sumx + 1;
                }
            }
            workingHoursBOS.add(sumx + "");
        } else {
            workingHoursBOS.add("0");
        }
        //???????????????
        SumBos.add("?????????????????????");
        int sumt = 0;
        if (impl5 != null && impl5.size() != 0) {
            for (int j = 0; j < impl5.size(); j++) {
                sumt = sumt + impl5.get(j).getProblemNumber();
            }
            workingHoursBOS.add(sumt + "");
        } else {
            workingHoursBOS.add("0");
        }
        // ????????????
        workingHoursDO.setSelectTime(date1);
        impl = iOperationProductionDao.findWeek(workingHoursDO);
        SumBos.add("????????????");
        int suma = 0;
        if (impl != null && impl.size() >= 0) {
            for (int j = 0; j < impl.size(); j++) {
                if ("????????????".equals(impl.get(j).getProStatus())) {
                    suma = suma + 1;
                }
            }
            workingHoursBOS.add(suma + "");
        } else {
            workingHoursBOS.add("0");
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getBuildFailedCount(String devpLeadDept, String date1, String date2) {
        List<BuildFailedCountDO> impl = null;
        BuildFailedCountDO buildFailedCountDO = new BuildFailedCountDO();
        buildFailedCountDO.setDepartment(devpLeadDept);
        impl = iBuildFailedCountDao.find(buildFailedCountDO);
        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            for (int i = 0; i < impl.size(); i++) {
                if (impl.get(i).getCount() == 1) {
                    a = a + 1;
                }
                if (impl.get(i).getCount() == 2) {
                    b = b + 1;
                }
                if (impl.get(i).getCount() >= 3) {
                    c = c + 1;
                }
            }
        }
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        SumBos.add("??????");
        workingHoursBOS.add(a + "");
        SumBos.add("??????");
        workingHoursBOS.add(b + "");
        SumBos.add("???????????????");
        workingHoursBOS.add(c + "");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getSmokeTestFailedCount(String devpLeadDept, String date1, String date2) {
        List<SmokeTestFailedCountDO> impl = null;
        SmokeTestFailedCountDO smokeTestFailedCountDO = new SmokeTestFailedCountDO();
        smokeTestFailedCountDO.setDepartment(devpLeadDept);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            smokeTestFailedCountDO.setTestDate(date1);
            impl = iSmokeTestFailedCountDao.findWeek(smokeTestFailedCountDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            smokeTestFailedCountDO.setTestDate(date2);
            impl = iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
        }
        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            for (int i = 0; i < impl.size(); i++) {
                if (impl.get(i).getCount() == 1) {
                    a = a + 1;
                }
                if (impl.get(i).getCount() == 2) {
                    b = b + 1;
                }
                if (impl.get(i).getCount() >= 3) {
                    c = c + 1;
                }
            }
        }
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        SumBos.add("??????");
        workingHoursBOS.add(a + "");
        SumBos.add("??????");
        workingHoursBOS.add(b + "");
        SumBos.add("???????????????");
        workingHoursBOS.add(c + "");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ????????????
     *
     * @return
     */
    @Override
    public DemandHoursRspBO getReportForm12() {
        List<WorkingHoursDO> impl = iWorkingHoursDao.findSumPer();
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        if (impl != null && impl.size() >= 0) {
            demandHoursRspBO.setSum(impl.get(0).getSumTime());
        }
        System.err.println(demandHoursRspBO);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????
     * @param list
     * @return
     */
    @Override
    public DemandHoursRspBO planSearch(List<DemandBO> list){
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        List<WorkingHoursDO> workingHoursDOList = null;
        //??????????????????
        List<OrganizationStructureDO> impl = iOrganizationStructureDao.find(new OrganizationStructureDO());
        DecimalFormat df = new DecimalFormat("0.##");
        if(list == null || list.size()<=0){
            return demandHoursRspBO;
        }
        // ??????????????????
        List<String> deptName = new LinkedList<>();
        for (int i = 0; i < impl.size(); i++) {
            int sum = 0;
            // ?????????????????????
            for(int j=0;j< list.size();j++){
                if(impl.get(i).getSecondlevelorganization().equals(list.get(j).getDevpLeadDept())){
                    sum =sum+1;
                }
            }
            String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ?????????????????????: '" + sum + "'}";
            workingHoursBOS.add(str);

        }
        SumBos.add("product");
        SumBos.add("?????????????????????");

        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }
    /**
     * ?????????????????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreWorkHours(String date1, String date2) {
        System.err.println("????????????????????????");
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        List<WorkingHoursDO> workingHoursDOList = null;
        List<OrganizationStructureDO> impl = iOrganizationStructureDao.find(new OrganizationStructureDO());
        DecimalFormat df = new DecimalFormat("0.##");
        // ??????????????????
        List<String> deptName = new LinkedList<>();
        for (int i = 0; i < impl.size(); i++) {
            deptName.add(impl.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                //??????????????????????????????
                WorkingHoursDO workingHoursDO = new WorkingHoursDO();
                workingHoursDO.setDevpLeadDept(impl.get(i).getSecondlevelorganization());
                workingHoursDOList = iWorkingHoursDao.findDeptView(workingHoursDO);
                String sum = workingHoursDOList.get(0).getSumDept();
                int sumx = 5 * 8 * Integer.parseInt(sum);
                DemandHoursRspBO demandHoursRspBO = getDeptWorkHoursAndPoint2(impl.get(i).getSecondlevelorganization(), date1, date2);
                List<String> listSum = demandHoursRspBO.getListSum();
                double  d1 = (double) (Math.round((long) Integer.parseInt(listSum.get(0)) * 100 / sumx) / 100.0);
                String str = "{ products: '" + impl.get(i).getSecondlevelorganization() + "', ??????????????????: '" + df.format(d1)  + "'}";
                // { product: '??????????????????', ??????: '70', ?????????: '150'},
                workingHoursBOS.add(str);
            }
            //?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                DemandHoursRspBO demandHoursRspBO = getDeptWorkHoursAndPoint(impl.get(i).getSecondlevelorganization(), date1, date2);
                List<String> listSum = demandHoursRspBO.getListSum();
                //??????????????????????????????
                WorkingHoursDO workingHoursDO = new WorkingHoursDO();
                workingHoursDO.setDevpLeadDept(impl.get(i).getSecondlevelorganization());
                workingHoursDOList = iWorkingHoursDao.findDeptView(workingHoursDO);
                String sum = workingHoursDOList.get(0).getSumDept();
                // ???????????????????????????
                MonthWorkdayDO monthWorkdayDO = monthWorkdayDao.getMonth(date2);
                int day = 21;
                if(JudgeUtils.isNotNull(monthWorkdayDO)){
                    day = monthWorkdayDao.getMonth(date2).getWorkPastDay();
                }
                int sumx = day * Integer.parseInt(sum);
                double d1 = 0.00;
                double d2 = 0.00;
                if(Integer.parseInt(listSum.get(0)) != 0){
                    d1 = (double) (Math.round((long) Integer.parseInt(listSum.get(0)) * 100 / sumx) / 100.0);
                    d2 = (double) (Math.round((long) Integer.parseInt(listSum.get(1)) * 100 / Integer.parseInt(listSum.get(0))) / 100.0);
                }
                String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ??????????????????: '" + df.format(d1) + "'}";
                // { product: '??????????????????', ??????: '70', ?????????: '150'},
                workingHoursBOS.add(str);
            }

        }
        SumBos.add("product");
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            SumBos.add("??????????????????");
        }
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            SumBos.add("??????????????????");
        }
        //{sum='null', listSum=[0, 0], stringList=[??????, ?????????]}
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }
    @Override
    public DemandHoursRspBO getCentreWorkHours2(String date1, String date2) {
        System.err.println("????????????????????????");
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        List<WorkingHoursDO> workingHoursDOList = null;
        List<OrganizationStructureDO> impl = iOrganizationStructureDao.find(new OrganizationStructureDO());
        DecimalFormat df = new DecimalFormat("0.##");
        // ??????????????????
        List<String> deptName = new LinkedList<>();
        //????????????
        double sumZhoTai = 0;
        int sumQiTa = 0;
        int x =0;
        for (int i = 0; i < impl.size(); i++) {
            deptName.add(impl.get(i).getSecondlevelorganization());
            //?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                DemandHoursRspBO demandHoursRspBO = getDeptWorkHoursAndPoint(impl.get(i).getSecondlevelorganization(), date1, date2);
                List<String> listSum = demandHoursRspBO.getListSum();
                double d2 = 0.00;
                //???????????? ???????????????
                if("?????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 371;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("?????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 234;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("?????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 382;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("?????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 527;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("?????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 868;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("??????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 203;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("????????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 483;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("????????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 187;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("??????&?????????????????????".equals(impl.get(i).getSecondlevelorganization())||"??????&??????&???????????????".equals(impl.get(i).getSecondlevelorganization())
                ||"???????????????".equals(impl.get(i).getSecondlevelorganization()) ||"???????????????".equals(impl.get(i).getSecondlevelorganization())){
                    x = x+1;
                    sumQiTa = 1785;
                    sumZhoTai = sumZhoTai +  Double.valueOf(listSum.get(1));
                    if(x==4){
                        d2 = (double) (Math.round( sumZhoTai * 100 / sumQiTa) / 100.0);
                        String rate = sumZhoTai+"/"+sumQiTa;
                        String str = "{ product: '" + "????????????" + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                        // { product: '??????????????????', ??????: '70', ?????????: '150'},
                        workingHoursBOS.add(str);
                    }

                }
                if("????????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 250;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
                if("????????????????????????".equals(impl.get(i).getSecondlevelorganization())){
                    sumQiTa = 675;
                    d2 = (double) (Math.round(Double.valueOf(listSum.get(1)) * 100 / sumQiTa) / 100.0);
                    String rate = listSum.get(1)+"/"+sumQiTa;
                    String str = "{ product: '" + impl.get(i).getSecondlevelorganization() + "', ????????????????????????: '" + df.format(d2) + "', rate: '" + rate + "'}";
                    // { product: '??????????????????', ??????: '70', ?????????: '150'},
                    workingHoursBOS.add(str);
                }
            }

        }
        SumBos.add("product");
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            SumBos.add("????????????????????????");
        }
        //{sum='null', listSum=[0, 0], stringList=[??????, ?????????]}
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreWorkHoursPoint(String date1, String date2) {
        System.err.println("??????????????????????????????");
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        List<WorkingHoursDO> impl = null;
        System.err.println(date1 + "=====" + date2);
        DemandBO demandBO = new DemandBO();
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekSumB(workingHoursDO);
            //demandBO.setReqImplMon(date1.substring(0,6));
            System.err.println(impl);
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    DecimalFormat df = new DecimalFormat("0");
                    SumBos.add(df.format(getWorkHours(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }
            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
            workingHoursBOS.add("?????????");
            SumBos.add("0");
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            // ??????????????? ??????
            workingHoursBOS.add("????????????");
            List<WorkingHoursDO> list = iWorkingHoursDao.findSumPer();
            MonthWorkdayDO monthWorkdayDO = monthWorkdayDao.getMonth(date2);
            int day = 21;
            if(JudgeUtils.isNotNull(monthWorkdayDO)){
                day = monthWorkdayDao.getMonth(date2).getWorkSumDay();
            }
            int workTime = day * Integer.parseInt(list.get(0).getSumTime());
            SumBos.add(workTime+"");
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthSumB(workingHoursDO);
            demandBO.setReqImplMon(date2);
            System.err.println("yuegos" + impl);
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add("??????");
                if (!"0".equals(impl.get(0).getSumTime())) {
                    DecimalFormat df = new DecimalFormat("0");
                    SumBos.add(df.format(getWorkHours(Integer.parseInt(impl.get(0).getSumTime()))));
                } else {
                    SumBos.add("0");
                }

            } else {
                workingHoursBOS.add("??????");
                SumBos.add("0");
            }
            workingHoursBOS.add("");
            SumBos.add("");
            workingHoursBOS.add("???????????????");
            SumBos.add("6382.75");
            // ????????????????????????
            System.err.println(demandBO);
            List<Double> dept = reqWorkLoadService.getExportCountForDevp2(demandBO);
            if (dept.get(0) != null) {
                workingHoursBOS.add("?????????");
                DecimalFormat df = new DecimalFormat("0");
                SumBos.add(df.format(dept.get(0)));
            } else {
                workingHoursBOS.add("?????????");
                SumBos.add("0");
            }
        }

        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        System.err.println("????????????????????????" + demandHoursRspBO);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProduction(String date1, String date2) {
        System.err.println("??????????????????");
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        List<ProductionDO> impl = null;
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findListDeptWeek(workingHoursDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findListDeptMonth(workingHoursDO);
        }
        System.err.println(impl);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            System.err.println(impl.size());
            sum = impl.size() + "";
            for (int i = 0; i < impl.size(); i++) {
                if ("????????????".equals(impl.get(i).getProType()) && "???".equals(impl.get(i).getIsOperationProduction())) {
                    a = a + 1;
                }
                if ("????????????".equals(impl.get(i).getProType()) && "???".equals(impl.get(i).getIsOperationProduction())) {
                    b = b + 1;
                }
                if ("????????????".equals(impl.get(i).getProType())) {
                    c = c + 1;
                }
            }
            if (a > 0) {
                String a1 = "{'value': '" + a + "', 'name': '????????????'}";
                workingHoursBOS.add(a1);
                SumBos.add("????????????");
            }
            if (b > 0) {
                String b1 = "{'value': '" + b + "', 'name': '????????????????????????'}";
                workingHoursBOS.add(b1);
                SumBos.add("????????????????????????");
            }
            if (c > 0) {
                String c1 = "{'value': '" + c + "', 'name': '????????????'}";
                workingHoursBOS.add(c1);
                SumBos.add("????????????");
            }
        } else {
            sum = "0";
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getCentreProductionDept(String date1, String date2) {
        System.err.println("???????????????????????????");
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        double sum = 0;
        List<OrganizationStructureDO> impl2 = iOrganizationStructureDao.find(new OrganizationStructureDO());
        // ??????????????????
        List<String> deptName = new LinkedList<>();
        for (int j = 0; j < impl2.size(); j++) {
            deptName.add(impl2.get(j).getSecondlevelorganization());

            WorkingHoursDO workingHoursDO = new WorkingHoursDO();
            workingHoursDO.setDevpLeadDept(impl2.get(j).getSecondlevelorganization());
            List<ProductionDO> impl = null;
            System.err.println(date1 + "=====" + date2);
            if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setSelectTime(date1);
                impl = iWorkingHoursDao.findListDeptWeek(workingHoursDO);
                System.err.println(impl);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setSelectTime(date2);
                impl = iWorkingHoursDao.findListDeptMonth(workingHoursDO);
                System.err.println(impl);
            }
            System.err.println(impl);
            int a = 0;
            int b = 0;
            int c = 0;
            if (impl != null && impl.size() >= 0) {
                System.err.println(impl.size());
                sum = sum + impl.size();
                for (int i = 0; i < impl.size(); i++) {
                    if ("????????????".equals(impl.get(i).getProType()) && "???".equals(impl.get(i).getIsOperationProduction())) {
                        a = a + 1;
                    }
                    if ("????????????".equals(impl.get(i).getProType()) && "???".equals(impl.get(i).getIsOperationProduction())) {
                        b = b + 1;
                    }
                    if ("????????????".equals(impl.get(i).getProType())) {
                        c = c + 1;
                    }
                }
                String str = "{ product: '" + impl2.get(j).getSecondlevelorganization() + "', ????????????: '" + a + "', ????????????????????????: '" + b + "', ????????????: '" + c + "'}";
                workingHoursBOS.add(str);
            }

        }
        //{sum='null', listSum=[0, 0], stringList=[??????, ?????????]}
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum + "");
        System.err.println(demandHoursRspBO);
        return demandHoursRspBO;
    }


    @Override
    public DemandHoursRspBO getTestStaffWork(String date1, String date2) {
        System.err.println(date1);
        System.err.println(date2);
        System.err.println("?????????????????????");
        List<String> workingHoursBOS = new LinkedList<>();
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        //???????????????????????????
        UserDO userDO = new UserDO();
        userDO.setDepartment("??????????????????");
        List<UserDO> userDOS = iUserDao.find(userDO);
        String startTime = "";
        String endTime = "";
        //?????????
        if (JudgeUtils.isBlank(date1) && JudgeUtils.isNotBlank(date2)) {

            //?????????????????????
            SimpleDateFormat simpleDateFormatMonth = new SimpleDateFormat("yyyy-MM");
            try {
                Date month = simpleDateFormatMonth.parse(date2);
                Calendar c = Calendar.getInstance();
                c.setTime(month);
                c.add(Calendar.MONTH, 1);
                startTime = date2;
                endTime = simpleDateFormatMonth.format(c.getTime());
            } catch (Exception e) {
                //todo  ??????????????????
            }
        }
        //?????????
        else if (JudgeUtils.isNotBlank(date1) && JudgeUtils.isBlank(date2)) {
            //?????????????????????
            SimpleDateFormat simpleDateFormatMonth = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date day = simpleDateFormatMonth.parse(date1);
                Calendar c = Calendar.getInstance();
                c.setTime(day);
                c.add(Calendar.DATE, 7);
                startTime = date1;
                endTime = simpleDateFormatMonth.format(c.getTime());
            } catch (Exception e) {
                //todo  ??????????????????
            }
        } else {
            //todo  ??????????????????
        }

        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setStartTime(startTime);
        workingHoursDO.setEndTime(endTime);
        int a1 = 0;
        int b1 = 0;
        int c1 = 0;
        int d1 = 0;
        for(UserDO m:userDOS) {
            DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
            defectDetailsDO.setDefectRegistrant(m.getFullname());
            workingHoursDO.setDisplayname(m.getFullname());
            List<WorkingHoursDO> workingHoursDOS = new LinkedList<>();
            List<DefectDetailsDO> list = new LinkedList<>();
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setSelectTime(date1);
                workingHoursDOS = iWorkingHoursDao.queryByTimeCycle(workingHoursDO);
                defectDetailsDO.setRegistrationDate(date1);
                list = defectDetailsExtDao.findWeekList(defectDetailsDO);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setSelectTime(date2);
                workingHoursDOS = iWorkingHoursDao.queryByTimeCycle(workingHoursDO);
                defectDetailsDO.setRegistrationDate(date2);

                list = defectDetailsExtDao.findList(defectDetailsDO);
            }
            int a = 0;
            int b = 0;
            int d = 0;
            int c = 0;
            if (JudgeUtils.isNotEmpty(workingHoursDOS)) {
                for (int i = 0; i < workingHoursDOS.size(); i++) {
                    a = a + workingHoursDOS.get(i).getCaseWritingNumber();
                    b = b + workingHoursDOS.get(i).getCaseExecutionNumber();
                    d = d + workingHoursDOS.get(i).getCaseCompletedNumber();
                }
            }
            c = c + list.size();
            a1 = a1 + a;
            b1 = b1 + b;
            c1 = c1 + c;
            d1 = d1 + d;
            String str = "{ product: '" + m.getFullname() + "', ???????????????: '" + a + "', ???????????????: '" + b + "', ???????????????: '" + d + "', ?????????: '" + c + "'}";
            workingHoursBOS.add(str);

        }
        String name = "???????????????: " + a1 + ", ???????????????: " + b1 + ", ???????????????: " + d1 + ", ?????????: " + c1 ;
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setSum(name);
        return demandHoursRspBO;
    }


    @Override
    public DemandHoursRspBO getCentreBuildFailedCount(String date1, String date2) {
        List<BuildFailedCountDO> impl = null;
        BuildFailedCountDO buildFailedCountDO = new BuildFailedCountDO();
        impl = iBuildFailedCountDao.find(buildFailedCountDO);
        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            for (int i = 0; i < impl.size(); i++) {
                if (impl.get(i).getCount() == 1) {
                    a = a + 1;
                }
                if (impl.get(i).getCount() == 2) {
                    b = b + 1;
                }
                if (impl.get(i).getCount() >= 3) {
                    c = c + 1;
                }
            }
        }
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        SumBos.add("??????");
        workingHoursBOS.add(a + "");
        SumBos.add("??????");
        workingHoursBOS.add(b + "");
        SumBos.add("???????????????");
        workingHoursBOS.add(c + "");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getCentreSmokeTestFailedCount(String date1, String date2) {
        List<SmokeTestFailedCountDO> impl = null;
        SmokeTestFailedCountDO smokeTestFailedCountDO = new SmokeTestFailedCountDO();
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            smokeTestFailedCountDO.setTestDate(date1);
            impl = iSmokeTestFailedCountDao.findWeek(smokeTestFailedCountDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            smokeTestFailedCountDO.setTestDate(date2);
            impl = iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
        }

        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            for (int i = 0; i < impl.size(); i++) {
                if (impl.get(i).getCount() == 1) {
                    a = a + 1;
                }
                if (impl.get(i).getCount() == 2) {
                    b = b + 1;
                }
                if (impl.get(i).getCount() >= 3) {
                    c = c + 1;
                }
            }
        }
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        SumBos.add("??????");
        workingHoursBOS.add(a + "");
        SumBos.add("??????");
        workingHoursBOS.add(b + "");
        SumBos.add("???????????????");
        workingHoursBOS.add(c + "");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ?????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreFlawNumber(String date1, String date2) {
        List<DefectDetailsDO> impl = null;
        DefectDetailsDO workingHoursDO = new DefectDetailsDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setRegistrationDate(list.get(i));
            impl = defectDetailsExtDao.findList(workingHoursDO);
            SumBos.add(list.get(i));
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }

        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreFlawNumberA(String date1, String date2) {
        List<ProductionDefectsDO> impl = null;
        List<OnlineDefectDO> impl2 = null;
        ProductionDefectsDO workingHoursDO = new ProductionDefectsDO();
        OnlineDefectDO onlineDefectDO = new OnlineDefectDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            // 2020-09 ????????????????????????
            if(list.get(i).compareTo("2020-09")<0){
                workingHoursDO.setProcessstartdate(list.get(i));
                impl = productionDefectsExtDao.findList(workingHoursDO);
                SumBos.add(list.get(i));
                if (impl != null && impl.size() >= 0) {
                    workingHoursBOS.add(impl.size() + "");
                } else {
                    workingHoursBOS.add("0");
                }
            }else{
                // 2020-09????????????????????????????????????
                onlineDefectDO.setProcessStartDate(list.get(i));
                impl2 = onlineDefectExtDao.findList(onlineDefectDO);
                SumBos.add(list.get(i));
                if (impl2 != null && impl2.size() >= 0) {
                    workingHoursBOS.add(impl2.size() + "");
                } else {
                    workingHoursBOS.add("0");
                }
            }


        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreIssueNumber(String date1, String date2) {
        List<IssueDetailsDO> impl = null;
        IssueDetailsDO workingHoursDO = new IssueDetailsDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setRegistrationDate(list.get(i));
            impl = issueDetailsExtDao.findList(workingHoursDO);
            SumBos.add(list.get(i));
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProductionDefects(String date1, String date2) {
        List<ProblemDO> impl = null;
        ProblemDO workingHoursDO = new ProblemDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setReqStartMon(list.get(i));
            impl = iProblemDao.findMonthList(workingHoursDO);
            SumBos.add(list.get(i));
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ?????????????????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProCheckTimeOutStatistics(String date1, String date2) {
        List<ProductionVerificationIsNotTimelyDO> impl = null;
        ProductionVerificationIsNotTimelyDO workingHoursDO = new ProductionVerificationIsNotTimelyDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setProDate(list.get(i));
            impl = iProductionVerificationIsNotTimelyExtDao.findMonth(workingHoursDO);
            SumBos.add(list.get(i));
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    sum = sum + Integer.parseInt(impl.get(j).getSumDay());
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProUnhandledIssues1(String date1, String date2) {
        List<ProUnhandledIssuesDO> impl = null;
        ProUnhandledIssuesDO workingHoursDO = new ProUnhandledIssuesDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setProductionDate(list.get(i));
            impl = proUnhandledIssuesExtDao.findMonth(workingHoursDO);
            SumBos.add(list.get(i));
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    if(impl.get(j).getDefectsNumber() > 0){
                        sum = sum + 1;
                    }
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProUnhandledIssues2(String date1, String date2) {
        List<ProUnhandledIssuesDO> impl = null;
        ProUnhandledIssuesDO workingHoursDO = new ProUnhandledIssuesDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setProductionDate(list.get(i));
            impl = proUnhandledIssuesExtDao.findMonth(workingHoursDO);
            SumBos.add(list.get(i));
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    if(impl.get(j).getProblemNumber() >0){
                        sum = sum + 1;
                    }
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        System.err.println(impl);
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ????????????????????? ???????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreFlawNumberDept(String date1, String date2) {
        // ????????????????????????
        List<OrganizationStructureDO> dos = iOrganizationStructureDao.find(new OrganizationStructureDO());
        List<DefectDetailsDO> impl = null;
        DefectDetailsDO workingHoursDO = new DefectDetailsDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < dos.size(); i++) {
            // ??????4??????????????????????????????????????????
            if("???????????????".equals(dos.get(i).getSecondlevelorganization())||"??????????????????".equals(dos.get(i).getSecondlevelorganization())||"?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                    ||"???????????????".equals(dos.get(i).getSecondlevelorganization()) ||"???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) ||"????????????????????????".equals(dos.get(i).getSecondlevelorganization())){
                continue;
            }
            System.err.println(date1 + "=====" + date2);
            if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            workingHoursDO.setProblemHandlerDepartment(dos.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setRegistrationDate(date1);
                impl = defectDetailsExtDao.findWeekList(workingHoursDO);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setRegistrationDate(date2);
                impl = defectDetailsExtDao.findList(workingHoursDO);
            }
            SumBos.add(dos.get(i).getSecondlevelorganization());
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getCentreFlawNumberDeptA(String date1, String date2) {
        // ????????????????????????
        List<OrganizationStructureDO> dos = iOrganizationStructureDao.find(new OrganizationStructureDO());
        // ??????????????????
        List<String> firstLevelOrganizationList = iOrganizationStructureDao.findFirstLevelOrganization(new OrganizationStructureDO());
        List<ProductionDefectsDO> impl = null;
        List<OnlineDefectDO> impl2 = null;
        ProductionDefectsDO workingHoursDO = new ProductionDefectsDO();
        OnlineDefectDO onlineDefectDO = new OnlineDefectDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        //???????????????????????????????????????????????????
        if(StringUtils.isNotBlank(date1)&& StringUtils.isBlank(date2)){
            if(date1.substring(0,7).compareTo("2020-09")>=0){
                for (int i = 0; i < firstLevelOrganizationList.size(); i++) {
                    onlineDefectDO.setFirstlevelorganization(firstLevelOrganizationList.get(i));
                    onlineDefectDO.setProcessStartDate(date1);
                    impl2 = onlineDefectExtDao.findWeekList(onlineDefectDO);
                    SumBos.add(firstLevelOrganizationList.get(i));
                    if (impl2 != null && impl2.size() >= 0) {
                        workingHoursBOS.add(impl2.size() + "");
                    } else {
                        workingHoursBOS.add("0");
                    }
                }
                demandHoursRspBO.setStringList(workingHoursBOS);
                demandHoursRspBO.setListSum(SumBos);
                return demandHoursRspBO;
            }else{
                for (int i = 0; i < dos.size(); i++) {
                    // ??????4??????????????????????????????????????????
                    if ("???????????????".equals(dos.get(i).getSecondlevelorganization()) || "??????????????????".equals(dos.get(i).getSecondlevelorganization()) || "?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                            || "???????????????".equals(dos.get(i).getSecondlevelorganization()) || "???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) || "????????????????????????".equals(dos.get(i).getSecondlevelorganization())) {
                        continue;
                    }
                    workingHoursDO.setProblemattributiondept(dos.get(i).getSecondlevelorganization());
                    // ?????????
                    workingHoursDO.setProcessstartdate(date1);
                    impl = productionDefectsExtDao.findWeekList(workingHoursDO);
                    SumBos.add(dos.get(i).getSecondlevelorganization());
                    if (impl != null && impl.size() >= 0) {
                        workingHoursBOS.add(impl.size() + "");
                    } else {
                        workingHoursBOS.add("0");
                    }
                }
                demandHoursRspBO.setStringList(workingHoursBOS);
                demandHoursRspBO.setListSum(SumBos);
                return demandHoursRspBO;
            }

        }
        if(StringUtils.isNotBlank(date2)&& StringUtils.isBlank(date1)){
            if(date2.compareTo("2020-09")>=0){
                for (int i = 0; i < firstLevelOrganizationList.size(); i++) {
                    onlineDefectDO.setFirstlevelorganization(firstLevelOrganizationList.get(i));
                    onlineDefectDO.setProcessStartDate(date2);
                    impl2 = onlineDefectExtDao.findMonthList(onlineDefectDO);
                    SumBos.add(firstLevelOrganizationList.get(i));
                    if (impl2 != null && impl2.size() >= 0) {
                        workingHoursBOS.add(impl2.size() + "");
                    } else {
                        workingHoursBOS.add("0");
                    }
                }
                demandHoursRspBO.setStringList(workingHoursBOS);
                demandHoursRspBO.setListSum(SumBos);
                return demandHoursRspBO;

            }else{
                for (int i = 0; i < dos.size(); i++) {
                    // ??????4??????????????????????????????????????????
                    if ("???????????????".equals(dos.get(i).getSecondlevelorganization()) || "??????????????????".equals(dos.get(i).getSecondlevelorganization()) || "?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                            || "???????????????".equals(dos.get(i).getSecondlevelorganization()) || "???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) || "????????????????????????".equals(dos.get(i).getSecondlevelorganization())) {
                        continue;
                    }
                    workingHoursDO.setProblemattributiondept(dos.get(i).getSecondlevelorganization());
                    workingHoursDO.setProcessstartdate(date2);
                    impl = productionDefectsExtDao.findList(workingHoursDO);
                    SumBos.add(dos.get(i).getSecondlevelorganization());
                    if (impl != null && impl.size() >= 0) {
                        workingHoursBOS.add(impl.size() + "");
                    } else {
                        workingHoursBOS.add("0");
                    }
                }
                demandHoursRspBO.setStringList(workingHoursBOS);
                demandHoursRspBO.setListSum(SumBos);
                return demandHoursRspBO;
            }
        }
        return demandHoursRspBO;
    }

    /**
     * ??????????????????????????????????????? ???????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreIssueNumberDept(String date1, String date2) {
        // ????????????????????????
        List<OrganizationStructureDO> dos = iOrganizationStructureDao.find(new OrganizationStructureDO());
        List<IssueDetailsDO> impl = null;
        IssueDetailsDO workingHoursDO = new IssueDetailsDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < dos.size(); i++) {
            // ??????4??????????????????????????????????????????
            if("???????????????".equals(dos.get(i).getSecondlevelorganization())||"??????????????????".equals(dos.get(i).getSecondlevelorganization())||"?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                    ||"???????????????".equals(dos.get(i).getSecondlevelorganization()) ||"???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) ||"????????????????????????".equals(dos.get(i).getSecondlevelorganization())){
                continue;
            }
            System.err.println(date1 + "=====" + date2);
            if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            workingHoursDO.setIssueDepartment(dos.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setRegistrationDate(date1);
                impl = issueDetailsExtDao.findWeekList(workingHoursDO);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setRegistrationDate(date2);
                impl = issueDetailsExtDao.findList(workingHoursDO);
            }
            SumBos.add(dos.get(i).getSecondlevelorganization());
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ?????????????????? ???????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProductionDefectsDept(String date1, String date2) {
        List<OrganizationStructureDO> dos = iOrganizationStructureDao.find(new OrganizationStructureDO());
        List<ProblemDO> impl = null;
        ProblemDO workingHoursDO = new ProblemDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < dos.size(); i++) {
            // ??????4??????????????????????????????????????????
            if("???????????????".equals(dos.get(i).getSecondlevelorganization())||"??????????????????".equals(dos.get(i).getSecondlevelorganization())||"?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                    ||"???????????????".equals(dos.get(i).getSecondlevelorganization()) ||"???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) ||"????????????????????????".equals(dos.get(i).getSecondlevelorganization())){
                continue;
            }
            if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            workingHoursDO.setDevpLeadDept(dos.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setReqStartMon(date1);
                impl = iProblemDao.findWeekList(workingHoursDO);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setReqStartMon(date2);
                impl = iProblemDao.findMonthList(workingHoursDO);
            }
            SumBos.add(dos.get(i).getSecondlevelorganization());
            if (impl != null && impl.size() >= 0) {
                workingHoursBOS.add(impl.size() + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ????????????????????????????????? ???????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProCheckTimeOutStatisticsDept(String date1, String date2) {
        List<OrganizationStructureDO> dos = iOrganizationStructureDao.find(new OrganizationStructureDO());
        List<ProductionVerificationIsNotTimelyDO> impl = null;
        ProductionVerificationIsNotTimelyDO workingHoursDO = new ProductionVerificationIsNotTimelyDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < dos.size(); i++) {
            // ??????4??????????????????????????????????????????
            if("???????????????".equals(dos.get(i).getSecondlevelorganization())||"??????????????????".equals(dos.get(i).getSecondlevelorganization())||"?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                    ||"???????????????".equals(dos.get(i).getSecondlevelorganization()) ||"???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) ||"????????????????????????".equals(dos.get(i).getSecondlevelorganization())){
                continue;
            }
            if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            workingHoursDO.setDepartment(dos.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setProDate(date1);
                impl = iProductionVerificationIsNotTimelyExtDao.findWeek(workingHoursDO);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setProDate(date2);
                impl = iProductionVerificationIsNotTimelyExtDao.findMonth(workingHoursDO);
            }
            SumBos.add(dos.get(i).getSecondlevelorganization());
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    sum = sum + Integer.parseInt(impl.get(j).getSumDay());
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????????????? ???????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProUnhandledIssues1Dept(String date1, String date2) {
        List<OrganizationStructureDO> dos = iOrganizationStructureDao.find(new OrganizationStructureDO());
        List<ProUnhandledIssuesDO> impl = null;
        ProUnhandledIssuesDO workingHoursDO = new ProUnhandledIssuesDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < dos.size(); i++) {
            // ??????4??????????????????????????????????????????
            if("???????????????".equals(dos.get(i).getSecondlevelorganization())||"??????????????????".equals(dos.get(i).getSecondlevelorganization())||"?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                    ||"???????????????".equals(dos.get(i).getSecondlevelorganization()) ||"???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) ||"????????????????????????".equals(dos.get(i).getSecondlevelorganization())){
                continue;
            }
            if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            workingHoursDO.setDepartment(dos.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setProductionDate(date1);
                impl = proUnhandledIssuesExtDao.findWeek(workingHoursDO);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setProductionDate(date2);
                impl = proUnhandledIssuesExtDao.findMonth(workingHoursDO);
            }
            SumBos.add(dos.get(i).getSecondlevelorganization());
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    if(impl.get(j).getDefectsNumber() > 0){
                        sum = sum + 1;
                    }
                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????????????? ???????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreProUnhandledIssues2Dept(String date1, String date2) {
        List<OrganizationStructureDO> dos = iOrganizationStructureDao.find(new OrganizationStructureDO());
        List<ProUnhandledIssuesDO> impl = null;
        ProUnhandledIssuesDO workingHoursDO = new ProUnhandledIssuesDO();
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < dos.size(); i++) {
            // ??????4??????????????????????????????????????????
            if("???????????????".equals(dos.get(i).getSecondlevelorganization())||"??????????????????".equals(dos.get(i).getSecondlevelorganization())||"?????????????????????".equals(dos.get(i).getSecondlevelorganization())
                    ||"???????????????".equals(dos.get(i).getSecondlevelorganization()) ||"???????????????????????????".equals(dos.get(i).getSecondlevelorganization()) ||"????????????????????????".equals(dos.get(i).getSecondlevelorganization())){
                continue;
            }
            if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            workingHoursDO.setDepartment(dos.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                workingHoursDO.setProductionDate(date1);
                impl = proUnhandledIssuesExtDao.findWeek(workingHoursDO);
            }
            // ?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                workingHoursDO.setProductionDate(date2);
                impl = proUnhandledIssuesExtDao.findMonth(workingHoursDO);
            }
            SumBos.add(dos.get(i).getSecondlevelorganization());
            int sum = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    if(impl.get(j).getProblemNumber() > 0){
                        sum = sum + 1;
                    }

                }
                workingHoursBOS.add(sum + "");
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }


    @Override
    public DemandHoursRspBO getCentreDispose(String date1, String date2) {
        List<DemandDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setSelectTime(date2);
        impl = demandDao.findListDeptDemand(workingHoursDO);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        int a = 0;
        int b = 0;
        int c = 0;
        if (impl != null && impl.size() >= 0) {
            // ??????????????????????????????????????????
            DemandBO demandBO = new DemandBO();
            demandBO.setReqImplMon(date2);
            demandBO.setReqAbnorType("02");
            demandBO.setPageNum(1);
            demandBO.setPageSize(500);
            DemandRspBO demandRspBO = reqPlanService.findDemand(demandBO);
            int d =0;
            if(demandRspBO.getDemandBOList()!=null ||demandRspBO.getDemandBOList().size()>0){
                d = demandRspBO.getDemandBOList().size();
            }
            sum = impl.size() + "";
            for (int i = 0; i < impl.size(); i++) {
                if (!impl.get(i).getReqSts().equals("30") && !impl.get(i).getReqSts().equals("40")) {
                    a = a + 1;
                }
                if (impl.get(i).getReqSts().equals("30")) {
                    b = b + 1;
                }
                if (impl.get(i).getReqSts().equals("40")) {
                    c = c + 1;
                }
            }
            if (a > 0) {
                int f = a - d;
                String a1 = "{'value': '" + f + "', 'name': '????????????'}";
                workingHoursBOS.add(a1);
                SumBos.add("????????????");
            }
            if (b > 0) {
                String b1 = "{'value': '" + b + "', 'name': '????????????'}";
                workingHoursBOS.add(b1);
                SumBos.add("????????????");
            }
            if (c > 0) {
                String c1 = "{'value': '" + c + "', 'name': '????????????'}";
                workingHoursBOS.add(c1);
                SumBos.add("????????????");
            }
            String d1 = "{'value': '" + d + "', 'name': '????????????'}";
            workingHoursBOS.add(d1);
            SumBos.add("????????????");
        } else {
            sum = "0";
        }

        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum);
        return demandHoursRspBO;
    }

    /**
     * ?????????????????????????????????
     *
     * @param date
     * @return
     */
    public static List<String> getSixMonth(String date) {
        //?????????
        List<String> list = new ArrayList<String>();
        int month = Integer.parseInt(date.substring(5, 7));
        int year = Integer.parseInt(date.substring(0, 4));
        for (int i = 5; i >= 0; i--) {
            if (month > 6) {
                if (month - i >= 10) {
                    list.add(year + "-" + String.valueOf(month - i));
                } else {
                    list.add(year + "-0" + String.valueOf(month - i));
                }
            } else {
                if (month - i <= 0) {
                    if (month - i + 12 >= 10) {
                        list.add(String.valueOf(year - 1) + "-" + String.valueOf(month - i + 12));
                    } else {
                        list.add(String.valueOf(year - 1) + "-0" + String.valueOf(month - i + 12));
                    }
                } else {
                    if (month - i >= 10) {
                        list.add(String.valueOf(year) + "-" + String.valueOf(month - i));
                    } else {
                        list.add(String.valueOf(year) + "-0" + String.valueOf(month - i));
                    }
                }
            }
        }
        return list;

    }

    @Override
    public WorkingHoursBO getReportForm11(String displayname, String date1, String date2) {
        WorkingHoursBO workingHoursBO = new WorkingHoursBO();
        List<WorkingHoursDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDisplayname(displayname);
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findWeekView(workingHoursDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findMonthView(workingHoursDO);
            System.err.println(impl);
        }
        if (impl != null && impl.size() >= 0) {
            BeanUtils.copyPropertiesReturnDest(workingHoursBO, impl.get(0));
        }
        System.err.println(impl);
        return workingHoursBO;
    }

    @Override
    public List<WorkingHoursBO> getDemandStaffView(String displayname, String date1, String date2) {
        List<WorkingHoursBO> workingHoursBos = new LinkedList<>();
        List<WorkingHoursDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDisplayname(displayname);
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findListWeekView(workingHoursDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findListMonthView(workingHoursDO);
            System.err.println(impl);
        }
        impl.forEach(m ->
                workingHoursBos.add(BeanUtils.copyPropertiesReturnDest(new WorkingHoursBO(), m))
        );
        if (impl != null && impl.size() >= 0) {
            for (int i = 0; i < impl.size(); i++) {
                DemandJiraDO demandJiraDO = new DemandJiraDO();
                if (impl.get(i).getEpickey() == null || impl.get(i).getEpickey() == "") {
                    continue;
                }
                demandJiraDO.setJiraKey(impl.get(i).getEpickey());
                // ??????jiraKey??????????????????
                List<DemandJiraDO> demandJiraDos = demandJiraDao.find(demandJiraDO);
                // ???????????????????????? ????????????????????????
                if (demandJiraDos != null && demandJiraDos.size() != 0) {
                    DemandDO demandDO = demandDao.get(demandJiraDos.get(demandJiraDos.size() - 1).getReqInnerSeq());
                    workingHoursBos.get(i).setReqNo(demandDO.getReqNo());
                    workingHoursBos.get(i).setReqNm(demandDO.getReqNm());
                }
            }
        }
        System.err.println(workingHoursBos);
        return workingHoursBos;
    }

    /**
     * ???????????? ??????????????????
     * @param displayname
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public List<GitlabDataBO> getGetLabDateView(String displayname, String date1, String date2){
        List<GitlabDataBO> gitlabDataBOLinkedList = new LinkedList<>();
        List<GitlabDataDO> impl = null;
        List<GitlabDataDO> impl2 = null;
        GitlabDataDO workingHoursDO = new GitlabDataDO();
        // ????????????????????????
        UserDO userDO =iUserDao.getUserByUserFullName(displayname);
        if(JudgeUtils.isNull(userDO)){
            // ??????????????????????????????????????????
            return gitlabDataBOLinkedList;
        }
        workingHoursDO.setCommitterEmail(userDO.getEmail());
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setCommittedDate(date1);
            impl = gitlabDataDao.findListWeekView(workingHoursDO);
            impl2 = gitlabDataDao.findWeekGitLabSum(workingHoursDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setCommittedDate(date2);
            impl = gitlabDataDao.findListMonthView(workingHoursDO);
            impl2 = gitlabDataDao.findMonthGitLabSum(workingHoursDO);
        }
        impl.forEach(m ->
                gitlabDataBOLinkedList.add(BeanUtils.copyPropertiesReturnDest(new GitlabDataBO(), m))
        );
        GitlabDataBO gitlabDataBO = BeanUtils.copyPropertiesReturnDest(new GitlabDataBO(),impl2.get(0));
        gitlabDataBO.setNameWithNamespace("??????");
        gitlabDataBO.setCommittedDate("-");
        gitlabDataBOLinkedList.add(gitlabDataBO);
        return gitlabDataBOLinkedList;
    }

    /**
     * ??????????????????
     *
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public List<WorkingHoursBO> getDeptStaffView(String devpLeadDept, String date1, String date2) {
        List<WorkingHoursBO> workingHoursBos = new LinkedList<>();
        List<WorkingHoursDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findListWeekViewDept(workingHoursDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findListMonthViewDept(workingHoursDO);
            System.err.println(impl);
        }
        impl.forEach(m ->
                workingHoursBos.add(BeanUtils.copyPropertiesReturnDest(new WorkingHoursBO(), m))
        );
        if (impl != null && impl.size() >= 0) {
            for (int i = 0; i < impl.size(); i++) {
                DemandJiraDO demandJiraDO = new DemandJiraDO();
                if (impl.get(i).getEpickey() == null || impl.get(i).getEpickey() == "") {
                    continue;
                }
                demandJiraDO.setJiraKey(impl.get(i).getEpickey());
                // ??????jiraKey??????????????????
                List<DemandJiraDO> demandJiraDos = demandJiraDao.find(demandJiraDO);
                // ???????????????????????? ????????????????????????
                if (demandJiraDos != null && demandJiraDos.size() != 0) {
                    DemandDO demandDO = demandDao.get(demandJiraDos.get(demandJiraDos.size() - 1).getReqInnerSeq());
                    workingHoursBos.get(i).setReqNo(demandDO.getReqNo());
                    workingHoursBos.get(i).setReqNm(demandDO.getReqNm());
                }
            }
        }
        System.err.println("112222222231132132" + workingHoursBos);
        return workingHoursBos;
    }

    /**
     * ??????????????????????????????????????????
     *
     * @param displayname
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getDemandStaffTask(String displayname, String date1, String date2) {
        DecimalFormat df = new DecimalFormat("0.##");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        List<WorkingHoursDO> impl = null;
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        double sumx = 0;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDisplayname(displayname);
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findListWeekView(workingHoursDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findListMonthView(workingHoursDO);
        }
        int sumDemand = 0;
        for (int i = 0; i < impl.size(); i++) {
            String sumTime = "";
            // ??????????????????????????????
            if (impl.get(i).getEpickey() == null || impl.get(i).getEpickey() == "") {
                String sumTime1 = impl.get(i).getTimespnet();
                sumx = sumx + getWorkHours(Integer.parseInt(sumTime1));
                // {value: 5, name: '????????????'}
                String qtDemand = "{'value': '" + df.format(getWorkHours(Integer.parseInt(sumTime1))) + "', 'name': '????????????'}";

                workingHoursBOS.add(String.valueOf(qtDemand));
                SumBos.add("????????????");
            } else {
                //????????????????????????
                sumTime = impl.get(i).getTimespnet();
                sumDemand = sumDemand + Integer.parseInt(sumTime);
            }

        }
        sumx = sumx + getWorkHours(sumDemand);
        String Demand = "{'value': '" + df.format(getWorkHours(sumDemand)) + "', 'name': '????????????'}";
        workingHoursBOS.add(Demand);
        SumBos.add("????????????");
        sum = df.format(sumx);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum);
        demandHoursRspBO.setStringList(workingHoursBOS);
        return demandHoursRspBO;
    }

    @Override
    public DefectDetailsRspBO getDemandDefectDetails(String displayname, String date1, String date2) {
        System.err.println("????????????");
        List<DefectDetailsDO> impl = null;
        List<DefectDetailsBO> defectDetailsBOList = null;
        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
        defectDetailsDO.setAssignee(displayname);
        System.err.println(date1 + "=====" + date2);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            defectDetailsDO.setRegistrationDate(date1);
            impl = defectDetailsExtDao.findWeekList(defectDetailsDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            defectDetailsDO.setRegistrationDate(date2);
            impl = defectDetailsExtDao.findList(defectDetailsDO);
            System.err.println(impl);
        }
        DefectDetailsRspBO defectDetailsRspBO = new DefectDetailsRspBO();
        defectDetailsBOList = BeanConvertUtils.convertList(impl, DefectDetailsBO.class);
        defectDetailsRspBO.setDefectDetailsBos(defectDetailsBOList);
        return defectDetailsRspBO;
    }

    /**
     * ????????????????????????
     *
     * @param displayname
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public IssueDetailsRspBO getDemandIssueDetails(String displayname, String date1, String date2) {
        IssueDetailsDO issueDetailsDO = new IssueDetailsDO();
        List<IssueDetailsDO> impl = null;
        List<IssueDetailsBO> issueDetailsBOList = null;
        issueDetailsDO.setAssignee(displayname);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            issueDetailsDO.setRegistrationDate(date1);
            impl = issueDetailsExtDao.findWeekList(issueDetailsDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            issueDetailsDO.setRegistrationDate(date2);
            impl = issueDetailsExtDao.findList(issueDetailsDO);
            System.err.println(impl);
        }
        IssueDetailsRspBO issueDetailsRspBO = new IssueDetailsRspBO();
        issueDetailsBOList = BeanConvertUtils.convertList(impl, IssueDetailsBO.class);
        issueDetailsRspBO.setIssueDetailsBOList(issueDetailsBOList);
        return issueDetailsRspBO;
    }

    @Override
    public ProductionDefectsRspBO getDemandProductionDetails(String displayname, String date1, String date2) {
        ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
        List<ProductionDefectsDO> impl = null;
        List<ProductionDefectsBO> productionDefectsBOList = null;
        productionDefectsDO.setPersonincharge(displayname);
        if (StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            productionDefectsDO.setProcessstartdate(date1);
            impl = productionDefectsExtDao.findWeekList(productionDefectsDO);
            System.err.println(impl);
        }
        // ?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            productionDefectsDO.setProcessstartdate(date2);
            impl = productionDefectsExtDao.findMonthList(productionDefectsDO);
            System.err.println(impl);
        }
        ProductionDefectsRspBO productionDefectsRspBO = new ProductionDefectsRspBO();
        productionDefectsBOList = BeanConvertUtils.convertList(impl, ProductionDefectsBO.class);
        productionDefectsRspBO.setProductionDefectsBOList(productionDefectsBOList);
        return productionDefectsRspBO;
    }

    @Override
    public DemandHoursRspBO getDemandHours(String epic) {
        DecimalFormat df = new DecimalFormat("0.##");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        List<String> workingHoursBOS = new LinkedList<>();
        DemandResourceInvestedDO demandResourceInvestedDO = new DemandResourceInvestedDO();
        demandResourceInvestedDO.setEpicKey(epic);
        List<DemandResourceInvestedDO> impl = iDemandResourceInvestedDao.find(demandResourceInvestedDO);
        impl.forEach(m ->
                workingHoursBOS.add(m.getWorkHoursToString())
        );
        List<String> listSumBos = new LinkedList<>();
        impl.forEach(m ->
                listSumBos.add(m.getDepartment())
        );
        String sum = "";
        List<String> SumBos = new LinkedList<>();
        impl.forEach(m ->
                SumBos.add(m.getValue())
        );
        int sum1 = 0;
        if (SumBos != null || SumBos.size() != 0) {
            for (int i = 0; i < SumBos.size(); i++) {
                int sumx = Integer.parseInt(SumBos.get(i));
                sum1 += sumx;
            }
            System.err.println(sum1);
            double s = getWorkHours(sum1);
            sum = df.format(s);
        }
        demandHoursRspBO.setListSum(listSumBos);
        demandHoursRspBO.setSum(sum);
        demandHoursRspBO.setStringList(workingHoursBOS);
        System.err.println(demandHoursRspBO);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getDemandHoursRole(String epic) {
        DecimalFormat df = new DecimalFormat("0.##");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        List<String> workingHoursBOS = new LinkedList<>();
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setEpickey(epic);
        List<WorkingHoursDO> impl = iWorkingHoursDao.getDemandHoursRole(workingHoursDO);
        if (impl == null || impl.size() == 0) {
            demandHoursRspBO.setSum("0");
            return demandHoursRspBO;
        }
        impl.forEach(m ->
                workingHoursBOS.add(m.getWorkHoursToString())
        );
        List<String> listSumBos = new LinkedList<>();
        impl.forEach(m ->
                listSumBos.add(m.getRoletype())
        );
        String sum = "";
        List<String> SumBos = new LinkedList<>();
        impl.forEach(m ->
                SumBos.add(m.getTimespnet())
        );
        int sum1 = 0;
        if (SumBos != null || SumBos.size() != 0) {
            for (int i = 0; i < SumBos.size(); i++) {
                int sumx = Integer.parseInt(SumBos.get(i));
                sum1 += sumx;
            }
            double s = getWorkHours(sum1);
            sum = df.format(s);
        }
        demandHoursRspBO.setListSum(listSumBos);
        demandHoursRspBO.setSum(sum);
        demandHoursRspBO.setStringList(workingHoursBOS);
        System.err.println(demandHoursRspBO);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getWorkLoad(String epic) {
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        System.err.println(epic + "?????????=========================");
        DemandDO demand = demandDao.get(epic);
        String str = getWorkLoad(demand);
        if (str == null || "".equals(str)) {
            System.err.println("??????????????????");
            demandHoursRspBO.setSum("0");
            return demandHoursRspBO;
        }
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        String[] pro_number_list = str.split(";");
        System.err.println(pro_number_list);
        double sumx = 0;
        for (int i = 0; i < pro_number_list.length; i++) {
            System.err.println(pro_number_list[i]);
            String[] dept_list = pro_number_list[i].split(":");
            System.err.println(dept_list[0]);
            System.err.println(dept_list[1]);
            SumBos.add(dept_list[0]);
            String dept = "{'value': '" + dept_list[1] + "', 'name': '" + dept_list[0] + "'}";
            workingHoursBOS.add(dept);
            sumx += Double.valueOf(dept_list[1]);
        }
        DecimalFormat df = new DecimalFormat("0.##");
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(df.format(sumx));
        demandHoursRspBO.setStringList(workingHoursBOS);
        System.err.println(demandHoursRspBO);
        return demandHoursRspBO;
    }

    /**
     * ??????epic?????????????????????
     *
     * @param epic
     * @return
     */
    @Override
    public DemandHoursRspBO getFlawNumber(String epic) {
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        ProblemStatisticDO problemStatisticDO = problemStatisticDao.get(epic);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        int sumx = 0;
        if (problemStatisticDO == null || "".equals(problemStatisticDO)) {
            demandHoursRspBO.setSum("0");
            return demandHoursRspBO;
        }
        /**
         * @Fields externalDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getExternalDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getExternalDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getExternalDefectsNumber();
        }
        /**
         * @Fields versionDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getVersionDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getVersionDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getVersionDefectsNumber();
        }
        /**
         * @Fields parameterDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getParameterDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getParameterDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getParameterDefectsNumber();
        }
        /**
         * @Fields functionDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getFunctionDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getFunctionDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getFunctionDefectsNumber();
        }
        /**
         * @Fields processDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getProcessDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getProcessDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getProcessDefectsNumber();
        }
        /**
         * @Fields promptDefectsNumber ????????????????????????
         */
        if (problemStatisticDO.getPromptDefectsNumber() != 0) {
            SumBos.add("????????????????????????");
            workingHoursBOS.add(problemStatisticDO.getPromptDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getPromptDefectsNumber();
        }
        /**
         * @Fields pageDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getPageDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getPageDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getPageDefectsNumber();
        }
        /**
         * @Fields backgroundDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getBackgroundDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getBackgroundDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getBackgroundDefectsNumber();
        }
        /**
         * @Fields modifyDefectsNumber ???????????????????????????
         */
        if (problemStatisticDO.getModifyDefectsNumber() != 0) {
            SumBos.add("???????????????????????????");
            workingHoursBOS.add(problemStatisticDO.getModifyDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getModifyDefectsNumber();
        }
        /**
         * @Fields designDefectsNumber ?????????????????????
         */
        if (problemStatisticDO.getDesignDefectsNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getDesignDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getDesignDefectsNumber();
        }
        /**
         * @Fields invalidDefectsNumber ???????????????
         */
        if (problemStatisticDO.getInvalidDefectsNumber() != 0) {
            SumBos.add("???????????????");
            workingHoursBOS.add(problemStatisticDO.getInvalidDefectsNumber() + "");
            sumx = sumx + problemStatisticDO.getInvalidDefectsNumber();
        }
        sum = String.valueOf(sumx);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum);
        demandHoursRspBO.setStringList(workingHoursBOS);
        return demandHoursRspBO;
    }

    /**
     * ??????epic???????????????????????????
     *
     * @param epic
     * @return
     */
    @Override
    public DemandHoursRspBO getReviewNumber(String epic) {
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        ProblemStatisticDO problemStatisticDO = problemStatisticDao.get(epic);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        String sum = "";
        int sumx = 0;
        if (problemStatisticDO == null || "".equals(problemStatisticDO)) {
            demandHoursRspBO.setSum("0");
            return demandHoursRspBO;
        }
        /**
         * @Fields requirementsReviewNumber ?????????????????????
         */
        if (problemStatisticDO.getRequirementsReviewNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getRequirementsReviewNumber() + "");
            sumx = sumx + problemStatisticDO.getRequirementsReviewNumber();
        }
        /**
         * @Fields codeReviewNumber ?????????????????????
         */
        if (problemStatisticDO.getCodeReviewNumber() != 0) {
            SumBos.add("?????????????????????");
            workingHoursBOS.add(problemStatisticDO.getCodeReviewNumber() + "");
            sumx = sumx + problemStatisticDO.getCodeReviewNumber();
        }
        /**
         * @Fields testReviewNumber ???????????????????????????
         */
        if (problemStatisticDO.getTestReviewNumber() != 0) {
            SumBos.add("???????????????????????????");
            workingHoursBOS.add(problemStatisticDO.getTestReviewNumber() + "");
            sumx = sumx + problemStatisticDO.getTestReviewNumber();
        }
        /**
         * @Fields productionReviewNumber ???????????????????????????
         */
        if (problemStatisticDO.getProductionReviewNumber() != 0) {
            SumBos.add("???????????????????????????");
            workingHoursBOS.add(problemStatisticDO.getProductionReviewNumber() + "");
            sumx = sumx + problemStatisticDO.getProductionReviewNumber();
        }
        /**
         * @Fields technicalReviewNumber ???????????????????????????
         */
        if (problemStatisticDO.getTechnicalReviewNumber() != 0) {
            SumBos.add("???????????????????????????");
            workingHoursBOS.add(problemStatisticDO.getTechnicalReviewNumber() + "");
            sumx = sumx + problemStatisticDO.getTechnicalReviewNumber();
        }
        sum = String.valueOf(sumx);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setSum(sum);
        demandHoursRspBO.setStringList(workingHoursBOS);
        return demandHoursRspBO;
    }

    /**
     * ???????????????
     *
     * @param time
     * @return
     */
    public Double getWorkHours(int time) {

        return (double) (Math.round((long) time * 100 / 28800) / 100.0);
    }

    /**
     * ???????????????
     *
     * @param time
     * @return
     */
    public Double getWorkHoursTime(int time) {
        return (double) (Math.round((long) time * 100 / 3600) / 100.0);
    }

    /**
     * ???????????????
     *
     * @param demand
     * @return
     */
    public String getWorkLoad(DemandDO demand) {
        if (com.cmpay.lemon.common.utils.StringUtils.isNotEmpty(demand.getDevpLeadDept()) && com.cmpay.lemon.common.utils.StringUtils.isEmpty(demand.getDevpCoorDept())) {
            demand.setLeadDeptPro(demand.getDevpLeadDept() + ":100%;");
            if (demand.getTotalWorkload() == 0) {
                demand.setLeadDeptWorkload(demand.getDevpLeadDept() + ":0.00;");
            } else {
                demand.setLeadDeptWorkload(demand.getDevpLeadDept() + ":" + String.format("%.2f", Double.valueOf(demand.getTotalWorkload())) + ";");
            }
            //updateReqWorkLoad(demand);
        }
        //???????????????
        double monInputWorkload = demand.getMonInputWorkload() + demand.getInputWorkload();
        //???????????????????????????
        String LeadDeptCurMonWorkLoad = "";
        String lead = demand.getLeadDeptPro();
        String req_sts = demand.getReqSts();
        // && !("30".equals(req_sts)) ???????????????????????????
        if (com.cmpay.lemon.common.utils.StringUtils.isNotBlank(lead) && monInputWorkload != 0) {
            String[] leadSplit = lead.replaceAll("%", "").split(":");
            leadSplit[1] = leadSplit[1].replaceAll(";", "");
            LeadDeptCurMonWorkLoad = leadSplit[0] + ":" + String.format("%.2f", (Double.valueOf(leadSplit[1]) / 100) * monInputWorkload) + ";";
        }

        //???????????????????????????
        String CoorDevpCurMonWorkLoad = "";
        //????????????????????????
        String CoorDevpPer = "";
        String[] coorList = new String[20];
        String coor = demand.getCoorDeptPro();
        // && !("30".equals(req_sts)) ???????????????????????????
        if (com.cmpay.lemon.common.utils.StringUtils.isNotBlank(coor) && monInputWorkload != 0) {
            coorList = demand.getCoorDeptPro().split(";");
            for (int i = 0; i < coorList.length; i++) {
                if (com.cmpay.lemon.common.utils.StringUtils.isNotBlank(coorList[i])) {
                    String[] CoorDevpCurMonWorkLoadSplit = coorList[i].split(":");
                    if (com.cmpay.lemon.common.utils.StringUtils.isNotBlank(CoorDevpCurMonWorkLoadSplit[0]) && com.cmpay.lemon.common.utils.StringUtils.isNotBlank(CoorDevpCurMonWorkLoadSplit[1])) {
                        CoorDevpPer = String.format("%.2f", ((Double.valueOf(CoorDevpCurMonWorkLoadSplit[1].replaceAll("%", ""))) / 100) * monInputWorkload);
                        CoorDevpCurMonWorkLoad += CoorDevpCurMonWorkLoadSplit[0] + ":" + CoorDevpPer + ";";
                    }
                }
            }
        }
        DemandHoursRspBO demand1 = new DemandHoursRspBO();
        String str = "";
        str = LeadDeptCurMonWorkLoad + CoorDevpCurMonWorkLoad;
        return str;
    }

    @Override
    public List<WorkingHoursBO> findList(String displayName, String date, String date1, String date2) {
        System.err.println(displayName + "++++++" + date + "++++++" + date1 + "++++++" + date2);
        List<WorkingHoursBO> workingHoursBOS = new LinkedList<>();
        List<WorkingHoursDO> impl = null;
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setDisplayname(displayName);
        if (StringUtils.isBlank(date) && StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("???????????????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (StringUtils.isNotBlank(date) && StringUtils.isBlank(date1) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date);
            impl = iWorkingHoursDao.findList(workingHoursDO);
        }
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date) && StringUtils.isBlank(date2)) {
            workingHoursDO.setSelectTime(date1);
            impl = iWorkingHoursDao.findListWeek(workingHoursDO);
        }
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date) && StringUtils.isBlank(date1)) {
            workingHoursDO.setSelectTime(date2);
            impl = iWorkingHoursDao.findListMonth(workingHoursDO);
        }
        impl.forEach(m ->
                workingHoursBOS.add(BeanUtils.copyPropertiesReturnDest(new WorkingHoursBO(), m))
        );
        return workingHoursBOS;
    }

    @Override
    public List<ReqDataCountBO> getStageByJd(String reqImplMon) {
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        reqDataCountDao.getStageByJd(reqImplMon).forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );
        return reqDataCountBOS;
    }

    @Override
    public List<ReqDataCountBO> selectDetl(ReqMngBO vo) {
        List<ReqDataCountDO> lst = reqDataCountDao.selectDetl(BeanUtils.copyPropertiesReturnDest(new ReqMngDO(), vo));
        String time = DateUtil.date2String(new Date(), "yyyy-MM-dd");
        for (int i = 0; i < lst.size(); i++) {
            String reqAbnorType = lst.get(i).getReqUnusual();
            DemandBO demand = reqPlanService.findById(lst.get(i).getReqInnerSeq());
            String reqAbnorTypeAll = "";
            //????????????????????????uat??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (StringUtils.isNotBlank(demand.getPrdFinshTm()) && StringUtils.isNotBlank(demand.getUatUpdateTm()) && StringUtils.isNotBlank(demand.getTestFinshTm()) && StringUtils.isNotBlank(demand.getPreCurPeriod()) && StringUtils.isNotBlank(demand.getReqSts())) {
                //?????????????????????????????????????????????????????????30,??????????????????????????????????????????30???40???,????????????????????????
                if (time.compareTo(demand.getPrdFinshTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) < 30 && "30".compareTo(demand.getReqSts()) != 0 && "40".compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (time.compareTo(demand.getUatUpdateTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) >= 30 && Integer.parseInt(demand.getPreCurPeriod()) < 120 && "30".compareTo(demand.getReqSts()) != 0 && "40".compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (time.compareTo(demand.getTestFinshTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) >= 120 && Integer.parseInt(demand.getPreCurPeriod()) < 140 && "30".compareTo(demand.getReqSts()) != 0 && "40".compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "??????????????????";
                }
                if (StringUtils.isBlank(reqAbnorTypeAll)) {
                    reqAbnorTypeAll += "??????";
                }
            } else if (reqAbnorType.indexOf("01") != -1) {
                lst.get(i).setReqUnusual("??????");
                continue;
            } else {
                if (reqAbnorType.indexOf("03") != -1) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (reqAbnorType.indexOf("04") != -1) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (reqAbnorType.indexOf("05") != -1) {
                    reqAbnorTypeAll += "??????????????????";
                }
            }
            if (reqAbnorTypeAll.length() >= 1 && ',' == reqAbnorTypeAll.charAt(reqAbnorTypeAll.length() - 1)) {
                reqAbnorTypeAll = reqAbnorTypeAll.substring(0, reqAbnorTypeAll.length() - 1);
                lst.get(i).setReqUnusual(reqAbnorTypeAll);
            } else {
                lst.get(i).setReqUnusual(reqAbnorTypeAll);
            }
        }
        List<ReqDataCountBO> reqDataCountBOS = new LinkedList<>();
        lst.forEach(m ->
                reqDataCountBOS.add(BeanUtils.copyPropertiesReturnDest(new ReqDataCountBO(), m))
        );
        return reqDataCountBOS;
    }

    @Override
    public Integer findNumberByCondition(ReqMngBO vo) throws Exception {
        return reqDataCountDao.findNumberByCondition(BeanUtils.copyPropertiesReturnDest(new ReqMngDO(), vo));
    }

    @Override
    public Map selectByCenter(ReqMngBO vo) {
        Map reMap = new TreeMap();
        Map DataMap = new TreeMap();

        ReqDataCountDO reqDataCount = reqDataCountDao.selectByCenter(BeanUtils.copyPropertiesReturnDest(new ReqMngDO(), vo));

        if ((reqDataCount == null)) {
            return reMap;
        }

        DataMap.put("????????????", reqDataCount.getReqPrd());
        DataMap.put("????????????", reqDataCount.getReqDevp());
        DataMap.put("????????????", reqDataCount.getReqTest());
        DataMap.put("???????????????", reqDataCount.getReqPre());
        DataMap.put("?????????", reqDataCount.getReqOper());
        DataMap.put("????????????", reqDataCount.getReqAbnormal());
        DataMap.put("????????????", reqDataCount.getReqCancel());
        DataMap.put("????????????", reqDataCount.getReqSuspend());

        reMap.put("DataMap", DataMap);
        reMap.put("totle", reqDataCount.getTotal());

        return reMap;
    }

    @Override
    public Map selectByProduct(ReqMngBO vo) {
        Map reMap = new TreeMap();
        Map DataMap = new TreeMap();

        ReqDataCountDO reqDataCount = reqDataCountDao.selectByProduct(BeanUtils.copyPropertiesReturnDest(new ReqMngDO(), vo));

        if ((reqDataCount == null)) {
            return reMap;
        }

        DataMap.put("????????????", reqDataCount.getReqPrd());
        DataMap.put("????????????", reqDataCount.getReqDevp());
        DataMap.put("????????????", reqDataCount.getReqTest());
        DataMap.put("???????????????", reqDataCount.getReqPre());
        DataMap.put("??????????????????", reqDataCount.getReqAbnormal());
        DataMap.put("????????????", reqDataCount.getReqCancel());
        DataMap.put("????????????", reqDataCount.getReqSuspend());

        reMap.put("DataMap", DataMap);
        reMap.put("totle", reqDataCount.getTotal());

        return reMap;
    }

    @Override
    public Map selectByTest(ReqMngBO vo) {
        Map reMap = new TreeMap();
        Map DataMap = new TreeMap();

        ReqDataCountDO reqDataCount = reqDataCountDao.selectByTest(BeanUtils.copyPropertiesReturnDest(new ReqMngDO(), vo));

        if ((reqDataCount == null)) {
            return reMap;
        }

        DataMap.put("???????????????", reqDataCount.getReqPrd());
        DataMap.put("???????????????", reqDataCount.getReqDevp());
        DataMap.put("???????????????", reqDataCount.getReqTest());
        DataMap.put("????????????", reqDataCount.getReqAbnormal());
        DataMap.put("????????????", reqDataCount.getReqCancel());
        DataMap.put("????????????", reqDataCount.getReqSuspend());

        reMap.put("DataMap", DataMap);
        reMap.put("totle", reqDataCount.getTotal());

        return reMap;
    }

    @Override
    public Map selectByEng(ReqMngBO vo) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void downloadDemandTypeStatistics(String month, HttpServletResponse response) {
        DemandTypeStatisticsBO demandTypeStatisticsBO = new DemandTypeStatisticsBO();
        List<DemandTypeStatisticsBO> DemandTypeStatisticsBOList = new ArrayList<>();
        List<ReqDataCountBO> reportLista = new ArrayList<>();
        reportLista = this.getReqSts(month);
        if (JudgeUtils.isNotEmpty(reportLista)) {
            demandTypeStatisticsBO.setReqIncre(reportLista.get(0).getReqIncre());
            demandTypeStatisticsBO.setReqJump(reportLista.get(0).getReqJump());
            demandTypeStatisticsBO.setReqReplace(reportLista.get(0).getReqReplace());
            demandTypeStatisticsBO.setReqTotal(reportLista.get(0).getReqTotal());
            demandTypeStatisticsBO.setReqStock(reportLista.get(0).getReqStock());
        }
        DemandTypeStatisticsBOList.add(demandTypeStatisticsBO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandTypeStatisticsBO.class, DemandTypeStatisticsBOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "????????????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public void downloadProductionTypeStatistics(String month, HttpServletResponse response) {
        List<DemandTypeProductionBO> DemandTypeStatisticsBOList = new ArrayList<>();
        List<ScheduleBO> reportLista = new ArrayList<>();
        reportLista = this.getProduction(month);
        if (JudgeUtils.isNotEmpty(reportLista)) {
            reportLista.forEach(m -> {
                DemandTypeProductionBO demandTypeStatisticsBO = new DemandTypeProductionBO();
                demandTypeStatisticsBO.setProNumber(m.getProNumber());
                demandTypeStatisticsBO.setProNeed(m.getProNeed());
                demandTypeStatisticsBO.setProOperator(m.getProOperator());
                demandTypeStatisticsBO.setScheduleTime(m.getScheduleTime());
                demandTypeStatisticsBO.setApplicationDept(m.getApplicationDept());
                demandTypeStatisticsBO.setFirstLevelOrganization(m.getFirstLevelOrganization());
                DemandTypeStatisticsBOList.add(demandTypeStatisticsBO);
            });
        }
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandTypeProductionBO.class, DemandTypeStatisticsBOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "???????????????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public void downloadDemandUploadDocumentBO(String month, String devpLeadDept, String productMng, String firstLevelOrganization, HttpServletResponse response) {
        List<DemandUploadDocumentBO> DemandTypeStatisticsBOList = new ArrayList<>();
        List<DemandBO> reportLista = new ArrayList<>();
        reportLista = this.getReportForm6(month, devpLeadDept, productMng, firstLevelOrganization);
        if (JudgeUtils.isNotEmpty(reportLista)) {
            reportLista.forEach(m -> {
                DemandUploadDocumentBO demandTypeStatisticsBO = new DemandUploadDocumentBO();
                demandTypeStatisticsBO.setReqNo(m.getReqNo());
                demandTypeStatisticsBO.setReqNm(m.getReqNm());
                demandTypeStatisticsBO.setProjectStartTm(m.getProjectStartTm());
                demandTypeStatisticsBO.setActPrdUploadTm(m.getActPrdUploadTm());
                demandTypeStatisticsBO.setActWorkloadUploadTm(m.getActWorkloadUploadTm());
                demandTypeStatisticsBO.setActSitUploadTm(m.getActSitUploadTm());
                demandTypeStatisticsBO.setActUatUploadTm(m.getActUatUploadTm());
                demandTypeStatisticsBO.setActPreUploadTm(m.getActPreUploadTm());
                demandTypeStatisticsBO.setActProductionUploadTm(m.getActProductionUploadTm());
                demandTypeStatisticsBO.setFirstLevelOrganization(m.getFirstLevelOrganization());
                demandTypeStatisticsBO.setDevpLeadDept(m.getDevpLeadDept());
                demandTypeStatisticsBO.setProductMng(m.getProductMng());
                demandTypeStatisticsBO.setDevpEng(m.getDevpEng());
                demandTypeStatisticsBO.setFrontEng(m.getFrontEng());
                demandTypeStatisticsBO.setTestEng(m.getTestEng());
                demandTypeStatisticsBO.setPreCurPeriod(m.getPreCurPeriod());
                demandTypeStatisticsBO.setNoUpload(m.getNoUpload().toString());
                DemandTypeStatisticsBOList.add(demandTypeStatisticsBO);
            });
        }
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandUploadDocumentBO.class, DemandTypeStatisticsBOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "??????????????????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public void downloadDemandImplementationReport(String month, HttpServletResponse response) {
        List<ReqDataCountBO> reportLista = this.getImpl(month);
        List<ReqDataCountBO> reportListb = this.getImplByDept(month);
        List<DemandImplementationReportBO> demandImplementationReportBOList = new ArrayList<>();
        if (JudgeUtils.isNotEmpty(reportListb)) {
            reportListb.forEach(m ->
                    {
                        DemandImplementationReportBO demandImplementationReportBO = new DemandImplementationReportBO();
                        demandImplementationReportBO.setFirstLevelOrganization(m.getFirstLevelOrganization());
                        demandImplementationReportBO.setDevpLeadDept(m.getDevpLeadDept());
                        demandImplementationReportBO.setReqDevp(m.getReqDevp());
                        demandImplementationReportBO.setReqOper(m.getReqOper());
                        demandImplementationReportBO.setReqPrd(m.getReqPrd());
                        demandImplementationReportBO.setReqPre(m.getReqPre());
                        demandImplementationReportBO.setReqTest(m.getReqTest());
                        demandImplementationReportBO.setTotal(m.getTotal());
                        demandImplementationReportBOList.add(demandImplementationReportBO);
                    }
            );
        }
        if (JudgeUtils.isNotEmpty(reportLista)) {
            reportLista.forEach(m ->
                    {
                        DemandImplementationReportBO demandImplementationReportBO = new DemandImplementationReportBO();
                        demandImplementationReportBO.setDevpLeadDept("??????");
                        demandImplementationReportBO.setReqDevp(m.getReqDevp());
                        demandImplementationReportBO.setReqOper(m.getReqOper());
                        demandImplementationReportBO.setReqPrd(m.getReqPrd());
                        demandImplementationReportBO.setReqPre(m.getReqPre());
                        demandImplementationReportBO.setReqTest(m.getReqTest());
                        demandImplementationReportBO.setTotal(m.getTotal());
                        demandImplementationReportBOList.add(demandImplementationReportBO);
                    }
            );
        }
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandImplementationReportBO.class, demandImplementationReportBOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "??????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public void downloadDemandCompletionReport(String month, HttpServletResponse response) {
        List<ReqDataCountBO> reportLista = this.getComp(month);
        List<ReqDataCountBO> reportListb = this.getCompByDept(month);
        List<DemandCompletionReportBO> demandCompletionReportBOList = new ArrayList<>();
        if (JudgeUtils.isNotEmpty(reportListb)) {
            reportListb.forEach(m ->
                    {
                        DemandCompletionReportBO demandCompletionReportBO = new DemandCompletionReportBO();
                        demandCompletionReportBO.setFirstLevelOrganization(m.getFirstLevelOrganization());
                        demandCompletionReportBO.setDevpLeadDept(m.getDevpLeadDept());
                        demandCompletionReportBO.setReqTotal(m.getReqTotal());
                        demandCompletionReportBO.setReqAcceptance(m.getReqAcceptance());
                        demandCompletionReportBO.setReqOper(m.getReqOper());
                        demandCompletionReportBO.setReqFinish(m.getReqFinish());
                        demandCompletionReportBO.setReqSuspend(m.getReqSuspend());
                        demandCompletionReportBO.setReqCancel(m.getReqCancel());
                        demandCompletionReportBO.setReqAbnormal(m.getReqAbnormal());
                        demandCompletionReportBO.setReqAbnormalRate(m.getReqAbnormalRate());
                        demandCompletionReportBO.setReqFinishRate(m.getReqFinishRate());
                        demandCompletionReportBO.setTotal(m.getTotal());
                        demandCompletionReportBO.setReqInaccuracyRate(m.getReqInaccuracyRate());
                        demandCompletionReportBOList.add(demandCompletionReportBO);
                    }
            );
        }
        if (JudgeUtils.isNotEmpty(reportLista)) {
            reportLista.forEach(m ->
                    {
                        DemandCompletionReportBO demandCompletionReportBO = new DemandCompletionReportBO();
                        demandCompletionReportBO.setDevpLeadDept("??????");
                        demandCompletionReportBO.setReqTotal(m.getReqTotal());
                        demandCompletionReportBO.setReqAcceptance(m.getReqAcceptance());
                        demandCompletionReportBO.setReqOper(m.getReqOper());
                        demandCompletionReportBO.setReqFinish(m.getReqFinish());
                        demandCompletionReportBO.setReqSuspend(m.getReqSuspend());
                        demandCompletionReportBO.setReqCancel(m.getReqCancel());
                        demandCompletionReportBO.setReqAbnormal(m.getReqAbnormal());
                        demandCompletionReportBO.setReqAbnormalRate(m.getReqAbnormalRate());
                        demandCompletionReportBO.setReqFinishRate(m.getReqFinishRate());
                        demandCompletionReportBO.setTotal(m.getTotal());
                        demandCompletionReportBO.setReqInaccuracyRate(m.getReqInaccuracyRate());
                        demandCompletionReportBOList.add(demandCompletionReportBO);
                    }
            );
        }
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandCompletionReportBO.class, demandCompletionReportBOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "????????????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }

    }

    @Override
    public void downloadDemandCompletionReport2(String month, HttpServletResponse response) {
        List<ReqDataCountBO> reportListb = this.getCompByDept2(month);
        List<DemandCompletionReportBO> demandCompletionReportBOList = new ArrayList<>();
        if (JudgeUtils.isNotEmpty(reportListb)) {
            reportListb.forEach(m ->
                    {
                        DemandCompletionReportBO demandCompletionReportBO = new DemandCompletionReportBO();
                        demandCompletionReportBO.setFirstLevelOrganization(m.getFirstLevelOrganization());
                        demandCompletionReportBO.setReqTotal(m.getReqTotal());
                        demandCompletionReportBO.setReqAcceptance(m.getReqAcceptance());
                        demandCompletionReportBO.setReqOper(m.getReqOper());
                        demandCompletionReportBO.setReqFinish(m.getReqFinish());
                        demandCompletionReportBO.setReqSuspend(m.getReqSuspend());
                        demandCompletionReportBO.setReqCancel(m.getReqCancel());
                        demandCompletionReportBO.setReqAbnormal(m.getReqAbnormal());
                        demandCompletionReportBO.setReqAbnormalRate(m.getReqAbnormalRate());
                        demandCompletionReportBO.setReqFinishRate(m.getReqFinishRate());
                        demandCompletionReportBO.setTotal(m.getTotal());
                        demandCompletionReportBO.setReqInaccuracyRate(m.getReqInaccuracyRate());
                        demandCompletionReportBOList.add(demandCompletionReportBO);
                    }
            );
        }
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandCompletionReportBO.class, demandCompletionReportBOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "????????????????????????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }

    }

    @Override
    public void downloadBaseOwnershipDepartmentStatistics(String month, HttpServletResponse response) {
        List<ReqDataCountBO> reportLista = new ArrayList<>();
        List<BaseOwnershipDepartmentStatisticsBO> BaseOwnershipDepartmentStatisticsBOList = new ArrayList<>();
        reportLista = this.getStageByJd(month);
        if (JudgeUtils.isNotEmpty(reportLista)) {
            reportLista.forEach(m -> {
                BaseOwnershipDepartmentStatisticsBO baseOwnershipDepartmentStatisticsBO = new BaseOwnershipDepartmentStatisticsBO();
                baseOwnershipDepartmentStatisticsBO.setReqPrd(m.getReqPrd());
                baseOwnershipDepartmentStatisticsBO.setFinanceDevp(m.getFinanceDevp());
                baseOwnershipDepartmentStatisticsBO.setQualityDevp(m.getQualityDevp());
                baseOwnershipDepartmentStatisticsBO.setInnoDevp(m.getInnoDevp());
                baseOwnershipDepartmentStatisticsBO.setElecDevp(m.getElecDevp());
                baseOwnershipDepartmentStatisticsBO.setRiskDevp(m.getRiskDevp());
                baseOwnershipDepartmentStatisticsBO.setFinancialDevp(m.getFinancialDevp());
                baseOwnershipDepartmentStatisticsBO.setCommDevp(m.getCommDevp());
                baseOwnershipDepartmentStatisticsBO.setInfoDevp(m.getInfoDevp());
                baseOwnershipDepartmentStatisticsBO.setBusiDevp(m.getBusiDevp());
                baseOwnershipDepartmentStatisticsBO.setGoveDevp(m.getGoveDevp());
                baseOwnershipDepartmentStatisticsBO.setTotal(m.getTotal());
                BaseOwnershipDepartmentStatisticsBOList.add(baseOwnershipDepartmentStatisticsBO);
            });
        }
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), BaseOwnershipDepartmentStatisticsBO.class, BaseOwnershipDepartmentStatisticsBOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "????????????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public DemandRspBO findDemand(DemandBO demandBO) {
        String time = DateUtil.date2String(new Date(), "yyyy-MM-dd");
        PageInfo<DemandBO> pageInfo = getPageInfo(demandBO);
        List<DemandBO> demandBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandBO.class);

        for (int i = 0; i < demandBOList.size(); i++) {
            String reqAbnorType = demandBOList.get(i).getReqAbnorType();
            String reqAbnorTypeAll = "";
            DemandBO demand = reqPlanService.findById(demandBOList.get(i).getReqInnerSeq());

            //????????????????????????uat??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (com.cmpay.lemon.common.utils.StringUtils.isNotBlank(demand.getPrdFinshTm()) && com.cmpay.lemon.common.utils.StringUtils.isNotBlank(demand.getUatUpdateTm())
                    && com.cmpay.lemon.common.utils.StringUtils.isNotBlank(demand.getTestFinshTm()) && com.cmpay.lemon.common.utils.StringUtils.isNotBlank(demand.getPreCurPeriod())
                    && com.cmpay.lemon.common.utils.StringUtils.isNotBlank(demand.getReqSts())) {
                //?????????????????????????????????????????????????????????30,??????????????????????????????????????????30???40???,????????????????????????
                if (time.compareTo(demand.getPrdFinshTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) < REQCONFIRM
                        && REQSUSPEND.compareTo(demand.getReqSts()) != 0 && REQCANCEL.compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (time.compareTo(demand.getUatUpdateTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) >= REQCONFIRM
                        && Integer.parseInt(demand.getPreCurPeriod()) < UPDATEUAT && REQSUSPEND.compareTo(demand.getReqSts()) != 0
                        && REQCANCEL.compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (time.compareTo(demand.getTestFinshTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) >= UPDATEUAT
                        && Integer.parseInt(demand.getPreCurPeriod()) < FINISHUATTEST && REQSUSPEND.compareTo(demand.getReqSts()) != 0
                        && REQCANCEL.compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "??????????????????";
                }
                if (com.cmpay.lemon.common.utils.StringUtils.isBlank(reqAbnorTypeAll)) {
                    reqAbnorTypeAll += "??????";
                }
            } else if (reqAbnorType.indexOf("01") != -1) {
                demandBOList.get(i).setReqAbnorType("??????");
                continue;
            } else {
                if (reqAbnorType.indexOf("03") != -1) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (reqAbnorType.indexOf("04") != -1) {
                    reqAbnorTypeAll += "??????????????????,";
                }
                if (reqAbnorType.indexOf("05") != -1) {
                    reqAbnorTypeAll += "??????????????????";
                }
            }

            if (reqAbnorTypeAll.length() >= 1 && ',' == reqAbnorTypeAll.charAt(reqAbnorTypeAll.length() - 1)) {
                reqAbnorTypeAll = reqAbnorTypeAll.substring(0, reqAbnorTypeAll.length() - 1);
                demandBOList.get(i).setReqAbnorType(reqAbnorTypeAll);
            } else {
                demandBOList.get(i).setReqAbnorType(reqAbnorTypeAll);
            }
        }
        DemandRspBO demandRspBO = new DemandRspBO();
        demandRspBO.setDemandBOList(demandBOList);
        demandRspBO.setPageInfo(pageInfo);
        return demandRspBO;
    }

    //???????????????????????????????????????????????????
    @Override
    public List<ProductionBO> getProductionVerificationIsNotTimely(int dayNumber) {
        String date = "";
        List<ProductionDO> productionDOList = new LinkedList<>();
        productionDOList = operationProductionService.getProductionVerificationIsNotTimely(date);
        List<ProductionBO> productionBOList = new LinkedList<>();
        productionDOList.forEach(m -> {
            try {
                ProductionBO productionBO = new ProductionBO();
                productionBOList.add(BeanUtils.copyPropertiesReturnDest(productionBO, m));
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                //???????????????
                Calendar c1 = Calendar.getInstance();
                Calendar c2 = Calendar.getInstance();
                c1.setTime(sdf.parse(sdf.format(new Date())));
                c2.setTime(sdf.parse(sdf.format(m.getProDate())));
                long day = (sdf.parse(sdf.format(new Date())).getTime() - sdf.parse(sdf.format(m.getProDate())).getTime()) / (24 * 60 * 60 * 1000);
                productionBO.setDayNumber(String.valueOf(day));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return productionBOList;

    }


    private PageInfo<DemandBO> getPageInfo(DemandBO demandBO) {
        DemandDO demandDO = new DemandDO();
        BeanConvertUtils.convert(demandDO, demandBO);
        PageInfo<DemandBO> pageInfo = PageUtils.pageQueryWithCount(demandBO.getPageNum(), demandBO.getPageSize(),
                () -> BeanConvertUtils.convertList(reqDataCountDao.find(demandDO), DemandBO.class));
        return pageInfo;
    }

    @Override
    public List<ProductionVerificationIsNotTimelyBO> listOfUntimelyStatusChanges(String dept,String selectTime1,String selectTime2) {
        List<ProductionVerificationIsNotTimelyBO> listOfUntimelyStatusChangesBos = new LinkedList<>();
        //?????????????????????????????????
        List<ProductionDO> productionDOList = new LinkedList<>();
        //???????????????????????????????????????
        List<OperationApplicationDO> operationApplicationDOList = new LinkedList<>();
        if (StringUtils.isBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        // ?????????
        if (StringUtils.isNotBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            productionDOList = operationProductionService.getProductionVerificationIsNotTimely2(selectTime1, dept);
            operationApplicationDOList = operationProductionService.getSystemEntryVerificationIsNotTimelyList2(selectTime1, dept);
        }
        // ?????????
        if (StringUtils.isNotBlank(selectTime2) && StringUtils.isBlank(selectTime1)) {
            productionDOList = operationProductionService.getProductionVerificationIsNotTimely3(selectTime2, dept);
            operationApplicationDOList = operationProductionService.getSystemEntryVerificationIsNotTimelyList3(selectTime2, dept);
        }
        if (JudgeUtils.isEmpty(productionDOList) && JudgeUtils.isEmpty(operationApplicationDOList)) {
            return listOfUntimelyStatusChangesBos;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if (productionDOList != null && productionDOList.size() != 0) {
                for (int i = 0; i < productionDOList.size(); i++) {
                    ProductionVerificationIsNotTimelyBO productionVerificationIsNotTimelyBO = new ProductionVerificationIsNotTimelyBO();
                    productionVerificationIsNotTimelyBO.setProNumber(productionDOList.get(i).getProNumber());
                    productionVerificationIsNotTimelyBO.setProNeed(productionDOList.get(i).getProNeed());
                    productionVerificationIsNotTimelyBO.setProType(productionDOList.get(i).getProType());
                    productionVerificationIsNotTimelyBO.setValidation(productionDOList.get(i).getValidation());
                    productionVerificationIsNotTimelyBO.setProDate(sdf.format(productionDOList.get(i).getProDate()));
                    productionVerificationIsNotTimelyBO.setIdentifier(productionDOList.get(i).getIdentifier());
                    productionVerificationIsNotTimelyBO.setDepartment(productionDOList.get(i).getApplicationDept());
                    Calendar c1 = Calendar.getInstance();
                    Calendar c2 = Calendar.getInstance();
                    c1.setTime(sdf.parse(sdf.format(new Date())));
                    c2.setTime(sdf.parse(sdf.format(productionDOList.get(i).getProDate())));
                    long day = 0;
                    if("????????????".equals(productionDOList.get(i).getValidation())){
                        day = (sdf.parse(sdf.format(new Date())).getTime() - sdf.parse(sdf.format(productionDOList.get(i).getProDate())).getTime()) / (24 * 60 * 60 * 1000);
                        if(day<=0){
                            continue;
                        }
                    }
                    if("????????????".equals(productionDOList.get(i).getValidation())){
                        day = ( (sdf.parse(sdf.format(new Date())).getTime() - sdf.parse(sdf.format(productionDOList.get(i).getProDate())).getTime()) / (24 * 60 * 60 * 1000) ) -2;
                        if(day<=0){
                            continue;
                        }
                    }
                    if("?????????????????????".equals(productionDOList.get(i).getValidation())){
                        day = ((sdf.parse(sdf.format(new Date())).getTime() - sdf.parse(sdf.format(productionDOList.get(i).getProDate())).getTime()) / (24 * 60 * 60 * 1000) ) -7;
                        if(day<=0){
                            continue;
                        }
                    }
                    productionVerificationIsNotTimelyBO.setSumDay(day + "");
                    listOfUntimelyStatusChangesBos.add(productionVerificationIsNotTimelyBO);
                }
            }
            if (operationApplicationDOList != null && operationApplicationDOList.size() != 0) {
                for (int i = 0; i < operationApplicationDOList.size(); i++) {
                    ProductionVerificationIsNotTimelyBO productionVerificationIsNotTimelyBO = new ProductionVerificationIsNotTimelyBO();
                    productionVerificationIsNotTimelyBO.setProNumber(operationApplicationDOList.get(i).getOperNumber());
                    productionVerificationIsNotTimelyBO.setProNeed(operationApplicationDOList.get(i).getOperRequestContent());
                    productionVerificationIsNotTimelyBO.setProType(operationApplicationDOList.get(i).getSysOperType());
                    productionVerificationIsNotTimelyBO.setValidation("");
                    productionVerificationIsNotTimelyBO.setProDate(sdf.format(operationApplicationDOList.get(i).getProposeDate()));
                    productionVerificationIsNotTimelyBO.setIdentifier(operationApplicationDOList.get(i).getIdentifier());
                    productionVerificationIsNotTimelyBO.setDepartment(operationApplicationDOList.get(i).getApplicationSector());
                    Calendar c1 = Calendar.getInstance();
                    Calendar c2 = Calendar.getInstance();
                    c1.setTime(sdf.parse(sdf.format(new Date())));
                    c2.setTime(sdf.parse(sdf.format(operationApplicationDOList.get(i).getProposeDate())));
                    long day = (sdf.parse(sdf.format(new Date())).getTime() - sdf.parse(sdf.format(operationApplicationDOList.get(i).getProposeDate())).getTime()) / (24 * 60 * 60 * 1000);
                    productionVerificationIsNotTimelyBO.setSumDay(day + "");
                    listOfUntimelyStatusChangesBos.add(productionVerificationIsNotTimelyBO);
                }
            }
        } catch (ParseException e) {
        }
        return listOfUntimelyStatusChangesBos;
    }

    //????????????
    @Override
    public  List<TestStatisticsRspBO> testStatisticsList(TestStatisticsBO destStatisticsBO) {
        String startTime = "";
        String endTime = "";
        //?????????
        if (JudgeUtils.isBlank(destStatisticsBO.getSelectTime1()) && JudgeUtils.isNotBlank(destStatisticsBO.getSelectTime2())) {

            //?????????????????????
            SimpleDateFormat simpleDateFormatMonth = new SimpleDateFormat("yyyy-MM");
            try {
                Date month = simpleDateFormatMonth.parse(destStatisticsBO.getSelectTime2());
            Calendar c = Calendar.getInstance();
            c.setTime(month);
            c.add(Calendar.MONTH, 1);
            startTime = destStatisticsBO.getSelectTime2();
            endTime = simpleDateFormatMonth.format(c.getTime());
        } catch (Exception e) {
            //todo  ??????????????????
        }
        }
        //?????????
        else if (JudgeUtils.isBlank(destStatisticsBO.getSelectTime2()) && JudgeUtils.isNotBlank(destStatisticsBO.getSelectTime1())) {
            //?????????????????????
            SimpleDateFormat simpleDateFormatMonth = new SimpleDateFormat("yyyy-MM-dd");
            try {
                Date day = simpleDateFormatMonth.parse(destStatisticsBO.getSelectTime1());
                Calendar c = Calendar.getInstance();
                c.setTime(day);
                c.add(Calendar.DATE, 7);
                startTime = destStatisticsBO.getSelectTime1();
                endTime = simpleDateFormatMonth.format(c.getTime());
            } catch (Exception e) {
                //todo  ??????????????????
            }
        } else {
            //todo  ??????????????????
        }

        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        workingHoursDO.setStartTime(startTime);
        workingHoursDO.setEndTime(endTime);
        workingHoursDO.setDevpLeadDept("??????????????????");
        //?????????????????????
        if (destStatisticsBO.getQueryFlag().equals("personal")) {
            workingHoursDO.setDisplayname(destStatisticsBO.getPersonName());
        }
        //??????????????????list
        List<WorkingHoursDO> workingHoursList = iWorkingHoursDao.queryByTimeCycle(workingHoursDO);
        //??????maq??????
        HashMap<String, TestStatisticsRspBO> testDataStatisticsMap = new HashMap<>();
        workingHoursList.forEach(m -> {
            if (JudgeUtils.isBlank(m.getEpickey())) {
                return;
            }

            TestStatisticsRspBO testStatisticsRspBO = testDataStatisticsMap.get(m.getEpickey());
            //???????????????????????????????????????
            if (JudgeUtils.isNull(testStatisticsRspBO)) {
                TestStatisticsRspBO testStatisticsRspBO1 = new TestStatisticsRspBO();
                testStatisticsRspBO1.setPeriod(workingHoursDO.getStartTime() + "---" + workingHoursDO.getEndTime());
                DemandJiraDO demandJiraDO = new DemandJiraDO();
                demandJiraDO.setJiraKey(m.getEpickey());
                // ??????jiraKey??????????????????
                List<DemandJiraDO> demandJiraDos = demandJiraDao.find(demandJiraDO);
                // ???????????????????????? ????????????????????????
                if (JudgeUtils.isNotEmpty(demandJiraDos)) {
                    DemandDO demandDO = demandDao.get(demandJiraDos.get(demandJiraDos.size() - 1).getReqInnerSeq());
                    if(JudgeUtils.isBlank(demandDO.getReqNm())){
                        System.err.println(demandJiraDos.get(demandJiraDos.size() - 1).getReqInnerSeq());
                    }
                    testStatisticsRspBO1.setDemandName(demandDO.getReqNm());
                }else{
                    testStatisticsRspBO1.setDemandName("???????????????"+m.getEpickey());
                }
                testStatisticsRspBO1.setWorkingHours(m.getTimespnet());
                testStatisticsRspBO1.setCaseCompletedNumber(m.getCaseCompletedNumber());
                testStatisticsRspBO1.setCaseExecutionNumber(m.getCaseExecutionNumber());
                testStatisticsRspBO1.setCaseWritingNumber(m.getCaseWritingNumber());
                testDataStatisticsMap.put(m.getEpickey(),testStatisticsRspBO1);
            }else{
                int workingHours = Integer.parseInt(m.getTimespnet());
                int totalWorkingHours = Integer.parseInt(testStatisticsRspBO.getWorkingHours());
                totalWorkingHours=workingHours+totalWorkingHours;
                testStatisticsRspBO.setWorkingHours(String.valueOf(totalWorkingHours));
                testStatisticsRspBO.setCaseCompletedNumber(m.getCaseCompletedNumber()+testStatisticsRspBO.getCaseCompletedNumber());
                testStatisticsRspBO.setCaseExecutionNumber(m.getCaseExecutionNumber()+testStatisticsRspBO.getCaseExecutionNumber());
                testStatisticsRspBO.setCaseWritingNumber(m.getCaseWritingNumber()+testStatisticsRspBO.getCaseWritingNumber());
                testDataStatisticsMap.put(m.getEpickey(),testStatisticsRspBO);
            }
        });
        List<TestStatisticsRspBO> testStatisticsRspBOlist = new LinkedList<>();
        for (Map.Entry<String, TestStatisticsRspBO> entry : testDataStatisticsMap.entrySet()) {
            TestStatisticsRspBO testStatisticsRspBO = entry.getValue();
            String key = entry.getKey();
            //??????????????????????????????
            int parseInt = Integer.parseInt(testStatisticsRspBO.getWorkingHours());
            String WorkingHours = String.format("%.2f", (float) parseInt / (float) 3600) + "??????";
            testStatisticsRspBO.setWorkingHours(WorkingHours);
            DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
            defectDetailsDO.setEndTime(endTime);
            defectDetailsDO.setStartTime(startTime);
            defectDetailsDO.setEpicKey(key);
            if (destStatisticsBO.getQueryFlag().equals("personal")) {
                defectDetailsDO.setDefectRegistrant(destStatisticsBO.getPersonName());
            }
            List<DefectDetailsDO> defectDetailList = defectDetailsExtDao.findByTime(defectDetailsDO);
            if(JudgeUtils.isNotEmpty(defectDetailList)){
                testStatisticsRspBO.setDefectsNumber(defectDetailList.size());
            }

            testStatisticsRspBOlist.add(testStatisticsRspBO);
        }
        return testStatisticsRspBOlist;

    }

    //???????????????????????????
    @Override
    public  List<ProductLineDefectsBO> productLineDefectRate(String month) {
        //  ???????????????
        DictionaryDO dictionaryDO = new DictionaryDO();
        dictionaryDO.setDicId("PRD_LINE");
        List<DictionaryDO> dictionaryDOList = dictionaryDao.getDicByDicId(dictionaryDO);

        // ??????????????????????????????
        dictionaryDOList = dictionaryDOList.subList(0,44);
        LinkedList<ProductLineDefectsBO> productLineDefectsList = new LinkedList();
        for(int i=0;i<dictionaryDOList.size();i++){

            ProductLineDefectsBO productLineDefectsBO = new ProductLineDefectsBO();
            productLineDefectsBO.setProductLine(dictionaryDOList.get(i).getValue());
            //??????????????????0
            productLineDefectsBO.setDefectsNumber("0");

            DemandDO demandDO1 = new DemandDO();
            demandDO1.setReqImplMon(month);
            demandDO1.setReqPrdLine(dictionaryDOList.get(i).getName());
            List<DemandDO> demandDOS1 = demandDao.find(demandDO1);
            //???????????????????????????
            double workload =0;
            int defectsNumber=0;
            if(JudgeUtils.isNotEmpty(demandDOS1)){
                for(int j=0;j<demandDOS1.size();j++){
                    //???????????? ,
                    int preMonPeriod = 0;
                    if(JudgeUtils.isNotBlank(demandDOS1.get(j).getPreMonPeriod())){
                        preMonPeriod= Integer.parseInt(demandDOS1.get(j).getPreMonPeriod());
                    }
                    //????????????
                    int preCurPeriod=0;
                    if(JudgeUtils.isNotBlank(demandDOS1.get(j).getPreMonPeriod())){
                        preCurPeriod = Integer.parseInt(demandDOS1.get(j).getPreCurPeriod());
                    }
                    //??????????????????uat??????  ??????????????????????????????uat??????????????? ??????????????????
                    if(preMonPeriod>=140||preCurPeriod<140){
                            continue;
                    }
                    workload=workload+demandDOS1.get(j).getTotalWorkload();
                    //????????????????????????  ???????????????jira?????????????????????????????????????????????
                    DemandJiraDO demandJiraDO = new DemandJiraDO();
                    demandJiraDO.setReqInnerSeq(demandDOS1.get(j).getReqInnerSeq());
                    List<DemandJiraDO> demandJiraDOS = demandJiraDao.find(demandJiraDO);
                    if(JudgeUtils.isEmpty(demandJiraDOS)){
                        continue;
                    }
                    String jiraKey = demandJiraDOS.get(0).getJiraKey();
                    DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                    defectDetailsDO.setEpicKey(jiraKey);
                    List<DefectDetailsDO> defectDetailsDOList = defectDetailsExtDao.find(defectDetailsDO);
                    defectsNumber=defectsNumber+defectDetailsDOList.size();
                }

            }
            productLineDefectsBO.setDefectsNumber(String.valueOf(defectsNumber));
            productLineDefectsBO.setWorkload(String.valueOf(workload));
            if(workload==0){
                productLineDefectsBO.setDefectRate("0.00%");
            }else{
                String defectRate = String.format("%.2f", (float) Integer.parseInt(productLineDefectsBO.getDefectsNumber()) / Double.parseDouble(productLineDefectsBO.getWorkload()) * 100) + "%";
                productLineDefectsBO.setDefectRate(defectRate);
            }
            //?????????????????????????????????0????????????
            if(workload !=0&&defectsNumber!=0){
                productLineDefectsList.add(productLineDefectsBO);
            }
        }
        return productLineDefectsList;
    }

    @Override
    public List<ProductLineDefectsBO> departmentalDefectRate(String month) {
        //  ????????????
        //??????????????????
        List<OrganizationStructureDO> impl = iOrganizationStructureDao.find(new OrganizationStructureDO());
        LinkedList<ProductLineDefectsBO> productLineDefectsList = new LinkedList();
        // ?????????????????????????????????????????????????????????????????????
        for(int i=0;i<impl.size();i++){
//            // ??????4??????????????????????????????????????????
//            if("???????????????".equals(impl.get(i).getSecondlevelorganization())||"??????????????????".equals(impl.get(i).getSecondlevelorganization())||"?????????????????????".equals(impl.get(i).getSecondlevelorganization())
//                    ||"???????????????".equals(impl.get(i).getSecondlevelorganization()) ||"???????????????????????????".equals(impl.get(i).getSecondlevelorganization()) ||"????????????????????????".equals(impl.get(i).getSecondlevelorganization())){
//                continue;
//            }
            ProductLineDefectsBO productLineDefectsBO = new ProductLineDefectsBO();
            productLineDefectsBO.setProductLine(impl.get(i).getSecondlevelorganization());
            //??????????????????0
            productLineDefectsBO.setDefectsNumber("0");

            // ????????????????????????
            DemandHoursRspBO demandHoursRspBO = getDeptWorkHoursAndPoint3(impl.get(i).getSecondlevelorganization(), null, month);
            List<String> listSum = demandHoursRspBO.getListSum();
            // ??????cr?????????
            double functionPoint =  Double.valueOf(listSum.get(1));
            //???????????????????????????
            int defectsNumber=0;

            // ?????????????????????plog???
            DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
            defectDetailsDO.setProblemHandlerDepartment(impl.get(i).getSecondlevelorganization());
            defectDetailsDO.setRegistrationDate(month);
            List<DefectDetailsDO> defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
            if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                defectsNumber = defectDetailsDOList.size();
            }
            productLineDefectsBO.setDefectsNumber(String.valueOf(defectsNumber));
            productLineDefectsBO.setWorkload(String.valueOf(functionPoint));
            productLineDefectsList.add(productLineDefectsBO);
        }
        LinkedList<ProductLineDefectsBO> productLineDefectsList2 = new LinkedList();
        DemandEaseDevelopmentDO easeDevelopmentDO = new DemandEaseDevelopmentDO();
        easeDevelopmentDO.setReqImplMon(month);
        //???????????????????????????
        List<String> firstLevelOrganizationList = iOrganizationStructureDao.findFirstLevelOrganization(new OrganizationStructureDO());
        // ?????????????????????????????????????????????
        List<DemandEaseDevelopmentDO>  mon_input_workload_list = easeDevelopmentExtDao.easeDevelopmentWorkloadCountForDevp(easeDevelopmentDO);
        for(int i=0;i<firstLevelOrganizationList.size();i++){
            double work = 0;
            int defectsNumber = 0;
            ProductLineDefectsBO productLineDefectsBO2 = new ProductLineDefectsBO();
            // ????????????
            productLineDefectsBO2.setProductLine(firstLevelOrganizationList.get(i));
            productLineDefectsBO2.setWorkload(0+"");
            // ?????????????????????????????????????????????
            for(int j=0;j<mon_input_workload_list.size();j++){
                if(firstLevelOrganizationList.get(i).equals(mon_input_workload_list.get(j).getFirstLevelOrganization())){
                    productLineDefectsBO2.setWorkload(mon_input_workload_list.get(j).getDevelopmentworkload());
                }
            }
            for(int j=0;j<productLineDefectsList.size();j++) {
                if ("???????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("???????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }

                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("????????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("??????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("??????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("????????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("?????????????????????".equals(productLineDefectsList.get(j).getProductLine()) || "??????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        defectsNumber = defectsNumber + Integer.parseInt(productLineDefectsList.get(j).getDefectsNumber());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(defectsNumber+"");
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("?????????????????????".equals(productLineDefectsList.get(j).getProductLine()) || "?????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        defectsNumber = defectsNumber + Integer.parseInt(productLineDefectsList.get(j).getDefectsNumber());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(defectsNumber+"");
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("?????????????????????".equals(productLineDefectsList.get(j).getProductLine()) || "?????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        defectsNumber = defectsNumber + Integer.parseInt(productLineDefectsList.get(j).getDefectsNumber());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(defectsNumber+"");
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("????????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("????????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("??????&?????????????????????".equals(productLineDefectsList.get(j).getProductLine()) || "??????&??????&???????????????".equals(productLineDefectsList.get(j).getProductLine())
                            || "???????????????".equals(productLineDefectsList.get(j).getProductLine())|| "???????????????".equals(productLineDefectsList.get(j).getProductLine()) ) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        defectsNumber = defectsNumber + Integer.parseInt(productLineDefectsList.get(j).getDefectsNumber());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(defectsNumber+"");
                    }
                }
                if ("?????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("?????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("???????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("???????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("???????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("???????????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
                if ("????????????????????????".equals(firstLevelOrganizationList.get(i))) {
                    if ("????????????????????????".equals(productLineDefectsList.get(j).getProductLine())) {
                        work = Double.valueOf(productLineDefectsBO2.getWorkload()) + Double.valueOf(productLineDefectsList.get(j).getWorkload());
                        productLineDefectsBO2.setWorkload(work+"");
                        productLineDefectsBO2.setDefectsNumber(productLineDefectsList.get(j).getDefectsNumber());
                    }
                }
            }
            if(work==0){
                productLineDefectsBO2.setDefectRate("0.00%");
            }else{
                String defectRate = String.format("%.2f", (float) Integer.parseInt(productLineDefectsBO2.getDefectsNumber()) /  Double.valueOf(productLineDefectsBO2.getWorkload()) * 100) + "%";
                productLineDefectsBO2.setDefectRate(defectRate);
            }
            System.err.println(productLineDefectsBO2);
            productLineDefectsList2.add(productLineDefectsBO2);
        }
        return productLineDefectsList2;
    }

    @Override
    public List<DemandTestStatusBO> demandTestStatusList() {
        //??????????????????
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        String month = simpleDateFormat.format(new Date());
        DemandDO demandDO1 = new DemandDO();
        demandDO1.setReqImplMon(month);
        List<DemandTestStatusBO> demandTestStatusBOList = new LinkedList<>();
        //???????????????????????????????????????????????????????????????????????????????????????
        List<DemandDO> demandDOS1 = demandDao.QueryIsExecutingDemand(demandDO1);
        if(JudgeUtils.isNotEmpty(demandDOS1)){
            demandDOS1.forEach(m->{
                //????????????
                int preMonPeriod = 0;
                if(JudgeUtils.isNotBlank(m.getPreMonPeriod())){
                    preMonPeriod= Integer.parseInt(m.getPreMonPeriod());
                }
                //???????????????????????????????????? ??????????????????????????????????????????
                if(preMonPeriod>=140){
                    return;
                }
                DemandTestStatusBO demandTestStatusBO = new DemandTestStatusBO();
                demandTestStatusBO.setReqNm(m.getReqNm());
                demandTestStatusBO.setReqNo(m.getReqNo());
                //????????????????????????  ???????????????jira?????????????????????????????????????????????
                DemandJiraDO demandJiraDO = new DemandJiraDO();
                demandJiraDO.setReqInnerSeq(m.getReqInnerSeq());
                List<DemandJiraDO> demandJiraDOS = demandJiraDao.find(demandJiraDO);

                if(JudgeUtils.isEmpty(demandJiraDOS)){
                    return;
                }
                String jiraKey = demandJiraDOS.get(0).getJiraKey();
                DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                defectDetailsDO.setEpicKey(jiraKey);
                //??????jirakey??????????????????
                List<DefectDetailsDO> defectDetailsDOList = defectDetailsExtDao.find(defectDetailsDO);
                //?????????
                demandTestStatusBO.setDefectsNumber(defectDetailsDOList.size());
                //?????????  ?????????/?????????
                String defectRate = String.format("%.2f", (float) demandTestStatusBO.getDefectsNumber() / (float) m.getTotalWorkload() * 100) + "%";
                demandTestStatusBO.setDefectRate(defectRate);
                JiraBasicInfoDO jiraBasicInfoDO = jiraBasicInfoDao.get(jiraKey);
                //??????????????? ?????????????????????????????????????????? ????????????
               if(JudgeUtils.isNull(jiraBasicInfoDO)){
                   return;
               }
                //??????????????????
                if (JudgeUtils.isBlank(jiraBasicInfoDO.getTestCaseNumber())){
                    demandTestStatusBO.setTestCaseNumber("0");
                }else{
                    demandTestStatusBO.setTestCaseNumber(jiraBasicInfoDO.getTestCaseNumber());
                }

                WorkingHoursDO workingHoursDO = new WorkingHoursDO();
                workingHoursDO.setRoletype("????????????");
                workingHoursDO.setEpickey(jiraKey);
                List<WorkingHoursDO> workingHoursList = iWorkingHoursDao.find(workingHoursDO);
                if(JudgeUtils.isNotEmpty(workingHoursList)){
                    for(int i=0;i<workingHoursList.size();i++){
                        //???????????????????????????????????????????????????
                        demandTestStatusBO.setCaseCompletedNumber(workingHoursList.get(i).getCaseCompletedNumber()+demandTestStatusBO.getCaseCompletedNumber());
                        demandTestStatusBO.setCaseExecutionNumber(workingHoursList.get(i).getCaseExecutionNumber()+demandTestStatusBO.getCaseExecutionNumber());
                    }
                }
                //???????????????
                if(demandTestStatusBO.getCaseCompletedNumber()!=0) {
                    String defectDiscoveryRate = String.format("%.2f", (float) demandTestStatusBO.getDefectsNumber() / (float) demandTestStatusBO.getCaseCompletedNumber() * 100) + "%";
                    demandTestStatusBO.setDefectDiscoveryRate(defectDiscoveryRate);
                }else{
                    demandTestStatusBO.setDefectDiscoveryRate("NaN%");
                }
                //????????????
                System.err.println(demandTestStatusBO.getTestCaseNumber());
                int intValue = Double.valueOf(demandTestStatusBO.getTestCaseNumber()).intValue();
                if(intValue!=0) {
                    String testProgress = String.format("%.2f", (float) demandTestStatusBO.getCaseCompletedNumber() / (float) intValue * 100) + "%";
                    demandTestStatusBO.setTestProgress(testProgress);
                }else{
                    demandTestStatusBO.setTestProgress("NaN%");
                }

                demandTestStatusBOList.add(demandTestStatusBO);
            });
        }
        return demandTestStatusBOList;
    }

    @Override
    public List<DemandTestStatusBO> demandTestStatusList2() {
        //??????????????????
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        String month = simpleDateFormat.format(new Date());
        DemandDO demandDO1 = new DemandDO();
        demandDO1.setReqImplMon(month);
        List<DemandTestStatusBO> demandTestStatusBOList = new LinkedList<>();
        //?????????????????????????????????
        List<DemandDO> demandDOS1 = demandDao.QueryIsExecutingDemand2(demandDO1);
        if(JudgeUtils.isNotEmpty(demandDOS1)){
            demandDOS1.forEach(m->{
                DemandTestStatusBO demandTestStatusBO = new DemandTestStatusBO();
                demandTestStatusBO.setReqNm(m.getReqNm());
                demandTestStatusBO.setReqNo(m.getReqNo());
                //????????????????????????  ???????????????jira?????????????????????????????????????????????
                DemandJiraDO demandJiraDO = new DemandJiraDO();
                demandJiraDO.setReqInnerSeq(m.getReqInnerSeq());
                List<DemandJiraDO> demandJiraDOS = demandJiraDao.find(demandJiraDO);

                if(JudgeUtils.isEmpty(demandJiraDOS)){
                    return;
                }
                String jiraKey = demandJiraDOS.get(0).getJiraKey();
                DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                defectDetailsDO.setEpicKey(jiraKey);
                //??????jirakey??????????????????
                List<DefectDetailsDO> defectDetailsDOList = defectDetailsExtDao.find(defectDetailsDO);
                //?????????
                demandTestStatusBO.setDefectsNumber(defectDetailsDOList.size());
                //?????????  ?????????/?????????
                String defectRate = String.format("%.2f", (float) demandTestStatusBO.getDefectsNumber() / (float) m.getTotalWorkload() * 100) + "%";
                demandTestStatusBO.setDefectRate(defectRate);
                JiraBasicInfoDO jiraBasicInfoDO = jiraBasicInfoDao.get(jiraKey);
                //??????????????? ?????????????????????????????????????????? ????????????
                if(JudgeUtils.isNull(jiraBasicInfoDO)){
                    return;
                }
                //??????????????????
                if (JudgeUtils.isBlank(jiraBasicInfoDO.getTestCaseNumber())){
                    demandTestStatusBO.setTestCaseNumber("0");
                }else{
                    demandTestStatusBO.setTestCaseNumber(jiraBasicInfoDO.getTestCaseNumber());
                }

                WorkingHoursDO workingHoursDO = new WorkingHoursDO();
                workingHoursDO.setRoletype("????????????");
                workingHoursDO.setEpickey(jiraKey);
                List<WorkingHoursDO> workingHoursList = iWorkingHoursDao.find(workingHoursDO);
                if(JudgeUtils.isNotEmpty(workingHoursList)){
                    for(int i=0;i<workingHoursList.size();i++){
                        //???????????????????????????????????????????????????
                        demandTestStatusBO.setCaseCompletedNumber(workingHoursList.get(i).getCaseCompletedNumber()+demandTestStatusBO.getCaseCompletedNumber());
                        demandTestStatusBO.setCaseExecutionNumber(workingHoursList.get(i).getCaseExecutionNumber()+demandTestStatusBO.getCaseExecutionNumber());
                    }
                }
                //???????????????
                if(demandTestStatusBO.getCaseCompletedNumber()!=0) {
                    String defectDiscoveryRate = String.format("%.2f", (float) demandTestStatusBO.getDefectsNumber() / (float) demandTestStatusBO.getCaseCompletedNumber() * 100) + "%";
                    demandTestStatusBO.setDefectDiscoveryRate(defectDiscoveryRate);
                }else{
                    demandTestStatusBO.setDefectDiscoveryRate("NaN%");
                }
                //????????????
                System.err.println(demandTestStatusBO.getTestCaseNumber());
                int intValue = Double.valueOf(demandTestStatusBO.getTestCaseNumber()).intValue();
                if(intValue!=0) {
                    String testProgress = String.format("%.2f", (float) demandTestStatusBO.getCaseCompletedNumber() / (float) intValue * 100) + "%";
                    demandTestStatusBO.setTestProgress(testProgress);
                }else{
                    demandTestStatusBO.setTestProgress("NaN%");
                }

                demandTestStatusBOList.add(demandTestStatusBO);
            });
        }
        return demandTestStatusBOList;
    }

    @Override
    public List<DefectProcesStatusBO> defectProcesStatus() {
        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
        List<DefectDetailsDO> defectDetailsDOList = defectDetailsExtDao.findUnfinishedDefects(defectDetailsDO);
        HashMap<String, DefectProcesStatusBO> defectProcesStatusMap = new HashMap<>();
        defectDetailsDOList.forEach(m->{
            DefectProcesStatusBO defectProcesStatusBO = defectProcesStatusMap.get(m.getDefectsDepartment());
            if(JudgeUtils.isNotNull(defectProcesStatusBO)){
                if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setProcess(defectProcesStatusBO.getProcess()+1);
                }else if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setPending(defectProcesStatusBO.getPending()+1);
                }else if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setPendingUpgrade(defectProcesStatusBO.getPendingUpgrade()+1);
                }else if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setWithRetest(defectProcesStatusBO.getWithRetest()+1);
                }else if(m.getDefectStatus().equals("????????????")){
                    defectProcesStatusBO.setProblemFreeze(defectProcesStatusBO.getProblemFreeze()+1);
                }
                defectProcesStatusMap.put(m.getDefectsDepartment(),defectProcesStatusBO);
            }else{
                defectProcesStatusBO=new DefectProcesStatusBO();
                if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setProcess(defectProcesStatusBO.getProcess()+1);
                }else if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setPending(defectProcesStatusBO.getPending()+1);
                }else if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setPendingUpgrade(defectProcesStatusBO.getPendingUpgrade()+1);
                }else if(m.getDefectStatus().equals("?????????")){
                    defectProcesStatusBO.setWithRetest(defectProcesStatusBO.getWithRetest()+1);
                }else if(m.getDefectStatus().equals("????????????")){
                    defectProcesStatusBO.setProblemFreeze(defectProcesStatusBO.getProblemFreeze()+1);
                }
                defectProcesStatusMap.put(m.getDefectsDepartment(),defectProcesStatusBO);
            }
        });
        List<DefectProcesStatusBO> DefectProcesStatusBOList = new LinkedList<>();

        for (Map.Entry<String, DefectProcesStatusBO> entry : defectProcesStatusMap.entrySet()) {
            DefectProcesStatusBO defectProcesStatusBO = entry.getValue();
            defectProcesStatusBO.setDepartment(entry.getKey());
            DefectProcesStatusBOList.add(defectProcesStatusBO);
        }
        return  DefectProcesStatusBOList;
    }

    @Override
    public OnlineLeakageRateBO getOnlineLeakageRate(String month) {

        SimpleDateFormat simpleDateFormatMonth = new SimpleDateFormat("yyyy-MM");
        String[] months = new String[6];
        months[5]=month;
         for(int i=months.length-2;i>=0;i--){
             //?????????????????????
             try {
                 Date month1 = simpleDateFormatMonth.parse(month);
                 Calendar c = Calendar.getInstance();
                 c.setTime(month1);
                 c.add(Calendar.MONTH, i-5);
                 String month2 = simpleDateFormatMonth.format(c.getTime());
                 months[i]=month2;
             } catch (Exception e) {
                 //todo  ??????????????????
             }
         }
        String[] leakageRate = new String[6];
      for(int i=0;i<months.length;i++){
          ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
          productionDefectsDO.setProcessstartdate(months[i]);
          List<ProductionDefectsDO> list = productionDefectsExtDao.findMonthList(productionDefectsDO);
          DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
          defectDetailsDO.setRegistrationDate(months[i]);
          List<DefectDetailsDO> list1 = defectDetailsExtDao.findList(defectDetailsDO);
          if(list.size()==0||list1.size()==0){
              leakageRate[i]="0";
          }else{
              String rate = String.format("%.2f", (float) list.size() / (float) (list1.size() + list.size()) * 100);
              leakageRate[i]=rate;
          }

      }
        OnlineLeakageRateBO onlineLeakageRateBO = new OnlineLeakageRateBO();

        onlineLeakageRateBO.setMonths(months);
        onlineLeakageRateBO.setLeakageRate(leakageRate);
    return onlineLeakageRateBO;

    }

    @Override
    public OnlineLeakageRateBO getDeptDefectRate(String devpLeadDept, String month) {
        //??????6?????????????????????
        SimpleDateFormat simpleDateFormatMonth = new SimpleDateFormat("yyyy-MM");
        String[] months = new String[6];
        months[5]=month;
        for(int i=months.length-2;i>=0;i--){
            //?????????????????????
            try {
                Date month1 = simpleDateFormatMonth.parse(month);
                Calendar c = Calendar.getInstance();
                c.setTime(month1);
                c.add(Calendar.MONTH, i-5);
                String month2 = simpleDateFormatMonth.format(c.getTime());
                months[i]=month2;
            } catch (Exception e) {
                //todo  ??????????????????
            }
        }
        String[] leakageRate = new String[6];
        for(int i=0;i<months.length;i++){
            //?????????????????????????????????
            DemandDO demandDO1 = new DemandDO();
            demandDO1.setReqImplMon(months[i]);
            demandDO1.setDevpLeadDept(devpLeadDept);
            List<DemandDO> demandDOS = demandDao.find(demandDO1);
            double workload =0;
            int defectsNumber=0;
            if(JudgeUtils.isNotEmpty(demandDOS)){
                for(int j=0;j<demandDOS.size();j++){
                    //???????????? ,
                    int preMonPeriod = 0;
                    if(JudgeUtils.isNotBlank(demandDOS.get(j).getPreMonPeriod())){
                        preMonPeriod= Integer.parseInt(demandDOS.get(j).getPreMonPeriod());
                    }
                    //????????????
                    int preCurPeriod=0;
                    if(JudgeUtils.isNotBlank(demandDOS.get(j).getPreMonPeriod())){
                        preCurPeriod = Integer.parseInt(demandDOS.get(j).getPreCurPeriod());
                    }
                    //??????????????????uat??????  ??????????????????????????????uat??????????????? ??????????????????
                    if(preMonPeriod>=140||preCurPeriod<140){
                        continue;
                    }
                    workload=workload+demandDOS.get(j).getTotalWorkload();
                    //????????????????????????  ???????????????jira?????????????????????????????????????????????
                    DemandJiraDO demandJiraDO = new DemandJiraDO();
                    demandJiraDO.setReqInnerSeq(demandDOS.get(j).getReqInnerSeq());
                    List<DemandJiraDO> demandJiraDOS = demandJiraDao.find(demandJiraDO);
                    if(JudgeUtils.isEmpty(demandJiraDOS)){
                        continue;
                    }
                    String jiraKey = demandJiraDOS.get(0).getJiraKey();
                    DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                    defectDetailsDO.setEpicKey(jiraKey);
                    List<DefectDetailsDO> defectDetailsDOList = defectDetailsExtDao.find(defectDetailsDO);
                    //???????????????
                    defectsNumber=defectsNumber+defectDetailsDOList.size();
                }
            }
            if(workload==0||defectsNumber==0){
                leakageRate[i]="0";
            }else{
                //????????????????????????????????????
                String rate = String.format("%.2f", (float) defectsNumber / (float)workload  * 100);
                leakageRate[i]=rate;
            }
        }
        OnlineLeakageRateBO onlineLeakageRateBO = new OnlineLeakageRateBO();
        onlineLeakageRateBO.setMonths(months);
        onlineLeakageRateBO.setLeakageRate(leakageRate);
        return onlineLeakageRateBO;
    }
    @Override
    public void downloadReportForm7(String devpLeadDept,String date,String date1,String date2, HttpServletResponse response){
        List<WorkloadMonthBO> workloadMonthBOArrayList = new ArrayList<>();
        List<WorkingHoursBO> reportLista = new ArrayList<>();
        reportLista = this.getReportForm7(devpLeadDept,date,date1,date2);
        if (JudgeUtils.isNotEmpty(reportLista)) {
            reportLista.forEach(m -> {
                WorkloadMonthBO workloadMonthBO = new WorkloadMonthBO();
                // ???????????????
                workloadMonthBO.setDisplayname(m.getDisplayname());
                workloadMonthBO.setSumTime(m.getSumTime());
                // ??????m.getListDay().size()?????????
                if(m.getListDay().size() == 31){
                    workloadMonthBO.setA1(m.getListDay().get(0));
                    workloadMonthBO.setA2(m.getListDay().get(1));
                    workloadMonthBO.setA3(m.getListDay().get(2));
                    workloadMonthBO.setA4(m.getListDay().get(3));
                    workloadMonthBO.setA5(m.getListDay().get(4));
                    workloadMonthBO.setA6(m.getListDay().get(5));
                    workloadMonthBO.setA7(m.getListDay().get(6));
                    workloadMonthBO.setA8(m.getListDay().get(7));
                    workloadMonthBO.setA9(m.getListDay().get(8));
                    workloadMonthBO.setA10(m.getListDay().get(9));
                    workloadMonthBO.setA11(m.getListDay().get(10));
                    workloadMonthBO.setA12(m.getListDay().get(11));
                    workloadMonthBO.setA13(m.getListDay().get(12));
                    workloadMonthBO.setA14(m.getListDay().get(13));
                    workloadMonthBO.setA15(m.getListDay().get(14));
                    workloadMonthBO.setA16(m.getListDay().get(15));
                    workloadMonthBO.setA17(m.getListDay().get(16));
                    workloadMonthBO.setA18(m.getListDay().get(17));
                    workloadMonthBO.setA19(m.getListDay().get(18));
                    workloadMonthBO.setA20(m.getListDay().get(19));
                    workloadMonthBO.setA21(m.getListDay().get(20));
                    workloadMonthBO.setA22(m.getListDay().get(21));
                    workloadMonthBO.setA23(m.getListDay().get(22));
                    workloadMonthBO.setA24(m.getListDay().get(23));
                    workloadMonthBO.setA25(m.getListDay().get(24));
                    workloadMonthBO.setA26(m.getListDay().get(25));
                    workloadMonthBO.setA27(m.getListDay().get(26));
                    workloadMonthBO.setA28(m.getListDay().get(27));
                    workloadMonthBO.setA29(m.getListDay().get(28));
                    workloadMonthBO.setA30(m.getListDay().get(29));
                    workloadMonthBO.setA31(m.getListDay().get(30));

                }
                // ??????m.getListDay().size()?????????
                if(m.getListDay().size() == 30){
                    workloadMonthBO.setA1(m.getListDay().get(0));
                    workloadMonthBO.setA2(m.getListDay().get(1));
                    workloadMonthBO.setA3(m.getListDay().get(2));
                    workloadMonthBO.setA4(m.getListDay().get(3));
                    workloadMonthBO.setA5(m.getListDay().get(4));
                    workloadMonthBO.setA6(m.getListDay().get(5));
                    workloadMonthBO.setA7(m.getListDay().get(6));
                    workloadMonthBO.setA8(m.getListDay().get(7));
                    workloadMonthBO.setA9(m.getListDay().get(8));
                    workloadMonthBO.setA10(m.getListDay().get(9));
                    workloadMonthBO.setA11(m.getListDay().get(10));
                    workloadMonthBO.setA12(m.getListDay().get(11));
                    workloadMonthBO.setA13(m.getListDay().get(12));
                    workloadMonthBO.setA14(m.getListDay().get(13));
                    workloadMonthBO.setA15(m.getListDay().get(14));
                    workloadMonthBO.setA16(m.getListDay().get(15));
                    workloadMonthBO.setA17(m.getListDay().get(16));
                    workloadMonthBO.setA18(m.getListDay().get(17));
                    workloadMonthBO.setA19(m.getListDay().get(18));
                    workloadMonthBO.setA20(m.getListDay().get(19));
                    workloadMonthBO.setA21(m.getListDay().get(20));
                    workloadMonthBO.setA22(m.getListDay().get(21));
                    workloadMonthBO.setA23(m.getListDay().get(22));
                    workloadMonthBO.setA24(m.getListDay().get(23));
                    workloadMonthBO.setA25(m.getListDay().get(24));
                    workloadMonthBO.setA26(m.getListDay().get(25));
                    workloadMonthBO.setA27(m.getListDay().get(26));
                    workloadMonthBO.setA28(m.getListDay().get(27));
                    workloadMonthBO.setA29(m.getListDay().get(28));
                    workloadMonthBO.setA30(m.getListDay().get(29));
                }
                // ??????m.getListDay().size()?????????
                if(m.getListDay().size() == 29){
                    workloadMonthBO.setA1(m.getListDay().get(0));
                    workloadMonthBO.setA2(m.getListDay().get(1));
                    workloadMonthBO.setA3(m.getListDay().get(2));
                    workloadMonthBO.setA4(m.getListDay().get(3));
                    workloadMonthBO.setA5(m.getListDay().get(4));
                    workloadMonthBO.setA6(m.getListDay().get(5));
                    workloadMonthBO.setA7(m.getListDay().get(6));
                    workloadMonthBO.setA8(m.getListDay().get(7));
                    workloadMonthBO.setA9(m.getListDay().get(8));
                    workloadMonthBO.setA10(m.getListDay().get(9));
                    workloadMonthBO.setA11(m.getListDay().get(10));
                    workloadMonthBO.setA12(m.getListDay().get(11));
                    workloadMonthBO.setA13(m.getListDay().get(12));
                    workloadMonthBO.setA14(m.getListDay().get(13));
                    workloadMonthBO.setA15(m.getListDay().get(14));
                    workloadMonthBO.setA16(m.getListDay().get(15));
                    workloadMonthBO.setA17(m.getListDay().get(16));
                    workloadMonthBO.setA18(m.getListDay().get(17));
                    workloadMonthBO.setA19(m.getListDay().get(18));
                    workloadMonthBO.setA20(m.getListDay().get(19));
                    workloadMonthBO.setA21(m.getListDay().get(20));
                    workloadMonthBO.setA22(m.getListDay().get(21));
                    workloadMonthBO.setA23(m.getListDay().get(22));
                    workloadMonthBO.setA24(m.getListDay().get(23));
                    workloadMonthBO.setA25(m.getListDay().get(24));
                    workloadMonthBO.setA26(m.getListDay().get(25));
                    workloadMonthBO.setA27(m.getListDay().get(26));
                    workloadMonthBO.setA28(m.getListDay().get(27));
                    workloadMonthBO.setA29(m.getListDay().get(28));
                }
                // ??????m.getListDay().size()?????????
                if(m.getListDay().size() == 28){
                    workloadMonthBO.setA1(m.getListDay().get(0));
                    workloadMonthBO.setA2(m.getListDay().get(1));
                    workloadMonthBO.setA3(m.getListDay().get(2));
                    workloadMonthBO.setA4(m.getListDay().get(3));
                    workloadMonthBO.setA5(m.getListDay().get(4));
                    workloadMonthBO.setA6(m.getListDay().get(5));
                    workloadMonthBO.setA7(m.getListDay().get(6));
                    workloadMonthBO.setA8(m.getListDay().get(7));
                    workloadMonthBO.setA9(m.getListDay().get(8));
                    workloadMonthBO.setA10(m.getListDay().get(9));
                    workloadMonthBO.setA11(m.getListDay().get(10));
                    workloadMonthBO.setA12(m.getListDay().get(11));
                    workloadMonthBO.setA13(m.getListDay().get(12));
                    workloadMonthBO.setA14(m.getListDay().get(13));
                    workloadMonthBO.setA15(m.getListDay().get(14));
                    workloadMonthBO.setA16(m.getListDay().get(15));
                    workloadMonthBO.setA17(m.getListDay().get(16));
                    workloadMonthBO.setA18(m.getListDay().get(17));
                    workloadMonthBO.setA19(m.getListDay().get(18));
                    workloadMonthBO.setA20(m.getListDay().get(19));
                    workloadMonthBO.setA21(m.getListDay().get(20));
                    workloadMonthBO.setA22(m.getListDay().get(21));
                    workloadMonthBO.setA23(m.getListDay().get(22));
                    workloadMonthBO.setA24(m.getListDay().get(23));
                    workloadMonthBO.setA25(m.getListDay().get(24));
                    workloadMonthBO.setA26(m.getListDay().get(25));
                    workloadMonthBO.setA27(m.getListDay().get(26));
                    workloadMonthBO.setA28(m.getListDay().get(27));
                }
                workloadMonthBOArrayList.add(workloadMonthBO);
            });
        }
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), WorkloadMonthBO.class, workloadMonthBOArrayList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "???????????????????????????" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }
    @Override
    public TestProgressDetailRspBO testProgressDetail(String selectTime2){
        if (StringUtils.isBlank(selectTime2) ) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        TestProgressDetailRspBO testProgressDetailRspBO = new TestProgressDetailRspBO();
        // ??????????????????????????????????????????????????????????????????
        //?????????
        String startTime =  DateUtil.getFriday(selectTime2);
        //?????????
        String endTime =  DateUtil.getThursday(selectTime2);
        TestProgressDetailDO testProgressDetailDO = new TestProgressDetailDO();
        testProgressDetailDO.setStartTime(startTime);
        testProgressDetailDO.setEndTime(endTime);
        //???????????????
        List<String> listLine =  testProgressDetailExtDao.findListLine(testProgressDetailDO);
        if(listLine == null || listLine.size() <=0){
            return testProgressDetailRspBO;
        }
        List<TestProgressDetailBO> list = new ArrayList<>();
        //?????????????????????????????????????????????????????????
        for(int i= 0;i<listLine.size();i++){
            TestProgressDetailBO testProgressDetailBO = new TestProgressDetailBO();
            testProgressDetailBO.setReqPrdLine(listLine.get(i));
             int completionNumber = 0;
             int underwayNumber = 0;
             int caseExecutionNumber = 0;
             int plogNumber = 0;
             testProgressDetailDO.setReqPrdLine(listLine.get(i));
            // ???????????????????????????????????????????????????epic
            List<String> listEpic =  testProgressDetailExtDao.findListEpic(testProgressDetailDO);
            //??????listEpic??????????????????????????????
            if(listEpic == null || listEpic.size() <=0){
               continue;
            }
            // ?????????epic?????????????????????????????????????????????
            for(int j=0;j<listEpic.size();j++){
                testProgressDetailDO.setEpickey(listEpic.get(j));
                //??????????????????????????????????????????
                List<TestProgressDetailDO> listDate =  testProgressDetailExtDao.findListDate(testProgressDetailDO);
                // ????????????????????????????????????
                List<TestProgressDetailDO> listPriorDate =  testProgressDetailExtDao.findListPriorDate(testProgressDetailDO);

                if(listDate == null || listDate.size() <=0){
                    continue;
                }
                //???????????? if?????????NaN ?????????????????????????????????????????????????????????0??????????????????+1
                //??????
                String testProgress =  listDate.get(listDate.size()-1).getTestProgress();
                //?????????????????????
                int caseExecution = Integer.parseInt(listDate.get(listDate.size()-1).getCaseExecutionNumber());
                //?????????????????????
                int CaseCompleted = Integer.parseInt(listDate.get(listDate.size()-1).getCaseCompletedNumber());
                //?????????
                int defects = Integer.parseInt(listDate.get(listDate.size()-1).getDefectsNumber());
                //???????????????????????????
                if(testProgress.equals("NaN%")){
                    if(caseExecution!=0 ||CaseCompleted!=0){
                        underwayNumber = underwayNumber+1;
                    }
                }
                if(!testProgress.equals("NaN%")){
                    double progressNumber = Double.valueOf(testProgress.replaceAll("%",""));
                    if(progressNumber>=100){
                        completionNumber = completionNumber+1;
                    }
                    if(progressNumber>0&&progressNumber<100){
                        underwayNumber = underwayNumber+1;
                    }
                }
                //??????????????????????????????????????????????????????????????????plog??????????????????????????????????????????
                if(listPriorDate == null || listPriorDate.size() <=0){
                    caseExecutionNumber = caseExecutionNumber + caseExecution;
                    plogNumber = plogNumber + defects;
                }else{
                    //?????????????????????
                    int caseExecution1 = caseExecution - Integer.parseInt(listPriorDate.get(listPriorDate.size()-1).getCaseExecutionNumber());
                    //?????????
                    int defects1 = defects -Integer.parseInt(listPriorDate.get(listPriorDate.size()-1).getDefectsNumber());
                    caseExecutionNumber = caseExecutionNumber + caseExecution1;
                    plogNumber = plogNumber + defects1;

                }


            }
            testProgressDetailBO.setCompletionNumber(completionNumber);
            testProgressDetailBO.setCaseExecutionNumber(caseExecutionNumber);
            testProgressDetailBO.setUnderwayNumber(underwayNumber);
            testProgressDetailBO.setPlogNumber(plogNumber);
            list.add(testProgressDetailBO);
        }
        testProgressDetailRspBO.setTestProgressDetailBOList(list);
        return testProgressDetailRspBO;
    }
    @Override
    public DemandHoursRspBO departmentalDefectRate2(String selectTime1, String selectTime2){
        List<ProductLineDefectsBO> impl = null;
        List<String> list = getSixMonth(selectTime2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> listRate = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        // ???????????? ???????????????????????????????????????
        for (int i = 0; i < list.size(); i++) {
            impl = departmentalDefectRate(list.get(i));
            SumBos.add(list.get(i));
            int sum = 0;
            double defectsNumber = 0;
            double workload = 0;
            if (impl != null && impl.size() >= 0) {
                for (int j = 0; j < impl.size(); j++) {
                    defectsNumber = defectsNumber + Double.valueOf(impl.get(j).getDefectsNumber());
                    workload = workload + Double.valueOf(impl.get(j).getWorkload());
                }
                if(workload==0){
                    workingHoursBOS.add("0");
                    listRate.add(defectsNumber +"/"+0);
                }else{
                    String defectRate = String.format("%.2f", defectsNumber / workload * 100);
                    workingHoursBOS.add(defectRate);
                    listRate.add(defectsNumber +"/"+workload);
                }
            } else {
                workingHoursBOS.add("0");
            }
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setListRate(listRate);
        return demandHoursRspBO;
    }

    /**
     * ??????????????????
     * @param selectTime1
     * @param selectTime2
     * @param seriesName
     * @param name
     * @return
     */
    @Override
    public DefectDetailsRspBO getDetails(String selectTime1, String selectTime2,String seriesName,String name){
        List<DefectDetailsDO> defectDetailsDOList = new LinkedList<>();
        if (StringUtils.isBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
        defectDetailsDO.setProblemHandlerDepartment(name);
        // ?????????
        if (StringUtils.isNotBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            defectDetailsDO.setRegistrationDate(selectTime1);
            defectDetailsDOList = defectDetailsExtDao.findWeekList(defectDetailsDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(selectTime2) && StringUtils.isBlank(selectTime1)) {
            defectDetailsDO.setRegistrationDate(selectTime2);
            defectDetailsDOList = defectDetailsExtDao.findList(defectDetailsDO);
        }
        List<DefectDetailsBO> defectDetailsBOList = new LinkedList<>();
        defectDetailsBOList = BeanConvertUtils.convertList(defectDetailsDOList, DefectDetailsBO.class);

        System.err.println(defectDetailsBOList.size());
        DefectDetailsRspBO defectDetailsRspBO = new DefectDetailsRspBO();
         defectDetailsRspBO.setDefectDetailsBos(defectDetailsBOList);

        return defectDetailsRspBO;
    }

    @Override
    public IssueDetailsRspBO getIssueDetails(String selectTime1, String selectTime2,String seriesName,String name){
        List<IssueDetailsDO> issueDetailsDOLinkedList = new LinkedList<>();
        if (StringUtils.isBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        IssueDetailsDO issueDetailsDO = new IssueDetailsDO();
        issueDetailsDO.setIssueDepartment(name);
        // ?????????
        if (StringUtils.isNotBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            issueDetailsDO.setRegistrationDate(selectTime1);
            issueDetailsDOLinkedList = issueDetailsExtDao.findWeekList(issueDetailsDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(selectTime2) && StringUtils.isBlank(selectTime1)) {
            issueDetailsDO.setRegistrationDate(selectTime2);
            issueDetailsDOLinkedList = issueDetailsExtDao.findList(issueDetailsDO);
        }
        List<IssueDetailsBO> issueDetailsBOList = new LinkedList<>();
        issueDetailsBOList = BeanConvertUtils.convertList(issueDetailsDOLinkedList, IssueDetailsBO.class);

        IssueDetailsRspBO issueDetailsRspBO = new IssueDetailsRspBO();
        issueDetailsRspBO.setIssueDetailsBOList(issueDetailsBOList);

        return issueDetailsRspBO;
    }

    @Override
    public ProductionDefectsRspBO getProDefectDetails(String selectTime1, String selectTime2,String seriesName,String name){
        List<ProductionDefectsDO> productionDefectsDOLinkedList = new LinkedList<>();
        List<OnlineDefectDO> onlineDefectDOList = new LinkedList<>();
        if (StringUtils.isBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
        productionDefectsDO.setProblemattributiondept(name);
        OnlineDefectDO onlineDefectDO = new OnlineDefectDO();
        onlineDefectDO.setFirstlevelorganization(name);
        // ?????????
        if (StringUtils.isNotBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            if(selectTime1.substring(0,7).compareTo("2020-09")<0){
                productionDefectsDO.setProcessstartdate(selectTime1);
                productionDefectsDOLinkedList = productionDefectsExtDao.findWeekList(productionDefectsDO);
            }else{
                onlineDefectDO.setProcessStartDate(selectTime1);
                onlineDefectDOList = onlineDefectExtDao.findWeekList(onlineDefectDO);
                if(JudgeUtils.isNotEmpty(onlineDefectDOList)){
                    for (OnlineDefectDO m : onlineDefectDOList) {
                        ProductionDefectsDO productionDefectsDO1 = new ProductionDefectsDO();
                        productionDefectsDO1.setQuestionnumber(m.getDocumentNumber());
                        productionDefectsDO1.setQuestiontitle(m.getDefectTheme());
                        productionDefectsDO1.setProcessstatus(m.getProcessStatus());
                        productionDefectsDO1.setProcessstartdate(m.getProcessStartDate());
                        productionDefectsDO1.setCurrentsession("");
                        productionDefectsDO1.setProblemraiser(m.getDefectProposer());
                        productionDefectsDO1.setPersonincharge(m.getDevelopmentLeader());
                        productionDefectsDO1.setQuestiontype(m.getQuestionType());
                        productionDefectsDO1.setIdentifytheproblem("");
                        productionDefectsDO1.setProblemattributiondept(m.getFirstlevelorganization());
                        productionDefectsDOLinkedList.add(productionDefectsDO1);
                    }
                }

            }

        }
        // ?????????
        if (StringUtils.isNotBlank(selectTime2) && StringUtils.isBlank(selectTime1)) {
            if(selectTime2.compareTo("2020-09")<0){
                productionDefectsDO.setProcessstartdate(selectTime2);
                productionDefectsDOLinkedList = productionDefectsExtDao.findList(productionDefectsDO);
            }else{
                onlineDefectDO.setProcessStartDate(selectTime2);
                onlineDefectDOList = onlineDefectExtDao.findMonthList(onlineDefectDO);
                if(JudgeUtils.isNotEmpty(onlineDefectDOList)){
                    for (OnlineDefectDO m : onlineDefectDOList) {
                        ProductionDefectsDO productionDefectsDO1 = new ProductionDefectsDO();
                        productionDefectsDO1.setQuestionnumber(m.getDocumentNumber());
                        productionDefectsDO1.setQuestiontitle(m.getDefectTheme());
                        productionDefectsDO1.setProcessstatus(m.getProcessStatus());
                        productionDefectsDO1.setProcessstartdate(m.getProcessStartDate());
                        productionDefectsDO1.setCurrentsession("");
                        productionDefectsDO1.setProblemraiser(m.getDefectProposer());
                        productionDefectsDO1.setPersonincharge(m.getDevelopmentLeader());
                        productionDefectsDO1.setQuestiontype(m.getQuestionType());
                        productionDefectsDO1.setIdentifytheproblem("");
                        productionDefectsDO1.setProblemattributiondept(m.getFirstlevelorganization());
                        productionDefectsDOLinkedList.add(productionDefectsDO1);
                    }
                }

            }

        }
        List<ProductionDefectsBO> productionDefectsBOList = new LinkedList<>();
        productionDefectsBOList = BeanConvertUtils.convertList(productionDefectsDOLinkedList, ProductionDefectsBO.class);

        ProductionDefectsRspBO productionDefectsRspBO = new ProductionDefectsRspBO();
        productionDefectsRspBO.setProductionDefectsBOList(productionDefectsBOList);
        return productionDefectsRspBO;
    }

    @Override
    public ProUnhandledIssuesRspBO getProUnhandledIssuesDetails(String selectTime1, String selectTime2,String seriesName,String name){
        List<ProUnhandledIssuesDO> proUnhandledIssuesDOLinkedList = new LinkedList<>();
        if (StringUtils.isBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        ProUnhandledIssuesDO proUnhandledIssuesDO = new ProUnhandledIssuesDO();
        proUnhandledIssuesDO.setDepartment(name);
        // ?????????
        if (StringUtils.isNotBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            proUnhandledIssuesDO.setProductionDate(selectTime1);
            proUnhandledIssuesDOLinkedList = proUnhandledIssuesExtDao.findWeek(proUnhandledIssuesDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(selectTime2) && StringUtils.isBlank(selectTime1)) {
            proUnhandledIssuesDO.setProductionDate(selectTime2);
            proUnhandledIssuesDOLinkedList = proUnhandledIssuesExtDao.findMonth(proUnhandledIssuesDO);
        }
        List<ProUnhandledIssuesBO> proUnhandledIssuesBOList = new LinkedList<>();
        proUnhandledIssuesBOList = BeanConvertUtils.convertList(proUnhandledIssuesDOLinkedList, ProUnhandledIssuesBO.class);
        if(!proUnhandledIssuesBOList.isEmpty()){
            //iterator??????
            Iterator<ProUnhandledIssuesBO> it = proUnhandledIssuesBOList.iterator();
            while(it.hasNext()){
                ProUnhandledIssuesBO proUnhandledIssuesBO = it.next();
                if("????????????????????????".equals(seriesName)){
                    if(proUnhandledIssuesBO.getProblemNumber() == 0){
                        it.remove();
                    }
                }
                if("??????????????????".equals(seriesName)){
                    if(proUnhandledIssuesBO.getDefectsNumber() == 0){
                        it.remove();
                    }
                }
            }
        }
        ProUnhandledIssuesRspBO proUnhandledIssuesRspBO = new ProUnhandledIssuesRspBO();
        proUnhandledIssuesRspBO.setProUnhandledIssuesBOList(proUnhandledIssuesBOList);
        return proUnhandledIssuesRspBO;
    }

    @Override
    public SmokeTestFailedCountRspBO getSmokeTestFailedCount2(String selectTime1, String selectTime2,int count ){
        List<SmokeTestFailedCountDO> smokeTestFailedCountDOLinkedList = new LinkedList<>();
        if (StringUtils.isBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        SmokeTestFailedCountDO smokeTestFailedCountDO = new SmokeTestFailedCountDO();
        smokeTestFailedCountDO.setCount(count);
        // ?????????
        if (StringUtils.isNotBlank(selectTime1) && StringUtils.isBlank(selectTime2)) {
            smokeTestFailedCountDO.setTestDate(selectTime1);
            smokeTestFailedCountDOLinkedList = iSmokeTestFailedCountDao.findWeek(smokeTestFailedCountDO);
        }
        // ?????????
        if (StringUtils.isNotBlank(selectTime2) && StringUtils.isBlank(selectTime1)) {
            smokeTestFailedCountDO.setTestDate(selectTime2);
            smokeTestFailedCountDOLinkedList = iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
        }
        List<SmokeTestFailedCountBO> smokeTestFailedCountBOList = new LinkedList<>();
        smokeTestFailedCountBOList = BeanConvertUtils.convertList(smokeTestFailedCountDOLinkedList, SmokeTestFailedCountBO.class);

        SmokeTestFailedCountRspBO smokeTestFailedCountRspBO = new SmokeTestFailedCountRspBO();
        smokeTestFailedCountRspBO.setSmokeTestFailedCountBOList(smokeTestFailedCountBOList);
        return smokeTestFailedCountRspBO;
    }

    @Override
    public void test(){
        //??????????????????????????????????????????  SUM????????????????????????+???????????????????????????+????????????????????????
        BigDecimal sum = new BigDecimal("0");
        DemandBO demandBO = new DemandBO();
        //??????????????????
        Date date = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("yyyyMM");
        String month = "2020-10";//simpleDateFormat.format(date);
        String month2 = "202010";//simpleDateFormat2.format(date);
        demandBO.setReqImplMon(month);
        //  ?????????????????????
        List<Double> dept = reqWorkLoadService.getExportCountForDevp3(demandBO);
        System.err.println("?????????????????????"+dept);
//        ???????????????????????? dept.get(6) ???????????????????????? dept.get(5)???????????????????????? dept.get(4)  ???????????????????????? dept.get(7)
//        ???????????????????????? dept.get(9) ???????????????????????? dept.get(8)???????????????????????? dept.get(1)  ???????????????????????? dept.get(3)
//        ?????????????????? dept.get(2)
// ???????????????????????????
        DemandEaseDevelopmentBO demandEaseDevelopmentBO = new DemandEaseDevelopmentBO();
        demandEaseDevelopmentBO.setReqImplMon(month);
        List<Double> dept2 =reqTaskService.easeDevelopmentWorkloadCountForDevp3(demandEaseDevelopmentBO);
        System.err.println("???????????????????????????"+dept2);
        //?????????????????????
        //[???????????????, ????????????????????????, ??????????????????, ????????????????????????, ????????????????????????, ????????????????????????, ????????????????????????, ????????????????????????, ????????????????????????, ????????????????????????, ?????????????????????, ???????????????, ???????????????????????????, ????????????????????????]
        SupportWorkloadBO supportWorkloadBO = new SupportWorkloadBO();
        supportWorkloadBO.setReqImplMon(month);
        List<Double> dept3 =reqWorkLoadService.supportWorkloadCountForDevp3(supportWorkloadBO);
        System.err.println("?????????????????????"+dept3);
        // ???????????????
        sum = new BigDecimal(dept.get(6)+dept2.get(6)+dept3.get(6));
        System.err.println(sum);
        //??????????????????
        DecimalFormat df = new DecimalFormat("0.00");
        System.err.println(df.format(sum));
        //2???????????????????????????????????????
        //3?????????????????????????????????????????????/???????????????????????? ??????????????????????????????
        //?????????????????????????????????????????????????????????????????????????????? reqFinishRate
        List<ReqDataCountBO> reportListb  = this.getCompByDept2(month);
        for(int i = 0;i<reportListb.size();i++){
            System.err.println(reportListb.get(i).getFirstLevelOrganization()+"--"+reportListb.get(i).getReqFinishRate()+"--"+reportListb.get(i).getTotal());
        }
        //5??????????????????????????????????????????0
        //6?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        //7??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        List<DemandBO> reportList6 =  this.getReportForm6(month,"","","????????????????????????");
        int sumA = 0;
        int sumB = 0;
        for(int i = 0;i<reportList6.size();i++){
            if(JudgeUtils.isNull(reportList6.get(i).getProjectStartTm())){
                sumA = sumA+1;
            }
            sumB = sumB +reportList6.get(i).getNoUpload();
        }
        System.err.println("??????????????????????????????=="+sumA);
        System.err.println("??????????????????????????????=="+sumB);
        //??????????????????????????? ????????? ????????????????????????????????????????????????
        int bjs = reqDataCountDao.getProduction2(month,"????????????????????????");
        System.err.println("????????????????????????=="+bjs);
        //???????????????????????????
        int defectsNumber=0;

        // ?????????????????????plog???
        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
        defectDetailsDO.setFirstlevelorganization("????????????????????????");
        defectDetailsDO.setRegistrationDate("2020-10");
        List<DefectDetailsDO> defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
        if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
            defectsNumber = defectDetailsDOList.size();
        }
        System.err.println("???????????????plog???=="+defectsNumber);
        //?????????????????????????????????????????????????????????????????????????????????????????????
        int jiuhu = iOperationProductionDao.findCount("2020-10","????????????????????????");
        System.err.println("?????????????????????=="+jiuhu);
        //????????????????????????
        int smokeNunber = 0;
        SmokeTestFailedCountDO smokeTestFailedCountDO = new SmokeTestFailedCountDO();
        smokeTestFailedCountDO.setDepartment("????????????????????????");
        smokeTestFailedCountDO.setTestDate("2020-10");
        List<SmokeTestFailedCountDO> smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
        if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
            smokeNunber = smokeTestFailedCountDOList.size();
        }
        System.err.println("????????????????????????=="+smokeNunber);
        //15??????????????????????????????????????????????????????
        //16???????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        //17?????????????????????????????????????????????5


        String  [] deftList= {"????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","??????????????????"};
        for (int i=0;i<deftList.length;i++){
            System.err.println(deftList[i]);
            QuantitativeDataDO quantitativeDataDO = new QuantitativeDataDO();
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(6)+dept2.get(6)+dept3.get(6))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("605");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio(df.format((dept.get(6)+dept2.get(6)+dept3.get(6))/605));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                 reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(6)+dept2.get(6)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(6)+dept2.get(6)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(6))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(6))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(6))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(5)+dept2.get(5)+dept3.get(5))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("909");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio(df.format((dept.get(5)+dept2.get(5)+dept3.get(5))/909));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(5)+dept2.get(5)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(5)+dept2.get(5)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(5))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(5))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(5))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(4)+dept2.get(4)+dept3.get(4))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("1071");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio(df.format((dept.get(4)+dept2.get(4)+dept3.get(4))/1071));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(4)+dept2.get(4)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(4)+dept2.get(4)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(4))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(4))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(4))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(7)+dept2.get(7)+dept3.get(7))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("483");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio(df.format((dept.get(7)+dept2.get(7)+dept3.get(7))/483));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(7)+dept2.get(7)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(7)+dept2.get(7)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(7))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(7))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(7))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(9)+dept2.get(9)+dept3.get(9))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("1785");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio(df.format((dept.get(9)+dept2.get(9)+dept3.get(9))/1785));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(9)+dept2.get(9)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(9)+dept2.get(9)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(9))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(9))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(9))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(8)+dept2.get(8)+dept3.get(8))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("187");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio(df.format((dept.get(8)+dept2.get(8)+dept3.get(8))/187));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(8)+dept2.get(8)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(8)+dept2.get(8)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(8))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(8))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(8))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(1)+dept2.get(1)+dept3.get(1))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("250");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio(df.format((dept.get(1)+dept2.get(1)+dept3.get(1))/250));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(1)+dept2.get(1)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(1)+dept2.get(1)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(1))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(1))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(1))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("????????????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(3)+dept2.get(3)+dept3.get(3))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("675");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio( df.format(((dept.get(3)+dept2.get(3)+dept3.get(3)) / 675 )));
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                if(JudgeUtils.isNotEmpty(reportListb)){
                    for(int j = 0;j<reportListb.size();j++){
                        if(deftList[i].equals(reportListb.get(j).getFirstLevelOrganization())){
                            quantitativeDataDO.setTargetCompletionRate(reportListb.get(j).getReqFinishRate());
                            quantitativeDataDO.setProductReleaseRate(reportListb.get(j).getTotal());
                        }
                    }
                }
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                defectsNumber=0;
                // ?????????????????????plog???
                defectDetailsDO.setFirstlevelorganization(deftList[i]);
                defectDetailsDO.setRegistrationDate(month);
                defectDetailsDOList = defectDetailsExtDao.findValidList(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    defectsNumber = defectDetailsDOList.size();
                }
                if((dept.get(3)+dept2.get(3)) != 0){
                    quantitativeDataDO.setDefectRate(df.format((defectsNumber/(dept.get(3)+dept2.get(3)))*100)+"%");
                }else{
                    quantitativeDataDO.setDefectRate("0.00%");
                }
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(3))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(3))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(3))));
                System.err.println(quantitativeDataDO);

            }
            if(deftList[i].equals("??????????????????")){
                //??????
                quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
                //??????
                quantitativeDataDO.setReqImplMon(month);
                //????????????????????????
                quantitativeDataDO.setFunctionPointsAssessWorkload(df.format(new BigDecimal(dept.get(2)+dept2.get(2)+dept3.get(2))));
                //????????????????????????
                quantitativeDataDO.setCostCoefficientsSum("-");
                //inputOutputRatio ???????????????
                quantitativeDataDO.setInputOutputRatio("-");
                //targetCompletionRate ???????????????
                //productReleaseRate ???????????????
                quantitativeDataDO.setTargetCompletionRate(reqDataCountDao.getTestFinished(month));
                quantitativeDataDO.setProductReleaseRate("-");
                //documentsOutputNumber ????????????????????????
                quantitativeDataDO.setDocumentsOutputNumber(0);
                //projectsNotCompletedNumber ??????????????????????????????
                //projectsDocumentsNotCompletedNumber ??????????????????????????????
                reportList6 =  this.getReportForm6(month,"","",deftList[i]);
                sumA = 0;
                sumB =0;
                if(JudgeUtils.isNotEmpty(reportList6)){
                    for(int j = 0;j<reportList6.size();j++){
                        if(JudgeUtils.isNull(reportList6.get(j).getProjectStartTm())){
                            sumA = sumA+1;
                        }
                        sumB = sumB +reportList6.get(j).getNoUpload();
                    }
                }
                quantitativeDataDO.setProjectsNotCompletedNumber(sumA);
                quantitativeDataDO.setProjectsDocumentsNotCompletedNumber(sumB);
                //notTimelyInputProductionNumber ????????????????????????
                quantitativeDataDO.setNotTimelyInputProductionNumber(reqDataCountDao.getProduction2(month,deftList[i]));
                //defectRate ?????????  ??????????????????=????????????/????????????PLOG?????????+???????????????????????????

                quantitativeDataDO.setDefectRate("????????????");
                //dataChangeProblemsNumber ?????????????????????
                quantitativeDataDO.setDataChangeProblemsNumber(iOperationProductionDao.findCount(month,deftList[i]));
                //productionProblemsNumber ?????????????????? ?????????????????????????????????????????????????????????????????????
                quantitativeDataDO.setProductionProblemsNumber(onlineDefectExtDao.findCount(month2,deftList[i]));
                //smokeTestFailed ????????????????????????
                smokeNunber = 0;
                smokeTestFailedCountDO.setDepartment(deftList[i]);
                smokeTestFailedCountDO.setTestDate(month);
                smokeTestFailedCountDOList =iSmokeTestFailedCountDao.findMonth(smokeTestFailedCountDO);
                if(JudgeUtils.isNotEmpty(smokeTestFailedCountDOList)){
                    smokeNunber = smokeTestFailedCountDOList.size();
                }
                quantitativeDataDO.setSmokeTestFailed(smokeNunber);
                //workingAttitude ???????????????????????????
                quantitativeDataDO.setWorkingAttitude(5);
                quantitativeDataDO.setDevelopWorkSum(Double.parseDouble(df.format(dept.get(2))));
                quantitativeDataDO.setEasyWorkSum(Double.parseDouble(df.format(dept2.get(2))));
                quantitativeDataDO.setSupportWorkSum(Double.parseDouble(df.format(dept3.get(2))));
                System.err.println(quantitativeDataDO);

            }
            List<QuantitativeDataDO> quantitativeDataDOList =  quantitativeDataExtDao.findOne(quantitativeDataDO);
            if(JudgeUtils.isEmpty(quantitativeDataDOList)){
                quantitativeDataExtDao.insert(quantitativeDataDO);
            }else{
                quantitativeDataDO.setQuantitativeId(quantitativeDataDOList.get(0).getQuantitativeId());
                quantitativeDataExtDao.update(quantitativeDataDO);
            }
        }

    }

    /**
     * ???????????????????????????git???????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreGitLab(String date1, String date2) {
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        List<GitlabDataDO> gitlabDataDOList = null;
        List<OrganizationStructureDO> impl = iOrganizationStructureDao.find(new OrganizationStructureDO());
        // ??????????????????
        List<String> deptName = new LinkedList<>();
        for (int i = 0; i < impl.size(); i++) {
            deptName.add(impl.get(i).getSecondlevelorganization());
            // ?????????
            if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
                GitlabDataDO gitlabDataDO = new GitlabDataDO();
                gitlabDataDO.setDevpLeadDept(impl.get(i).getSecondlevelorganization());
                gitlabDataDO.setCommittedDate(date1);
                gitlabDataDOList = gitlabDataDao.findWeekGit(gitlabDataDO);
                String rate = "?????????????????????"+gitlabDataDOList.get(0).getStatsTotal()+"</br>??????????????????"+gitlabDataDOList.get(0).getStatsAdditions()+"</br>??????????????????" +gitlabDataDOList.get(0).getStatsDeletions();
                String str = "{ products: '" + impl.get(i).getSecondlevelorganization() + "', ???????????????: '" + gitlabDataDOList.get(0).getStatsTotal()  + "', rate: '" + rate + "'}";
                workingHoursBOS.add(str);
            }
            //?????????
            if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
                GitlabDataDO gitlabDataDO = new GitlabDataDO();
                gitlabDataDO.setDevpLeadDept(impl.get(i).getSecondlevelorganization());
                gitlabDataDO.setCommittedDate(date2);
                gitlabDataDOList = gitlabDataDao.findMonthGit(gitlabDataDO);
                String rate = "?????????????????????"+gitlabDataDOList.get(0).getStatsTotal()+"</br>??????????????????"+gitlabDataDOList.get(0).getStatsAdditions()+"</br>??????????????????" +gitlabDataDOList.get(0).getStatsDeletions();
                String str = "{ products: '" + impl.get(i).getSecondlevelorganization() + "', ???????????????: '" + gitlabDataDOList.get(0).getStatsTotal()  + "', rate: '" + rate + "'}";
                workingHoursBOS.add(str);
            }

        }
        SumBos.add("product");
        SumBos.add("???????????????");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????????????????
     * @param devpLeadDept
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreGitLabDept(String devpLeadDept,String date1, String date2) {
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        List<String> listRate = new LinkedList<>();
        List<GitlabDataDO> gitlabDataDOList = null;
        List<GitlabDataDO> gitlabDataDOList2 = null;
        // ?????????
        if (StringUtils.isNotBlank(date1) && StringUtils.isBlank(date2)) {
            GitlabDataDO gitlabDataDO = new GitlabDataDO();
            gitlabDataDO.setDevpLeadDept(devpLeadDept);
            gitlabDataDO.setCommittedDate(date1);
            gitlabDataDOList = gitlabDataDao.findWeekGitLab(gitlabDataDO);
            gitlabDataDOList2 = gitlabDataDao.findWeekGitLabSum(gitlabDataDO);

        }
        //?????????
        if (StringUtils.isNotBlank(date2) && StringUtils.isBlank(date1)) {
            GitlabDataDO gitlabDataDO = new GitlabDataDO();
            gitlabDataDO.setDevpLeadDept(devpLeadDept);
            gitlabDataDO.setCommittedDate(date2);
            gitlabDataDOList = gitlabDataDao.findMonthGitLab(gitlabDataDO);
            gitlabDataDOList2 = gitlabDataDao.findMonthGitLabSum(gitlabDataDO);
        }
        if(JudgeUtils.isNotEmpty(gitlabDataDOList)){
            for(int i =0;i<gitlabDataDOList.size();i++){
                String rate = "?????????????????????"+gitlabDataDOList.get(i).getStatsTotal()+"</br>??????????????????"+gitlabDataDOList.get(i).getStatsAdditions()+"</br>??????????????????" +gitlabDataDOList.get(i).getStatsDeletions();
                String str = "{ products: '" + gitlabDataDOList.get(i).getDisplayName() + "', ???????????????: '" + gitlabDataDOList.get(i).getStatsTotal()  + "', rate: '" + rate + "'}";
                // { product: '??????????????????', ??????: '70', ?????????: '150'},
                workingHoursBOS.add(str);
                listRate.add(gitlabDataDOList.get(i).getDisplayName());
            }
            List<UserDO> list = iUserDao.getUserByDept(devpLeadDept);
            if(JudgeUtils.isNotEmpty(list)){
                int sum = list.size()-gitlabDataDOList.size();
                for(int i =0;i<sum;i++){
                    String str = "{ products: '" + ""+ "'}";
                    workingHoursBOS.add(str);
                    listRate.add("");
                }
            }
        } else {
            List<UserDO> list = iUserDao.getUserByDept(devpLeadDept);
            if(JudgeUtils.isNotEmpty(list)){
                for(int i =0;i<list.size();i++){
                    String str = "{ products: '" + list.get(i).getFullname() + "', ???????????????: '" + 0  + "'}";
                    listRate.add(list.get(i).getFullname());
                    workingHoursBOS.add(str);
                }
            }
        }

        SumBos.add("product");
        SumBos.add("???????????????");
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        demandHoursRspBO.setListRate(listRate);
        demandHoursRspBO.setSum(gitlabDataDOList2.get(0).getStatsTotal()+"");
        return demandHoursRspBO;
    }

    /**
     * ???????????????????????????
     *
     * @param date1
     * @param date2
     * @return
     */
    @Override
    public DemandHoursRspBO getCentreGitlabMonth(String date1, String date2) {
        List<GitlabDataDO> impl = null;
        GitlabDataDO workingHoursDO = new GitlabDataDO();
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setCommittedDate(list.get(i));
            impl = gitlabDataDao.findMonthGitLabSum(workingHoursDO);
            SumBos.add(list.get(i));
            workingHoursBOS.add(impl.get(0).getStatsTotal()+"");
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }

    @Override
    public DemandHoursRspBO getTeamGitlabMonth(String devpLeadDept,String date1, String date2){
        List<GitlabDataDO> impl = null;
        GitlabDataDO workingHoursDO = new GitlabDataDO();
        workingHoursDO.setDevpLeadDept(devpLeadDept);
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setCommittedDate(list.get(i));
            impl = gitlabDataDao.findMonthGitLabSum(workingHoursDO);
            SumBos.add(list.get(i));
            workingHoursBOS.add(impl.get(0).getStatsTotal()+"");
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }
    @Override
    public DemandHoursRspBO getEmployeeGitlabMonth(String displayName ,String date1, String date2){
        List<GitlabDataDO> impl = null;
        GitlabDataDO workingHoursDO = new GitlabDataDO();
        workingHoursDO.setDisplayName(displayName);
        List<String> list = getSixMonth(date2);
        List<String> workingHoursBOS = new LinkedList<>();
        List<String> SumBos = new LinkedList<>();
        for (int i = 0; i < list.size(); i++) {
            workingHoursDO.setCommittedDate(list.get(i));
            impl = gitlabDataDao.findMonthGitLabSum(workingHoursDO);
            SumBos.add(list.get(i));
            workingHoursBOS.add(impl.get(0).getStatsTotal()+"");
        }
        DemandHoursRspBO demandHoursRspBO = new DemandHoursRspBO();
        demandHoursRspBO.setStringList(workingHoursBOS);
        demandHoursRspBO.setListSum(SumBos);
        return demandHoursRspBO;
    }
}

