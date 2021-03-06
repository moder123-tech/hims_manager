package com.cmpay.lemon.monitor.service.impl.jira;

import com.cmpay.lemon.common.Env;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.BeanUtils;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.framework.security.SecurityUtils;
import com.cmpay.lemon.framework.utils.LemonUtils;
import com.cmpay.lemon.monitor.bo.DemandBO;
import com.cmpay.lemon.monitor.bo.ProblemBO;
import com.cmpay.lemon.monitor.bo.jira.*;
import com.cmpay.lemon.monitor.dao.*;
import com.cmpay.lemon.monitor.entity.*;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import com.cmpay.lemon.monitor.service.jira.JiraOperationService;
import com.cmpay.lemon.monitor.utils.DateUtil;
import com.cmpay.lemon.monitor.utils.ReadExcelUtils;
import com.cmpay.lemon.monitor.utils.jira.JiraUtil;
import io.restassured.response.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class JiraOperationServiceImpl implements JiraOperationService {
    @Autowired
    IJiraDepartmentDao jiraDepartmentDao;
    @Autowired
    IDemandJiraDao demandJiraDao;
    @Autowired
    private IUserExtDao iUserDao;
    @Autowired
    IDemandJiraDevelopMasterTaskDao demandJiraDevelopMasterTaskDao;
    @Autowired
    IDemandJiraSubtaskDao demandJiraSubtaskDao;
    @Autowired
    IProductionProblemJiraDao  productionProblemJiraDao;
    @Autowired
    private IProblemExtDao iProblemDao;
    @Autowired
    private IDemandExtDao demandDao;
    @Autowired
    private IProductionFollowDao productionFollowDao;
    //jira???????????? ???????????? jira??????10106(????????????)
    final static Integer PROJECTTYPE_CMPAY_dev = 10106;
    //jira???????????? ???????????? jira??????10100
    final static Integer PROJECTTYPE_CMPAY = 10100;
    //jira???????????? ?????????????????? jira??????
    final static Integer PROJECTTYPE_FCPT = 10104;
    //jira???????????? ?????????????????? jira??????
    final static Integer PROJECTTYPE_GPPT = 10102;
    //jira???????????? ?????????????????? jira??????
    final static Integer PROJECTTYPE_CSPT = 10103;
    //EPIC?????? jira??????
    final static Integer ISSUETYPE_EPIC = 10000;
    //??????????????? jira??????
    final static Integer ISSUETYPE_DEVELOPMAINTASK = 10100;
    //??????????????? jira??????
    final static Integer ISSUETYPE_TESTMAINTASK = 10102;
    //??????????????? jira??????
    final static Integer ISSUETYPE_TESTSUBTASK = 10103;
    // 10300 ????????????
    final static Integer ISSUETYPE_PRODUCTIONTASK = 10300;


    //Epic??????
    final static String EPIC = "Epic";
    //???????????????
    final static String DEVELOPMAINTASK = "???????????????";
    //???????????????
    final static String TESTMAINTASK = "???????????????";
    //??????????????????
    final static String FAIL = "fail";
    //??????????????????
    final static String SUCCESS = "success";
    //????????????
    final static String TESTINGDIVISION = "??????????????????";
    //?????????????????????
    final static String DEPARTMENTDIDNOTMATCH = "??????????????????(?????????????????????)";

    //jira?????????????????????????????? 201
    final static int JIRA_SUCCESS = 201;

    /**
     * jira??????epic??????
     * @param demandBO
     */
    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createEpic(DemandBO demandBO) {
        DateFormat format1 = new SimpleDateFormat("yyyy-MM");
        Date date1 = null;
        Date date2 = null;
        String str = "2020-05";
        // String???Date
        String reqImplMon = demandBO.getReqImplMon();
        try {
            date1 = format1.parse(reqImplMon);
            date2 = format1.parse(str);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        // ???????????????5?????????????????????????????????jira??????
        if(date1.getTime()<=date2.getTime()){
            return;
        }

        //jira??????????????????????????????????????????,???????????????jira??????
        DemandJiraDO demandJiraDO1 = demandJiraDao.get(demandBO.getReqInnerSeq());
        if (JudgeUtils.isNotNull(demandJiraDO1) && demandJiraDO1.getCreateState().equals(SUCCESS)) {
            // ?????????epic???????????????????????????
            this.createMasterTask(demandBO, demandJiraDO1);
            return;
        }


        CreateIssueEpicRequestBO createIssueEpicRequestBO = new CreateIssueEpicRequestBO();
        createIssueEpicRequestBO.setSummary(demandBO.getReqNm());
        createIssueEpicRequestBO.setDescription(demandBO.getReqDesc());
        //????????????????????????????????????????????????????????????
        if ("?????????????????????".equals(demandBO.getDevpLeadDept())) {
            createIssueEpicRequestBO.setProject(PROJECTTYPE_FCPT);
        } else if ("???????????????????????????".equals(demandBO.getDevpLeadDept())) {
            createIssueEpicRequestBO.setProject(PROJECTTYPE_GPPT);
        } else if ("????????????????????????".equals(demandBO.getDevpLeadDept())) {
            createIssueEpicRequestBO.setProject(PROJECTTYPE_CSPT);
        } else {
            if (LemonUtils.getEnv().equals(Env.SIT)) {
                createIssueEpicRequestBO.setProject(PROJECTTYPE_CMPAY);
            } else {
                createIssueEpicRequestBO.setProject(PROJECTTYPE_CMPAY_dev);
            }
        }

        createIssueEpicRequestBO.setIssueType(ISSUETYPE_EPIC);
        createIssueEpicRequestBO.setDevpLeadDept(demandBO.getDevpLeadDept());
        // ???????????? ?????????????????????????????????????????????
        String description = "??????????????????"+demandBO.getReqBackground().replaceAll("\r|\n", "")+"??????????????????"+demandBO.getReqRetrofit().replaceAll("\r|\n", "")+"????????????"+demandBO.getReqValue().replaceAll("\r|\n", "");
        createIssueEpicRequestBO.setDescription(description);
        createIssueEpicRequestBO.setReqInnerSeq(demandBO.getReqInnerSeq());
        //????????????????????????
        JiraDepartmentDO jiraDepartmentDO = jiraDepartmentDao.get(demandBO.getDevpLeadDept());
        //???????????????????????????????????????jira??????????????????
        if (JudgeUtils.isNull(jiraDepartmentDO)) {
            DemandJiraDO demandJiraDO = new DemandJiraDO();
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDO.setReqInnerSeq(demandBO.getReqInnerSeq());
            demandJiraDO.setReqNm(demandBO.getReqNm());
            demandJiraDO.setCreateState(FAIL);
            demandJiraDO.setRemarks(DEPARTMENTDIDNOTMATCH);
            demandJiraDao.insert(demandJiraDO);
            return;
        }
        createIssueEpicRequestBO.setManager(jiraDepartmentDO.getManagerjiranm());
        Response response = JiraUtil.CreateIssue(createIssueEpicRequestBO);
        //???????????????????????????????????????jira?????????
        if (response.getStatusCode() == JIRA_SUCCESS) {
            CreateIssueResponseBO createIssueResponseBO = response.getBody().as(CreateIssueResponseBO.class);
            DemandJiraDO demandJiraDO = new DemandJiraDO();
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDO.setJiraId(createIssueResponseBO.getId());
            demandJiraDO.setJiraKey(createIssueResponseBO.getKey());
            demandJiraDO.setReqInnerSeq(demandBO.getReqInnerSeq());
            demandJiraDO.setReqNm(demandBO.getReqNm());
            demandJiraDO.setAssignmentDepartment(demandBO.getDevpLeadDept());
            demandJiraDO.setIssueType(EPIC);
            demandJiraDO.setCreateState(SUCCESS);
            demandJiraDO.setRemarks("");
            //??????jira???????????????????????????????????????
            DemandJiraDO demandJiraDO2 = demandJiraDao.get(demandJiraDO.getReqInnerSeq());
            if (JudgeUtils.isNull(demandJiraDO2)) {
                demandJiraDao.insert(demandJiraDO);
            } else {
                demandJiraDao.update(demandJiraDO);
            }
            //???????????????
            this.createMasterTask(demandBO, demandJiraDO);

        } else {
            DemandJiraDO demandJiraDO = new DemandJiraDO();
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDO.setReqInnerSeq(demandBO.getReqInnerSeq());
            demandJiraDO.setReqNm(demandBO.getReqNm());
            demandJiraDO.setCreateState(FAIL);
            demandJiraDO.setRemarks(response.getBody().print());
            DemandJiraDO demandJiraDO2 = demandJiraDao.get(demandJiraDO.getReqInnerSeq());
            if (JudgeUtils.isNull(demandJiraDO2)) {
                demandJiraDao.insert(demandJiraDO);
            } else {
                demandJiraDao.update(demandJiraDO);
            }
        }
    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createProduction(ProblemBO problemBO){

        //jira??????????????????????????????????????????,???????????????jira??????
        ProductionProblemJiraDO demandJiraDO1 = productionProblemJiraDao.get(problemBO.getProblemSerialNumber()+"");
        if (JudgeUtils.isNotNull(demandJiraDO1) && demandJiraDO1.getCreateState().equals(SUCCESS)) {
            return;
        }
        CreateIssueProductionRequestBO createIssueProductionRequestBO = new CreateIssueProductionRequestBO();
        createIssueProductionRequestBO.setSummary(problemBO.getProNeed());
        //????????????????????????????????????????????????????????????
        if ("?????????????????????".equals(problemBO.getDevpLeadDept())) {
            createIssueProductionRequestBO.setProject(PROJECTTYPE_FCPT);
        } else if ("???????????????????????????".equals(problemBO.getDevpLeadDept())) {
            createIssueProductionRequestBO.setProject(PROJECTTYPE_GPPT);
        } else if ("????????????????????????".equals(problemBO.getDevpLeadDept())) {
            createIssueProductionRequestBO.setProject(PROJECTTYPE_CSPT);
        } else {
            if (LemonUtils.getEnv().equals(Env.SIT)) {
                createIssueProductionRequestBO.setProject(PROJECTTYPE_CMPAY);
            } else {
                createIssueProductionRequestBO.setProject(PROJECTTYPE_CMPAY_dev);
            }
        }
        createIssueProductionRequestBO.setIssuetype(ISSUETYPE_PRODUCTIONTASK);
        //????????????
        createIssueProductionRequestBO.setCustomfield_10400(problemBO.getProNumber());
        //????????????
        createIssueProductionRequestBO.setCustomfield_10403(problemBO.getProblemDetail());
        // ?????????????????????????????????
        UserDO userDO = iUserDao.getUserByUserFullName(problemBO.getDisplayname());
        // ?????????
        createIssueProductionRequestBO.setCustomfield_10207(userDO.getUsername());
        // ?????????
        createIssueProductionRequestBO.setAssignee(userDO.getUsername());

        Response response = JiraUtil.CreateIssue(createIssueProductionRequestBO);
        //???????????????????????????????????????jira?????????
        if (response.getStatusCode() == JIRA_SUCCESS) {
            CreateIssueResponseBO createIssueResponseBO = response.getBody().as(CreateIssueResponseBO.class);
            ProductionProblemJiraDO demandJiraDO = new ProductionProblemJiraDO();
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDO.setJiraId(createIssueResponseBO.getId());
            demandJiraDO.setJiraKey(createIssueResponseBO.getKey());
            demandJiraDO.setIssueType("????????????");
            demandJiraDO.setCreateState(SUCCESS);
            demandJiraDO.setRemarks("");
            demandJiraDO.setProblemSerialNumber(problemBO.getProblemSerialNumber()+"");
            demandJiraDO.setProNumber(problemBO.getProNumber());
            demandJiraDO.setProNeed(problemBO.getProNeed());
            //??????jira???????????????????????????????????????
            ProductionProblemJiraDO demandJiraDO2 = productionProblemJiraDao.get(problemBO.getProblemSerialNumber()+"");
            if (JudgeUtils.isNull(demandJiraDO2)) {
                productionProblemJiraDao.insert(demandJiraDO);
            } else {
                productionProblemJiraDao.update(demandJiraDO);
            }
            // ???jira id????????????????????????
            ProblemDO problemDO = BeanUtils.copyPropertiesReturnDest(new ProblemDO(), problemBO);
            problemDO.setIssuekey(createIssueResponseBO.getKey());
            iProblemDao.update(problemDO);

        } else {
            ProductionProblemJiraDO demandJiraDO = new ProductionProblemJiraDO();
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDO.setCreateState(FAIL);
            demandJiraDO.setRemarks(response.getBody().print());
            demandJiraDO.setIssueType("????????????");
            demandJiraDO.setProblemSerialNumber(problemBO.getProblemSerialNumber()+"");
            demandJiraDO.setProNumber(problemBO.getProNumber());
            demandJiraDO.setProNeed(problemBO.getProNeed());
            ProductionProblemJiraDO demandJiraDO2 = productionProblemJiraDao.get(problemBO.getProblemSerialNumber()+"");
            if (JudgeUtils.isNull(demandJiraDO2)) {
                productionProblemJiraDao.insert(demandJiraDO);
            } else {
                productionProblemJiraDao.update(demandJiraDO);
            }
        }

    }

    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void createProduction2(ProductionFollowDO productionFollowDO){

        //jira??????????????????????????????????????????,???????????????jira??????
        ProductionProblemJiraDO demandJiraDO1 = productionProblemJiraDao.get(productionFollowDO.getFollowId()+"");
        if (JudgeUtils.isNotNull(demandJiraDO1) && demandJiraDO1.getCreateState().equals(SUCCESS)) {
            return;
        }
        ProductionFollowDO productionFollowDO2 = productionFollowDao.get(productionFollowDO.getFollowId());
        CreateIssueProductionRequestBO createIssueProductionRequestBO = new CreateIssueProductionRequestBO();
        createIssueProductionRequestBO.setSummary(productionFollowDO.getFollowDetail());
        //????????????????????????????????????????????????????????????
        if ("?????????????????????".equals(productionFollowDO.getDevpLeadDept())) {
            createIssueProductionRequestBO.setProject(PROJECTTYPE_FCPT);
        } else if ("???????????????????????????".equals(productionFollowDO.getDevpLeadDept())) {
            createIssueProductionRequestBO.setProject(PROJECTTYPE_GPPT);
        } else if ("????????????????????????".equals(productionFollowDO.getDevpLeadDept())) {
            createIssueProductionRequestBO.setProject(PROJECTTYPE_CSPT);
        } else {
            if (LemonUtils.getEnv().equals(Env.SIT)) {
                createIssueProductionRequestBO.setProject(PROJECTTYPE_CMPAY);
            } else {
                createIssueProductionRequestBO.setProject(PROJECTTYPE_CMPAY_dev);
            }
        }
        createIssueProductionRequestBO.setIssuetype(ISSUETYPE_PRODUCTIONTASK);
        //????????????
        createIssueProductionRequestBO.setCustomfield_10400(productionFollowDO.getProNumber());
        //????????????
        createIssueProductionRequestBO.setCustomfield_10403(productionFollowDO.getFollowDetail());
        // ?????????????????????????????????
        UserDO userDO = iUserDao.getUserByUserFullName(productionFollowDO.getUpdateUser());
        // ?????????????????????????????????
        UserDO userDO2 = iUserDao.getUserByUserFullName(productionFollowDO.getFollowUser());
        // ?????????
        createIssueProductionRequestBO.setCustomfield_10207(userDO.getUsername());
        // ?????????
        createIssueProductionRequestBO.setAssignee(userDO2.getUsername());

        Response response = JiraUtil.CreateIssue(createIssueProductionRequestBO);
        //???????????????????????????????????????jira?????????
        if (response.getStatusCode() == JIRA_SUCCESS) {
            CreateIssueResponseBO createIssueResponseBO = response.getBody().as(CreateIssueResponseBO.class);
            ProductionProblemJiraDO demandJiraDO = new ProductionProblemJiraDO();
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDO.setJiraId(createIssueResponseBO.getId());
            demandJiraDO.setJiraKey(createIssueResponseBO.getKey());
            demandJiraDO.setIssueType("????????????");
            demandJiraDO.setCreateState(SUCCESS);
            demandJiraDO.setRemarks("");
            demandJiraDO.setProblemSerialNumber(productionFollowDO.getFollowId()+"");
            demandJiraDO.setProNumber(productionFollowDO.getProNumber());
            demandJiraDO.setProNeed(productionFollowDO.getFollowDetail());
            //??????jira???????????????????????????????????????
            ProductionProblemJiraDO demandJiraDO2 = productionProblemJiraDao.get(productionFollowDO.getFollowId()+"");
            if (JudgeUtils.isNull(demandJiraDO2)) {
                productionProblemJiraDao.insert(demandJiraDO);
            } else {
                productionProblemJiraDao.update(demandJiraDO);
            }
            // ???jira id?????????????????????
            productionFollowDO.setIssuekey(createIssueResponseBO.getKey());
            // jira??????
            productionFollowDO.setIssueStatus("?????????");
            productionFollowDao.update(productionFollowDO);

        } else {
            ProductionProblemJiraDO demandJiraDO = new ProductionProblemJiraDO();
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDO.setCreateState(FAIL);
            demandJiraDO.setRemarks(response.getBody().print());
            demandJiraDO.setIssueType("????????????");
            demandJiraDO.setProblemSerialNumber(productionFollowDO.getFollowId()+"");
            demandJiraDO.setProNumber(productionFollowDO.getProNumber());
            demandJiraDO.setProNeed(productionFollowDO.getFollowDetail());
            ProductionProblemJiraDO demandJiraDO2 = productionProblemJiraDao.get(productionFollowDO.getFollowId()+"");
            if (JudgeUtils.isNull(demandJiraDO2)) {
                productionProblemJiraDao.insert(demandJiraDO);
            } else {
                productionProblemJiraDao.update(demandJiraDO);
            }
        }

    }

    /**?????????????????????
     * @param devpCoorDept
     * @param demandBO
     * @param epicDemandJiraDO
     * @param taskType
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void CreateJiraMasterTask(String devpCoorDept, DemandBO demandBO, DemandJiraDO epicDemandJiraDO, String taskType) {
        //?????????????????????????????????
        DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO1 = demandJiraDevelopMasterTaskDao.get(epicDemandJiraDO.getJiraKey() + "_" + devpCoorDept + "_" + taskType);
        if (JudgeUtils.isNotNull(demandJiraDevelopMasterTaskDO1) && demandJiraDevelopMasterTaskDO1.getCreateState().equals(SUCCESS)) {
            return;
        }
        //????????????????????????
        JiraDepartmentDO jiraDepartmentDO = jiraDepartmentDao.get(devpCoorDept);
        //???????????????????????????????????????jira??????????????????
        if (JudgeUtils.isNull(jiraDepartmentDO)) {
            DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO = new DemandJiraDevelopMasterTaskDO();
            demandJiraDevelopMasterTaskDO.setCreatTime(LocalDateTime.now());
            //??????jira????????????
            demandJiraDevelopMasterTaskDO.setMasterTaskKey(epicDemandJiraDO.getJiraKey() + "_" + devpCoorDept + "_" + taskType);
            demandJiraDevelopMasterTaskDO.setReqNm(demandBO.getReqNm());
            demandJiraDevelopMasterTaskDO.setCreateState(FAIL);
            demandJiraDevelopMasterTaskDO.setRemarks(DEPARTMENTDIDNOTMATCH);
            demandJiraDevelopMasterTaskDao.insert(demandJiraDevelopMasterTaskDO);
            return;
        }
        CreateIssueMainTaskRequestBO createMainTaskRequestBO = new CreateIssueMainTaskRequestBO();
        createMainTaskRequestBO.setEpicKey(epicDemandJiraDO.getJiraKey());
        //??????jira???????????????  ??????+?????????
        createMainTaskRequestBO.setSummary("???" + devpCoorDept + "???" + demandBO.getReqNm());
        // ???????????? ?????????????????????????????????????????????
        String description = "??????????????????"+demandBO.getReqBackground().replaceAll("\r|\n", "")+"??????????????????"+demandBO.getReqRetrofit().replaceAll("\r|\n", "")+"????????????"+demandBO.getReqValue().replaceAll("\r|\n", "");
        createMainTaskRequestBO.setDescription(description);
        //???????????????????????????????????????????????????
        if (taskType.equals(DEVELOPMAINTASK)) {
            createMainTaskRequestBO.setIssueType(ISSUETYPE_DEVELOPMAINTASK);
        } else {
            createMainTaskRequestBO.setIssueType(ISSUETYPE_TESTMAINTASK);
        }

        if ("?????????????????????".equals(demandBO.getDevpLeadDept())) {
            createMainTaskRequestBO.setProject(PROJECTTYPE_FCPT);
        } else if ("???????????????????????????".equals(demandBO.getDevpLeadDept())) {
            createMainTaskRequestBO.setProject(PROJECTTYPE_GPPT);
        } else if ("????????????????????????".equals(demandBO.getDevpLeadDept())) {
            createMainTaskRequestBO.setProject(PROJECTTYPE_CSPT);
        } else {
            if (LemonUtils.getEnv().equals(Env.SIT)) {
                createMainTaskRequestBO.setProject(PROJECTTYPE_CMPAY);
            } else {
                createMainTaskRequestBO.setProject(PROJECTTYPE_CMPAY_dev);
            }
        }

        createMainTaskRequestBO.setDevpLeadDept(devpCoorDept);
        createMainTaskRequestBO.setReqInnerSeq(demandBO.getReqInnerSeq());
        createMainTaskRequestBO.setManager(jiraDepartmentDO.getManagerjiranm());
        //???????????????
        Response response = JiraUtil.CreateIssue(createMainTaskRequestBO);
        if (response.getStatusCode() == JIRA_SUCCESS)  {
            CreateIssueResponseBO createIssueResponseBO = response.getBody().as(CreateIssueResponseBO.class);
            DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO = new DemandJiraDevelopMasterTaskDO();
            demandJiraDevelopMasterTaskDO.setCreatTime(LocalDateTime.now());
            demandJiraDevelopMasterTaskDO.setJiraId(createIssueResponseBO.getId());
            demandJiraDevelopMasterTaskDO.setJiraKey(createIssueResponseBO.getKey());
            demandJiraDevelopMasterTaskDO.setMasterTaskKey(epicDemandJiraDO.getJiraKey() + "_" + devpCoorDept + "_" + taskType);
            demandJiraDevelopMasterTaskDO.setReqNm(demandBO.getReqNm());
            demandJiraDevelopMasterTaskDO.setAssignmentDepartment(devpCoorDept);
            demandJiraDevelopMasterTaskDO.setIssueType(taskType);
            demandJiraDevelopMasterTaskDO.setCreateState(SUCCESS);
            //???????????????????????????
            demandJiraDevelopMasterTaskDO.setRemarks("");
            //????????????Epic
            demandJiraDevelopMasterTaskDO.setRelevanceEpic(epicDemandJiraDO.getJiraKey());
            //??????jira???????????????????????????????????????
            DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO2 = demandJiraDevelopMasterTaskDao.get(demandJiraDevelopMasterTaskDO.getMasterTaskKey());
            if (JudgeUtils.isNull(demandJiraDevelopMasterTaskDO2)) {
                demandJiraDevelopMasterTaskDao.insert(demandJiraDevelopMasterTaskDO);
            } else {
                demandJiraDevelopMasterTaskDao.update(demandJiraDevelopMasterTaskDO);
            }

            if(taskType.equals(TESTMAINTASK)){
                CreateIssueTestSubtaskRequestBO createIssueTestSubtaskRequestBO = new CreateIssueTestSubtaskRequestBO();
                createIssueTestSubtaskRequestBO.setIssueType(ISSUETYPE_TESTSUBTASK);
                if (LemonUtils.getEnv().equals(Env.SIT)) {
                    createIssueTestSubtaskRequestBO.setProject(PROJECTTYPE_CMPAY);
                } else {
                    createIssueTestSubtaskRequestBO.setProject(PROJECTTYPE_CMPAY_dev);
                }
                createIssueTestSubtaskRequestBO.setParentKey(createIssueResponseBO.getKey());
                createIssueTestSubtaskRequestBO.setAssignee(jiraDepartmentDO.getManagerjiranm());
                String selectTime = DateUtil.date2String(new Date(), "yyyy-MM-dd");
                createIssueTestSubtaskRequestBO.setStartTime(selectTime);
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DATE, + 7);
                Date time = c.getTime();
                String preDay = sdf.format(time);
                createIssueTestSubtaskRequestBO.setEndTime(preDay);

                createIssueTestSubtaskRequestBO.setSummary("????????????????????????"+demandBO.getReqNm());
                createIssueTestSubtaskRequestBO.setDescription("??????????????????");
                Response response1 = JiraUtil.CreateIssue(createIssueTestSubtaskRequestBO);
                if (response1.getStatusCode() == JIRA_SUCCESS) {
                    CreateIssueResponseBO as = response1.getBody().as(CreateIssueResponseBO.class);
                    DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
                    demandJiraSubtaskDO.setSubtaskkey(as.getKey());
                    demandJiraSubtaskDO.setParenttaskkey(createIssueTestSubtaskRequestBO.getParentKey());
                    demandJiraSubtaskDO.setSubtaskname(createIssueTestSubtaskRequestBO.getSummary());
                    demandJiraSubtaskDao.insert(demandJiraSubtaskDO);
                }


                createIssueTestSubtaskRequestBO.setSummary("????????????????????????"+demandBO.getReqNm());
                createIssueTestSubtaskRequestBO.setDescription("??????????????????");
                response1 = JiraUtil.CreateIssue(createIssueTestSubtaskRequestBO);
                if (response1.getStatusCode() == JIRA_SUCCESS) {
                    CreateIssueResponseBO as = response1.getBody().as(CreateIssueResponseBO.class);
                    DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
                    demandJiraSubtaskDO.setSubtaskkey(as.getKey());
                    demandJiraSubtaskDO.setParenttaskkey(createIssueTestSubtaskRequestBO.getParentKey());
                    demandJiraSubtaskDO.setSubtaskname(createIssueTestSubtaskRequestBO.getSummary());
                    demandJiraSubtaskDao.insert(demandJiraSubtaskDO);
                }

                createIssueTestSubtaskRequestBO.setSummary("????????????????????????"+demandBO.getReqNm());
                createIssueTestSubtaskRequestBO.setDescription("??????????????????");
                response1 = JiraUtil.CreateIssue(createIssueTestSubtaskRequestBO);
                if (response1.getStatusCode() == JIRA_SUCCESS) {
                    CreateIssueResponseBO as = response1.getBody().as(CreateIssueResponseBO.class);
                    DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
                    demandJiraSubtaskDO.setSubtaskkey(as.getKey());
                    demandJiraSubtaskDO.setParenttaskkey(createIssueTestSubtaskRequestBO.getParentKey());
                    demandJiraSubtaskDO.setSubtaskname(createIssueTestSubtaskRequestBO.getSummary());
                    demandJiraSubtaskDao.insert(demandJiraSubtaskDO);
                }
            }

        } else {
            DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO = new DemandJiraDevelopMasterTaskDO();
            demandJiraDevelopMasterTaskDO.setCreatTime(LocalDateTime.now());
            demandJiraDevelopMasterTaskDO.setAssignmentDepartment(devpCoorDept);
            demandJiraDevelopMasterTaskDO.setMasterTaskKey(epicDemandJiraDO.getJiraKey() + "_" + devpCoorDept + "_" + taskType);
            demandJiraDevelopMasterTaskDO.setReqNm(demandBO.getReqNm());
            demandJiraDevelopMasterTaskDO.setCreateState(FAIL);
            demandJiraDevelopMasterTaskDO.setRemarks(response.getBody().print());
            //????????????Epic
            demandJiraDevelopMasterTaskDO.setRelevanceEpic(epicDemandJiraDO.getJiraKey());
            DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO2 = demandJiraDevelopMasterTaskDao.get(demandJiraDevelopMasterTaskDO.getMasterTaskKey());
            if (JudgeUtils.isNull(demandJiraDevelopMasterTaskDO2)) {
                demandJiraDevelopMasterTaskDao.insert(demandJiraDevelopMasterTaskDO);
            } else {
                demandJiraDevelopMasterTaskDao.update(demandJiraDevelopMasterTaskDO);
            }
        }
    }


    @Async
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void batchCreateEpic(List<DemandDO> demandDOList) {
        demandDOList.forEach(m -> {
            DemandBO demandBO = BeanUtils.copyPropertiesReturnDest(new DemandBO(), m);
            this.createEpic(demandBO);
        });
    }

    @Override
    public void createMasterTask(DemandBO demandBO, DemandJiraDO demandJiraDO) {
        //????????????????????????
        List<String> developmentDepartmenList = new ArrayList<>();
        //????????????????????????
        if (demandBO.getDevpCoorDept() != null && demandBO.getDevpCoorDept() != "") {
            String[] split = demandBO.getDevpCoorDept().split(",");
            Arrays.stream(split).forEach(arr -> developmentDepartmenList.add(arr));
        }
        //????????????????????????
        developmentDepartmenList.add(demandBO.getDevpLeadDept());
        developmentDepartmenList.forEach(m -> {
            if (m.isEmpty()) {
                return;
            }
            //????????????????????????
            if (m.equals("?????????????????????")) {
                return;
            }
            //?????????????????????
            this.CreateJiraMasterTask(m, demandBO, demandJiraDO, DEVELOPMAINTASK);
        });
        //??????????????????????????????????????????????????????????????????????????????????????????
        if (!demandBO.getDevpLeadDept().equals("???????????????????????????") && !demandBO.getDevpLeadDept().equals("?????????????????????")&& !demandBO.getDevpLeadDept().equals("????????????????????????")) {
            this.CreateJiraMasterTask(TESTINGDIVISION, demandBO, demandJiraDO, TESTMAINTASK);
        }
    }


    @Override
    public void getJiraIssue(List<DemandDO> demandDOList) {
        demandDOList.forEach(demandDO -> {
            this.jiraEpicKey(demandDO);
        });
    }

    @Async
    @Override
    public void jiraEpicKey(DemandDO demandDO) {
        //??????jira epic key
        DemandJiraDO demandJiraDO = demandJiraDao.get(demandDO.getReqInnerSeq());
        if (demandJiraDO == null) {
            return;
        }
        String epicKey = demandJiraDO.getJiraKey();

        //??????jira epic key?????????????????????
        DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO = demandJiraDevelopMasterTaskDao.get(epicKey + "_??????????????????_???????????????");
        if (demandJiraDevelopMasterTaskDO == null) {
            return;
        }
        String jiraKey = demandJiraDevelopMasterTaskDO.getJiraKey();
        JiraTaskBodyBO jiraTaskBodyBO = JiraUtil.GetIssue(jiraKey);
        demandDO.setAssignee(jiraTaskBodyBO.getAssignee());
        demandDO.setPlanStartTime(jiraTaskBodyBO.getPlanStartTime());
        demandDO.setPlanEndTime(jiraTaskBodyBO.getPlanEndTime());
    }

    /*
     *???????????????????????????
     */
    @Async
    @Override
    public void jiraTestMainTaskBatchEdit(MultipartFile file) {
        List<JiraTaskBodyBO> jiraTaskBodyBOS = new ArrayList<>();
        List<DemandDO> demandDOList = new ArrayList<>();
           File f = null;

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
                JiraTaskBodyBO jiraTaskBodyBO = new JiraTaskBodyBO();
                DemandDO demandDO = new DemandDO();
                // ????????????
                demandDO.setReqInnerSeq(map.get(i).get(1).toString());
                // uat????????????
                if (map.get(i).get(8) instanceof String) {
                    demandDO.setTestFinshTm(map.get(i).get(8).toString().trim());
                }
                if (map.get(i).get(8) instanceof Date) {
                    Date date = (Date)map.get(i).get(8);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    demandDO.setTestFinshTm(dt.trim());
                }
                // ????????????
                demandDO.setTestEng(map.get(i).get(17).toString().trim());
                jiraTaskBodyBO.setReqInnerSeq(map.get(i).get(1).toString());
                if (!JudgeUtils.isBlank(map.get(i).get(19).toString().trim())) {
                    jiraTaskBodyBO.setAssignee(map.get(i).get(19).toString().trim());
                }
                if (!JudgeUtils.isBlank(map.get(i).get(20).toString())) {
                    jiraTaskBodyBO.setPlanStartTime(map.get(i).get(20).toString().trim());
                } else {
                    jiraTaskBodyBO.setPlanStartTime("");
                }
                if (!JudgeUtils.isBlank(map.get(i).get(21).toString())) {
                    jiraTaskBodyBO.setPlanEndTime(map.get(i).get(21).toString().trim());
                } else {
                    jiraTaskBodyBO.setPlanEndTime("");
                }
                demandDOList.add(demandDO);
                jiraTaskBodyBOS.add(jiraTaskBodyBO);
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
        // ???????????? uat??????????????????????????????????????????
        demandDOList.forEach(m -> {
            if(JudgeUtils.isNotEmpty(m.getTestFinshTm())||JudgeUtils.isNotEmpty(m.getTestEng())){
                demandDao.updateTest(m);
            }
        });
        //?????????????????????????????????????????????list
        List<JiraTaskBodyBO> jiraTastBodyAllTestTesk = new ArrayList<>();
        //??????????????????????????????jirekey
        jiraTaskBodyBOS.forEach(m -> {
            DemandJiraDO demandJiraDO;
            try {
                demandJiraDO = demandJiraDao.get(m.getReqInnerSeq());
            } catch (Exception e) {
                return;
            }
            String epicKey = "";
            try {
                epicKey = demandJiraDO.getJiraKey();
            } catch (Exception e) {
                return;
            }
            //??????jira epic key?????????????????????
            DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO = demandJiraDevelopMasterTaskDao.get(epicKey + "_??????????????????_???????????????");
            if (demandJiraDevelopMasterTaskDO == null) {
                return;
            }
            String jiraKey = demandJiraDevelopMasterTaskDO.getJiraKey();
            m.setJiraKey(jiraKey);
            if (m.getAssignee() != null) {
                UserDO userDO = new UserDO();
                userDO.setFullname(m.getAssignee());
                List<UserDO> userDOS = iUserDao.find(userDO);
                if (userDOS.isEmpty()) {
                    return;
                }
                m.setAssignee(userDOS.get(0).getUsername());
                userDO.setFullname(m.getAssignee());
            }
            jiraTastBodyAllTestTesk.add(m);
            DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
            demandJiraSubtaskDO.setParenttaskkey(m.getJiraKey());


            List<DemandJiraSubtaskDO> demandJiraSubtaskDOS = demandJiraSubtaskDao.find(demandJiraSubtaskDO);

            //???????????????????????????????????????????????????
            if(JudgeUtils.isNotEmpty(demandJiraSubtaskDOS)){
                for(int i=0 ;i<demandJiraSubtaskDOS.size();i++){
                    JiraTaskBodyBO jiraTaskBodyBO = new JiraTaskBodyBO();
                    jiraTaskBodyBO.setJiraKey(demandJiraSubtaskDOS.get(i).getSubtaskkey());
                    jiraTaskBodyBO.setAssignee(m.getAssignee());
                    jiraTaskBodyBO.setPlanStartTime(m.getPlanStartTime());
                    jiraTaskBodyBO.setPlanEndTime(m.getPlanEndTime());
                    jiraTastBodyAllTestTesk.add(jiraTaskBodyBO);
                }
            }
        });
            //??????jira??????
        jiraTastBodyAllTestTesk.forEach(m->{
             JiraUtil.EditTheTestMainTask(m);
        });



    }


}
