package com.cmpay.lemon.monitor.service.impl.jira;

import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.monitor.bo.DemandBO;
import com.cmpay.lemon.monitor.bo.DemandTestStatusBO;
import com.cmpay.lemon.monitor.bo.UserInfoBO;
import com.cmpay.lemon.monitor.bo.jira.JiraSubtasksBO;
import com.cmpay.lemon.monitor.bo.jira.JiraTaskBodyBO;
import com.cmpay.lemon.monitor.bo.jira.JiraWorklogBO;
import com.cmpay.lemon.monitor.dao.*;
import com.cmpay.lemon.monitor.entity.*;
import com.cmpay.lemon.monitor.service.SystemRoleService;
import com.cmpay.lemon.monitor.service.SystemUserService;
import com.cmpay.lemon.monitor.service.jira.JiraDataCollationService;
import com.cmpay.lemon.monitor.service.jira.JiraOperationService;
import com.cmpay.lemon.monitor.service.reportForm.ReqDataCountService;
import com.cmpay.lemon.monitor.utils.DateUtil;
import com.cmpay.lemon.monitor.utils.jira.JiraUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.compile;

@Service
public class JiraDataCollationServiceImpl implements JiraDataCollationService {
    @Autowired
    IDemandJiraDao demandJiraDao;
    @Autowired
    IDemandExtDao demandDao;
    @Autowired
    IJiraBasicInfoDao jiraBasicInfoDao;
    @Autowired
    IDemandJiraSubtaskDao demandJiraSubtaskDao;

    @Autowired
    IDemandJiraDevelopMasterTaskDao demandJiraDevelopMasterTaskDao;
    @Autowired
    JiraOperationService jiraOperationService;
    @Autowired
    IJiraWorklogDao jiraWorklogDao;
    @Autowired
    private IOrganizationStructureDao iOrganizationStructureDao;
    @Autowired
    private IWorkingHoursExtDao iWorkingHoursDao;
    @Autowired
    SystemUserService systemUserService;
    @Autowired
    IUserExtDao userExtDao;
    @Autowired
    IDefectDetailsExtDao defectDetailsDao;

    @Autowired
    IIssueDetailsExtDao issueDetailsDao;
    @Autowired
    IProblemStatisticDao problemStatisticDao;
    @Autowired
    SystemRoleService systemRoleService;

    @Autowired
    private IUserRoleExtDao userRoleExtDao;
    @Autowired
    private IUserExtDao iUserDao;
    @Autowired
    private ReqDataCountService reqDataCountService;
    @Autowired
    private ITestProgressDetailExtDao testProgressDetailDao;

    @Autowired
    private IProUnhandledIssuesExtDao proUnhandledIssuesDao;

    @Autowired
    private IProductionIssueDetailsDao productionIssueDetailsDao;
    @Autowired
    private IProductionFollowDao productionFollowDao;
    @Async
    @Override
    public void getIssueModifiedWithinOneDay() {
        List<JiraTaskBodyBO> jiraTaskBodyBOList = new LinkedList<>();
        int i = 0;
        //????????????????????????????????????jira????????????
        while (true) {
            List<JiraTaskBodyBO> jiraTaskBodyBOS = JiraUtil.batchQueryIssuesModifiedWithinOneDay(i);
            if (JudgeUtils.isEmpty(jiraTaskBodyBOS)) {
                break;
            }
            jiraTaskBodyBOList.addAll(jiraTaskBodyBOS);
            i = i + 50;
        }
//        JiraTaskBodyBO jiraTaskBodyBO1 = new JiraTaskBodyBO();
//        jiraTaskBodyBO1.setJiraKey("CMPAY-16174");
//        jiraTaskBodyBOList.add(jiraTaskBodyBO1);
        if (JudgeUtils.isNotEmpty(jiraTaskBodyBOList)) {
            HashSet<String> epicList = new HashSet<>();
            jiraTaskBodyBOList.forEach(m -> {
                try {
                    //??????jiraKey??????jira????????????
                        JiraTaskBodyBO jiraTaskBodyBO = JiraUtil.GetIssue(m.getJiraKey());
//                        //??????jira???????????????????????????????????????
                        this.registerJiraBasicInfo(jiraTaskBodyBO);
//                        //????????????
                        this.registerWorklogs(jiraTaskBodyBO);
                        epicList.add(jiraTaskBodyBO.getEpicKey());
                } catch (Exception e) {

                    e.printStackTrace();
                }
            });
            //epic????????????????????????????????????????????????
            epicList.forEach(m -> {
                if (m == null) {
                    return;
                }
                ProblemStatisticDO problemStatisticDO = new ProblemStatisticDO();
                problemStatisticDO.setEpicKey(m);
                DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                defectDetailsDO.setEpicKey(m);
                List<DefectDetailsDO> defectDetailsDOList = defectDetailsDao.find(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    for (int j = 0; j < defectDetailsDOList.size(); j++) {
                        if(JudgeUtils.isBlank(defectDetailsDOList.get(j).getDefectType())){
                            continue;
                        }
                        if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setExternalDefectsNumber(problemStatisticDO.getExternalDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setVersionDefectsNumber(problemStatisticDO.getVersionDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setParameterDefectsNumber(problemStatisticDO.getParameterDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setFunctionDefectsNumber(problemStatisticDO.getFunctionDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setProcessDefectsNumber(problemStatisticDO.getProcessDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-???????????????")) {
                            problemStatisticDO.setPromptDefectsNumber(problemStatisticDO.getPromptDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setPageDefectsNumber(problemStatisticDO.getPageDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setBackgroundDefectsNumber(problemStatisticDO.getBackgroundDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setFrontDefectsNumber(problemStatisticDO.getFrontDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-??????????????????")) {
                            problemStatisticDO.setModifyDefectsNumber(problemStatisticDO.getModifyDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setDesignDefectsNumber(problemStatisticDO.getDesignDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????")) {
                            problemStatisticDO.setInvalidDefectsNumber(problemStatisticDO.getInvalidDefectsNumber() + 1);
                        }
                    }
                }
                IssueDetailsDO issueDetailsDO = new IssueDetailsDO();
                issueDetailsDO.setEpicKey(m);
                List<IssueDetailsDO> issueDetailsDOList = issueDetailsDao.find(issueDetailsDO);
                if(JudgeUtils.isNotEmpty(issueDetailsDOList)){
                    for (int j = 0; j < issueDetailsDOList.size(); j++) {
                        if(JudgeUtils.isBlank(issueDetailsDOList.get(j).getIssueType())){
                            continue;
                        }
                        if (issueDetailsDOList.get(j).getIssueType().equals("????????????")) {
                            problemStatisticDO.setRequirementsReviewNumber(problemStatisticDO.getRequirementsReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("??????????????????")) {
                            problemStatisticDO.setVersionDefectsNumber(problemStatisticDO.getVersionDefectsNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("????????????")) {
                            problemStatisticDO.setCodeReviewNumber(problemStatisticDO.getCodeReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("??????????????????")) {
                            problemStatisticDO.setTestReviewNumber(problemStatisticDO.getTestReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("??????????????????")) {
                            problemStatisticDO.setProductionReviewNumber(problemStatisticDO.getProductionReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("????????????")) {
                            problemStatisticDO.setOtherReviewsNumber(problemStatisticDO.getOtherReviewsNumber() + 1);
                        }
                    }
                }
                ProblemStatisticDO problemStatisticDO1 = problemStatisticDao.get(problemStatisticDO.getEpicKey());
                if (JudgeUtils.isNull(problemStatisticDO1)) {
                    problemStatisticDao.insert(problemStatisticDO);
                } else {
                    problemStatisticDao.update(problemStatisticDO);
                }


            });
        }


    }

    @Async
    @Override
    public void getDefectAndProblem(){
        List<JiraTaskBodyBO> jiraTaskBodyBOList = new LinkedList<>();
        int i = 0;
        //??????????????????????????????jira????????????
        while (true) {
            List<JiraTaskBodyBO> jiraTaskBodyBOS = JiraUtil.batchQueryIssuesModifiedWithinOneDay2(i);
            if (JudgeUtils.isEmpty(jiraTaskBodyBOS)) {
                break;
            }
            jiraTaskBodyBOList.addAll(jiraTaskBodyBOS);
            i = i + 50;
        }
        if (JudgeUtils.isNotEmpty(jiraTaskBodyBOList)) {
            HashSet<String> epicList = new HashSet<>();
            jiraTaskBodyBOList.forEach(m -> {
                try {
                    //??????jiraKey??????jira????????????
                    JiraTaskBodyBO jiraTaskBodyBO = JiraUtil.GetIssue(m.getJiraKey());
                    //??????jira???????????????????????????????????????
                    this.registerJiraBasicInfo(jiraTaskBodyBO);
                    epicList.add(jiraTaskBodyBO.getEpicKey());
                } catch (Exception e) {

                    e.printStackTrace();
                }
            });
            //epic????????????????????????????????????????????????
            epicList.forEach(m -> {
                if (m == null) {
                    return;
                }
                ProblemStatisticDO problemStatisticDO = new ProblemStatisticDO();
                problemStatisticDO.setEpicKey(m);
                DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                defectDetailsDO.setEpicKey(m);
                List<DefectDetailsDO> defectDetailsDOList = defectDetailsDao.find(defectDetailsDO);
                if(JudgeUtils.isNotEmpty(defectDetailsDOList)){
                    for (int j = 0; j < defectDetailsDOList.size(); j++) {
                        if(JudgeUtils.isBlank(defectDetailsDOList.get(j).getDefectType())){
                            continue;
                        }
                        if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setExternalDefectsNumber(problemStatisticDO.getExternalDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setVersionDefectsNumber(problemStatisticDO.getVersionDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setParameterDefectsNumber(problemStatisticDO.getParameterDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setFunctionDefectsNumber(problemStatisticDO.getFunctionDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setProcessDefectsNumber(problemStatisticDO.getProcessDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-???????????????")) {
                            problemStatisticDO.setPromptDefectsNumber(problemStatisticDO.getPromptDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setPageDefectsNumber(problemStatisticDO.getPageDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setBackgroundDefectsNumber(problemStatisticDO.getBackgroundDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setFrontDefectsNumber(problemStatisticDO.getFrontDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-??????????????????")) {
                            problemStatisticDO.setModifyDefectsNumber(problemStatisticDO.getModifyDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????-????????????")) {
                            problemStatisticDO.setDesignDefectsNumber(problemStatisticDO.getDesignDefectsNumber() + 1);
                        } else if (defectDetailsDOList.get(j).getDefectType().equals("????????????")) {
                            problemStatisticDO.setInvalidDefectsNumber(problemStatisticDO.getInvalidDefectsNumber() + 1);
                        }
                    }
                }
                IssueDetailsDO issueDetailsDO = new IssueDetailsDO();
                issueDetailsDO.setEpicKey(m);
                List<IssueDetailsDO> issueDetailsDOList = issueDetailsDao.find(issueDetailsDO);
                if(JudgeUtils.isNotEmpty(issueDetailsDOList)){
                    for (int j = 0; j < issueDetailsDOList.size(); j++) {
                        if(JudgeUtils.isBlank(issueDetailsDOList.get(j).getIssueType())){
                            continue;
                        }
                        if (issueDetailsDOList.get(j).getIssueType().equals("????????????")) {
                            problemStatisticDO.setRequirementsReviewNumber(problemStatisticDO.getRequirementsReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("??????????????????")) {
                            problemStatisticDO.setVersionDefectsNumber(problemStatisticDO.getVersionDefectsNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("????????????")) {
                            problemStatisticDO.setCodeReviewNumber(problemStatisticDO.getCodeReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("??????????????????")) {
                            problemStatisticDO.setTestReviewNumber(problemStatisticDO.getTestReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("??????????????????")) {
                            problemStatisticDO.setProductionReviewNumber(problemStatisticDO.getProductionReviewNumber() + 1);
                        } else if (issueDetailsDOList.get(j).getIssueType().equals("????????????")) {
                            problemStatisticDO.setOtherReviewsNumber(problemStatisticDO.getOtherReviewsNumber() + 1);
                        }
                    }
                }
                ProblemStatisticDO problemStatisticDO1 = problemStatisticDao.get(problemStatisticDO.getEpicKey());
                if (JudgeUtils.isNull(problemStatisticDO1)) {
                    problemStatisticDao.insert(problemStatisticDO);
                } else {
                    problemStatisticDao.update(problemStatisticDO);
                }

            });
        }
        //?????????????????? ?????????????????????//?????????????????????????????????
        getproductedsRemainingQuestions();
    }

    @Async
    @Override
    public void TestProgressDetailOneDay(){
        //??????????????????????????????
        String date = DateUtil.getBeforeDay();
        //??????????????????
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM");
        String month = simpleDateFormat.format(new Date());
        DemandDO demandDO = new DemandDO();
        demandDO.setReqImplMon(month);
        // ?????????????????????
        List<DemandTestStatusBO>  list =  reqDataCountService.demandTestStatusList2();
        for(int i =0;i<list.size();i++){
            // ???????????????????????????????????????????????????????????????NAN???????????? ???????????????????????????????????????????????????0???????????????
            if("NaN%".equals(list.get(i).getTestProgress()) && list.get(i).getCaseCompletedNumber() == 0
            && list.get(i).getCaseExecutionNumber() == 0 && list.get(i).getDefectsNumber() == 0) {
                continue;
            }
            // ??????????????????????????????????????? ?????? ?????????jira??????
            demandDO.setReqNo(list.get(i).getReqNo());
            demandDO.setReqNm(list.get(i).getReqNm());
            List<DemandDO> demandDos = demandDao.QueryIsExecutingDemand2(demandDO);

            //????????????????????????  ???????????????jira??????
            DemandJiraDO demandJiraDO = new DemandJiraDO();
            demandJiraDO.setReqInnerSeq(demandDos.get(0).getReqInnerSeq());
            List<DemandJiraDO> demandJiraDOS = demandJiraDao.find(demandJiraDO);
            // jira??????
            String jiraKey = demandJiraDOS.get(0).getJiraKey();
            // ?????????
            String  reqPrdLind = demandDos.get(0).getReqPrdLine();
            TestProgressDetailDO testProgressDetailDO = new TestProgressDetailDO();
            testProgressDetailDO.setEpickey(jiraKey);
            testProgressDetailDO.setTestDate(date);
            testProgressDetailDO.setReqPrdLine(reqPrdLind);
            testProgressDetailDO.setTestProgress(list.get(i).getTestProgress());
            testProgressDetailDO.setTestCaseNumber(list.get(i).getTestCaseNumber());
            testProgressDetailDO.setCaseCompletedNumber(list.get(i).getCaseCompletedNumber()+"");
            testProgressDetailDO.setCaseExecutionNumber(list.get(i).getCaseExecutionNumber()+"");
            testProgressDetailDO.setDefectsNumber(list.get(i).getDefectsNumber()+"");
            // ??????epic??????????????????????????????????????????
            TestProgressDetailDO testProgressDetailDO1 = new TestProgressDetailDO();
            testProgressDetailDO1.setEpickey(jiraKey);
            testProgressDetailDO1.setTestDate(date);
            List<TestProgressDetailDO> testProgressDetailDOList = testProgressDetailDao.find(testProgressDetailDO1);
            if(testProgressDetailDOList ==null || testProgressDetailDOList.size() <=0){
                // ??????epic??????????????????????????????
                TestProgressDetailDO testProgressDetailDO2 = new TestProgressDetailDO();
                testProgressDetailDO2.setEpickey(jiraKey);
                List<TestProgressDetailDO> testProgressDetailDos = testProgressDetailDao.find(testProgressDetailDO2);
                //???????????????????????????????????????
                if(testProgressDetailDos ==null || testProgressDetailDos.size() <=0){
                    //??????
                    testProgressDetailDao.insert(testProgressDetailDO);
                }else{
                    // ????????????????????????????????????????????????????????????????????????????????????
                    if(!testProgressDetailDO.getCaseCompletedNumber().equals(testProgressDetailDos.get(testProgressDetailDos.size()-1).getCaseCompletedNumber())
                    || !testProgressDetailDO.getCaseExecutionNumber().equals(testProgressDetailDos.get(testProgressDetailDos.size()-1).getCaseExecutionNumber())
                    || !testProgressDetailDO.getTestCaseNumber().equals(testProgressDetailDos.get(testProgressDetailDos.size()-1).getTestCaseNumber())
                    || !testProgressDetailDO.getDefectsNumber().equals(testProgressDetailDos.get(testProgressDetailDos.size()-1).getDefectsNumber())){
                        //??????????????????????????????????????????????????????????????????????????????????????????????????????????????????
                        //??????
                        testProgressDetailDao.insert(testProgressDetailDO);
                    }
                }

            }else{
                //??????
                testProgressDetailDO.setId(testProgressDetailDOList.get(0).getId());
                testProgressDetailDao.update(testProgressDetailDO);
            }

        }
    }

    private void getproductedsRemainingQuestions() {
        ProUnhandledIssuesDO proUnhandledIssuesDO = new ProUnhandledIssuesDO();
        proUnhandledIssuesDO.setCalculateFlag("N");
        List<ProUnhandledIssuesDO> proUnhandledIssuesDOS = proUnhandledIssuesDao.find(proUnhandledIssuesDO);
        proUnhandledIssuesDOS.forEach(m->{
            DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
            defectDetailsDO.setEpicKey(m.getJirakey());
            // ????????????????????????
            List<DefectDetailsDO> list = defectDetailsDao.findNotCompleted(defectDetailsDO);
            // ????????????
            List<DefectDetailsDO> listSum = defectDetailsDao.findList(defectDetailsDO);
            if(JudgeUtils.isNotEmpty(list)){
                m.setDefectsNumber(list.size());
            }else{
                m.setDefectsNumber(0);
            }
            if(JudgeUtils.isNotEmpty(listSum)){
                m.setDefectsNumberSum(listSum.size());
            }else{
                m.setDefectsNumberSum(0);
            }
            IssueDetailsDO issueDetailsDO = new IssueDetailsDO();
            issueDetailsDO.setEpicKey(m.getJirakey());
            // ????????????????????????????????????
            List<IssueDetailsDO> list1 = issueDetailsDao.findNotCompleted(issueDetailsDO);
            //??????????????????
            List<IssueDetailsDO> list1Sum = issueDetailsDao.findList(issueDetailsDO);
            if(JudgeUtils.isNotEmpty(list1)){
                m.setProblemNumber(list1.size());
            }else{
                m.setProblemNumber(0);
            }
            if(JudgeUtils.isNotEmpty(list1Sum)){
                m.setProblemNumberSum(list1Sum.size());
            }else{
                m.setProblemNumberSum(0);
            }
            m.setCalculateFlag("Y");
            proUnhandledIssuesDao.update(m);
        });
    }

    /**
     * ????????????????????????????????????????????????????????????????????????
     * @param reqNo
     */
    @Override
    @Async
    public void inquiriesAboutRemainingProblems(String reqNo) {
        DemandDO demandDO = new DemandDO();
        demandDO.setReqNo(reqNo);
        List<DemandDO> demandDOS = demandDao.find(demandDO);
        if(JudgeUtils.isEmpty(demandDOS)){
            return;
        }
        DemandJiraDO demandJiraDO = demandJiraDao.get(demandDOS.get(0).getReqInnerSeq());

        if(JudgeUtils.isNull(demandJiraDO)){
            return;
        }
        ProUnhandledIssuesDO proUnhandledIssuesDO = new ProUnhandledIssuesDO();
        proUnhandledIssuesDO.setReqNo(reqNo);
        proUnhandledIssuesDO.setJirakey(demandJiraDO.getJiraKey());
        String selectTime = DateUtil.date2String(new Date(), "yyyy-MM-dd");
        proUnhandledIssuesDO.setProductionDate(selectTime);
        proUnhandledIssuesDO.setDepartment( demandDOS.get(0).getDevpLeadDept());

        //N??????????????????  ???????????????????????????????????????????????????????????????
        proUnhandledIssuesDO.setCalculateFlag("N");

        ProUnhandledIssuesDO proUnhandledIssuesDO1 = proUnhandledIssuesDao.get(reqNo);
        if(JudgeUtils.isNull(proUnhandledIssuesDO1)){
            proUnhandledIssuesDO.setProblemNumber(0);
            proUnhandledIssuesDO.setDefectsNumber(0);
            proUnhandledIssuesDO.setProblemNumberSum(0);
            proUnhandledIssuesDO.setDefectsNumberSum(0);
            proUnhandledIssuesDao.insert(proUnhandledIssuesDO);
        }else{
            proUnhandledIssuesDao.update(proUnhandledIssuesDO);
        }

    }

    @Override
    public void getEpicRelatedTasks(DemandBO demandBO) {
        //1.???????????????????????????epic
        DemandJiraDO demandJiraDO = demandJiraDao.get(demandBO.getReqInnerSeq());
        if (JudgeUtils.isNull(demandJiraDO)) {
            //?????????????????????????????????epic?????????epic
            jiraOperationService.createEpic(demandBO);
            demandJiraDO = demandJiraDao.get(demandBO.getReqInnerSeq());
        }
        //???????????????
        DemandJiraDevelopMasterTaskDO demandJiraDevelopMasterTaskDO = new DemandJiraDevelopMasterTaskDO();
        demandJiraDevelopMasterTaskDO.setRelevanceEpic(demandJiraDO.getJiraKey());
        List<DemandJiraDevelopMasterTaskDO> demandJiraDevelopMasterTaskDOS = demandJiraDevelopMasterTaskDao.find(demandJiraDevelopMasterTaskDO);
        //??????epic?????????????????????key
        LinkedList<String> JIRAKeys = new LinkedList<>();
        JIRAKeys.add(demandJiraDO.getJiraKey());
        demandJiraDevelopMasterTaskDOS.forEach(m -> JIRAKeys.add(m.getJiraKey()));

        //??????key??????????????????
        JIRAKeys.forEach(m -> {
            JiraTaskBodyBO jiraTaskBodyBO = registerJiraBasicInfo(m, null);
            List<JiraSubtasksBO> subtasks = JiraUtil.getSubtasks(jiraTaskBodyBO);
            if (JudgeUtils.isNotEmpty(subtasks)) {
                for (int i = 0; i < subtasks.size(); i++) {
                    JiraTaskBodyBO jiraTaskBodyBO1 = this.registerJiraBasicInfo(subtasks.get(i).getSubtaskkey(), jiraTaskBodyBO.getDepartment());
                    DemandJiraSubtaskDO demandJiraSubtaskDO = new DemandJiraSubtaskDO();
                    demandJiraSubtaskDO.setSubtaskkey(jiraTaskBodyBO1.getJiraKey());
                    demandJiraSubtaskDO.setAssignee(jiraTaskBodyBO1.getAssignee());
                    demandJiraSubtaskDO.setSubtaskname(jiraTaskBodyBO1.getIssueName());
                    demandJiraSubtaskDO.setParenttaskkey(jiraTaskBodyBO.getJiraKey());
                    DemandJiraSubtaskDO demandJiraSubtaskDO1 = demandJiraSubtaskDao.get(jiraTaskBodyBO1.getJiraKey());
                    if (JudgeUtils.isNotNull(demandJiraSubtaskDO1)) {
                        demandJiraSubtaskDao.update(demandJiraSubtaskDO);
                    } else {
                        demandJiraSubtaskDao.insert(demandJiraSubtaskDO);
                    }
                    this.registerWorklogs(jiraTaskBodyBO1);
                }
            }
            this.registerWorklogs(jiraTaskBodyBO);
        });
    }

    @Async
    public JiraTaskBodyBO registerJiraBasicInfo(JiraTaskBodyBO jiraTaskBodyBO) {
        JiraBasicInfoDO jiraBasicInfoDO = new JiraBasicInfoDO();
        jiraBasicInfoDO.setJirakey(jiraTaskBodyBO.getJiraKey());
        jiraBasicInfoDO.setAggregatetimespent(jiraTaskBodyBO.getAggregatetimespent());
        jiraBasicInfoDO.setTimespent(jiraTaskBodyBO.getTimespent());
        jiraBasicInfoDO.setAssignee(jiraTaskBodyBO.getAssignee());
        jiraBasicInfoDO.setCreator(jiraTaskBodyBO.getCreator());
        jiraBasicInfoDO.setJiratype(jiraTaskBodyBO.getJiraType());
        jiraBasicInfoDO.setDescription(jiraTaskBodyBO.getIssueName());
        jiraBasicInfoDO.setEpickey(jiraTaskBodyBO.getEpicKey());
        jiraBasicInfoDO.setParenttaskkey(jiraTaskBodyBO.getParentTaskKey());
        jiraBasicInfoDO.setTestCaseNumber(jiraTaskBodyBO.getTestCaseNumber());
        if (JudgeUtils.isBlank(jiraTaskBodyBO.getEpicKey())) {
            JiraBasicInfoDO jiraBasicInfoDO1 = new JiraBasicInfoDO();
            jiraBasicInfoDO1.setJirakey(jiraTaskBodyBO.getParentTaskKey());
            List<JiraBasicInfoDO> jiraBasicInfoDOS = jiraBasicInfoDao.find(jiraBasicInfoDO1);
            jiraBasicInfoDO.setEpickey(jiraBasicInfoDOS.get(0).getEpickey());
            jiraTaskBodyBO.setEpicKey(jiraBasicInfoDOS.get(0).getEpickey());
        }
        if (jiraTaskBodyBO.getJiraType().equals("Epic")) {
            jiraTaskBodyBO.setEpicCreator(jiraTaskBodyBO.getCreator());
        } else if (JudgeUtils.isNotBlank(jiraTaskBodyBO.getEpicKey())) {
            JiraBasicInfoDO jiraBasicInfoDO1 = new JiraBasicInfoDO();
            jiraBasicInfoDO1.setJirakey(jiraTaskBodyBO.getEpicKey());
            List<JiraBasicInfoDO> jiraBasicInfoDOS = jiraBasicInfoDao.find(jiraBasicInfoDO1);
            jiraTaskBodyBO.setEpicCreator(jiraBasicInfoDOS.get(0).getCreator());
            //????????????????????????????????????????????????????????????epic??????
            if(jiraTaskBodyBO.getJiraType().equals("???????????????")&&JudgeUtils.isNotBlank(jiraBasicInfoDO.getTestCaseNumber())){
                jiraBasicInfoDOS.get(0).setTestCaseNumber(jiraBasicInfoDO.getTestCaseNumber());
                jiraBasicInfoDao.update(jiraBasicInfoDOS.get(0));
            }
        }
        if (jiraTaskBodyBO.getJiraType().equals("???????????????")) {
            jiraBasicInfoDO.setDepartment("??????????????????");
        } else if (jiraTaskBodyBO.getJiraType().equals("???????????????")) {
            jiraBasicInfoDO.setDepartment("??????????????????");
        } else if (jiraTaskBodyBO.getJiraType().equals("???????????????")) {
            jiraBasicInfoDO.setDepartment(systemUserService.getDepartmentByUser(jiraTaskBodyBO.getAssignee()));
        } else {
            jiraBasicInfoDO.setDepartment(jiraTaskBodyBO.getDepartment());
        }
        jiraBasicInfoDO.setPlanstarttime(jiraTaskBodyBO.getPlanStartTime());
        jiraBasicInfoDO.setPlanendtime(jiraTaskBodyBO.getPlanEndTime());
        JiraBasicInfoDO jiraBasicInfoDO1 = jiraBasicInfoDao.get(jiraTaskBodyBO.getJiraKey());
        if (JudgeUtils.isNotNull(jiraBasicInfoDO1)) {
            jiraBasicInfoDao.update(jiraBasicInfoDO);
        } else {
            jiraBasicInfoDao.insert(jiraBasicInfoDO);
        }
        //????????????????????????????????????????????????????????????????????????????????????
        if (JudgeUtils.isNotBlank(jiraTaskBodyBO.getJiraType())) {
            if (jiraTaskBodyBO.getJiraType().equals("????????????")) {
                DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                defectDetailsDO.setJireKey(jiraTaskBodyBO.getJiraKey());
                defectDetailsDO.setEpicKey(jiraTaskBodyBO.getEpicKey());
                defectDetailsDO.setDefectType(jiraTaskBodyBO.getProblemType());
                defectDetailsDO.setDefectStatus(jiraTaskBodyBO.getStatus());
                //????????????
                defectDetailsDO.setSolution(jiraTaskBodyBO.getSolution());
                // ??????????????????????????????
                if(JudgeUtils.isNotBlank(jiraTaskBodyBO.getProblemHandler())){
                    defectDetailsDO.setAssignee(jiraTaskBodyBO.getProblemHandler());
                }else{
                    defectDetailsDO.setAssignee(jiraTaskBodyBO.getAssignee());
                }
                // ????????????????????????
                defectDetailsDO.setProblemHandler(jiraTaskBodyBO.getProblemHandlers());
                // ??????????????????????????????
                defectDetailsDO.setProblemHandlerDepartment(jiraTaskBodyBO.getProblemHandlerDepartment());
                // ?????????????????????
                defectDetailsDO.setDefectsDepartment(jiraTaskBodyBO.getDefectsDepartment());

                defectDetailsDO.setDefectRegistrant(jiraTaskBodyBO.getCreator());
                defectDetailsDO.setSecurityLevel(jiraTaskBodyBO.getSecurityLevel());

                // ???????????????????????????????????????
//                if(JudgeUtils.isNotBlank(jiraTaskBodyBO.getDefectsDepartment())){
//                    //?????????????????????????????????????????????????????????????????????
//                    if("??????????????????".equals(jiraTaskBodyBO.getDefectsDepartment())){
                        // ???????????????????????????????????????????????? ?????????????????????
                        if(JudgeUtils.isNotBlank(defectDetailsDO.getAssignee())){
                            UserDO userDO = new UserDO();
                            userDO.setFullname(defectDetailsDO.getAssignee());
                            List<UserDO> userDOS = iUserDao.find(userDO);
                            if(!userDOS.isEmpty()){
                                defectDetailsDO.setProblemHandlerDepartment(userDOS.get(0).getDepartment());
                            }
//                            else{
//                                // ?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
//                                if(JudgeUtils.isNotBlank(jiraTaskBodyBO.getDefectsDepartment())){
//                                    if("??????????????????".equals(jiraTaskBodyBO.getDefectsDepartment())){
//                                        defectDetailsDO.setProblemHandlerDepartment("??????????????????");
//                                    }else{
//                                        defectDetailsDO.setProblemHandlerDepartment(jiraTaskBodyBO.getDefectsDepartment());
//                                    }
//                                }
//                            }
                        }
//                    }
//                }
                // ??????????????????
                if(JudgeUtils.isNotBlank(defectDetailsDO.getProblemHandlerDepartment())){
                    OrganizationStructureDO organizationStructureDO = new OrganizationStructureDO();
                    organizationStructureDO.setSecondlevelorganization(defectDetailsDO.getProblemHandlerDepartment());
                    List<OrganizationStructureDO> organizationStructureDOList =  iOrganizationStructureDao.find(organizationStructureDO);
                    if (organizationStructureDOList!=null && organizationStructureDOList.size()>0){
                        // ??????????????????
                        defectDetailsDO.setFirstlevelorganization(organizationStructureDOList.get(0).getFirstlevelorganization());
                    }
                }
                defectDetailsDO.setRegistrationDate(jiraTaskBodyBO.getCreateTime());
                defectDetailsDO.setDefectDetails(jiraTaskBodyBO.getDefectDetails());
                defectDetailsDO.setTestNumber(jiraTaskBodyBO.getRetestTimes());
                defectDetailsDO.setDefectName(jiraTaskBodyBO.getDefectName());
                //?????????????????????  ????????????
                if(JudgeUtils.isNotBlank(defectDetailsDO.getEpicKey())){
                    DemandJiraDO demandJiraDO = new DemandJiraDO();
                    demandJiraDO.setJiraKey(defectDetailsDO.getEpicKey());
                    // ??????epicKey??????????????????
                    List<DemandJiraDO> demandJiraDos = demandJiraDao.find(demandJiraDO);
                    if(JudgeUtils.isNotEmpty(demandJiraDos)){
                        DemandDO demandDO = demandDao.get(demandJiraDos.get(demandJiraDos.size()-1).getReqInnerSeq());
                        //?????????
                        defectDetailsDO.setProductLine(demandDO.getReqPrdLine());
                        //??????????????????
                        defectDetailsDO.setReqNo(demandDO.getReqNo());
                    }
                }

                DefectDetailsDO defectDetailsDO1 = defectDetailsDao.get(defectDetailsDO.getJireKey());
                if (JudgeUtils.isNull(defectDetailsDO1)) {
                    defectDetailsDao.insert(defectDetailsDO);
                } else {
                    defectDetailsDao.update(defectDetailsDO);
                }
            } else if (jiraTaskBodyBO.getJiraType().equals("????????????")) {
                IssueDetailsDO issueDetailsDO = new IssueDetailsDO();
                issueDetailsDO.setJireKey(jiraTaskBodyBO.getJiraKey());
                issueDetailsDO.setEpicKey(jiraTaskBodyBO.getEpicKey());
                issueDetailsDO.setIssueType(jiraTaskBodyBO.getReviewQuestionType());
                issueDetailsDO.setIssueStatus(jiraTaskBodyBO.getStatus());
                issueDetailsDO.setAssignee(jiraTaskBodyBO.getAssignee());
                issueDetailsDO.setIssueDetails(jiraTaskBodyBO.getIssueName());
                issueDetailsDO.setRegistrationDate(jiraTaskBodyBO.getCreateTime());
                issueDetailsDO.setIssueRegistrant(jiraTaskBodyBO.getCreator());
                issueDetailsDO.setIssueDepartment(systemUserService.getDepartmentByUser(jiraTaskBodyBO.getAssignee()));
                IssueDetailsDO issueDetailsDO1 = issueDetailsDao.get(issueDetailsDO.getJireKey());
                if (JudgeUtils.isNull(issueDetailsDO1)) {
                    issueDetailsDao.insert(issueDetailsDO);
                } else {
                    issueDetailsDao.update(issueDetailsDO);
                }
            }else if (jiraTaskBodyBO.getJiraType().equals("????????????")) {
                ProductionIssueDetailsDO issueDetailsDO = new ProductionIssueDetailsDO();
                //jira??????
                issueDetailsDO.setJiraKey(jiraTaskBodyBO.getJiraKey());
                // ??????????????????
                issueDetailsDO.setProductionIssueStatus(jiraTaskBodyBO.getStatus());
                // ?????????
                issueDetailsDO.setAssignee(jiraTaskBodyBO.getAssignee());
                // ???????????? ????????????
                issueDetailsDO.setProductionIssueDetails(jiraTaskBodyBO.getIssueName());
                // ????????????
                issueDetailsDO.setRegistrationDate(jiraTaskBodyBO.getCreateTime());
                // ?????????????????????
                issueDetailsDO.setProductionIssueRegistrant(jiraTaskBodyBO.getProductionIssueRegistrant());
                // ????????????
                issueDetailsDO.setProNumber(jiraTaskBodyBO.getProNumber());
                //????????????
                issueDetailsDO.setProductionIssueDepartment(systemUserService.getDepartmentByUser(jiraTaskBodyBO.getAssignee()));
                ProductionIssueDetailsDO issueDetailsDO1 = productionIssueDetailsDao.get(issueDetailsDO.getJiraKey());
                if (JudgeUtils.isNull(issueDetailsDO1)) {
                    productionIssueDetailsDao.insert(issueDetailsDO);
                } else {
                    productionIssueDetailsDao.update(issueDetailsDO);
                }
                // ?????????????????????jira??????
                ProductionFollowDO productionFollowDO = new ProductionFollowDO();
                productionFollowDO.setIssuekey(jiraTaskBodyBO.getJiraKey());
                List<ProductionFollowDO> list  = productionFollowDao.find(productionFollowDO);
                if(JudgeUtils.isNotEmpty(list)){
                    // ????????????
                    productionFollowDO = list.get(0);
                    productionFollowDO.setIssueStatus(jiraTaskBodyBO.getStatus());
                    productionFollowDao.update(productionFollowDO);
                }

            }
        }


        return jiraTaskBodyBO;
    }


    private JiraTaskBodyBO registerJiraBasicInfo(String m, String department) {
        JiraTaskBodyBO jiraTaskBodyBO = JiraUtil.GetIssue(m);
        JiraBasicInfoDO jiraBasicInfoDO = new JiraBasicInfoDO();
        jiraBasicInfoDO.setJirakey(jiraTaskBodyBO.getJiraKey());
        jiraBasicInfoDO.setAggregatetimespent(jiraTaskBodyBO.getAggregatetimespent());
        jiraBasicInfoDO.setTimespent(jiraTaskBodyBO.getTimespent());
        jiraBasicInfoDO.setAssignee(jiraTaskBodyBO.getAssignee());
        jiraBasicInfoDO.setJiratype(jiraTaskBodyBO.getJiraType());
        jiraBasicInfoDO.setDescription(jiraTaskBodyBO.getIssueName());
        if (jiraTaskBodyBO.getJiraType().equals("???????????????")) {
            jiraBasicInfoDO.setDepartment("??????????????????");
        } else if (jiraTaskBodyBO.getJiraType().equals("???????????????")) {
            jiraBasicInfoDO.setDepartment("??????????????????");
        } else if (jiraTaskBodyBO.getJiraType().equals("???????????????")) {
            jiraBasicInfoDO.setDepartment(department);
        } else {
            jiraBasicInfoDO.setDepartment(jiraTaskBodyBO.getDepartment());
        }
        jiraBasicInfoDO.setPlanstarttime(jiraTaskBodyBO.getPlanStartTime());
        jiraBasicInfoDO.setPlanendtime(jiraTaskBodyBO.getPlanEndTime());
        JiraBasicInfoDO jiraBasicInfoDO1 = jiraBasicInfoDao.get(jiraTaskBodyBO.getJiraKey());
        if (JudgeUtils.isNotNull(jiraBasicInfoDO1)) {
            jiraBasicInfoDao.update(jiraBasicInfoDO);
        } else {
            jiraBasicInfoDao.insert(jiraBasicInfoDO);
        }


        return jiraTaskBodyBO;
    }

    @Async
    void registerWorklogs(JiraTaskBodyBO jiraTaskBodyBO) {
        List<JiraWorklogBO> worklogs = JiraUtil.getWorklogs(jiraTaskBodyBO);
        for (int i = 0; i < worklogs.size(); i++) {
            if(!worklogs.get(i).getActive()){
                continue;
            }
            JiraWorklogDO jiraWorklogDO = new JiraWorklogDO();
            jiraWorklogDO.setJiraworklogkey(worklogs.get(i).getJiraWorklogKey());
            jiraWorklogDO.setIssuekey(jiraTaskBodyBO.getJiraKey());
            jiraWorklogDO.setName(worklogs.get(i).getName());
            jiraWorklogDO.setDisplayname(worklogs.get(i).getDisplayname());
            jiraWorklogDO.setComment(worklogs.get(i).getComment());
            jiraWorklogDO.setCreatedtime(worklogs.get(i).getCreatedtime());
            jiraWorklogDO.setUpdatedtime(worklogs.get(i).getUpdatedtime());
            jiraWorklogDO.setStartedtime(worklogs.get(i).getStartedtime());
            jiraWorklogDO.setTimespnet(worklogs.get(i).getTimespnet());
            JiraWorklogDO jiraWorklogDO1 = jiraWorklogDao.get(worklogs.get(i).getJiraWorklogKey());
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            String selectTime = DateUtil.date2String(new Date(), "yyyy-MM-dd");
            String week = DateUtil.testDate(selectTime);
            //?????????????????????????????? ???????????????t_jira_worklog????????????????????????
            if (JudgeUtils.isNotNull(jiraWorklogDO1)) {
                jiraWorklogDao.update(jiraWorklogDO);
            } else {
                jiraWorklogDao.insert(jiraWorklogDO);
            }
            String date1 = StringUtils.substring(LocalDateTime.now().toString().trim(), 0, 10);
            String date2 = StringUtils.substring(jiraWorklogDO.getUpdatedtime().trim(), 0, 10);

            boolean flag = true;
            //?????????????????????????????????????????????????????????,??????????????????????????????
            if(week.equals("?????????")){
                int betweenDate = 0;
                try {
                    Date d1 = sdf.parse(StringUtils.substring(jiraWorklogDO.getCreatedtime().trim(), 0, 10));
                    //????????????????????????
                    Date d2 = sdf.parse(StringUtils.substring(jiraWorklogDO.getStartedtime().trim(), 0, 10));
                    betweenDate = (int) (d1.getTime() - d2.getTime()) / (60 * 60 * 24 * 1000);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //????????????????????????????????????????????????????????????????????????
                if (betweenDate > 4 || betweenDate < 0){
                    flag = false;
                }
            }else if(week.equals("?????????")){
                int betweenDate = 0;
                try {
                    Date d1 = sdf.parse(StringUtils.substring(jiraWorklogDO.getCreatedtime().trim(), 0, 10));
                    //????????????????????????
                    Date d2 = sdf.parse(StringUtils.substring(jiraWorklogDO.getStartedtime().trim(), 0, 10));
                    betweenDate = (int) (d1.getTime() - d2.getTime()) / (60 * 60 * 24 * 1000);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //??????????????????????????????????????????????????????2???????????????
                if (betweenDate > 2 || betweenDate < 0){
                    flag = false;
                }
            }else if(week.equals("?????????")){
                int betweenDate = 0;
                try {
                    Date d1 = sdf.parse(StringUtils.substring(jiraWorklogDO.getCreatedtime().trim(), 0, 10));
                    //????????????????????????
                    Date d2 = sdf.parse(StringUtils.substring(jiraWorklogDO.getStartedtime().trim(), 0, 10));
                    betweenDate = (int) (d1.getTime() - d2.getTime()) / (60 * 60 * 24 * 1000);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //??????????????????????????????????????????????????????3???????????????
                if (betweenDate > 3 || betweenDate < 0){
                    flag = false;
                }
            }else{
                int betweenDate = 0;
                try {
                    Date d1 = sdf.parse(StringUtils.substring(jiraWorklogDO.getCreatedtime().trim(), 0, 10));
                    Date d2 = sdf.parse(StringUtils.substring(jiraWorklogDO.getStartedtime().trim(), 0, 10));
                    betweenDate = (int) (d1.getTime() - d2.getTime()) / (60 * 60 * 24 * 1000);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                //?????????  ?????????????????????????????????????????????1???????????????,??????????????????????????????
                if (betweenDate > 1 || betweenDate < 0) {
                    flag = false;
                }
            }
            System.err.println(jiraWorklogDO.getJiraworklogkey() +"===="+jiraWorklogDO.getStartedtime()+flag);
            // ?????????????????????????????????  t_working_hours ????????????????????????????????????
            if (flag) {
                WorkingHoursDO workingHoursDO = new WorkingHoursDO();
                workingHoursDO.setJiraworklogkey(jiraWorklogDO.getJiraworklogkey());
                //?????????
                workingHoursDO.setSubtaskname(jiraTaskBodyBO.getIssueName());
                workingHoursDO.setIssuekey(jiraWorklogDO.getIssuekey());
                workingHoursDO.setName(jiraWorklogDO.getName());

                workingHoursDO.setDisplayname(jiraWorklogDO.getDisplayname());
                workingHoursDO.setTimespnet(jiraWorklogDO.getTimespnet());
                if (StringUtils.isNotBlank(jiraTaskBodyBO.getDepartment())) {
                    workingHoursDO.setDevpLeadDept(jiraTaskBodyBO.getDepartment());
                } else {
                    UserDO userDO = userExtDao.getUserByUserFullName(jiraWorklogDO.getDisplayname());
                    workingHoursDO.setDevpLeadDept(userDO.getDepartment());
                }
                workingHoursDO.setComment(jiraWorklogDO.getComment());
                //?????????????????????   ???????????????????????????
                if(JudgeUtils.isNotBlank(jiraWorklogDO.getComment())){
                    if(jiraTaskBodyBO.getJiraType().equals("???????????????")){
                        //?????????????????????????????? ???????????????????????? ????????? ????????????????????????????????????????????????
                        if(jiraTaskBodyBO.getIssueName().indexOf("????????????????????????")!=-1){
                            String[] split = jiraWorklogDO.getComment().split("#");
                            Pattern pattern = compile("^[-\\+]?[\\d]*$");
                            for(int j=0;j<split.length-1;j++) {
                                if (split[j].equals("?????????????????????") && pattern.matcher(split[j+1]).matches()) {
                                    workingHoursDO.setCaseWritingNumber(Integer.parseInt(split[2]));
                                }
                            }
                        }
                        //?????????????????????????????? ???????????????????????? ????????? ????????????????????????????????????????????????????????????????????????
                        if(jiraTaskBodyBO.getIssueName().indexOf("????????????????????????")!=-1){
                            String[] split = jiraWorklogDO.getComment().split("#");
                            Pattern pattern = compile("^[-\\+]?[\\d]*$");
                            for(int j=0;j<split.length-1;j++){
                                if(split[j].equals("?????????????????????")&&pattern.matcher(split[j+1]).matches()){
                                    workingHoursDO.setCaseExecutionNumber(Integer.parseInt(split[j+1]));
                                }
                                if(split[j].equals("?????????????????????")&&pattern.matcher(split[j+1]).matches()){
                                    workingHoursDO.setCaseCompletedNumber(Integer.parseInt(split[j+1]));
                                }
                            }
                        }
                    }

                }

                workingHoursDO.setCreatedtime(jiraWorklogDO.getCreatedtime());
                workingHoursDO.setStartedtime(jiraWorklogDO.getStartedtime());
                workingHoursDO.setUpdatedtime(jiraWorklogDO.getUpdatedtime());
                workingHoursDO.setEpickey(jiraTaskBodyBO.getEpicKey());
                workingHoursDO.setEpiccreator(jiraTaskBodyBO.getEpicCreator());
                workingHoursDO.setRegisterflag("N");
                UserInfoBO userbyLoginName = systemUserService.getUserbyLoginName(jiraWorklogDO.getName());
                workingHoursDO.setAssignmentDepartment(userbyLoginName.getDepartment());
                WorkingHoursDO workingHoursDO1 = iWorkingHoursDao.get(workingHoursDO.getJiraworklogkey());
                UserRoleDO userRoleDO = new UserRoleDO();
                userRoleDO.setUserNo(userbyLoginName.getUserNo());
                //??????
                List<UserRoleDO> userRoleDOS = new LinkedList<>();

                if (workingHoursDO.getDevpLeadDept().equals("??????????????????")) {
                    workingHoursDO.setRoletype("????????????");
                } else {
                    userRoleDO.setRoleId((long) 5002);
                    userRoleDOS = userRoleExtDao.find(userRoleDO);

                    if (JudgeUtils.isNotEmpty(userRoleDOS)) {
                        workingHoursDO.setRoletype("????????????");
                    } else {
                        workingHoursDO.setRoletype("????????????");
                    }
                }
                if (JudgeUtils.isNotNull(workingHoursDO1)) {
                    if (JudgeUtils.isNotBlank(workingHoursDO1.getRegisterflag()) && workingHoursDO1.getRegisterflag().equals("Y")) {
                        //  todo ???????????????????????????????????????
                    }
                    iWorkingHoursDao.update(workingHoursDO);
                } else {
                    iWorkingHoursDao.insert(workingHoursDO);
                }
            }
        }

    }

}
