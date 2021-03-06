package com.cmpay.lemon.monitor.service.impl.demand;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.cmpay.lemon.common.Env;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.BeanUtils;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.common.utils.StringUtils;
import com.cmpay.lemon.framework.page.PageInfo;
import com.cmpay.lemon.framework.security.SecurityUtils;
import com.cmpay.lemon.framework.utils.LemonUtils;
import com.cmpay.lemon.framework.utils.PageUtils;
import com.cmpay.lemon.monitor.bo.*;
import com.cmpay.lemon.monitor.bo.jira.CreateIssueResponseBO;
import com.cmpay.lemon.monitor.bo.jira.CreateIssueTestSubtaskRequestBO;
import com.cmpay.lemon.monitor.bo.jira.JiraTaskBodyBO;
import com.cmpay.lemon.monitor.dao.*;
import com.cmpay.lemon.monitor.entity.*;
import com.cmpay.lemon.monitor.entity.sendemail.MailFlowDO;
import com.cmpay.lemon.monitor.entity.sendemail.MultiMailSenderInfo;
import com.cmpay.lemon.monitor.entity.sendemail.MultiMailsender;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import com.cmpay.lemon.monitor.service.SystemUserService;
import com.cmpay.lemon.monitor.service.demand.ReqPlanService;
import com.cmpay.lemon.monitor.service.demand.ReqTaskService;
import com.cmpay.lemon.monitor.service.dic.DictionaryService;
import com.cmpay.lemon.monitor.service.jira.JiraOperationService;
import com.cmpay.lemon.monitor.service.reportForm.GenRptService;
import com.cmpay.lemon.monitor.utils.BeanConvertUtils;
import com.cmpay.lemon.monitor.utils.DateUtil;
import com.cmpay.lemon.monitor.utils.ReadExcelUtils;
import com.cmpay.lemon.monitor.utils.jira.JiraUtil;
import io.restassured.response.Response;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.zip.CRC32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.cmpay.lemon.monitor.constant.MonitorConstants.FILE;
import static com.cmpay.lemon.monitor.utils.FileUtils.importExcel;
import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * @author: zhou_xiong
 */
@Service
public class ReqTaskServiceImpl implements ReqTaskService {
    //???????????????
    private static final Long SUPERADMINISTRATOR = (long) 10506;
    //????????????
    private static final Long SUPERADMINISTRATOR1 = (long) 5004;

    //???????????????
    private static final Long SUPERADMINISTRATOR2 = (long) 5001;
    //???????????????
    private static final Long SUPERADMINISTRATOR3 = (long) 5006;
    //????????????
    private static final Long SUPERADMINISTRATOR4 = (long) 5002;
    //30 ?????????????????????
    private static final String REQSUSPEND = "30";
    //40 ?????????????????????
    private static final String REQCANCEL = "40";
    // 30 ????????????
    private static final int REQCONFIRM = 30;
    // 120 UAT????????????
    private static final int UPDATEUAT = 120;
    // 140 ??????UAT??????
    private static final int FINISHUATTEST = 140;
    // ????????????
    private static final String OLDPRDLINE = "????????????";
    //  ????????????
    private static final String WUXIAOWENTI = "????????????";
    private static final String TESTDEVP = "??????????????????";
    private static final Logger LOGGER = LoggerFactory.getLogger(ReqTaskServiceImpl.class);
    @Autowired
    private IDemandExtDao demandDao;
    @Autowired
    private IDictionaryExtDao dictionaryDao;
    @Autowired
    private IDemandStateHistoryExtDao demandStateHistoryDao;
    @Autowired
    private IDemandCurperiodHistoryDao demandCurperiodHistoryDao;
    @Autowired
    private IDemandJiraDao demandJiraDao;
    @Autowired
    private IJiraDepartmentDao jiraDepartmentDao;
    @Autowired
    IDemandJiraSubtaskDao demandJiraSubtaskDao;
    @Autowired
    IDemandJiraDevelopMasterTaskDao demandJiraDevelopMasterTaskDao;
    @Autowired
    IJiraBasicInfoDao jiraBasicInfoDao;
    @Autowired
    private ITPermiDeptDao permiDeptDao;

    @Autowired
    private IPermiUserDao permiUserDao;
    @Autowired
    private IUserRoleExtDao userRoleExtDao;
    @Autowired
    SystemUserService userService;
    @Autowired
    private IDemandChangeDetailsExtDao demandChangeDetailsDao;
    @Autowired
    private IErcdmgErorDao iErcdmgErorDao;
    @Autowired
    private IUserExtDao iUserDao;
    @Autowired
    private IDemandPictureDao iDemandPictureDao;
    @Autowired
    private IDemandNameChangeExtDao iDemandNameChangeExtDao;
    @Autowired
    private IProductionDefectsExtDao productionDefectsDao;
    @Autowired
    ISmokeTestRegistrationExtDao smokeTestRegistrationDao;
    @Autowired
    ISmokeTestFailedCountExtDao smokeTestFailedCountDao;
    @Autowired
    IWorkingHoursExtDao workingHoursDao;
    @Autowired
    IDemandResourceInvestedDao demandResourceInvestedDao;
    @Autowired
    DictionaryService dictionaryService;
    @Autowired
    IDefectDetailsExtDao defectDetailsDao;
    @Resource
    private GenRptService genRptService;
    @Resource
    private IDemandEaseDevelopmentExtDao easeDevelopmentExtDao;

    @Autowired
    private ReqPlanService reqPlanService;

    @Autowired
    private JiraOperationService jiraOperationService;
    @Autowired
    private IOrganizationStructureDao iOrganizationStructureDao;
    @Autowired
    private IQuantitativeDataExtDao quantitativeDataExtDao;

    @Override
    public DemandBO findById(String req_inner_seq) {
        DemandDO demandDO = demandDao.get(req_inner_seq);
        if (JudgeUtils.isNull(demandDO)) {
            LOGGER.warn("id???[{}]??????????????????", req_inner_seq);
            BusinessException.throwBusinessException(MsgEnum.DB_FIND_FAILED);
        }
        return BeanUtils.copyPropertiesReturnDest(new DemandBO(), demandDO);
    }

    @Override
    public DemandDO findById1(String req_inner_seq) {
        DemandDO demandDO = demandDao.get(req_inner_seq);
        return demandDO;
    }

    @Override
    public List<DemandDO> findById(List<String> ids) {
        LinkedList<DemandDO> demandDOList = new LinkedList<>();
        ids.forEach(m -> {
            DemandDO demandDO = demandDao.get(m);
            if (JudgeUtils.isNotNull(demandDO)) {
                demandDOList.add(demandDO);
            }
        });
        return demandDOList;
    }

    @Override
    public DemandRspBO find(DemandBO demandBO) {
        String time = DateUtil.date2String(new Date(), "yyyy-MM-dd");
        PageInfo<DemandBO> pageInfo = getPageInfo(demandBO);
        List<DemandBO> demandBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandBO.class);

        for (int i = 0; i < demandBOList.size(); i++) {
            if (JudgeUtils.isEmpty(demandBOList.get(i).getIsSvnBuild())) {
                demandBOList.get(i).setIsSvnBuild("???");
            }
            String reqAbnorType = demandBOList.get(i).getReqAbnorType();
            String reqAbnorTypeAll = "";
            DemandBO demand = findById(demandBOList.get(i).getReqInnerSeq());

            //????????????????????????uat??????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (StringUtils.isNotBlank(demand.getPrdFinshTm()) && StringUtils.isNotBlank(demand.getUatUpdateTm())
                    && StringUtils.isNotBlank(demand.getTestFinshTm()) && StringUtils.isNotBlank(demand.getPreCurPeriod())
                    && StringUtils.isNotBlank(demand.getReqSts())) {
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
                if (StringUtils.isBlank(reqAbnorTypeAll)) {
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
        demandBOList.forEach(m -> {
            DemandJiraDO demandJiraDO = demandJiraDao.get(m.getReqInnerSeq());
            if (demandJiraDO != null) {
                m.setJiraKey(demandJiraDO.getJiraKey());
            }
        });

        DemandRspBO demandRspBO = new DemandRspBO();
        demandRspBO.setDemandBOList(demandBOList);
        demandRspBO.setPageInfo(pageInfo);
        return demandRspBO;
    }

    private PageInfo<DemandBO> getPageInfo(DemandBO demandBO) {
        DemandDO demandDO = new DemandDO();
        BeanConvertUtils.convert(demandDO, demandBO);
        PageInfo<DemandBO> pageInfo = PageUtils.pageQueryWithCount(demandBO.getPageNum(), demandBO.getPageSize(),
                () -> BeanConvertUtils.convertList(demandDao.find(demandDO), DemandBO.class));
        return pageInfo;
    }

    //??????
    @Override
    public void getReqTask(HttpServletResponse response, DemandBO demandBO) {
        List<DemandDO> demandDOList = reqTask(demandBO);

        demandDOList.forEach(m -> {
            List<DemandStateHistoryDO> list = demandStateHistoryDao.getLastRecordByReqInnerSeq(m.getReqInnerSeq());
            if (JudgeUtils.isNotEmpty(list)) {
                m.setRequirementStatusModificationNotes(list.get(0).getRemarks());
            }
        });
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandDO.class, demandDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "reqTask_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    private List<DemandDO> reqTask(DemandBO demandBO) {
        DemandDO demandDO = new DemandDO();
        BeanConvertUtils.convert(demandDO, demandBO);
        return demandDao.getReqTask(demandDO);
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void add(DemandBO demandBO) {
        System.err.println(demandBO);
        // ?????????
        String user = userService.getFullname(SecurityUtils.getLoginName());
        // ????????????
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //????????????
        checkReqTask(demandBO);
        //????????????
        String mon = DateUtil.date2String(new Date(), "yyyy-MM");
        demandBO.setReqImplMon(mon);
        demandBO.setReqStartMon(mon);
        // ?????????????????????????????????????????????
        List<DemandBO> list = getReqTaskByUK(demandBO);
        if (list.size() > 0) {
            BusinessException.throwBusinessException(MsgEnum.NON_UNIQUE);
        }

        //???????????????
        //1????????????????????????????????????????????????????????????????????????????????????????????????????????????
        //2?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        int year = Integer.parseInt(demandBO.getReqImplMon().substring(0, 4));
        int month = Integer.parseInt(demandBO.getReqImplMon().substring(5, 7));
        String startdata = demandBO.getReqImplMon() + "-01";
        String enddata = demandBO.getReqImplMon() + "-" + getMonthLastDay(year, month);
        if ("180".equals(demandBO.getCurMonTarget())) {
            if (demandBO.getExpPrdReleaseTm().compareTo(startdata) < 0 || demandBO.getExpPrdReleaseTm().compareTo(enddata) > 0) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???????????????????????????????????????????????????????????????????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        } else {
            if (demandBO.getExpPrdReleaseTm().compareTo(enddata) < 0) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("????????????????????????????????????????????????????????????????????????????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }

        demandBO.setReqInnerSeq(getNextInnerSeq());
        if (JudgeUtils.isEmpty(demandBO.getQaMng())) {
            demandBO.setQaMng("?????????");
        }
        if (JudgeUtils.isEmpty(demandBO.getReqType())) {
            demandBO.setReqType("01");
        }
        if (JudgeUtils.isEmpty(demandBO.getConfigMng())) {
            demandBO.setConfigMng("?????????");
        }
        if (JudgeUtils.isEmpty(demandBO.getReqAbnorType())) {
            demandBO.setReqAbnorType("01");
        }
        // ??????????????????svn????????? ??????
        if (JudgeUtils.isEmpty(demandBO.getIsSvnBuild())) {
            demandBO.setIsSvnBuild("???");
        }
        setDefaultValue(demandBO);
        setDefaultUser(demandBO);
        setReqSts(demandBO);
        DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
        DemandChangeDetailsDO demandChangeDetailsDO = new DemandChangeDetailsDO();
        try {
            //???????????????
            demandDao.insert(BeanUtils.copyPropertiesReturnDest(new DemandDO(), demandBO));

            // ?????????????????????????????????????????? ???????????????????????????????????????????????????
            DemandNameChangeDO demandNameChangeDO = new DemandNameChangeDO();
            // ???????????????
            demandNameChangeDO.setNewReqInnerSeq(demandBO.getReqInnerSeq());
            // ?????????
            demandNameChangeDO.setNewReqNm(demandBO.getReqNm());
            // ?????????
            demandNameChangeDO.setNewReqNo(demandBO.getReqNo());
            // ?????????
            demandNameChangeDO.setOperator(user);
            // ????????????
            demandNameChangeDO.setOperationTime(time);
            // ????????????
            demandNameChangeDO.setUuid(UUID.randomUUID().toString());
            // ????????????
            iDemandNameChangeExtDao.insert(demandNameChangeDO);
            // ?????????????????????????????????????????????????????????svn???????????????
            if(JudgeUtils.isNotEmpty(demandBO.getReqNm())&&JudgeUtils.isNotEmpty(demandBO.getReqNo())){
                reqPlanService.bulidSVNProjrcts(demandBO.getReqInnerSeq(),demandBO.getReqNo(),demandBO.getReqNm());
            }

            String reqInnerSeq = demandDao.getMaxInnerSeq().getReqInnerSeq();
            // ???????????????????????????
            demandChangeDetailsDO.setReqInnerSeq(reqInnerSeq);
            demandChangeDetailsDO.setReqNo(demandBO.getReqNo());
            demandChangeDetailsDO.setReqNm(demandBO.getReqNm());
            demandChangeDetailsDO.setCreatUser(user);
            demandChangeDetailsDO.setCreatTime(LocalDateTime.now());
            demandChangeDetailsDO.setIdentification(reqInnerSeq);
            demandChangeDetailsDO.setReqImplMon(demandBO.getReqImplMon());
            demandChangeDetailsDao.insert(demandChangeDetailsDO);
            //?????????????????????
            demandStateHistoryDO.setReqInnerSeq(reqInnerSeq);
            demandStateHistoryDO.setReqSts("??????");
            demandStateHistoryDO.setRemarks("????????????");
            demandStateHistoryDO.setReqNm(demandBO.getReqNm());
            demandStateHistoryDO.setReqNo(demandBO.getReqNo());
            //???????????????????????????
            //???????????????????????????????????????
            String identificationByReqInnerSeq = demandChangeDetailsDao.getIdentificationByReqInnerSeq(reqInnerSeq);
            if (identificationByReqInnerSeq == null) {
                identificationByReqInnerSeq = reqInnerSeq;
            }
            demandStateHistoryDO.setIdentification(identificationByReqInnerSeq);
            //?????????????????????
            demandStateHistoryDO.setCreatUser(user);
            demandStateHistoryDO.setCreatTime(LocalDateTime.now());
            //???????????????????????????
            demandStateHistoryDao.insert(demandStateHistoryDO);

            //????????????jira
            jiraOperationService.createEpic(demandBO);
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.DB_INSERT_FAILED);
        }
    }


    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void delete(String reqInnerSeq) {
        DemandDO demandDO = demandDao.get(reqInnerSeq);
        if (StringUtils.isNotEmpty(demandDO.getReqNo())) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????REQ????????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (permissionCheck(reqInnerSeq)) {
            demandDao.logicDelete(reqInnerSeq);
        } else {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("????????????????????????????????????????????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void deleteBatch(List<String> ids) {
        ids.forEach(m -> {
            this.delete(m);
        });
      //  ids.forEach(demandJiraDao::delete);
    }

    /**
     * ????????????????????????
     */
    public int getMonthLastDay(int year, int month) {
        Calendar a = Calendar.getInstance();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month - 1);
        a.set(Calendar.DATE, 1);//?????????????????????????????????
        a.roll(Calendar.DATE, -1);//??????????????????????????????????????????
        int maxDate = a.get(Calendar.DATE);
        return maxDate;
    }

    // ???????????????????????????
    public boolean isDepartmentManager(Long juese, Long userid) {
        //????????????????????????
        UserRoleDO userRoleDO = new UserRoleDO();
        userRoleDO.setRoleId(juese);
        userRoleDO.setUserNo(userid);
        List<UserRoleDO> userRoleDOS = new LinkedList<>();
        userRoleDOS = userRoleExtDao.find(userRoleDO);
        if (!userRoleDOS.isEmpty()) {
            return true;
        }
        return false;
    }

    // ???????????????????????????
    public boolean isDepartmentManager(Long juese) {
        //????????????????????????
        UserRoleDO userRoleDO = new UserRoleDO();
        userRoleDO.setRoleId(juese);
        userRoleDO.setUserNo(Long.parseLong(SecurityUtils.getLoginUserId()));
        List<UserRoleDO> userRoleDOS = new LinkedList<>();
        userRoleDOS = userRoleExtDao.find(userRoleDO);
        if (!userRoleDOS.isEmpty()) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void update(DemandBO demandBO) {
        // ?????????
        String user = userService.getFullname(SecurityUtils.getLoginName());
        // ????????????
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        //????????????
        checkReqTask(demandBO);
        // ?????????????????????????????????????????????????????????
        if (!isDepartmentManager(SUPERADMINISTRATOR) && !isDepartmentManager(SUPERADMINISTRATOR2)) {
            // ????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
            if (StringUtils.isNotBlank(demandBO.getProjectMng())) {
                TPermiUser tPermiUser = iErcdmgErorDao.findByUsername(demandBO.getProjectMng());
                if (tPermiUser == null) {
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????:" + demandBO.getProjectMng() + "????????????????????????????????????????????????");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
                UserDO userByUserName = iUserDao.getUserByUserName(tPermiUser.getUserId());
                if (!isDepartmentManager(SUPERADMINISTRATOR1, userByUserName.getUserNo()) && !isDepartmentManager(SUPERADMINISTRATOR3, userByUserName.getUserNo()) && !isDepartmentManager(SUPERADMINISTRATOR4, userByUserName.getUserNo())) {
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????,???????????????????????????????????????");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
            }
        }
        //???????????????
        setDefaultValue(demandBO);
        setDefaultUser(demandBO);
        setReqSts(demandBO);
        //???????????????
        DemandDO demandDO = demandDao.get(demandBO.getReqInnerSeq());
        //??????????????????????????????REQ??????,??????????????????????????????
        if (!demandBO.getReqInnerSeq().trim().startsWith("REQ")) {
            //????????????????????????????????????
            checkReqNo(demandBO, demandDO);
        }
        try {
            // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
            String oldReqNo = demandDO.getReqNo();
            String oldReqNm = demandDO.getReqNm();
            String newReqNo = demandBO.getReqNo();
            String newReqNm = demandBO.getReqNm();
            if (newReqNo == null) {
                newReqNm = "";
            }
            if (newReqNm == null) {
                newReqNm = "";
            }
            if (oldReqNo == null) {
                oldReqNo = "";
            }
            if (oldReqNm == null) {
                oldReqNm = "";
            }
            if (!oldReqNo.equals(newReqNo) || !oldReqNm.equals(newReqNm)) {
                DemandNameChangeDO demandNameChangeDO = new DemandNameChangeDO();
                // ???????????????
                demandNameChangeDO.setNewReqInnerSeq(demandDO.getReqInnerSeq());
                // ?????????
                demandNameChangeDO.setNewReqNm(demandDO.getReqNm());
                // ?????????
                demandNameChangeDO.setNewReqNo(demandDO.getReqNo());
                // ?????????
                demandNameChangeDO.setOperator(user);
                // ????????????
                demandNameChangeDO.setOperationTime(time);
                // ???????????????????????????????????????
                List<DemandNameChangeDO> nameChangeDO = iDemandNameChangeExtDao.findOne(demandNameChangeDO);
                // ??????nameChangeDO??????
                if (nameChangeDO == null || nameChangeDO.size() == 0) {
                    // ???????????????
                    demandNameChangeDO.setNewReqInnerSeq(demandBO.getReqInnerSeq());
                    // ?????????
                    demandNameChangeDO.setNewReqNm(demandBO.getReqNm());
                    // ?????????
                    demandNameChangeDO.setNewReqNo(demandBO.getReqNo());
                    // ???????????????
                    demandNameChangeDO.setOldReqInnerSeq(demandDO.getReqInnerSeq());
                    // ?????????
                    demandNameChangeDO.setOldReqNm(demandDO.getReqNm());
                    // ?????????
                    demandNameChangeDO.setOldReqNo(demandDO.getReqNo());
                    // ????????????
                    demandNameChangeDO.setUuid(UUID.randomUUID().toString());
                } else {
                    // ???????????????
                    demandNameChangeDO.setNewReqInnerSeq(demandBO.getReqInnerSeq());
                    // ?????????
                    demandNameChangeDO.setNewReqNm(demandBO.getReqNm());
                    // ?????????
                    demandNameChangeDO.setNewReqNo(demandBO.getReqNo());
                    // ???????????????
                    demandNameChangeDO.setOldReqInnerSeq(nameChangeDO.get(nameChangeDO.size() - 1).getNewReqInnerSeq());
                    // ?????????
                    demandNameChangeDO.setOldReqNm(nameChangeDO.get(nameChangeDO.size() - 1).getNewReqNm());
                    // ?????????
                    demandNameChangeDO.setOldReqNo(nameChangeDO.get(nameChangeDO.size() - 1).getNewReqNo());
                    // ????????????
                    demandNameChangeDO.setUuid(nameChangeDO.get(nameChangeDO.size() - 1).getUuid());
                }
                // ??????
                iDemandNameChangeExtDao.insert(demandNameChangeDO);
                // ?????????????????????????????????????????????????????????????????????svn????????????
                if(JudgeUtils.isNotEmpty(demandBO.getReqNm())&&JudgeUtils.isNotEmpty(demandBO.getReqNo())){
                    reqPlanService.bulidSVNProjrcts(demandBO.getReqInnerSeq(),demandBO.getReqNo(),demandBO.getReqNm());
                }

            }
            //???????????????????????????????????????
            if (demandBO.getRevisionTimeNote() != null && !demandBO.getRevisionTimeNote().isEmpty()) {
                reqPlanService.registrationTimeNodeHistoryTable(demandBO);
            }
            //?????????????????????????????????
            if (!demandBO.getPreCurPeriod().equals(demandDO.getPreCurPeriod())) {
                //???????????????????????????
                String remarks = "????????????";
                reqPlanService.registrationDemandPhaseRecordForm(demandBO, remarks);
            }
            // ???????????????int????????????????????????????????????????????????????????????????????????0????????????????????????????????????
            //????????????
            demandBO.setTotalWorkload(demandDO.getTotalWorkload());
            //?????????????????????
            demandBO.setInputWorkload(demandDO.getInputWorkload());
            //????????????
            demandBO.setLastInputWorkload(demandDO.getLastInputWorkload());
            //?????????????????????
            demandBO.setRemainWorkload(demandDO.getRemainWorkload());
            //?????????????????????
            demandBO.setMonInputWorkload(demandDO.getMonInputWorkload());

            demandDao.update(BeanUtils.copyPropertiesReturnDest(demandDO, demandBO));
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.DB_UPDATE_FAILED);
        }
        jiraOperationService.createEpic(demandBO);
    }

    private void checkReqNo(DemandBO demandBO, DemandDO demandDO) {
        //??????????????????????????????,???????????????????????????????????????????????????????????????????????????
        if (!demandBO.getReqNo().trim().equals(demandDO.getReqNo().trim())) {
            DemandDO demandDO1 = new DemandDO();
            demandDO1.setReqImplMon(demandBO.getReqImplMon());
            demandDO1.setReqNo(demandBO.getReqNo());
            List<DemandDO> demandDOS = demandDao.getReqTaskByNo(demandDO1);
            if (!demandDOS.isEmpty()) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???????????????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }
    }

    @Override
    public List<DemandBO> findAll() {
        return BeanConvertUtils.convertList(demandDao.find(new DemandDO()), DemandBO.class);
    }

    @Override
    public List<DemandBO> getReqTaskByUK(DemandBO demandBO) {
        DemandDO demandDO = new DemandDO();
        BeanUtils.copyPropertiesReturnDest(demandDO, demandBO);
        return BeanConvertUtils.convertList(demandDao.getReqTaskByUK(demandDO), DemandBO.class);
    }

    @Override
    public Boolean checkNumber(String req_no) {
        boolean bool = false;
        String[] reqNo = req_no.split("-");
        if (reqNo.length == 3) {
            if ((("REQ".equals(reqNo[0]) || "REQJIRA".equals(reqNo[0]))
                    && reqNo[1].matches("^\\d{8}$") && reqNo[2].matches("^\\d{4,5}$"))) {
                bool = true;
            }
        }
        return bool;
    }

    /**
     * ???????????????
     *
     * @param demandBO
     */
    public void setDefaultValue(DemandBO demandBO) {
        // ??????????????? =????????????*????????????/22
//        int inpoyRes = demandBO.getInputRes();
//        int devCycle = demandBO.getDevCycle();
//        Double expInput = new BigDecimal(new Double(inpoyRes) * new Double(devCycle))
//                .divide(new BigDecimal(String.valueOf(22)), 2, BigDecimal.ROUND_HALF_UP).doubleValue();
//        //Double expInput = (double) (inpoyRes * devCycle / 22);
//        demandBO.setExpInput(expInput);
    }

    /**
     * ????????????
     *
     * @param demandBO
     */
    public void setDefaultUser(DemandBO demandBO) {
        if (StringUtils.isBlank(demandBO.getCreatUser())) {
            demandBO.setCreatUser(SecurityUtils.getLoginName());
            demandBO.setCreatTime(new Date());
        }
        demandBO.setUpdateUser(SecurityUtils.getLoginName());
        demandBO.setUpdateTime(new Date());
    }

    /**
     * ??????????????????
     *
     * @param demandBO
     */
    public void setReqSts(DemandBO demandBO) {
        if (!"30".equals(demandBO.getReqSts()) && !"40".equals(demandBO.getReqSts())) {
            //??????????????????
            if ("10".equals(demandBO.getPreCurPeriod())) {
                //??????
                demandBO.setReqSts("10");
            } else if ("180".equals(demandBO.getPreCurPeriod())) {
                //??????
                demandBO.setReqSts("50");
            } else {
                //?????????
                demandBO.setReqSts("20");
            }
        }
    }

    /**
     * ????????????
     *
     * @param demandBO
     */
    public void checkReqTask(DemandBO demandBO) {
        //????????????????????????
        String reqNo = demandBO.getReqNo();
        if (StringUtils.isNotBlank(reqNo) && !checkNumber(reqNo)) {
            BusinessException.throwBusinessException(MsgEnum.ERROR_REQ_NO);
        }

        //??????????????????????????????????????????????????????
        if ("30".equals(demandBO.getReqSts()) && StringUtils.isBlank(demandBO.getMonRemark())
                || "40".equals(demandBO.getReqSts()) && StringUtils.isBlank(demandBO.getMonRemark())) {
            BusinessException.throwBusinessException(MsgEnum.NULL_REMARK);
        }

        //???????????????????????????????????????
        if (StringUtils.isNotBlank(demandBO.getDevpCoorDept())) {
            String[] devp_coor_dept = demandBO.getDevpCoorDept().split(",");
            for (int i = 0; i < devp_coor_dept.length; i++) {
                if (devp_coor_dept[i].equals(demandBO.getDevpLeadDept())) {
                    BusinessException.throwBusinessException(MsgEnum.ERROR_DEVP);
                }
            }
        }
    }


    @Override
    public String getNextInnerSeq() {
        // ????????????????????????
        DemandBO reqTask = getMaxInnerSeq();
        if (reqTask == null) {
            return "XQ00000001";
        } else {
            String maxInnerSeq = reqTask.getReqInnerSeq();
            if (StringUtils.isBlank(maxInnerSeq)) {
                return "XQ00000001";
            } else {
                int nextSeq = Integer.parseInt(maxInnerSeq.substring(2)) + 1;
                String innerSeq = StringUtils.leftPad(String.valueOf(nextSeq), 8, "0");
                return "XQ" + innerSeq;
            }
        }
    }

    @Override
    public DemandBO getMaxInnerSeq() {
        DemandDO demandDO = null;
        demandDO = demandDao.getMaxInnerSeq();
        if (demandDO != null) {
            return BeanUtils.copyPropertiesReturnDest(new DemandBO(), demandDO);
        } else {
            return null;
        }

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void doBatchImport(MultipartFile file) {
        // ?????????
        String user = userService.getFullname(SecurityUtils.getLoginName());
        // ????????????
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        File f = null;
        List<DemandDO> demandDOS = new ArrayList<>();
        try {
            //MultipartFile???file
            String originalFilename = file.getOriginalFilename();
            //???????????????
            String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            if (suffix.equals("xls")) {
                suffix = ".xls";
            } else if (suffix.equals("xlsm") || suffix.equals("xlsx")) {
                suffix = ".xlsx";
            } else {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            f = File.createTempFile("tmp", suffix);
            file.transferTo(f);
            String filepath = f.getPath();
            //excel???java???
            ReadExcelUtils excelReader = new ReadExcelUtils(filepath);
            Map<Integer, Map<Integer, Object>> map = excelReader.readExcelContent();
            for (int i = 1; i <= map.size(); i++) {
                DemandDO demandDO = new DemandDO();
                demandDO.setReqProDept(map.get(i).get(0).toString().trim());
                demandDO.setReqProposer(map.get(i).get(1).toString().trim());
                demandDO.setReqMnger(map.get(i).get(2).toString().trim());
                demandDO.setReqPrdLine(map.get(i).get(3).toString().trim());
                demandDO.setReqNm(map.get(i).get(4).toString().trim());
                //demandDO.setReqDesc(map.get(i).get(5).toString().trim());
                demandDO.setReqBackground(map.get(i).get(5).toString().trim());
                demandDO.setReqRetrofit(map.get(i).get(6).toString().trim());
                demandDO.setReqValue(map.get(i).get(7).toString().trim());
                if (!JudgeUtils.isEmpty(map.get(i).get(8).toString().trim())) {
                    demandDO.setExpInput(Double.parseDouble(map.get(i).get(8).toString().trim()));
                }
                if (!JudgeUtils.isEmpty(map.get(i).get(9).toString().trim())) {
                    demandDO.setProjectedWorkload(Double.parseDouble(map.get(i).get(9).toString().trim()));
                }
                demandDO.setIsCut(map.get(i).get(10).toString().trim());
                demandDO.setMonRemark(map.get(i).get(11).toString().trim());
                if (map.get(i).get(12) instanceof String) {
                    demandDO.setExpPrdReleaseTm(map.get(i).get(12).toString().trim());
                    demandDO.setProductionTime(map.get(i).get(12).toString().trim());
                }
                if (map.get(i).get(12) instanceof Date) {
                    Date date = (Date)map.get(i).get(12);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    demandDO.setExpPrdReleaseTm(dt.trim());
                    demandDO.setProductionTime(dt.trim());
                }
                demandDO.setPreMonPeriod(map.get(i).get(13).toString().trim());
                demandDO.setCurMonTarget(map.get(i).get(14).toString().trim());
                demandDO.setReqInnerSeq(map.get(i).get(15).toString().trim());
                demandDO.setReqNo(map.get(i).get(16).toString().trim());
                demandDO.setRiskFeedbackTm(map.get(i).get(17).toString().trim());
                demandDO.setPreCurPeriod(map.get(i).get(18).toString().trim());
                demandDO.setRiskSolution(map.get(i).get(19).toString().trim());
                demandDO.setPrdFinshTm(map.get(i).get(20).toString().trim());
                demandDO.setUatUpdateTm(map.get(i).get(21).toString().trim());
                demandDO.setDevpLeadDept(map.get(i).get(22).toString().trim());
                demandDO.setDevpCoorDept(map.get(i).get(23).toString().trim());
                demandDO.setProductMng(map.get(i).get(24).toString().trim());
                if (map.get(i).get(25) instanceof String) {
                    demandDO.setReqStartMon(map.get(i).get(25).toString().trim());
                }
                if (map.get(i).get(25) instanceof Date) {
                    Date date = (Date)map.get(i).get(25);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    demandDO.setReqStartMon(dt.trim());
                }
                if (map.get(i).get(26) instanceof String) {
                    demandDO.setReqImplMon(map.get(i).get(26).toString().trim());
                }
                if (map.get(i).get(26) instanceof Date) {
                    Date date = (Date)map.get(i).get(26);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    demandDO.setReqImplMon(dt.trim());
                }
                demandDOS.add(demandDO);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } finally {
            f.delete();
        }
        //  List<DemandDO> demandDOS = importExcel(file, 0, 1, DemandDO.class);
        List<DemandDO> insertList = new ArrayList<>();
        List<DemandDO> updateList = new ArrayList<>();
        demandDOS.forEach(m -> {
            int i = demandDOS.indexOf(m) + 2;
            if (StringUtils.isBlank(m.getReqProDept())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "??????????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqNm())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqBackground())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqRetrofit())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqValue())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "??????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqProposer())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "???????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqMnger())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "???????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqPrdLine())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqStartMon())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if (StringUtils.isBlank(m.getReqImplMon())) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }

            //????????????????????????
            String reqNo = m.getReqNo();
            if (StringUtils.isNotBlank(reqNo) && !checkNumber(reqNo)) {
                BusinessException.throwBusinessException(MsgEnum.ERROR_REQ_NO);
            }

            DictionaryDO dictionaryDO = new DictionaryDO();
            dictionaryDO.setDicId("PRD_LINE");
            dictionaryDO.setValue(m.getReqPrdLine());
            List<DictionaryDO> dic = dictionaryDao.getDicByDicId(dictionaryDO);
            if (dic.size() == 0) {
                MsgEnum.ERROR_IMPORT.setMsgInfo("???" + i + "???" + "??????????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }
            if (OLDPRDLINE.equals(dic.get(0).getRemark())) {
                MsgEnum.ERROR_IMPORT.setMsgInfo("???" + i + "???" + "??????????????????????????????????????????????????????????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }
            m.setReqPrdLine(dic.get(0).getName());

            dictionaryDO.setUserName(m.getReqProposer());
            dic = dictionaryDao.getJdInfo(dictionaryDO);
            if (dic.size() == 0) {
                MsgEnum.ERROR_IMPORT.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }

            dictionaryDO.setUserName(m.getReqMnger());
            dic = dictionaryDao.getJdInfo(dictionaryDO);
            if (dic.size() == 0) {
                MsgEnum.ERROR_IMPORT.setMsgInfo("???" + i + "???" + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }

            if (StringUtils.isNotBlank(m.getPreCurPeriod())) {
                dictionaryDO.setDicId("REQ_PEROID");
                dictionaryDO.setValue(m.getPreCurPeriod());
                dic = dictionaryDao.getDicByDicId(dictionaryDO);
                if (dic.size() == 0) {
                    MsgEnum.ERROR_IMPORT.setMsgInfo("???" + i + "???" + "??????????????????????????????");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
                }
                m.setPreCurPeriod(dic.get(0).getName());
            }


            if (StringUtils.isNotBlank(m.getPreMonPeriod())) {
                dictionaryDO.setDicId("REQ_PEROID");
                dictionaryDO.setValue(m.getPreMonPeriod());
                dic = dictionaryDao.getDicByDicId(dictionaryDO);
                if (dic.size() == 0) {
                    MsgEnum.ERROR_IMPORT.setMsgInfo("???" + i + "???" + "????????????????????????????????????");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
                }
                m.setPreMonPeriod(dic.get(0).getName());
            }
            m.setPreCurPeriod(StringUtils.isBlank(m.getPreCurPeriod()) ? m.getPreMonPeriod() : m.getPreCurPeriod());
            m.setPreMonPeriod(StringUtils.isBlank(m.getPreMonPeriod()) ? m.getPreCurPeriod() : m.getPreMonPeriod());

            if (StringUtils.isNotBlank(m.getCurMonTarget())) {
                dictionaryDO.setDicId("REQ_PEROID");
                dictionaryDO.setValue(m.getCurMonTarget());
                dic = dictionaryDao.getDicByDicId(dictionaryDO);
                if (dic.size() == 0) {
                    MsgEnum.ERROR_IMPORT.setMsgInfo("???" + i + "???" + "??????????????????????????????????????????");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
                }
                m.setCurMonTarget(dic.get(0).getName());
            }

            DemandBO tmp = new DemandBO();
            BeanUtils.copyPropertiesReturnDest(tmp, m);
            setReqSts(tmp);
            setDefaultUser(tmp);
            BeanUtils.copyPropertiesReturnDest(m, tmp);

            List<DemandDO> dem = demandDao.getReqTaskByUKImpl(m);
            if (dem.size() == 0) {
                // ?????????????????????????????????
                m.setReqType("01");
                // ????????????qa??????????????????
                m.setQaMng("?????????");
                // ????????????????????????????????????
                m.setConfigMng("?????????");
                m.setReqAbnorType("01");
                //???????????????
                //??????SVN??????
                m.setIsSvnBuild("???");
                m.setReqSts("10");
                insertList.add(m);
            } else {
                //???????????????
                m.setReqInnerSeq(dem.get(0).getReqInnerSeq());
                m.setReqStartMon("");
                updateList.add(m);
            }


        });

        try {
            // ???????????????
            insertList.forEach(m -> {
                //???????????????????????????
                String nextInnerSeq = getNextInnerSeq();
                String reqInnerSeq = m.getReqInnerSeq();
                m.setReqInnerSeq(nextInnerSeq);
                demandDao.insert(m);
                // ?????????????????????????????????????????? ???????????????????????????????????????????????????
                DemandNameChangeDO demandNameChangeDO = new DemandNameChangeDO();
                // ???????????????
                demandNameChangeDO.setNewReqInnerSeq(m.getReqInnerSeq());
                // ?????????
                demandNameChangeDO.setNewReqNm(m.getReqNm());
                // ?????????
                demandNameChangeDO.setNewReqNo(m.getReqNo());
                // ?????????
                demandNameChangeDO.setOperator(user);
                // ????????????
                demandNameChangeDO.setOperationTime(time);
                // ????????????
                demandNameChangeDO.setUuid(UUID.randomUUID().toString());
                // ????????????
                iDemandNameChangeExtDao.insert(demandNameChangeDO);

                if(JudgeUtils.isNotEmpty(m.getReqNm())&&JudgeUtils.isNotEmpty(m.getReqNo())){
                    reqPlanService.bulidSVNProjrcts(m.getReqInnerSeq(),m.getReqNo(),m.getReqNm());
                }

                // ???????????????????????????
                DemandChangeDetailsDO demandChangeDetailsDO = new DemandChangeDetailsDO();
                demandChangeDetailsDO.setReqInnerSeq(m.getReqInnerSeq());
                demandChangeDetailsDO.setReqNo(m.getReqNo());
                demandChangeDetailsDO.setReqNm(m.getReqNm());
                demandChangeDetailsDO.setCreatUser(user);
                demandChangeDetailsDO.setCreatTime(LocalDateTime.now());
                demandChangeDetailsDO.setIdentification(nextInnerSeq);
                demandChangeDetailsDO.setReqImplMon(m.getReqImplMon());
                demandChangeDetailsDao.insert(demandChangeDetailsDO);
                DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
                demandStateHistoryDO.setReqInnerSeq(nextInnerSeq);
                demandStateHistoryDO.setReqSts("??????");
                demandStateHistoryDO.setRemarks("????????????");
                demandStateHistoryDO.setReqNm(m.getReqNm());
                //???????????????????????????????????????
                String identificationByReqInnerSeq = demandChangeDetailsDao.getIdentificationByReqInnerSeq(reqInnerSeq);
                if (identificationByReqInnerSeq == null) {
                    identificationByReqInnerSeq = reqInnerSeq;
                }
                demandStateHistoryDO.setIdentification(identificationByReqInnerSeq);
                //???????????????????????????
                //?????????????????????
                demandStateHistoryDO.setCreatUser(user);
                demandStateHistoryDO.setCreatTime(LocalDateTime.now());
                demandStateHistoryDao.insert(demandStateHistoryDO);
            });

            // ???????????????
            updateList.forEach(m -> {
                DemandDO demandDO = demandDao.get(m.getReqInnerSeq());
                // ???????????????int????????????????????????????????????????????????????????????????????????0????????????????????????????????????
                //????????????
                m.setTotalWorkload(demandDO.getTotalWorkload());
                //?????????????????????
                m.setInputWorkload(demandDO.getInputWorkload());
                //????????????
                m.setLastInputWorkload(demandDO.getLastInputWorkload());
                //?????????????????????
                m.setRemainWorkload(demandDO.getRemainWorkload());
                //?????????????????????
                m.setMonInputWorkload(demandDO.getMonInputWorkload());
                //????????????????????????  ????????????setInputRes  ????????????setDevCycle  ??????setExpInput
                m.setInputRes(demandDO.getInputRes());
                m.setDevCycle(demandDO.getDevCycle());
                m.setExpInput(demandDO.getExpInput());
                // ????????????????????????????????????????????????????????????????????????????????????????????????????????????
                String oldReqNo = demandDO.getReqNo();
                String oldReqNm = demandDO.getReqNm();
                String newReqNo = m.getReqNo();
                String newReqNm = m.getReqNm();
                if (newReqNo == null) {
                    newReqNm = "";
                }
                if (newReqNm == null) {
                    newReqNm = "";
                }
                if (oldReqNo == null) {
                    oldReqNo = "";
                }
                if (oldReqNm == null) {
                    oldReqNm = "";
                }
                if (!oldReqNo.equals(newReqNo) || !oldReqNm.equals(newReqNm)) {
                    DemandNameChangeDO demandNameChangeDO = new DemandNameChangeDO();
                    // ???????????????
                    demandNameChangeDO.setNewReqInnerSeq(demandDO.getReqInnerSeq());
                    // ?????????
                    demandNameChangeDO.setNewReqNm(demandDO.getReqNm());
                    // ?????????
                    demandNameChangeDO.setNewReqNo(demandDO.getReqNo());
                    // ?????????
                    demandNameChangeDO.setOperator(user);
                    // ????????????
                    demandNameChangeDO.setOperationTime(time);
                    // ???????????????????????????????????????
                    List<DemandNameChangeDO> nameChangeDO = iDemandNameChangeExtDao.findOne(demandNameChangeDO);
                    // ??????nameChangeDO??????
                    if (JudgeUtils.isEmpty(nameChangeDO)) {
                        // ???????????????
                        demandNameChangeDO.setNewReqInnerSeq(m.getReqInnerSeq());
                        // ?????????
                        demandNameChangeDO.setNewReqNm(m.getReqNm());
                        // ?????????
                        demandNameChangeDO.setNewReqNo(m.getReqNo());
                        // ???????????????
                        demandNameChangeDO.setOldReqInnerSeq(demandDO.getReqInnerSeq());
                        // ?????????
                        demandNameChangeDO.setOldReqNm(demandDO.getReqNm());
                        // ?????????
                        demandNameChangeDO.setOldReqNo(demandDO.getReqNo());
                        // ????????????
                        demandNameChangeDO.setUuid(UUID.randomUUID().toString());
                    } else {
                        // ???????????????
                        demandNameChangeDO.setNewReqInnerSeq(m.getReqInnerSeq());
                        // ?????????
                        demandNameChangeDO.setNewReqNm(m.getReqNm());
                        // ?????????
                        demandNameChangeDO.setNewReqNo(m.getReqNo());
                        // ???????????????
                        demandNameChangeDO.setOldReqInnerSeq(nameChangeDO.get(nameChangeDO.size() - 1).getNewReqInnerSeq());
                        // ?????????
                        demandNameChangeDO.setOldReqNm(nameChangeDO.get(nameChangeDO.size() - 1).getNewReqNm());
                        // ?????????
                        demandNameChangeDO.setOldReqNo(nameChangeDO.get(nameChangeDO.size() - 1).getNewReqNo());
                        // ????????????
                        demandNameChangeDO.setUuid(nameChangeDO.get(nameChangeDO.size() - 1).getUuid());
                    }
                    // ??????
                    iDemandNameChangeExtDao.insert(demandNameChangeDO);
                    if(JudgeUtils.isNotEmpty(m.getReqNm())&&JudgeUtils.isNotEmpty(m.getReqNo())){
                        reqPlanService.bulidSVNProjrcts(m.getReqInnerSeq(),m.getReqNo(),m.getReqNm());
                    }
                }
                demandDao.update(m);
            });

        } catch (Exception e) {
            LOGGER.error("????????????????????????", e);
            BusinessException.throwBusinessException(MsgEnum.DB_INSERT_FAILED);
        }

        jiraOperationService.batchCreateEpic(demandDOS);

    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void doBatchDown(MultipartHttpServletRequest request, HttpServletResponse response) {
        MultipartFile file = request.getFile(FILE);
        //response.resetBuffer();
        response.reset();
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            Map<String, Object> resMap = BatchDown(file);

            File srcfile[] = (File[]) resMap.get("srcfile");
            //???????????????
            String zipPath = "";
            if (LemonUtils.getEnv().equals(Env.SIT)) {
                zipPath = "/home/devms/temp/propkg/";
            } else if (LemonUtils.getEnv().equals(Env.DEV)) {
                zipPath = "/home/devadm/temp/propkg/";
            } else {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            String zipName = DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".zip";
            //????????????
            File zip = new File(zipPath + zipName);
            ZipFiles(srcfile, zip, true);
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + zipName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            //???????????????????????????????????????
            //?????? * ??????????????????cookie?????????,Origin ??????????????????
            //resp.addHeader("Access-Control-Allow-Origin", "*");
            //????????????????????????Origin????????????????????????
            String origin = request.getHeader("Origin");
            if (StringUtils.isNotBlank(origin)) {
                response.addHeader("Access-Control-Allow-Origin", origin);
            }
            //????????????cookie??????
            response.addHeader("Access-Control-Allow-Credentials", "true");
            //??????????????????????????????????????????
            response.addHeader("Access-Control-Allow-Methods", "*");
            //???????????????????????????Content-Type,header1,header2??????????????????
            //resp.addHeader("Access-Control-Allow-Headers", "Content-Type,header1,header2");
            //???????????????????????????????????????
            String headers = request.getHeader("Access-Control-Request-Headers");
            if (StringUtils.isNotBlank(headers)) {
                response.addHeader("Access-Control-Allow-Headers", headers);
            }
            //?????????????????????OPTIONS????????????1??????,?????????????????????????????????????????????,????????????
            response.addHeader("Access-Control-Max-Age", "3600");
            response.setContentType("application/octet-stream; charset=utf-8");
            int len = 0 ;
            byte[] buffer = new byte[1024];
            FileInputStream fis = new FileInputStream(zip);
            while ((len = fis.read(buffer)) > 0) {
                output.write(buffer, 0, len);
                output.flush();
            }
           // output.write(org.apache.commons.io.FileUtils.readFileToByteArray(zip));
            bufferedOutPut.flush();

            // ????????????
            zip.delete();
        } catch (Exception e) {
            LOGGER.error("???????????????", e);
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    private Map<String, Object> BatchDown(MultipartFile file) {
        List<DemandDO> demandDOS = importExcel(file, 0, 1, DemandDO.class);
        List<DemandDO> List = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        demandDOS.forEach(m -> {
            if (StringUtils.isBlank(m.getReqNo())) {
                MsgEnum.ERROR_IMPORT.setMsgInfo(MsgEnum.ERROR_IMPORT.getMsgInfo() + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }
            if (StringUtils.isBlank(m.getReqNm())) {
                MsgEnum.ERROR_IMPORT.setMsgInfo(MsgEnum.ERROR_IMPORT.getMsgInfo() + "????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }

            //????????????????????????
            String reqNo = m.getReqNo();
            if (StringUtils.isNotBlank(reqNo) && !checkNumber(reqNo)) {
                BusinessException.throwBusinessException(MsgEnum.ERROR_REQ_NO);
            }

            int start = m.getReqNo().indexOf("-") + 1;
            String reqMonth = m.getReqNo().substring(start, start + 6);
            m.setReqStartMon(reqMonth);
            List.add(m);
        });

        //??????????????????
        if (List != null) {
            File srcfile[] = new File[List.size() * 5];
            int num = 0;
            //??????????????????
            for (int i = 0; i < List.size(); i++) {
                //?????????????????????????????????????????????????????????
                String path = "";
                if (LemonUtils.getEnv().equals(Env.SIT)) {
                    path = "/home/devms/temp/Projectdoc/" + List.get(i).getReqStartMon() + "/"
                            //  String path = "D:\\home\\devadm\\temp\\Projectdoc" + List.get(i).getReqStartMon() + "/"
                            + List.get(i).getReqNo() + "_" + List.get(i).getReqNm();
                } else if (LemonUtils.getEnv().equals(Env.DEV)) {
                    path = "/home/devadm/temp/Projectdoc/" + List.get(i).getReqStartMon() + "/"
                            //  String path = "D:\\home\\devadm\\temp\\Projectdoc" + List.get(i).getReqStartMon() + "/"
                            + List.get(i).getReqNo() + "_" + List.get(i).getReqNm();
                } else {
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????!");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
                File file1 = new File(path + "/??????????????????/");
                if (!file1.exists() && !file1.isDirectory()) {
                    file1.mkdir();
                }
                File[] tempFile1 = file1.listFiles();
                if (tempFile1 == null) {
                    tempFile1 = new File[0];
                }
                for (int j = 0; j < tempFile1.length; j++) {
                    if (tempFile1[j].getName().contains("????????????????????????(????????????)") || tempFile1[j].getName().contains("?????????????????????")) {
                        srcfile[num] = new File(path + "/??????????????????/" + tempFile1[j].getName());
                        num++;
                    }
                }

                File file2 = new File(path + "/????????????/");
                if (!file2.exists() && !file2.isDirectory()) {
                    file2.mkdir();
                }
                File[] tempFile2 = file2.listFiles();
                if (tempFile2 == null) {
                    tempFile2 = new File[0];
                }
                for (int j = 0; j < tempFile2.length; j++) {
                    if (tempFile2[j].getName().contains("?????????????????????")) {
                        srcfile[num] = new File(path + "/????????????/" + tempFile2[j].getName());
                        num++;
                    }
                }

            }
            map.put("srcfile", srcfile);
        }

        return map;
    }

    /**
     * ??????????????
     */
    @Override
    public String ZipFiles(File[] srcfile, File zipfile, boolean flag) {
        try {
            byte[] buf = new byte[1024];
            FileOutputStream fileOutputStream = new FileOutputStream(zipfile);
            CheckedOutputStream cos = new CheckedOutputStream(fileOutputStream, new CRC32());
            ZipOutputStream out = new ZipOutputStream(cos);

            for (int i = 0; i < srcfile.length; i++) {
                if (srcfile[i] != null) {
                    FileInputStream in = new FileInputStream(srcfile[i]);
                    if (flag) {
                        String demandName = "";
                        if (LemonUtils.getEnv().equals(Env.SIT)) {
                            demandName = srcfile[i].getPath().substring(35, srcfile[i].getPath().length());
                        } else if (LemonUtils.getEnv().equals(Env.DEV)) {
                            demandName = srcfile[i].getPath().substring(36, srcfile[i].getPath().length());
                        } else {
                            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????!");
                            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                        }
                        String name = demandName.substring(0, demandName.indexOf("/"));
                        String path = demandName.substring(demandName.lastIndexOf("/") + 1);
                        out.putNextEntry(new ZipEntry(name + "/" + path));
                    } else {
                        out.putNextEntry(new ZipEntry(srcfile[i].getPath()));
                    }

                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    out.closeEntry();
                    in.close();
                }
            }
            out.close();
        } catch (IOException e) {
            LOGGER.error("???????????????", e);
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }

        return "";
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void updateReqSts(String reqInnerSeq, String reqNo, String reqSts, String reqStsRemarks, String reqNm,String stateCauseClassification) {
        if (!permissionCheck(reqInnerSeq)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????????????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (JudgeUtils.isEmpty(reqInnerSeq) || JudgeUtils.isEmpty(reqSts)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        DemandDO demandDO1 = demandDao.get(reqInnerSeq);
        if (demandDO1.getReqSts().equals("50")) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (reqSts.equals("50")) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (demandDO1.getReqSts().equals(reqSts)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        demandDO1.getReqSts();


        DemandDO demandDO = new DemandDO();
        demandDO.setReqInnerSeq(reqInnerSeq);
        demandDO.setReqSts(reqSts);
        demandDO.setStateCauseClassification(stateCauseClassification);
        demandDao.updateReqSts(demandDO);
        DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
        demandStateHistoryDO.setReqNm(reqNm);
        demandStateHistoryDO.setReqInnerSeq(reqInnerSeq);
        demandStateHistoryDO.setRemarks(reqStsRemarks);
        demandStateHistoryDO.setOldReqSts(reqStsCheck(demandDO1.getReqSts()));
        reqSts = reqStsCheck(reqSts);
        demandStateHistoryDO.setReqSts(reqSts);
        demandStateHistoryDO.setReqNo(reqNo);
        demandStateHistoryDO.setCreatTime(LocalDateTime.now());
        //??????????????????????????????????????????????????????
        String identificationByReqInnerSeq = demandChangeDetailsDao.getIdentificationByReqInnerSeq(reqInnerSeq);
        if (identificationByReqInnerSeq == null) {
            identificationByReqInnerSeq = reqInnerSeq;
        }
        demandStateHistoryDO.setIdentification(identificationByReqInnerSeq);
        //?????????????????????
        demandStateHistoryDO.setCreatUser(userService.getFullname(SecurityUtils.getLoginName()));
        demandStateHistoryDO.setStateCauseClassification(stateCauseClassification);
        demandStateHistoryDao.insert(demandStateHistoryDO);
    }


    @Override
    public String reqStsCheck(String reqSts) {
        switch (reqSts) {
            case "10": {
                reqSts = "??????";
                break;
            }
            case "20": {
                reqSts = "?????????";
                break;
            }
            case "30": {
                reqSts = "??????";
                break;
            }
            case "40": {
                reqSts = "??????";
                break;
            }
            case "50": {
                reqSts = "?????????";
                break;
            }
            default: {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("???????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }
        return reqSts;
    }

    @Override
    public void updatePreCurPeriod(DemandBO demand) {
        DemandDO demandDO = BeanUtils.copyPropertiesReturnDest(new DemandDO(), demand);
        demandDao.updatePreCurPeriod(demandDO);
    }

    //??????????????????????????????????????????????????????
    public boolean permissionCheck(String reqInnerSeq) {
        //??????????????????????????????????????????
        UserRoleDO userRoleDO = new UserRoleDO();
        userRoleDO.setRoleId(SUPERADMINISTRATOR);
        userRoleDO.setUserNo(Long.parseLong(SecurityUtils.getLoginUserId()));
        List<UserRoleDO> userRoleDOS = new LinkedList<>();
        userRoleDOS = userRoleExtDao.find(userRoleDO);
        if (!userRoleDOS.isEmpty()) {
            return true;
        }
        //??????????????????????????????????????????
        userRoleDO.setRoleId(SUPERADMINISTRATOR2);
        userRoleDO.setUserNo(Long.parseLong(SecurityUtils.getLoginUserId()));
        userRoleDOS = userRoleExtDao.find(userRoleDO);
        if (!userRoleDOS.isEmpty()) {
            return true;
        }


        //?????????????????????
        PermiUserDO permiUserDO = new PermiUserDO();
        String loginName = SecurityUtils.getLoginName();
        permiUserDO.setUserId(loginName);
        List<PermiUserDO> permiUserDOS = permiUserDao.find(permiUserDO);
        if (permiUserDOS.isEmpty()) {
            return false;
        }
        String userName = permiUserDOS.get(0).getUserName();
        //???????????????????????????????????????
        DemandDO demandDO = demandDao.get(reqInnerSeq);
        //?????????????????????????????????????????????????????????
        String productMng = demandDO.getProductMng();
        String devpLeadDept = demandDO.getDevpLeadDept();
        TPermiDeptDO permiDeptDO = new TPermiDeptDO();
        //????????????????????????????????????????????????????????????
        OrganizationStructureDO organizationStructureDO = new OrganizationStructureDO();
        organizationStructureDO.setSecondlevelorganization(devpLeadDept);
        organizationStructureDO.setFirstlevelorganizationleader(userName);
        List<OrganizationStructureDO> organizationStructureDOList =  iOrganizationStructureDao.find(organizationStructureDO);
        if (organizationStructureDOList!=null && organizationStructureDOList.size()>0){
            //???????????????????????????????????????????????????????????????
            return true;
        }

        //????????????????????????????????????????????????
        if (!(StringUtils.isBlank(devpLeadDept) && (StringUtils.isBlank(productMng)))) {
            //?????????????????????????????????,??????????????????????????????????????????
            if (!StringUtils.isBlank(productMng) && (StringUtils.isBlank(devpLeadDept))) {


                if (productMng.equals(userName)) {
                    return true;
                } else {
                    return false;
                }
            }
            //?????????????????????????????????,?????????????????????????????????????????????????????? devpLeadDept
            if (!StringUtils.isBlank(devpLeadDept) && (StringUtils.isBlank(productMng))) {
                permiDeptDO.setDeptName(devpLeadDept);

                //???????????????????????????????????????????????????
                List<TPermiDeptDO> tPermiDeptDOS = permiDeptDao.find(permiDeptDO);
                String deptManagerName = tPermiDeptDOS.get(0).getDeptManagerName();
                if (deptManagerName.equals(userName)) {
                    return true;
                } else {
                    return false;
                }
            }
            //??????????????????????????????,?????????????????????????????????????????????????????? ???????????????
            if (!StringUtils.isBlank(productMng) && (!StringUtils.isBlank(devpLeadDept))) {
                permiDeptDO.setDeptName(devpLeadDept);
                //???????????????????????????????????????????????????
                List<TPermiDeptDO> tPermiDeptDOS = permiDeptDao.find(permiDeptDO);
                String deptManagerName = tPermiDeptDOS.get(0).getDeptManagerName();
                if (deptManagerName.equals(userName) || productMng.equals(userName)) {
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
        return false;
    }

    @Override
    public List<String> lists(DemandBO demand) {
        DemandDO demandDO = BeanUtils.copyPropertiesReturnDest(new DemandDO(), demand);
        Map<String, Object> resMap = BatLists(demandDO);
        File srcfile[] = (File[]) resMap.get("srcfile");
        String list[] = new String[srcfile.length];
        List<String> li = new ArrayList<>();
        for (int i = 0; i < srcfile.length; i++) {
            if (srcfile[i] != null) {
                System.err.println(srcfile[i].getName());
                li.add(srcfile[i].getName());
            }
        }
        if (li.size() == 0) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("???????????????,???????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        return li;
    }

    @Override
    public DemandStateHistoryRspBO findDemandChangeDetails(DemandChangeDetailsBO demandChangeDetailsBO) {
        if (!demandChangeDetailsBO.getReqInnerSeq().isEmpty() && !demandChangeDetailsBO.getReqNo().isEmpty()) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (!demandChangeDetailsBO.getReqInnerSeq().isEmpty() && demandChangeDetailsBO.getReqNo().isEmpty()) {
            String identification = demandChangeDetailsDao.getIdentificationByReqInnerSeq(demandChangeDetailsBO.getReqInnerSeq());
            if (identification == null) {
                DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
                demandStateHistoryDO.setIdentification(demandChangeDetailsBO.getReqInnerSeq());
                PageInfo<DemandStateHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                        () -> BeanConvertUtils.convertList(demandStateHistoryDao.find(demandStateHistoryDO), DemandStateHistoryBO.class));
                List<DemandStateHistoryBO> demandStateHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandStateHistoryBO.class);
                DemandStateHistoryRspBO demandStateHistoryRspBO = new DemandStateHistoryRspBO();
                demandStateHistoryRspBO.setDemandStateHistoryBOList(demandStateHistoryBOList);
                demandStateHistoryRspBO.setPageInfo(pageInfo);
                return demandStateHistoryRspBO;
            }
            DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
            demandStateHistoryDO.setIdentification(identification);
            PageInfo<DemandStateHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                    () -> BeanConvertUtils.convertList(demandStateHistoryDao.find(demandStateHistoryDO), DemandStateHistoryBO.class));
            List<DemandStateHistoryBO> demandStateHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandStateHistoryBO.class);
            DemandStateHistoryRspBO demandStateHistoryRspBO = new DemandStateHistoryRspBO();
            demandStateHistoryRspBO.setDemandStateHistoryBOList(demandStateHistoryBOList);
            demandStateHistoryRspBO.setPageInfo(pageInfo);
            return demandStateHistoryRspBO;
        } else if (demandChangeDetailsBO.getReqInnerSeq().isEmpty() && !demandChangeDetailsBO.getReqNo().isEmpty()) {
            DemandStateHistoryDO demandStateHistoryDO1 = new DemandStateHistoryDO();
            demandStateHistoryDO1.setReqNo(demandChangeDetailsBO.getReqNo());
            List<DemandStateHistoryDO> demandStateHistoryDOS = demandStateHistoryDao.find(demandStateHistoryDO1);
            if (JudgeUtils.isEmpty(demandStateHistoryDOS)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????????????????(??????????????????????????????????????????)");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            DemandStateHistoryDO demandStateHistoryDO2 = new DemandStateHistoryDO();
            demandStateHistoryDO2.setReqInnerSeq(demandStateHistoryDOS.get(0).getReqInnerSeq());
            List<DemandStateHistoryDO> list = demandStateHistoryDao.find(demandStateHistoryDO2);
            String identification = list.get(0).getIdentification();
            if (JudgeUtils.isBlank(identification)) {
                identification = demandStateHistoryDOS.get(0).getReqInnerSeq();
            }
            DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
            demandStateHistoryDO.setIdentification(identification);
            PageInfo<DemandStateHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                    () -> BeanConvertUtils.convertList(demandStateHistoryDao.find(demandStateHistoryDO), DemandStateHistoryBO.class));
            List<DemandStateHistoryBO> demandStateHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandStateHistoryBO.class);
            DemandStateHistoryRspBO demandStateHistoryRspBO = new DemandStateHistoryRspBO();
            demandStateHistoryRspBO.setDemandStateHistoryBOList(demandStateHistoryBOList);
            demandStateHistoryRspBO.setPageInfo(pageInfo);
            return demandStateHistoryRspBO;
        } else {
            //???????????? ????????????
            DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
            PageInfo<DemandStateHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                    () -> BeanConvertUtils.convertList(demandStateHistoryDao.find(demandStateHistoryDO), DemandStateHistoryBO.class));
            List<DemandStateHistoryBO> demandStateHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandStateHistoryBO.class);
            DemandStateHistoryRspBO demandStateHistoryRspBO = new DemandStateHistoryRspBO();
            demandStateHistoryRspBO.setDemandStateHistoryBOList(demandStateHistoryBOList);
            demandStateHistoryRspBO.setPageInfo(pageInfo);
            return demandStateHistoryRspBO;
        }
    }

    @Override
    public DemandCurperiodHistoryRspBO findDemandCurperiodDetails(DemandChangeDetailsBO demandChangeDetailsBO) {
        if (!demandChangeDetailsBO.getReqInnerSeq().isEmpty() && !demandChangeDetailsBO.getReqNo().isEmpty()) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (!demandChangeDetailsBO.getReqInnerSeq().isEmpty() && demandChangeDetailsBO.getReqNo().isEmpty()) {
            String identification = demandChangeDetailsDao.getIdentificationByReqInnerSeq(demandChangeDetailsBO.getReqInnerSeq());
            if (identification == null) {
                DemandCurperiodHistoryDO demandCurperiodHistoryDO = new DemandCurperiodHistoryDO();
                demandCurperiodHistoryDO.setIdentification(demandChangeDetailsBO.getReqInnerSeq());
                PageInfo<DemandCurperiodHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                        () -> BeanConvertUtils.convertList(demandCurperiodHistoryDao.find(demandCurperiodHistoryDO), DemandCurperiodHistoryBO.class));
                List<DemandCurperiodHistoryBO> demandCurperiodHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandCurperiodHistoryBO.class);
                DemandCurperiodHistoryRspBO demandCurperiodHistoryRspBO = new DemandCurperiodHistoryRspBO();
                demandCurperiodHistoryRspBO.setDemandCurperiodHistoryBOList(demandCurperiodHistoryBOList);
                demandCurperiodHistoryRspBO.setPageInfo(pageInfo);
                return demandCurperiodHistoryRspBO;
            }
            DemandCurperiodHistoryDO demandCurperiodHistoryDO = new DemandCurperiodHistoryDO();
            demandCurperiodHistoryDO.setIdentification(identification);
            PageInfo<DemandCurperiodHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                    () -> BeanConvertUtils.convertList(demandCurperiodHistoryDao.find(demandCurperiodHistoryDO), DemandCurperiodHistoryBO.class));
            List<DemandCurperiodHistoryBO> demandCurperiodHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandCurperiodHistoryBO.class);
            DemandCurperiodHistoryRspBO demandCurperiodHistoryRspBO = new DemandCurperiodHistoryRspBO();
            demandCurperiodHistoryRspBO.setDemandCurperiodHistoryBOList(demandCurperiodHistoryBOList);
            demandCurperiodHistoryRspBO.setPageInfo(pageInfo);
            return demandCurperiodHistoryRspBO;
        } else if (demandChangeDetailsBO.getReqInnerSeq().isEmpty() && !demandChangeDetailsBO.getReqNo().isEmpty()) {
            DemandChangeDetailsDO demandChangeDetailsDO = new DemandChangeDetailsDO();
            demandChangeDetailsDO.setReqNo(demandChangeDetailsBO.getReqNo());
            List<DemandChangeDetailsDO> demandChangeDetailsDOS = null;
            demandChangeDetailsDOS = demandChangeDetailsDao.find(demandChangeDetailsDO);
            if (JudgeUtils.isEmpty(demandChangeDetailsDOS)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????????????????(??????????????????????????????????????????)");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            String identification = demandChangeDetailsDao.getIdentificationByReqInnerSeq(demandChangeDetailsDOS.get(0).getReqInnerSeq());
            DemandCurperiodHistoryDO demandCurperiodHistoryDO = new DemandCurperiodHistoryDO();
            demandCurperiodHistoryDO.setIdentification(identification);
            PageInfo<DemandCurperiodHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                    () -> BeanConvertUtils.convertList(demandCurperiodHistoryDao.find(demandCurperiodHistoryDO), DemandCurperiodHistoryBO.class));
            List<DemandCurperiodHistoryBO> demandCurperiodHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandCurperiodHistoryBO.class);
            DemandCurperiodHistoryRspBO demandCurperiodHistoryRspBO = new DemandCurperiodHistoryRspBO();
            demandCurperiodHistoryRspBO.setDemandCurperiodHistoryBOList(demandCurperiodHistoryBOList);
            demandCurperiodHistoryRspBO.setPageInfo(pageInfo);
            return demandCurperiodHistoryRspBO;
        } else {
            //???????????? ????????????
            DemandCurperiodHistoryDO demandCurperiodHistoryDO = new DemandCurperiodHistoryDO();
            PageInfo<DemandCurperiodHistoryBO> pageInfo = PageUtils.pageQueryWithCount(demandChangeDetailsBO.getPageNum(), demandChangeDetailsBO.getPageSize(),
                    () -> BeanConvertUtils.convertList(demandCurperiodHistoryDao.find(demandCurperiodHistoryDO), DemandCurperiodHistoryBO.class));
            List<DemandCurperiodHistoryBO> demandCurperiodHistoryBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandCurperiodHistoryBO.class);
            DemandCurperiodHistoryRspBO demandCurperiodHistoryRspBO = new DemandCurperiodHistoryRspBO();
            demandCurperiodHistoryRspBO.setDemandCurperiodHistoryBOList(demandCurperiodHistoryBOList);
            demandCurperiodHistoryRspBO.setPageInfo(pageInfo);
            return demandCurperiodHistoryRspBO;
        }
    }

    //???????????????????????????
    private Map<String, Object> BatLists(DemandDO demandDO) {
        System.err.println(demandDO);
        List<DemandDO> List = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        int start = demandDO.getReqNo().indexOf("-") + 1;
        String reqMonth = demandDO.getReqNo().substring(start, start + 6);
        demandDO.setReqStartMon(reqMonth);
        List.add(demandDO);
        //??????????????????
        if (List != null) {
            File srcfile[] = new File[List.size() * 50];
            int num = 0;
            //??????????????????
            for (int i = 0; i < List.size(); i++) {
                //?????????????????????????????????????????????????????????
                String path = "";
                if (LemonUtils.getEnv().equals(Env.SIT)) {
                    path = "/home/devms/temp/Projectdoc/" + List.get(i).getReqStartMon() + "/"
                            + List.get(i).getReqNo() + "_" + List.get(i).getReqNm();
                } else if (LemonUtils.getEnv().equals(Env.DEV)) {
                    path = "/home/devadm/temp/Projectdoc/" + List.get(i).getReqStartMon() + "/"
                            + List.get(i).getReqNo() + "_" + List.get(i).getReqNm();
                } else {
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????!");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
                File file1 = new File(path + "/??????????????????/");
                if (!file1.exists() && !file1.isDirectory()) {
                    file1.mkdir();
                }
                File[] tempFile1 = file1.listFiles();
                if (tempFile1 == null) {
                    tempFile1 = new File[0];
                }
                for (int j = 0; j < tempFile1.length; j++) {
                    srcfile[num] = new File(path + "/??????????????????/" + tempFile1[j].getName());
                    num++;

                }

                File file2 = new File(path + "/????????????/");
                if (!file2.exists() && !file2.isDirectory()) {
                    file2.mkdir();
                }
                File[] tempFile2 = file2.listFiles();
                if (tempFile2 == null) {
                    tempFile2 = new File[0];
                }
                for (int j = 0; j < tempFile2.length; j++) {
                    srcfile[num] = new File(path + "/????????????/" + tempFile2[j].getName());
                    num++;
                }

                File file3 = new File(path + "/????????????/");
                if (!file3.exists() && !file3.isDirectory()) {
                    file3.mkdir();
                }
                File[] tempFile3 = file3.listFiles();
                if (tempFile3 == null) {
                    tempFile3 = new File[0];
                }
                for (int j = 0; j < tempFile3.length; j++) {
                    srcfile[num] = new File(path + "/????????????/" + tempFile3[j].getName());
                    num++;
                }

                File file4 = new File(path + "/????????????/");
                if (!file4.exists() && !file4.isDirectory()) {
                    file4.mkdir();
                }
                File[] tempFile4 = file4.listFiles();
                if (tempFile4 == null) {
                    tempFile4 = new File[0];
                }
                for (int j = 0; j < tempFile4.length; j++) {
                    srcfile[num] = new File(path + "/????????????/" + tempFile4[j].getName());
                    num++;
                }

                File file5 = new File(path + "/??????????????????/");
                if (!file5.exists() && !file5.isDirectory()) {
                    file5.mkdir();
                }
                File[] tempFile5 = file5.listFiles();
                if (tempFile5 == null) {
                    tempFile5 = new File[0];
                }
                for (int j = 0; j < tempFile5.length; j++) {
                    srcfile[num] = new File(path + "/??????????????????/" + tempFile5[j].getName());
                    num++;
                }

                File file6 = new File(path + "/?????????????????????/");
                if (!file6.exists() && !file6.isDirectory()) {
                    file6.mkdir();
                }
                File[] tempFile6 = file6.listFiles();
                if (tempFile6 == null) {
                    tempFile6 = new File[0];
                }
                for (int j = 0; j < tempFile6.length; j++) {
                    srcfile[num] = new File(path + "/?????????????????????/" + tempFile6[j].getName());
                    num++;
                }


            }
            map.put("srcfile", srcfile);
        }

        return map;
    }

    @Override
    public List<DemandBO> getPrdFnishAbnor(String month) {
        return BeanConvertUtils.convertList(demandDao.getPrdFnishAbnor(month), DemandBO.class);
    }

    @Override
    public List<DemandBO> getTestFnishAbnor(String month) {
        return BeanConvertUtils.convertList(demandDao.getTestFnishAbnor(month), DemandBO.class);
    }

    @Override
    public List<DemandBO> getUatUpdateAbnor(String month) {
        return BeanConvertUtils.convertList(demandDao.getUatUpdateAbnor(month), DemandBO.class);
    }

    @Override
    public void updateReqAbnorType(DemandBO reqTask) {
        DemandDO demandDO = new DemandDO();
        BeanConvertUtils.convert(demandDO, reqTask);
        demandDao.updateReqAbnorType(demandDO);
    }

    @Override
    public List<DemandBO> getPrdFnishWarn() {
        return BeanConvertUtils.convertList(demandDao.getPrdFnishWarn(), DemandBO.class);
    }

    @Override
    public List<DemandBO> getUatUpdateWarn() {
        return BeanConvertUtils.convertList(demandDao.getUatUpdateWarn(), DemandBO.class);
    }

    @Override
    public List<DemandBO> getTestFnishWarn() {
        return BeanConvertUtils.convertList(demandDao.getTestFnishWarn(), DemandBO.class);
    }

    @Override
    public void WeedAndMonthFeedback(DemandBO reqTask) {
        //???????????????
        DemandDO demandDO = demandDao.get(reqTask.getReqInnerSeq());
        //?????????????????????????????????
        if (!reqTask.getPreCurPeriod().equals(demandDO.getPreCurPeriod())) {
            //???????????????????????????
            String remarks = "????????????";
            reqPlanService.registrationDemandPhaseRecordForm(reqTask, remarks);
        }
        DemandDO aDo = new DemandDO();
        BeanConvertUtils.convert(aDo, reqTask);
        demandDao.WeedAndMonthFeedback(aDo);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void approvalProcess(MultipartFile file, String ids) {
        // ?????????
        String user = userService.getFullname(SecurityUtils.getLoginName());
        // ????????????
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String[] idsList = ids.split("~");
        if (file.isEmpty()) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }

        //????????????????????????
        String path = "";
        String wwwpath = "";
        if (LemonUtils.getEnv().equals(Env.SIT)) {
            path = "/home/devms/temp/approval/process/";
            wwwpath = "/home/devms/wwwroot/home/devms/temp/approval/process/";
        } else if (LemonUtils.getEnv().equals(Env.DEV)) {
            path = "/home/devadm/temp/approval/process/";
            wwwpath = "/home/devadm/wwwroot/home/devadm/temp/approval/process/";
        } else {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        for (int i = 0; i < idsList.length; i++) {
            String reqInnerSeq = idsList[i];
            // ??????????????????
            DemandDO demandDO = demandDao.get(reqInnerSeq);
            // ????????????
            String picLocal = path + reqInnerSeq;
            File fileDir = new File(wwwpath + reqInnerSeq);
            File filePath = new File(fileDir.getPath() + "/" + file.getOriginalFilename());
            if (fileDir.exists()) {
                File[] oldFile = fileDir.listFiles();
                for (File o : oldFile) o.delete();
            } else {
                fileDir.mkdir();
            }
            try {
                file.transferTo(filePath);
                // ????????????????????????
                if ("???".equals(demandDO.getIsApprovalProcess())) {
                    DemandPictureDO demandPictureDO = new DemandPictureDO();
                    demandPictureDO.setPicReqinnerseq(demandDO.getReqInnerSeq());
                    demandPictureDO.setPicMoth(demandDO.getReqImplMon());
                    DemandPictureDO demandPicture = iDemandPictureDao.showOne(demandPictureDO);
                    demandPicture.setPicName(file.getOriginalFilename());
                    demandPicture.setPicLocal(picLocal);
                    demandPicture.setPicUser(user);
                    demandPicture.setPicTime(time);
                    demandPicture.setPicMoth(demandDO.getReqImplMon());
                    demandPicture.setPicReqnm(demandDO.getReqNm());
                    demandPicture.setPicReqno(demandDO.getReqNo());
                    //??????JK???????????????
                    iDemandPictureDao.update(demandPicture);
                } else {
                    // ??????????????????
                    DemandPictureDO demandPicture = new DemandPictureDO();
                    demandPicture.setPicReqinnerseq(reqInnerSeq);
                    demandPicture.setPicName(file.getOriginalFilename());
                    demandPicture.setPicLocal(picLocal);
                    demandPicture.setPicUser(user);
                    demandPicture.setPicTime(time);
                    demandPicture.setPicMoth(demandDO.getReqImplMon());
                    demandPicture.setPicReqnm(demandDO.getReqNm());
                    demandPicture.setPicReqno(demandDO.getReqNo());
                    //??????JK???????????????
                    iDemandPictureDao.insert(demandPicture);
                }

            } catch (IllegalStateException e) {
                e.printStackTrace();
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            } catch (IOException e) {
                e.printStackTrace();
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }

    }

    @Override
    public DemandBO approvalFindOne(String id, String month) {
        DemandPictureDO demandPictureDO = new DemandPictureDO();
        demandPictureDO.setPicReqinnerseq(id);
        demandPictureDO.setPicMoth(month);
        DemandPictureDO demandPicture = iDemandPictureDao.showOne(demandPictureDO);
        System.err.println(demandPicture);
        DemandBO demandBO = new DemandBO();
        String site = demandPicture.getPicLocal() + "/" + demandPicture.getPicName();
        System.err.println(site);
        demandBO.setSite(site);
        return demandBO;
    }

    @Override
    public DemandnNameChangeRspBO numberNameChangeDetail(DemandNameChangeBO demandNameChangeBO) {
        PageInfo<DemandNameChangeBO> pageInfo = getDemandnNameChange(demandNameChangeBO);
        List<DemandNameChangeBO> demandBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandNameChangeBO.class);
        DemandnNameChangeRspBO demandnNameChangeRspBO = new DemandnNameChangeRspBO();
        demandnNameChangeRspBO.setDemandNameChangeBOS(demandBOList);
        demandnNameChangeRspBO.setPageInfo(pageInfo);
        return demandnNameChangeRspBO;
    }

    @Override
    public void productionDefectIntroduction(MultipartFile file) {
        File f = null;
        LinkedList<ProductionDefectsDO> productionDefectsDOLinkedList = new LinkedList<>();
        try {
            //MultipartFile???file
            String originalFilename = file.getOriginalFilename();
            //???????????????
            String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            if (suffix.equals("xls")) {
                suffix = ".xls";
            } else if (suffix.equals("xlsm") || suffix.equals("xlsx")) {
                suffix = ".xlsx";
            } else {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            f = File.createTempFile("tmp", suffix);
            file.transferTo(f);
            String filepath = f.getPath();
            //excel???java???
            ReadExcelUtils excelReader = new ReadExcelUtils(filepath);
            Map<Integer, Map<Integer, Object>> map = excelReader.readExcelContent();
            for (int i = 1; i <= map.size(); i++) {
                //productionDefects
//????????? serialNumber   ??????  documentNumber	???????????? processStatus	??????????????????	 processStartDate ????????????	currentSession
// ???????????????	currentExecutor ????????????(???????????????) problemRaiser	????????????(????????????)	questionTitle
// ????????????(????????????)	questionNumber ????????????(????????????)questionDetails	????????????(???????????????????????????)	solution
// ????????????(??????????????????)problemAttributionDept	????????????(???????????????)	personInCharge ????????????(????????????)	questionType
// ????????????(????????????) identifyTheProblem	????????????(???????????????) developmentLeader	????????????(????????????) updateType
                ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
                productionDefectsDO.setSerialnumber(map.get(i).get(0).toString().trim());
                productionDefectsDO.setDocumentnumber(map.get(i).get(1).toString().trim());
                productionDefectsDO.setProcessstatus(map.get(i).get(2).toString().trim());
                productionDefectsDO.setProcessstartdate(map.get(i).get(3).toString().trim());
                productionDefectsDO.setCurrentsession(map.get(i).get(4).toString().trim());
                productionDefectsDO.setCurrentexecutor(map.get(i).get(5).toString().trim());
                productionDefectsDO.setProblemraiser(map.get(i).get(6).toString().trim());
                productionDefectsDO.setQuestiontitle(map.get(i).get(7).toString().trim());
                productionDefectsDO.setQuestionnumber(map.get(i).get(8).toString().trim());
                productionDefectsDO.setQuestiondetails(map.get(i).get(9).toString().trim());
                productionDefectsDO.setSolution(map.get(i).get(10).toString().trim());
                productionDefectsDO.setProblemattributiondept(map.get(i).get(11).toString().trim());
                productionDefectsDO.setPersonincharge(map.get(i).get(12).toString().trim());
                productionDefectsDO.setQuestiontype(map.get(i).get(13).toString().trim());
                productionDefectsDO.setIdentifytheproblem(map.get(i).get(14).toString().trim());
                productionDefectsDO.setDevelopmentleader(map.get(i).get(15).toString().trim());
                productionDefectsDO.setUpdatetype(map.get(i).get(16).toString().trim());
                productionDefectsDOLinkedList.add(productionDefectsDO);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } finally {
            f.delete();
        }
        productionDefectsDOLinkedList.forEach(m -> {
            ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
            productionDefectsDO.setSerialnumber(m.getSerialnumber());
            List<ProductionDefectsDO> productionDefectsDOS = productionDefectsDao.find(productionDefectsDO);
            if (JudgeUtils.isEmpty(productionDefectsDOS)) {
                productionDefectsDao.insert(m);
            } else {
                m.setId(productionDefectsDOS.get(0).getId());
                productionDefectsDao.update(m);
            }
        });
    }

    @Override
    public void smokeTestRegistration(SmokeTestRegistrationBO smokeTestRegistrationBO) {
        SmokeTestRegistrationDO smokeTestRegistrationDO = BeanUtils.copyPropertiesReturnDest(new SmokeTestRegistrationDO(), smokeTestRegistrationBO);
        smokeTestRegistrationDao.insert(smokeTestRegistrationDO);
        SmokeTestFailedCountDO smokeTestFailedCountDO = new SmokeTestFailedCountDO();
        smokeTestFailedCountDO.setJiraKey(smokeTestRegistrationBO.getJiraKey());
        List<SmokeTestFailedCountDO> smokeTestFailedCountDOS = smokeTestFailedCountDao.find(smokeTestFailedCountDO);
        if (JudgeUtils.isEmpty(smokeTestFailedCountDOS)) {
            smokeTestFailedCountDO.setJiraKey(smokeTestRegistrationBO.getJiraKey());
            smokeTestFailedCountDO.setCount(1);
            smokeTestFailedCountDO.setReqNo(smokeTestRegistrationBO.getReqNo());
            smokeTestFailedCountDO.setDepartment(smokeTestRegistrationBO.getDepartment());
            smokeTestFailedCountDO.setTestDate(smokeTestRegistrationBO.getTestDate());
            smokeTestFailedCountDao.insert(smokeTestFailedCountDO);
        } else {
            smokeTestFailedCountDOS.get(0).setCount(smokeTestFailedCountDOS.get(0).getCount() + 1);
            smokeTestFailedCountDOS.get(0).setTestDate(smokeTestRegistrationBO.getTestDate());
            smokeTestFailedCountDao.update(smokeTestFailedCountDOS.get(0));
        }
        // ??????????????????
        // ????????????????????????????????????
        // ???????????????????????? ???????????????????????????????????????????????????????????????????????? ???????????????
        Map<String, String> resMap = new HashMap<>();
        resMap = reqPlanService.getMailbox2(smokeTestRegistrationBO.getReqInnerSeq());
        String proMemberEmail = resMap.get("proMemberEmail");
        String devpEmail = resMap.get("devpEmail");
        //????????????
        // ??????????????????
        MultiMailSenderInfo mailInfo = new MultiMailSenderInfo();
        mailInfo.setMailServerHost("smtp.qiye.163.com");
        mailInfo.setMailServerPort("25");
        mailInfo.setValidate(true);
        mailInfo.setUsername(Constant.EMAIL_NAME);
        mailInfo.setPassword(Constant.EMAIL_PSWD);
        mailInfo.setFromAddress(Constant.EMAIL_NAME);
        String[] mailToAddress  = proMemberEmail.split(";");
        mailInfo.setReceivers(mailToAddress);
        String[] mailToCss = devpEmail.split(";");
        mailInfo.setCcs(mailToCss);
        mailInfo.setSubject("?????????????????????????????????");
        StringBuffer sb = new StringBuffer();
        sb.append("<table border='1' style='border-collapse: collapse;background-color: white; white-space: nowrap;'>");
        sb.append("<tr><td colspan='6' style='text-align: center;font-weight: bold;'>???????????????????????????</td></tr>");
        sb.append("<tr><td style='font-weight: bold;'>?????????</td><td>" + smokeTestRegistrationBO.getReqNm() + "</td><td style='font-weight: bold;'>????????????</td><td>" + smokeTestRegistrationBO.getReqNo() + "</td></tr>");
        sb.append("<tr><td style='font-weight: bold;'>????????????</td><td>" + smokeTestRegistrationBO.getTestDate() + "</td><td style='font-weight: bold;'>???????????????</td><td>" + smokeTestRegistrationBO.getTesters() + "</td></tr>");
        sb.append("<tr><td style='font-weight: bold;'>????????????</td><td>" + smokeTestRegistrationBO.getDepartment() + "</td><td style='font-weight: bold;'>jira??????</td><td>" + smokeTestRegistrationBO.getJiraKey() + "</td></tr>");
        sb.append("<tr><td style='font-weight: bold;'>???????????????????????????</td><td colspan='5'>" + smokeTestRegistrationBO.getTestdescription() + "</td></tr></table>");

        mailInfo.setContent("?????????:<br/>???" + smokeTestRegistrationBO.getReqNo() +"_"+smokeTestRegistrationBO.getReqNm()+ "?????????????????????????????????????????????????????????<br/>" + sb.toString());
        // ??????????????????????????????
        boolean isSend = MultiMailsender.sendMailtoMultiTest(mailInfo);
        if (!(isSend)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
    }



    /**
     * ????????????????????????
     */
    @Override
    public void demandInputResourceStatistics() {
        WorkingHoursDO workingHoursDO = new WorkingHoursDO();
        //?????????????????????N?????????
        workingHoursDO.setRegisterflag("N");
        List<WorkingHoursDO> workingHoursDOS = workingHoursDao.find(workingHoursDO);
        workingHoursDOS.forEach(m -> {
            //??????epic?????????????????????
            if (JudgeUtils.isBlank(m.getEpickey())) {
                m.setRegisterflag("Y");
                workingHoursDao.update(m);
                return;
            } else {
                DemandResourceInvestedDO demandResourceInvestedDO = new DemandResourceInvestedDO();
                demandResourceInvestedDO.setValueType("??????");
                demandResourceInvestedDO.setEpicKey(m.getEpickey());
                demandResourceInvestedDO.setDepartment(m.getDevpLeadDept());
                List<DemandResourceInvestedDO> demandResourceInvestedDOS = demandResourceInvestedDao.find(demandResourceInvestedDO);
                if (JudgeUtils.isNotEmpty(demandResourceInvestedDOS)) {
                    String value1 = demandResourceInvestedDOS.get(0).getValue();
                    String value2 = m.getTimespnet();
                    int parseInt = Integer.parseInt(value1) + Integer.parseInt(value2);
                    String value = String.valueOf(parseInt);
                    demandResourceInvestedDOS.get(0).setValue(value);
                    demandResourceInvestedDao.update(demandResourceInvestedDOS.get(0));
                } else {
                    demandResourceInvestedDO.setValue(m.getTimespnet());
                    demandResourceInvestedDao.insert(demandResourceInvestedDO);
                }
                //???????????????????????????
                m.setRegisterflag("Y");
                workingHoursDao.update(m);
            }

        });

    }

    @Override
    public TimeAxisDataBO getTimeAxisData(String reqInnerSeq) {
        String[] timeAxisData = null;
        int position =0;
        DemandBO demandBO = findById(reqInnerSeq);
        if (Integer.parseInt(demandBO.getPreCurPeriod()) < 30) {
            timeAxisData = this.getTimeAxisData(0);
            position=0;
        } else if (Integer.parseInt(demandBO.getPreCurPeriod()) < 120) {
            timeAxisData = this.getTimeAxisData(1);
            position=1;
        } else if (Integer.parseInt(demandBO.getPreCurPeriod()) == 120) {
            timeAxisData = this.getTimeAxisData(2);
            position=2;
        } else if (Integer.parseInt(demandBO.getPreCurPeriod()) < 160) {
            timeAxisData = this.getTimeAxisData(3);
            position=3;
        } else if (Integer.parseInt(demandBO.getPreCurPeriod()) < 180) {
            timeAxisData = this.getTimeAxisData(4);
            position=4;
        } else if (Integer.parseInt(demandBO.getPreCurPeriod()) == 180) {
            timeAxisData = this.getTimeAxisData(5);
            position=5;
        }


        TimeAxisDataBO timeAxisDataBO = new TimeAxisDataBO();
        timeAxisDataBO.setxAxisDate(timeAxisData);
        timeAxisDataBO.setPosition(position);

        //????????????
        timeAxisDataBO.setPrdFinshTm(demandBO.getPrdFinshTm());
        //uat????????????
        timeAxisDataBO.setUatUpdateTm(demandBO.getUatUpdateTm());
        //????????????
        timeAxisDataBO.setTestFinshTm(demandBO.getTestFinshTm());
        //???????????????
        timeAxisDataBO.setPreTm(demandBO.getPreTm());
        //????????????
        timeAxisDataBO.setExpPrdReleaseTm(demandBO.getProductionTime());
        //????????????
        String selectTime = DateUtil.date2String(new Date(), "yyyy-MM-dd");

        String req_peroid = dictionaryService.findFieldValue("REQ_PEROID", demandBO.getPreCurPeriod());
        timeAxisDataBO.setPreCurPeriod(req_peroid);
        timeAxisDataBO.setSelectTime(selectTime);
        return timeAxisDataBO;
    }

    @Override
    public void defectMonthlyDownload(HttpServletResponse response,String startDateTime,String endDateTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse=new Date();
        try {
            parse= sdf.parse(endDateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(parse);
        c.add(Calendar.DATE, + 1);
        Date time = c.getTime();
        String preDay = sdf.format(time);

        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();

        defectDetailsDO.setStartTime(startDateTime);
        defectDetailsDO.setEndTime(preDay);
        List<DefectDetailsDO> defectDetailsList = defectDetailsDao.findByTime(defectDetailsDO);

        HashMap<String,DefectMonthlyBO> defectMonthlyMap = new HashMap<>();
        ArrayList<DefectMonthlyBO> defectMonthlyList = new ArrayList<>();
        for(int i=0;i<defectDetailsList.size();i++){
            if(JudgeUtils.isNotBlank( defectDetailsList.get(i).getEpicKey())){
                if(defectDetailsList.get(i).getEpicKey().equals("CMPAY-2802")){
                    defectDetailsList.get(i).setEpicKey("CMPAY-2058");
                }
                if(defectDetailsList.get(i).getEpicKey().equals("CMPAY-1040")){
                    defectDetailsList.get(i).setEpicKey("CMPAY-999");
                }
                // ??????????????????????????????????????????
                if(WUXIAOWENTI.equals(defectDetailsList.get(i).getDefectType())){
                    continue;
                }
                DemandJiraDO demandJiraDO = new DemandJiraDO();
                demandJiraDO.setJiraKey(defectDetailsList.get(i).getEpicKey());
                // ??????jiraKey??????????????????
                List<DemandJiraDO> demandJiraDos = demandJiraDao.find(demandJiraDO);
                    // ???????????????????????? ????????????????????????
                    if(demandJiraDos!=null && demandJiraDos.size()!=0){
                        if(JudgeUtils.isNull(defectMonthlyMap.get(defectDetailsList.get(i).getEpicKey()))){
                            DemandDO demandDO = demandDao.get(demandJiraDos.get(demandJiraDos.size()-1).getReqInnerSeq());
                            DefectMonthlyBO defectMonthlyBO = new DefectMonthlyBO();
                            defectMonthlyBO.setReqNo(demandDO.getReqNo());
                            defectMonthlyBO.setReqNm(demandDO.getReqNm());
                            defectMonthlyBO.setReqPrdLine(demandDO.getReqPrdLine());
                            defectMonthlyBO.setTotalWorkload(demandDO.getTotalWorkload());
                            defectMonthlyBO.setReqMnger(demandDO.getReqMnger());
                            defectMonthlyBO.setReqMngerDept(demandDO.getReqProDept());
                            defectMonthlyBO.setDevpResMng(demandDO.getDevpResMng());
                            defectMonthlyBO.setDevpLeadDept(demandDO.getDevpLeadDept());
                            defectMonthlyBO.setDefectsNumber(1);
                            defectMonthlyBO.setDefectRate(String.format("%.2f", (float) defectMonthlyBO.getDefectsNumber() / (float) defectMonthlyBO.getTotalWorkload() * 100)+"%");
                            defectMonthlyMap.put(defectDetailsList.get(i).getEpicKey(),defectMonthlyBO);
                            if(JudgeUtils.isNotBlank(defectDetailsList.get(i).getSecurityLevel())){
                                if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setSeriousDefectsNumber(defectMonthlyBO.getSeriousDefectsNumber()+1);
                                }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setOrdinaryDefectsNumber(defectMonthlyBO.getOrdinaryDefectsNumber()+1);
                                }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setMinorDefectsNumber(defectMonthlyBO.getMinorDefectsNumber()+1);
                                }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setFatalDefectsNumber(defectMonthlyBO.getFatalDefectsNumber()+1);
                                }
                            }

                        }else{
                            DefectMonthlyBO defectMonthlyBO = defectMonthlyMap.get(defectDetailsList.get(i).getEpicKey());
                            defectMonthlyBO.setDefectsNumber(defectMonthlyBO.getDefectsNumber()+1);
                            defectMonthlyBO.setDefectRate(String.format("%.2f", (float) defectMonthlyBO.getDefectsNumber() / (float) defectMonthlyBO.getTotalWorkload() * 100)+"%");
                            if(JudgeUtils.isNotBlank(defectDetailsList.get(i).getSecurityLevel())){
                                if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setSeriousDefectsNumber(defectMonthlyBO.getSeriousDefectsNumber()+1);
                                }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setOrdinaryDefectsNumber(defectMonthlyBO.getOrdinaryDefectsNumber()+1);
                                }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setMinorDefectsNumber(defectMonthlyBO.getMinorDefectsNumber()+1);
                                }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                    defectMonthlyBO.setFatalDefectsNumber(defectMonthlyBO.getFatalDefectsNumber()+1);
                                }
                            }
                            defectMonthlyMap.put(defectDetailsList.get(i).getEpicKey(),defectMonthlyBO);
                        }

                    }else{
                        System.err.println(2);
                    }

            }else{
                System.err.println(1);
            }
        }

        for (Map.Entry<String, DefectMonthlyBO> entry : defectMonthlyMap.entrySet()) {
            DefectMonthlyBO defectMonthlyBO = entry.getValue();

            //??????????????? ?????????????????????
            //defectMonthlyBO.setExecutionCaseNumber(Integer.parseInt(jiraBasicInfoDO.getTestCaseNumber()));
          /*
            defectMonthlyBO.setSuccessCaseNumber(defectMonthlyBO.getExecutionCaseNumber()-defectMonthlyBO.getDefectsNumber());
            defectMonthlyBO.setTestPassRate(String.format("%.2f", (float) defectMonthlyBO.getSuccessCaseNumber() / (float) defectMonthlyBO.getExecutionCaseNumber() * 100)+"%");
         */

            //???????????????
            String req_peroid = dictionaryService.findFieldValue("PRD_LINE", defectMonthlyBO.getReqPrdLine());
            defectMonthlyBO.setReqPrdLine(req_peroid);
            defectMonthlyList.add(defectMonthlyBO);

        }

        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DefectMonthlyBO.class, defectMonthlyList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "reqTask_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public List<DefectMonthlyBO> getDefectMonthlyReport(String defectStartTime, String defectEndTime) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date parse=new Date();
        try {
            parse= sdf.parse(defectEndTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        Calendar c = Calendar.getInstance();
        c.setTime(parse);
        c.add(Calendar.DATE, + 1);
        Date time = c.getTime();
        String preDay = sdf.format(time);

        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();

        defectDetailsDO.setStartTime(defectStartTime);
        defectDetailsDO.setEndTime(preDay);
        List<DefectDetailsDO> defectDetailsList = defectDetailsDao.findByTime(defectDetailsDO);
        HashMap<String,DefectMonthlyBO> defectMonthlyMap = new HashMap<>();
        List<DefectMonthlyBO> defectMonthlyList = new ArrayList<>();
        for(int i=0;i<defectDetailsList.size();i++){
            if(JudgeUtils.isNotBlank( defectDetailsList.get(i).getEpicKey())){
                if(defectDetailsList.get(i).getEpicKey().equals("CMPAY-2802")){
                    defectDetailsList.get(i).setEpicKey("CMPAY-2058");
                }
                if(defectDetailsList.get(i).getEpicKey().equals("CMPAY-1040")){
                    defectDetailsList.get(i).setEpicKey("CMPAY-999");
                }
                // ??????????????????????????????????????????
                if(WUXIAOWENTI.equals(defectDetailsList.get(i).getDefectType())){
                    continue;
                }

                DemandJiraDO demandJiraDO = new DemandJiraDO();
                demandJiraDO.setJiraKey(defectDetailsList.get(i).getEpicKey());
                // ??????jiraKey??????????????????
                List<DemandJiraDO> demandJiraDos = demandJiraDao.find(demandJiraDO);
                // ???????????????????????? ????????????????????????

                if(demandJiraDos!=null && demandJiraDos.size()!=0){
                    if(JudgeUtils.isNull(defectMonthlyMap.get(defectDetailsList.get(i).getEpicKey()))){
                        DemandDO demandDO = demandDao.get(demandJiraDos.get(demandJiraDos.size()-1).getReqInnerSeq());
                        DefectMonthlyBO defectMonthlyBO = new DefectMonthlyBO();
                        defectMonthlyBO.setReqNo(demandDO.getReqNo());
                        defectMonthlyBO.setReqNm(demandDO.getReqNm());
                        defectMonthlyBO.setReqPrdLine(demandDO.getReqPrdLine());
                        defectMonthlyBO.setTotalWorkload(demandDO.getTotalWorkload());
                        defectMonthlyBO.setReqMnger(demandDO.getReqMnger());
                        defectMonthlyBO.setReqMngerDept(demandDO.getReqProDept());
                        defectMonthlyBO.setDevpResMng(demandDO.getDevpResMng());
                        defectMonthlyBO.setDevpLeadDept(demandDO.getDevpLeadDept());
                        defectMonthlyBO.setDefectsNumber(1);
                        defectMonthlyBO.setDefectRate(String.format("%.2f", (float) defectMonthlyBO.getDefectsNumber() / (float) defectMonthlyBO.getTotalWorkload() * 100)+"%");
                        defectMonthlyMap.put(defectDetailsList.get(i).getEpicKey(),defectMonthlyBO);
                        if(JudgeUtils.isNotBlank(defectDetailsList.get(i).getSecurityLevel())){
                            if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setSeriousDefectsNumber(defectMonthlyBO.getSeriousDefectsNumber()+1);
                            }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setOrdinaryDefectsNumber(defectMonthlyBO.getOrdinaryDefectsNumber()+1);
                            }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setMinorDefectsNumber(defectMonthlyBO.getMinorDefectsNumber()+1);
                            }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setFatalDefectsNumber(defectMonthlyBO.getFatalDefectsNumber()+1);
                            }
                        }

                    }else{
                        DefectMonthlyBO defectMonthlyBO = defectMonthlyMap.get(defectDetailsList.get(i).getEpicKey());
                        defectMonthlyBO.setDefectsNumber(defectMonthlyBO.getDefectsNumber()+1);
                        defectMonthlyBO.setDefectRate(String.format("%.2f", (float) defectMonthlyBO.getDefectsNumber() / (float) defectMonthlyBO.getTotalWorkload() * 100)+"%");
                        if(JudgeUtils.isNotBlank(defectDetailsList.get(i).getSecurityLevel())){
                            if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setSeriousDefectsNumber(defectMonthlyBO.getSeriousDefectsNumber()+1);
                            }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setOrdinaryDefectsNumber(defectMonthlyBO.getOrdinaryDefectsNumber()+1);
                            }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setMinorDefectsNumber(defectMonthlyBO.getMinorDefectsNumber()+1);
                            }else if(defectDetailsList.get(i).getSecurityLevel().equals("??????")){
                                defectMonthlyBO.setFatalDefectsNumber(defectMonthlyBO.getFatalDefectsNumber()+1);
                            }
                        }
                        defectMonthlyMap.put(defectDetailsList.get(i).getEpicKey(),defectMonthlyBO);
                    }

                }else{
                    System.err.println(defectDetailsList.get(i).getEpicKey());
                }
            }

        }

        for (Map.Entry<String, DefectMonthlyBO> entry : defectMonthlyMap.entrySet()) {
            DefectMonthlyBO defectMonthlyBO = entry.getValue();
            String key = entry.getKey();
            //???????????????
            String req_peroid = dictionaryService.findFieldValue("PRD_LINE", defectMonthlyBO.getReqPrdLine());
            defectMonthlyBO.setReqPrdLine(req_peroid);



            //??????????????? ?????????????????????
            //defectMonthlyBO.setExecutionCaseNumber(Integer.parseInt(jiraBasicInfoDO.getTestCaseNumber()));
          /*
            defectMonthlyBO.setSuccessCaseNumber(defectMonthlyBO.getExecutionCaseNumber()-defectMonthlyBO.getDefectsNumber());
            defectMonthlyBO.setTestPassRate(String.format("%.2f", (float) defectMonthlyBO.getSuccessCaseNumber() / (float) defectMonthlyBO.getExecutionCaseNumber() * 100)+"%");
         */

            defectMonthlyList.add(defectMonthlyBO);

        }

        return defectMonthlyList;
    }

    public String[] getTimeAxisData(int position) {
        String[] strings = new String[6];
        String[] date = {"??????????????????", "UAT????????????", "??????????????????", "???????????????", "??????????????????"};
        strings[position] = "";
        for (int j = 0,k=0; j < strings.length ; j++,k++) {
            if ("".equals(strings[j])) {
                k--;
                continue;
            }else{
                strings[j]=date[k];
            }
        }
        return strings;
    }

    private PageInfo<DemandNameChangeBO> getDemandnNameChange(DemandNameChangeBO demandNameChangeBO) {
        DemandNameChangeDO demandNameChangeDO = new DemandNameChangeDO();
        BeanConvertUtils.convert(demandNameChangeDO, demandNameChangeBO);
        PageInfo<DemandNameChangeBO> pageInfo = PageUtils.pageQueryWithCount(demandNameChangeBO.getPageNum(), demandNameChangeBO.getPageSize(),
                () -> BeanConvertUtils.convertList(iDemandNameChangeExtDao.findList(demandNameChangeDO), DemandNameChangeBO.class));
        return pageInfo;
    }




    /**
     * ?????????????????????
     */
    public void  supplementaryTestSubtask() {
        DemandDO demandDO1 = new DemandDO();
        demandDO1.setReqImplMon("2020-08");
        List<DemandDO> demandDOS1 = demandDao.find(demandDO1);

        System.err.println(demandDOS1.size());

        for (int i = 0; i < demandDOS1.size(); i++) {
            System.err.println(demandDOS1.get(i).getReqInnerSeq());
            DemandJiraDO demandJiraDO = new DemandJiraDO();
            demandJiraDO.setReqInnerSeq(demandDOS1.get(i).getReqInnerSeq());
            List<DemandJiraDO> demandJiraDOS = demandJiraDao.find(demandJiraDO);
            if (JudgeUtils.isEmpty(demandJiraDOS)) {
                continue;
            }
            String jiraKey = demandJiraDOS.get(0).getJiraKey();
            System.err.println(jiraKey);
            DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO = new DemandJiraDevelopMasterTaskDO();
            demandJiraDevelopMasterTaskDO.setRelevanceEpic(jiraKey);
            demandJiraDevelopMasterTaskDO.setIssueType("???????????????");
            List<DemandJiraDevelopMasterTaskDO> demandJiraDevelopMasterTaskDOS = demandJiraDevelopMasterTaskDao.find(demandJiraDevelopMasterTaskDO);
            System.err.println(demandJiraDevelopMasterTaskDOS.size());
            if (JudgeUtils.isEmpty(demandJiraDevelopMasterTaskDOS)) {
                continue;
            }
            System.err.println(demandJiraDevelopMasterTaskDOS.get(0).getJiraKey());
            JiraTaskBodyBO jiraTaskBodyBO = new JiraTaskBodyBO();
            try {
                jiraTaskBodyBO = JiraUtil.GetIssue(demandJiraDevelopMasterTaskDOS.get(0).getJiraKey());
            } catch (Exception e) {
                continue;
            }

            DemandJiraSubtaskDO demandJiraSubtaskDO1 = new DemandJiraSubtaskDO();
            demandJiraSubtaskDO1.setParenttaskkey(demandJiraDevelopMasterTaskDOS.get(0).getJiraKey());
            List<DemandJiraSubtaskDO> demandJiraSubtaskDOS = demandJiraSubtaskDao.find(demandJiraSubtaskDO1);
            if (JudgeUtils.isEmpty(demandJiraSubtaskDOS)) {

                CreateIssueTestSubtaskRequestBO createIssueTestSubtaskRequestBO = new CreateIssueTestSubtaskRequestBO();
                createIssueTestSubtaskRequestBO.setIssueType(10103);
                if (LemonUtils.getEnv().equals(Env.SIT)) {
                    createIssueTestSubtaskRequestBO.setProject(10100);
                } else {
                    createIssueTestSubtaskRequestBO.setProject(10106);
                }
                createIssueTestSubtaskRequestBO.setParentKey(demandJiraDevelopMasterTaskDOS.get(0).getJiraKey());
                UserDO userDO = new UserDO();
                userDO.setFullname(jiraTaskBodyBO.getAssignee());
                List<UserDO> userDOS = iUserDao.find(userDO);
                createIssueTestSubtaskRequestBO.setAssignee(userDOS.get(0).getUsername());
                String selectTime = DateUtil.date2String(new Date(), "yyyy-MM-dd");
                createIssueTestSubtaskRequestBO.setStartTime(selectTime);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DATE, +7);
                Date time = c.getTime();
                String preDay = sdf.format(time);
                createIssueTestSubtaskRequestBO.setEndTime(preDay);

                createIssueTestSubtaskRequestBO.setSummary("????????????????????????" + demandDOS1.get(i).getReqNm());
                createIssueTestSubtaskRequestBO.setDescription("??????????????????");
                Response response1 = JiraUtil.CreateIssue(createIssueTestSubtaskRequestBO);
                System.err.println(response1.getStatusCode());
                System.err.println(response1.prettyPrint());
                if (response1.getStatusCode() == 201) {
                    CreateIssueResponseBO as = response1.getBody().as(CreateIssueResponseBO.class);
                    DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
                    demandJiraSubtaskDO.setSubtaskkey(as.getKey());
                    demandJiraSubtaskDO.setParenttaskkey(createIssueTestSubtaskRequestBO.getParentKey());
                    demandJiraSubtaskDO.setSubtaskname(createIssueTestSubtaskRequestBO.getSummary());
                    demandJiraSubtaskDao.insert(demandJiraSubtaskDO);
                }


                createIssueTestSubtaskRequestBO.setSummary("????????????????????????" + demandDOS1.get(i).getReqNm());
                createIssueTestSubtaskRequestBO.setDescription("??????????????????");
                response1 = JiraUtil.CreateIssue(createIssueTestSubtaskRequestBO);
                if (response1.getStatusCode() == 201) {
                    CreateIssueResponseBO as = response1.getBody().as(CreateIssueResponseBO.class);
                    DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
                    demandJiraSubtaskDO.setSubtaskkey(as.getKey());
                    demandJiraSubtaskDO.setParenttaskkey(createIssueTestSubtaskRequestBO.getParentKey());
                    demandJiraSubtaskDO.setSubtaskname(createIssueTestSubtaskRequestBO.getSummary());
                    demandJiraSubtaskDao.insert(demandJiraSubtaskDO);
                }

                createIssueTestSubtaskRequestBO.setSummary("????????????????????????" + demandDOS1.get(i).getReqNm());
                createIssueTestSubtaskRequestBO.setDescription("??????????????????");
                response1 = JiraUtil.CreateIssue(createIssueTestSubtaskRequestBO);
                if (response1.getStatusCode() == 201) {
                    CreateIssueResponseBO as = response1.getBody().as(CreateIssueResponseBO.class);
                    DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
                    demandJiraSubtaskDO.setSubtaskkey(as.getKey());
                    demandJiraSubtaskDO.setParenttaskkey(createIssueTestSubtaskRequestBO.getParentKey());
                    demandJiraSubtaskDO.setSubtaskname(createIssueTestSubtaskRequestBO.getSummary());
                    demandJiraSubtaskDao.insert(demandJiraSubtaskDO);
                }
            }

        }
    }
    @Override
    public File getReportForm11(String displayname,String date1,String date2){
        if (org.apache.commons.lang3.StringUtils.isBlank(date1) && org.apache.commons.lang3.StringUtils.isBlank(date2)) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        //??????????????????
        String[] raqArgs = new String[2];
        raqArgs[0] = displayname;
        String reportId = "RPX-Z-2011_HBST";
        // ?????????
        if (org.apache.commons.lang3.StringUtils.isNotBlank(date1) && org.apache.commons.lang3.StringUtils.isBlank(date2)) {
            reportId = "RPX-Z-2011_HBST";
            raqArgs[1] = date1;
        }
        // ?????????
        if (org.apache.commons.lang3.StringUtils.isNotBlank(date2) && org.apache.commons.lang3.StringUtils.isBlank(date1)) {
            reportId = "RPX-Z-2012_HBST";
            raqArgs[1] = date2;
        }
        GenerateReportBO generateReportBO = new GenerateReportBO();
        generateReportBO.setReportId(reportId);
        generateReportBO.setRaqArgs(raqArgs);
        generateReportBO.setReportStyle("xlsx");
        return genRptService.genRaqRpt(generateReportBO);
    }

    @Override
    public DemandEaseDevelopmentRspBO easeDevelopmentfindList(DemandEaseDevelopmentBO demandEaseDevelopmentBO){
        PageInfo<DemandEaseDevelopmentBO> pageInfo = getPageInfo1(demandEaseDevelopmentBO);
        List<DemandEaseDevelopmentBO> easeDevelopmentBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandEaseDevelopmentBO.class);
        DemandEaseDevelopmentRspBO easeDevelopmentRspBO = new DemandEaseDevelopmentRspBO();
        easeDevelopmentRspBO.setDemandEaseDevelopmentBOList(easeDevelopmentBOList);
        easeDevelopmentRspBO.setPageInfo(pageInfo);
        return easeDevelopmentRspBO;
    }
    private PageInfo<DemandEaseDevelopmentBO>  getPageInfo1(DemandEaseDevelopmentBO demandEaseDevelopmentBO) {
        DemandEaseDevelopmentDO easeDevelopmentDO = new DemandEaseDevelopmentDO();
        BeanConvertUtils.convert(easeDevelopmentDO, demandEaseDevelopmentBO);
        PageInfo<DemandEaseDevelopmentBO> pageInfo = PageUtils.pageQueryWithCount(demandEaseDevelopmentBO.getPageNum(), demandEaseDevelopmentBO.getPageSize(),
                () -> BeanConvertUtils.convertList(easeDevelopmentExtDao.findList(easeDevelopmentDO), DemandEaseDevelopmentBO.class));
        return pageInfo;
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void easeDevelopmentDown(MultipartFile file){
        File f = null;
        LinkedList<DemandEaseDevelopmentDO> easeDevelopmentDOLinkedList = new LinkedList<>();
        try {
            //MultipartFile???file
            String originalFilename = file.getOriginalFilename();
            //???????????????
            String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            if (suffix.equals("xls")) {
                suffix = ".xls";
            } else if (suffix.equals("xlsm") || suffix.equals("xlsx")) {
                suffix = ".xlsx";
            } else {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            f = File.createTempFile("tmp", suffix);
            file.transferTo(f);
            String filepath = f.getPath();
            //excel???java???
            ReadExcelUtils excelReader = new ReadExcelUtils(filepath);
            Map<Integer, Map<Integer, Object>> map = excelReader.readExcelContent();
            for (int i = 1; i <= map.size(); i++) {
                DemandEaseDevelopmentDO easeDevelopmentDO = new DemandEaseDevelopmentDO();
                easeDevelopmentDO.setReqImplMon(map.get(i).get(0).toString().trim());
                easeDevelopmentDO.setFirstLevelOrganization(map.get(i).get(1).toString().trim());
                easeDevelopmentDO.setProductmanagementdepartment(map.get(i).get(2).toString().trim());
                easeDevelopmentDO.setCostdepartment(map.get(i).get(3).toString().trim());
                easeDevelopmentDO.setDocumentnumber(map.get(i).get(4).toString().trim());
                easeDevelopmentDO.setProcessstartdate(map.get(i).get(5).toString().trim());
                easeDevelopmentDO.setDevelopmentowner(map.get(i).get(6).toString().trim());
                easeDevelopmentDO.setSupportingmanufacturers(map.get(i).get(7).toString().trim());
                easeDevelopmentDO.setSupportingmanufacturerproducts(map.get(i).get(8).toString().trim());
                easeDevelopmentDO.setCuttype(map.get(i).get(9).toString().trim());
                easeDevelopmentDO.setDemandtheme(map.get(i).get(10).toString().trim());
                easeDevelopmentDO.setRequirementdescription(map.get(i).get(11).toString().trim());
                easeDevelopmentDO.setDevelopmentworkloadassess(map.get(i).get(12).toString().trim());
                easeDevelopmentDO.setDevelopmentworkload(map.get(i).get(13).toString().trim());
                easeDevelopmentDO.setCommissioningdate(map.get(i).get(14).toString().trim());
                easeDevelopmentDO.setAcceptor(map.get(i).get(15).toString().trim());
                easeDevelopmentDO.setAcceptancedate(map.get(i).get(16).toString().trim());
                easeDevelopmentDO.setRemark(map.get(i).get(17).toString().trim());
                easeDevelopmentDOLinkedList.add(easeDevelopmentDO);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } finally {
            f.delete();
        }
        easeDevelopmentDOLinkedList.forEach(m -> {
            DemandEaseDevelopmentDO supportWorkloadDO1 = new DemandEaseDevelopmentDO();
            supportWorkloadDO1.setDocumentnumber(m.getDocumentnumber());
            List<DemandEaseDevelopmentDO> productionDefectsDOS = easeDevelopmentExtDao.find(supportWorkloadDO1);
            if (JudgeUtils.isEmpty(productionDefectsDOS)) {
                easeDevelopmentExtDao.insert(m);
            } else {
                easeDevelopmentExtDao.update(m);
            }
        });
    }
    @Override
    public void getDownload(HttpServletResponse response, DemandEaseDevelopmentBO demandEaseDevelopmentBO){
        DemandEaseDevelopmentDO easeDevelopmentDO = new DemandEaseDevelopmentDO();
        BeanConvertUtils.convert(easeDevelopmentDO, demandEaseDevelopmentBO);
        List<DemandEaseDevelopmentDO> demandDOList = easeDevelopmentExtDao.findList(easeDevelopmentDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandEaseDevelopmentDO.class, demandDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "easeDevelopmentDO" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }
    @Override
    public void easeDevelopmentWorkloadCountForDevp(HttpServletRequest request, HttpServletResponse response, DemandEaseDevelopmentBO demandEaseDevelopmentBO ){
        DemandEaseDevelopmentDO easeDevelopmentDO = new DemandEaseDevelopmentDO();
        BeanConvertUtils.convert(easeDevelopmentDO, demandEaseDevelopmentBO);
        // ?????????????????????????????????????????????
        List<DemandEaseDevelopmentDO>  mon_input_workload_list = easeDevelopmentExtDao.easeDevelopmentWorkloadCountForDevp(easeDevelopmentDO);
        if(mon_input_workload_list == null || mon_input_workload_list.size()<=0){
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        //???????????????????????????
        List<String> firstLevelOrganizationList = iOrganizationStructureDao.findFirstLevelOrganization(new OrganizationStructureDO());
        firstLevelOrganizationList.add("????????????");
        List<DemandEaseDevelopment2DO> DevpWorkLoadList = new ArrayList<>();
        BigDecimal sum = new BigDecimal("0");
        for(int i=0;i<firstLevelOrganizationList.size();i++){
            DemandEaseDevelopment2DO demandEaseDevelopment2DO = new DemandEaseDevelopment2DO();
            // ??????????????????
            demandEaseDevelopment2DO.setSecondlevelorganization(firstLevelOrganizationList.get(i));
            demandEaseDevelopment2DO.setDevelopmentworkload("0");
            // ?????????????????????????????????????????????
            for(int j=0;j<mon_input_workload_list.size();j++){
                if(firstLevelOrganizationList.get(i).equals(mon_input_workload_list.get(j).getFirstLevelOrganization())){
                    demandEaseDevelopment2DO.setDevelopmentworkload(mon_input_workload_list.get(j).getDevelopmentworkload());
                }
            }
            sum = sum.add(new BigDecimal(demandEaseDevelopment2DO.getDevelopmentworkload()));
            DevpWorkLoadList.add(demandEaseDevelopment2DO);
        }
        // ????????????
        DemandEaseDevelopment2DO supportWorkload2DO = new DemandEaseDevelopment2DO();
        supportWorkload2DO.setSecondlevelorganization("??????????????????");
        supportWorkload2DO.setDevelopmentworkload(sum+"");
        DevpWorkLoadList.add(supportWorkload2DO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandEaseDevelopment2DO.class, DevpWorkLoadList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "base_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public List<Double> easeDevelopmentWorkloadCountForDevp3( DemandEaseDevelopmentBO demandEaseDevelopmentBO ){
        List<Double> DevpWorkLoadList = Arrays.asList(new Double[14]);
        //?????????
        for(int i =0;i<DevpWorkLoadList.size();i++){
            DevpWorkLoadList.set(i,new Double(0));
        }
        DemandEaseDevelopmentDO easeDevelopmentDO = new DemandEaseDevelopmentDO();
        BeanConvertUtils.convert(easeDevelopmentDO, demandEaseDevelopmentBO);
        // ?????????????????????????????????????????????
        List<DemandEaseDevelopmentDO>  mon_input_workload_list = easeDevelopmentExtDao.easeDevelopmentWorkloadCountForDevp(easeDevelopmentDO);
        if( JudgeUtils.isEmpty(mon_input_workload_list)){
            return  DevpWorkLoadList;
        }
        //???????????????????????????
        List<String> firstLevelOrganizationList = iOrganizationStructureDao.findFirstLevelOrganization(new OrganizationStructureDO());
        for(int i=0;i<firstLevelOrganizationList.size();i++){
            DemandEaseDevelopment2DO demandEaseDevelopment2DO = new DemandEaseDevelopment2DO();
            // ??????????????????
            demandEaseDevelopment2DO.setSecondlevelorganization(firstLevelOrganizationList.get(i));
            demandEaseDevelopment2DO.setDevelopmentworkload("0");
            // ?????????????????????????????????????????????
            for(int j=0;j<mon_input_workload_list.size();j++){
                if(firstLevelOrganizationList.get(i).equals(mon_input_workload_list.get(j).getFirstLevelOrganization())){
                    demandEaseDevelopment2DO.setDevelopmentworkload(mon_input_workload_list.get(j).getDevelopmentworkload());
                }
            }
            DevpWorkLoadList.set(i,Double.valueOf(demandEaseDevelopment2DO.getDevelopmentworkload()));
        }
        return  DevpWorkLoadList;
    }
    @Override
    public void easeDevelopmentWorkloadCountForDevp2(HttpServletRequest request, HttpServletResponse response, DemandEaseDevelopmentBO demandEaseDevelopmentBO){
        DemandEaseDevelopmentDO easeDevelopmentDO = new DemandEaseDevelopmentDO();
        BeanConvertUtils.convert(easeDevelopmentDO, demandEaseDevelopmentBO);
        // ?????????????????????????????????????????????
        List<DemandEaseDevelopmentDO>  mon_input_workload_list = easeDevelopmentExtDao.easeDevelopmentWorkloadCountForDevp2(easeDevelopmentDO);
        if(mon_input_workload_list == null || mon_input_workload_list.size()<=0){
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????????????????????????????!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        //???????????????????????????
        List<OrganizationStructureDO> secondlevelorganizationList = iOrganizationStructureDao.find(new OrganizationStructureDO());
        List<DemandEaseDevelopment2DO> DevpWorkLoadList = new ArrayList<>();
        BigDecimal sum = new BigDecimal("0");
        for(int i=0;i<secondlevelorganizationList.size();i++){
            DemandEaseDevelopment2DO supportWorkload2DO = new DemandEaseDevelopment2DO();
            // ??????????????????
            supportWorkload2DO.setSecondlevelorganization(secondlevelorganizationList.get(i).getSecondlevelorganization());
            supportWorkload2DO.setDevelopmentworkload("0");
            // ?????????????????????????????????????????????
            for(int j=0;j<mon_input_workload_list.size();j++){
                if(secondlevelorganizationList.get(i).getSecondlevelorganization().equals(mon_input_workload_list.get(j).getSecondlevelorganization())){
                    supportWorkload2DO.setDevelopmentworkload(mon_input_workload_list.get(j).getDevelopmentworkload());
                }
            }
            sum = sum.add(new BigDecimal(supportWorkload2DO.getDevelopmentworkload()));
            DevpWorkLoadList.add(supportWorkload2DO);
        }
        // ????????????
        DemandEaseDevelopment2DO supportWorkload2DO = new DemandEaseDevelopment2DO();
        supportWorkload2DO.setSecondlevelorganization("??????????????????");
        supportWorkload2DO.setDevelopmentworkload(sum+"");
        DevpWorkLoadList.add(supportWorkload2DO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DemandEaseDevelopment2DO.class, DevpWorkLoadList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "base_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void uploadDataExcel(MultipartFile file){
        File f = null;
        LinkedList<QuantitativeDataDO> quantitativeDataDOLinkedList = new LinkedList<>();
        try {
            //MultipartFile???file
            String originalFilename = file.getOriginalFilename();
            //???????????????
            String suffix = originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
            if (suffix.equals("xls")) {
                suffix = ".xls";
            } else if (suffix.equals("xlsm") || suffix.equals("xlsx")) {
                suffix = ".xlsx";
            } else {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            f = File.createTempFile("tmp", suffix);
            file.transferTo(f);
            String filepath = f.getPath();
            //excel???java???
            ReadExcelUtils excelReader = new ReadExcelUtils(filepath);
            Map<Integer, Map<Integer, Object>> map = excelReader.readExcelContent();
            for (int i = 1; i <= map.size(); i++) {
                QuantitativeDataDO quantitativeDataDO = new QuantitativeDataDO();
                quantitativeDataDO.setReqImplMon(map.get(i).get(0).toString().trim());
                quantitativeDataDO.setFirstlevelOrganization(map.get(i).get(1).toString().trim());
                //?????????????????????	?????????????????????
                quantitativeDataDO.setDevelopWork(Double.valueOf(map.get(i).get(2).toString().trim()));
                quantitativeDataDO.setSupportWork(Double.valueOf(map.get(i).get(3).toString().trim()));
                // ????????????????????????????????????
                quantitativeDataDO.setWorkNotCompletedNumber(Double.valueOf(map.get(i).get(4).toString().trim()).intValue());
                // ??????????????????????????????
                quantitativeDataDO.setUnstandardizedFeedbackWorksNumber(Double.valueOf(map.get(i).get(5).toString().trim()).intValue());
                // ?????????????????????
                quantitativeDataDO.setVersionUpdateIssues(Double.valueOf(map.get(i).get(6).toString().trim()).intValue());
                // ????????????
                quantitativeDataDO.setBaseAwardedMarks(Double.valueOf(map.get(i).get(7).toString().trim()).intValue());
                // ????????????
                quantitativeDataDO.setPraiseAwardedMarks(Double.valueOf(map.get(i).get(8).toString().trim()).intValue());
                // ????????????
                quantitativeDataDO.setQualityAwardedMarks(Double.valueOf(map.get(i).get(9).toString().trim()).intValue());
                // ????????????
                quantitativeDataDO.setInnovateAwardedMarks(Double.valueOf(map.get(i).get(10).toString().trim()).intValue());
                // ????????????
                quantitativeDataDO.setBaseDeductMarks(Double.valueOf(map.get(i).get(11).toString().trim()).intValue());
                // 	????????????
                quantitativeDataDO.setCriticismDeductMarks(Double.valueOf(map.get(i).get(12).toString().trim()).intValue());
                // ????????????
                quantitativeDataDO.setFundLoss(Double.valueOf(map.get(i).get(13).toString().trim()).intValue());
                quantitativeDataDOLinkedList.add(quantitativeDataDO);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        } finally {
            f.delete();
        }
        quantitativeDataDOLinkedList.forEach(m -> {
            List<QuantitativeDataDO> quantitativeDataDOList = quantitativeDataExtDao.findOne(m);
            if (JudgeUtils.isNotEmpty(quantitativeDataDOList)) {
                m.setQuantitativeId(quantitativeDataDOList.get(0).getQuantitativeId());
                DecimalFormat df = new DecimalFormat("0.00");
                // ????????????????????????
                double work = quantitativeDataDOList.get(0).getDevelopWorkSum()+quantitativeDataDOList.get(0).getEasyWorkSum()+quantitativeDataDOList.get(0).getSupportWorkSum()+m.getDevelopWork()+m.getSupportWork();
                m.setFunctionPointsAssessWorkload(work+"");
                //defectRate ?????????  ??????PLOG???/(?????????????????????+???????????????????????????)???
                if(!m.getFirstlevelOrganization().equals("??????????????????")){
                    //inputOutputRatio ???????????????
                    m.setInputOutputRatio( df.format(( work/ Double.parseDouble(quantitativeDataDOList.get(0).getCostCoefficientsSum()) )));

                    int defectsNumber=0;
                    // ?????????????????????plog???
                    DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                    defectDetailsDO.setFirstlevelorganization(m.getFirstlevelOrganization());
                    defectDetailsDO.setRegistrationDate(m.getReqImplMon());
                    List<DefectDetailsDO> defectDetailsDOList = defectDetailsDao.findValidList(defectDetailsDO);
                    if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                        defectsNumber = defectDetailsDOList.size();
                    }
                    if((quantitativeDataDOList.get(0).getDevelopWorkSum()+quantitativeDataDOList.get(0).getEasyWorkSum()) != 0){
                        m.setDefectRate(df.format((defectsNumber/(quantitativeDataDOList.get(0).getDevelopWorkSum()+quantitativeDataDOList.get(0).getEasyWorkSum()+m.getDevelopWork()))*100)+"%");
                    }else{
                        m.setDefectRate("0.00%");
                    }
                }
                quantitativeDataExtDao.updateDataExcel(m);
            } else {
                //??????
                MsgEnum.ERROR_CUSTOM.setMsgInfo("??????????????????????????????????????????????????????????????????");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        });
    }

    @Override
    public void doDownloadData(HttpServletResponse response,DemandBO demandBO){
        // ????????????????????????
        if(JudgeUtils.isEmpty(demandBO.getReqImplMon())){
            //??????
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        QuantitativeDataDO quantitativeDataDO = new QuantitativeDataDO();
        quantitativeDataDO.setReqImplMon(demandBO.getReqImplMon());
        List<QuantitativeDataDO> quantitativeDataDOList = quantitativeDataExtDao.findOne(quantitativeDataDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), QuantitativeDataDO.class, quantitativeDataDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "reqTask_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }

    }
    @Override
    public void doDownloadData2(HttpServletResponse response,DemandBO demandBO){
        // ????????????????????????
        if(JudgeUtils.isEmpty(demandBO.getReqImplMon())){
            //??????
            MsgEnum.ERROR_CUSTOM.setMsgInfo("?????????????????????????????????");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        QuantitativeDataDO quantitativeDataDO = new QuantitativeDataDO();
        quantitativeDataDO.setReqImplMon(demandBO.getReqImplMon());
        List<QuantitativeDataDO> quantitativeDataDOList = quantitativeDataExtDao.findOne(quantitativeDataDO);
        double centreOutput = 0;
        // ??????????????????????????????
        for (int i=0;i<quantitativeDataDOList.size();i++){
            if(!TESTDEVP.equals(quantitativeDataDOList.get(i).getFirstlevelOrganization())){
                centreOutput += Double.parseDouble(quantitativeDataDOList.get(i).getInputOutputRatio());
            }
        }
        centreOutput = centreOutput / 8;
        String  [] deftList= {"????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","????????????????????????","??????????????????"};
        DecimalFormat df = new DecimalFormat("0.00");
        List<QuantitativeScoreDO> quantitativeScoreDOList = new ArrayList<>();
        for (int i=0;i<deftList.length;i++){
            quantitativeDataDO.setFirstlevelOrganization(deftList[i]);
            quantitativeDataDOList = quantitativeDataExtDao.findOne(quantitativeDataDO);
            QuantitativeScoreDO  quantitativeScoreDO = new QuantitativeScoreDO();
            if(deftList[i].equals("????????????????????????")){
                //???????????????????????????????????????20
                //???????????????????????????=????????????????????????/???????????????????????????*85*0.2???
                double inputOutput = (Double.parseDouble(quantitativeDataDOList.get(0).getInputOutputRatio())/ centreOutput) * 85 * 0.2;
//                if(){
//
//                }
//                quantitativeScoreDO.setInputOutputRatioMarks();
            }

        }

    }
}
