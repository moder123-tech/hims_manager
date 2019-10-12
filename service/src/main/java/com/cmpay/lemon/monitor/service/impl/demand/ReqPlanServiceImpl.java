package com.cmpay.lemon.monitor.service.impl.demand;

import cn.afterturn.easypoi.excel.entity.TemplateExportParams;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.BeanUtils;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.common.utils.StringUtils;
import com.cmpay.lemon.framework.page.PageInfo;
import com.cmpay.lemon.framework.security.SecurityUtils;
import com.cmpay.lemon.framework.utils.PageUtils;
import com.cmpay.lemon.monitor.bo.DemandBO;
import com.cmpay.lemon.monitor.bo.DemandRspBO;
import com.cmpay.lemon.monitor.bo.ProjectStartBO;
import com.cmpay.lemon.monitor.dao.*;
import com.cmpay.lemon.monitor.entity.Constant;
import com.cmpay.lemon.monitor.entity.*;
import com.cmpay.lemon.monitor.entity.sendemail.MailFlowDO;
import com.cmpay.lemon.monitor.entity.sendemail.MailGroupDO;
import com.cmpay.lemon.monitor.entity.sendemail.MailSenderInfo;
import com.cmpay.lemon.monitor.entity.sendemail.SimpleMailSender;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import com.cmpay.lemon.monitor.service.demand.ReqPlanService;
import com.cmpay.lemon.monitor.service.demand.ReqTaskService;
import com.cmpay.lemon.monitor.service.dic.DictionaryService;
import com.cmpay.lemon.monitor.utils.*;
import org.apache.commons.lang.time.DateUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.SVNWCUtil;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 需求计划
 * @author: ty
 */
@Service
public class ReqPlanServiceImpl implements ReqPlanService {
    //30 需求状态为暂停
    private static final String REQSUSPEND ="30";
    //40 需求状态为取消
    private static final String REQCANCEL ="40";
    // 30 需求定稿
    private static final int REQCONFIRM = 30;
    // 50 技术方案定稿
    private static final int TECHDOCCONFIRM = 50;
    // 70 测试用例定稿
    private static final int TESTCASECONFIRM = 70;
    // 110 完成SIT测试
    private static final int FINISHSITTEST = 110;
    // 120 UAT版本更新
    private static final int UPDATEUAT = 120;
    // 140 完成UAT测试
    private static final int FINISHUATTEST = 140;
    // 160 完成预投产
    private static final int FINISHPRETEST = 160;
    // 180 完成产品发布
    private static final int FINISHPRD = 180;

    private static final Logger LOGGER = LoggerFactory.getLogger(ReqPlanServiceImpl.class);
    @Autowired
    private IDemandExtDao demandDao;
    @Autowired
    private IPlanDao planDao;
    @Autowired
    private IOperationProductionDao operationProductionDao;
    @Autowired
    private DictionaryService dictionaryService;
    @Autowired
    private IDemandJiraDao demandJiraDao;
    @Autowired
    private IDemandStateHistoryDao demandStateHistoryDao;
    /**
     * 自注入,解决getAppsByName中调用findAll的缓存不生效问题
     */
    @Autowired
    private ReqPlanService reqPlanService;
    @Autowired
    private ReqTaskService reqTaskService;


    @Override
    public DemandBO findById(String req_inner_seq) {
        DemandDO demandDO = demandDao.get(req_inner_seq);
        if (JudgeUtils.isNull(demandDO)) {
            LOGGER.warn("id为[{}]的记录不存在", req_inner_seq);
            BusinessException.throwBusinessException(MsgEnum.DB_FIND_FAILED);
        }
        return BeanUtils.copyPropertiesReturnDest(new DemandBO(), demandDO);
    }
    @Override
    public DemandRspBO findDemand(DemandBO demandBO) {
        String time= DateUtil.date2String(new Date(), "yyyy-MM-dd");
        System.err.println("需求月份"+demandBO.getReqImplMon());
        PageInfo<DemandBO> pageInfo = getPageInfo(demandBO);
        List<DemandBO> demandBOList = BeanConvertUtils.convertList(pageInfo.getList(), DemandBO.class);

        for (int i = 0; i < demandBOList.size(); i++) {
            String reqAbnorType = demandBOList.get(i).getReqAbnorType();
            String reqAbnorTypeAll = "";
            DemandBO demand = reqTaskService.findById(demandBOList.get(i).getReqInnerSeq());

            //当需求定稿时间、uat更新时间、测试完成时间、需求当前阶段、需求状态都不为空的时候，执行进度实时显示逻辑。
            if (StringUtils.isNotBlank(demand.getPrdFinshTm()) && StringUtils.isNotBlank(demand.getUatUpdateTm())
                    && StringUtils.isNotBlank(demand.getTestFinshTm()) && StringUtils.isNotBlank(demand.getPreCurPeriod())
                    && StringUtils.isNotBlank(demand.getReqSts())) {
                //当前时间大于预计时间，并且所处阶段小于30,并且需求状态不为暂停或取消（30，40）,则该需求进度异常
                if (time.compareTo(demand.getPrdFinshTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) < REQCONFIRM
                        && REQSUSPEND.compareTo(demand.getReqSts()) != 0 && REQCANCEL.compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "需求进度滞后,";
                }
                if (time.compareTo(demand.getUatUpdateTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) >= REQCONFIRM
                        && Integer.parseInt(demand.getPreCurPeriod()) < UPDATEUAT && REQSUSPEND.compareTo(demand.getReqSts()) != 0
                        && REQCANCEL.compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "开发进度滞后,";
                }
                if (time.compareTo(demand.getTestFinshTm()) > 0 && Integer.parseInt(demand.getPreCurPeriod()) >= UPDATEUAT
                        && Integer.parseInt(demand.getPreCurPeriod()) < FINISHUATTEST && REQSUSPEND.compareTo(demand.getReqSts()) != 0
                        && REQCANCEL.compareTo(demand.getReqSts()) != 0) {
                    reqAbnorTypeAll += "测试进度滞后";
                }
                if (StringUtils.isBlank(reqAbnorTypeAll)) {
                    reqAbnorTypeAll += "正常";
                }
            } else if (reqAbnorType.indexOf("01") != -1) {
                demandBOList.get(i).setReqAbnorType("正常");
                continue;
            } else {
                if (reqAbnorType.indexOf("03") != -1) {
                    reqAbnorTypeAll += "需求进度滞后,";
                }
                if (reqAbnorType.indexOf("04") != -1) {
                    reqAbnorTypeAll += "开发进度滞后,";
                }
                if (reqAbnorType.indexOf("05") != -1) {
                    reqAbnorTypeAll += "测试进度滞后";
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

    private PageInfo<DemandBO>  getPageInfo(DemandBO demandBO) {
        DemandDO demandDO = new DemandDO();
        BeanConvertUtils.convert(demandDO, demandBO);
        PageInfo<DemandBO> pageInfo = PageUtils.pageQueryWithCount(demandBO.getPageNum(), demandBO.getPageSize(),
                () -> BeanConvertUtils.convertList(demandDao.find(demandDO), DemandBO.class));
        return pageInfo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void add(DemandBO demandBO) {
        try {
            if (!"30".equals(demandBO.getReqSts()) && !"40".equals(demandBO.getReqSts())) {
                //修改需求状态
                if ("10".equals(demandBO.getPreCurPeriod())) {
                    //提出
                    demandBO.setReqSts("10");
                } else if ("180".equals(demandBO.getPreCurPeriod())) {
                    //完成
                    demandBO.setReqSts("50");
                } else {
                    //进行中
                    demandBO.setReqSts("20");
                }
            }
            demandDao.insert(BeanUtils.copyPropertiesReturnDest(new DemandDO(), demandBO));
        } catch (Exception e) {
            BusinessException.throwBusinessException(MsgEnum.DB_INSERT_FAILED);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void delete(String req_inner_seq) {
        try {
            demandDao.delete(req_inner_seq);
            demandJiraDao.delete(req_inner_seq);
        } catch (Exception e) {
            BusinessException.throwBusinessException(MsgEnum.DB_DELETE_FAILED);
        }
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void update(DemandBO demandBO) {
        try {
            if (!"30".equals(demandBO.getReqSts()) && !"40".equals(demandBO.getReqSts())) {
                //修改需求状态
                if ("10".equals(demandBO.getPreCurPeriod())) {
                    //提出
                    demandBO.setReqSts("10");
                } else if ("180".equals(demandBO.getPreCurPeriod())) {
                    //完成
                    demandBO.setReqSts("50");
                } else {
                    //进行中
                    demandBO.setReqSts("20");
                }
            }
            demandDao.update(BeanUtils.copyPropertiesReturnDest(new DemandDO(), demandBO));
        } catch (Exception e) {
            BusinessException.throwBusinessException(MsgEnum.DB_UPDATE_FAILED);
        }
    }

    @Override
    public List<DemandBO> findAll() {
        return BeanConvertUtils.convertList(demandDao.find(new DemandDO()), DemandBO.class);
    }

    /**
     * 根据内部编号查询项目启动信息
     */
    @Override
    public  ProjectStartBO goProjectStart(String reqInnerSeq){
        System.out.println("内部编号："+reqInnerSeq);
        DemandDO demandDO = demandDao.get(reqInnerSeq);
        ProjectStartDO projectStartDO = new ProjectStartDO();
        projectStartDO.setReqNm(demandDO.getReqNm());
        projectStartDO.setReqNo(demandDO.getReqNo());
        projectStartDO.setReqInnerSeq(demandDO.getReqInnerSeq());
        Map<String, String> resMap = new HashMap<>();
        resMap = reqPlanService.getMailbox(reqInnerSeq);
        System.out.println("邮箱信息resMap："+resMap);
        String proMemberEmail = resMap.get("proMemberEmail");
        String testDevpEmail = resMap.get("testDevpEmail");
        String devpEmail =  resMap.get("devpEmail");
        String jdEmail = resMap.get("jdEmail");
        if (StringUtils.isNotBlank(proMemberEmail)){
            projectStartDO.setSendTo(proMemberEmail);
        }else{
            projectStartDO.setSendTo("");
        }
        if (StringUtils.isNotBlank(testDevpEmail)){
            projectStartDO.setCopyTo(testDevpEmail+devpEmail);
        }else{
            projectStartDO.setCopyTo("");
        }
        System.out.println("projectStartDO："+projectStartDO);
        return BeanUtils.copyPropertiesReturnDest(new ProjectStartBO(), projectStartDO);
    }

    /**
     * 获取邮箱
     */
    @Override
    public Map<String,String> getMailbox(String reqInnerSeq){
        Map<String, String> resMap = new HashMap<>();
        DemandDO demandDO = new DemandDO();
        //发送的邮箱
        String proMemberEmail = "";
        //抄送的邮箱
        String testDevpEmail = "";
        //基地所有邮箱
        String jdEmail = "";
        //所有部门主管邮箱
        String devpEmail = "";
        //根据内部编号查询所有相关人员
        DemandDO bean = demandDao.get(reqInnerSeq);
        //后台开发工程师
        String[] devp_eng = StringUtils.isBlank(bean.getDevpEng()) ? null : bean.getDevpEng().split("、");
        String devp_eng_email = "";
        //前端开发工程师
        String[] front_eng = StringUtils.isBlank(bean.getFrontEng()) ? null : bean.getFrontEng().split("、");
        String front_eng_email = "";
        //测试工程师
        String[] test_eng = StringUtils.isBlank(bean.getTestEng()) ? null : bean.getTestEng().split("、");
        String test_eng_email = "";
        //配置人员
        String[] config_mng = StringUtils.isBlank(bean.getConfigMng()) ? null : bean.getConfigMng().split("、");
        String config_mng_email = "";
        //查询项目成员邮箱(后台开发工程师、项目经理、产品经理、前端开发工程师、测试工程师、配置人员、QA人员)
        if (devp_eng != null) {
            if (devp_eng.length >= 2) {
                for (int i = 0; i < devp_eng.length; i++) {
                    demandDO = planDao.searchUserEmail(devp_eng[i]);
                    if (demandDO != null) {
                        devp_eng_email += demandDO.getMonRemark() + ";";
                    }
                }
            } else {
                demandDO = planDao.searchUserEmail(bean.getDevpEng());
                if (demandDO != null) {
                    devp_eng_email += demandDO.getMonRemark() + ";";
                }
            }
        }

        if (front_eng != null) {
            if (front_eng.length >= 2) {
                for (int i = 0; i < front_eng.length; i++) {
                    demandDO = planDao.searchUserEmail(front_eng[i]);
                    if (demandDO != null) {
                        front_eng_email += demandDO.getMonRemark() + ";";
                    }
                }
            } else {
                demandDO = planDao.searchUserEmail(bean.getFrontEng());
                if (demandDO != null) {
                    front_eng_email += demandDO.getMonRemark() + ";";
                }
            }
        }

        if (test_eng != null) {
            if (test_eng.length >= 2) {
                for (int i = 0; i < test_eng.length; i++) {
                    demandDO = planDao.searchUserEmail(test_eng[i]);
                    if (demandDO != null) {
                        test_eng_email += demandDO.getMonRemark() + ";";
                    }
                }
            } else {
                demandDO = planDao.searchUserEmail(bean.getTestEng());
                if (demandDO != null) {
                    test_eng_email += demandDO.getMonRemark() + ";";
                }
            }
        }

        if (config_mng != null) {
            if (config_mng.length >= 2) {
                for (int i = 0; i < config_mng.length; i++) {
                    demandDO = planDao.searchUserEmail(config_mng[i]);
                    if (demandDO != null) {
                        config_mng_email += demandDO.getMonRemark() + ";";
                    }
                }
            } else {
                demandDO = planDao.searchUserEmail(bean.getConfigMng());
                if (demandDO != null) {
                    config_mng_email += demandDO.getMonRemark() + ";";
                }
            }
        }
        demandDO = planDao.searchOtherUserEmail(reqInnerSeq);
        if (demandDO != null) {
            String proMemberEmailAll = devp_eng_email + front_eng_email + test_eng_email + config_mng_email + demandDO.getMonRemark() + ";";
            String[] proMemberEmailSplit = proMemberEmailAll.split(";");
            //去除重复的字符串
            List<String> list = new ArrayList<>();
            for (int i = 0; i < proMemberEmailSplit.length; i++) {
                if (!list.contains(proMemberEmailSplit[i])) {
                    list.add(proMemberEmailSplit[i]);
                }
            }
            for (int i = 0; i < list.size(); i++) {
                proMemberEmail += list.get(i) + ";";
            }
        }
        //查询基地邮箱(需求提出人，需求负责人,开发负责人)
        demandDO = planDao.findBaseChargeEmailByName(reqInnerSeq);
        if (demandDO != null) {
            jdEmail = demandDO.getMonRemark() + ";";
        }
        //查询部门邮箱(主导部门和配合部门，去除配合部门测试部)
        demandDO = planDao.findDevpEmail(reqInnerSeq);
        if (demandDO != null) {
            devpEmail = demandDO.getMonRemark() + ";";
        }
        //测试部邮箱(田群、吴暇、谭杰、张勇、左娟)
        MailGroupDO mailBean = operationProductionDao.findMailGroupBeanDetail("4");
        testDevpEmail += mailBean.getMail_user();
        resMap.put("proMemberEmail", proMemberEmail);
        resMap.put("testDevpEmail", testDevpEmail);
        resMap.put("devpEmail", devpEmail);
        resMap.put("jdEmail", jdEmail);
        return resMap;
    }
    /**
     * 项目启动
     *
     * @param
     */
    @Override
    public void projectStart(ProjectStartBO projectStartBO, HttpServletRequest request){
        // 项目启动邮件
        String sendTo = projectStartBO.getSendTo();
        String copyTo = projectStartBO.getCopyTo();
        if (StringUtils.isBlank(sendTo)) {
            //"项目启动失败，收件人必填，多个“;”分割!"
            BusinessException.throwBusinessException(MsgEnum.ERROR_SENDT0_ISBLANK);
        }
        String req_inner_seq = projectStartBO.getReqInnerSeq();
        DemandDO reqPlan = demandDao.get(req_inner_seq);
        DemandDO bean = new DemandDO();
        //String currentUser =  SecurityUtils.getLoginName();
        if (null == reqPlan) {
            //"项目启动失败，找不到该需求对应信息!"
            BusinessException.throwBusinessException(MsgEnum.ERROR_PLAN_NULL);
        } else {
            //将页面的需求编码值set进去，并更新
            bean.setReqInnerSeq(reqPlan.getReqInnerSeq());
            bean.setReqNo(projectStartBO.getReqNo());
            demandDao.update(bean);
            reqPlan.setReqNo(projectStartBO.getReqNo());

            if(StringUtils.isBlank(reqPlan.getReqNo())||StringUtils.isBlank(reqPlan.getReqNm())){
                //"项目启动失败，需求编号和需求名称不能为空!"
                BusinessException.throwBusinessException(MsgEnum.ERROR_REQNO_REQNM_ISBLANK);
            }
            String projectMng = reqPlan.getProjectMng();
//            if (!currentUser.equals(projectMng)) {
//                //"项目启动失败，只能有项目经理进行项目启动"
//                BusinessException.throwBusinessException(MsgEnum.ERROR_NOT_PROJECTMNG);
//            }
        }
        // 判断必填内容不为空 时间人员配置
        if (notFinishInfo(reqPlan)) {
            //"项目启动失败:人员或时间配置不完善"
            BusinessException.throwBusinessException(MsgEnum.ERROR_NOT_FINISHINFO);
        }
        try {
            // 建立svn项目
            String message = bulidSvnProjrct(reqPlan, request);
            if (StringUtils.isNotBlank(message)) {
                //return ajaxDoneError("项目启动失败:" + message);
                BusinessException.throwBusinessException(MsgEnum.ERROR_NOT_SVN+message);
            }
            //文档建立成功后，更新字段 is_svn_build
            bean.setIsSvnBuild("是");
            demandDao.update(bean);
        } catch (Exception e1) {
            // "项目启动失败，SVN项目建立失败：" + e1.getMessage()
            BusinessException.throwBusinessException(MsgEnum.ERROR_NOT_SVNBULID+ e1.getMessage());
        }
        try {
            String subject = "【项目启动】" + reqPlan.getReqNo() + "_" + reqPlan.getReqNm() + "_"
                    + reqPlan.getProjectMng();
            String content = genProjectStartContent(reqPlan);
            String message = reqPlanService.sendMail(sendTo, copyTo, content, subject, null);
            if (StringUtils.isNotBlank(message)) {
                //return ajaxDoneError("项目启动失败,SVN项目建立成功，启动邮件发送失败:" + message);
                BusinessException.throwBusinessException(MsgEnum.ERROR_MAIL_FAIL+message);
            }
            //启动成功后记录时间
            reqPlan.setProjectStartTm(DateUtil.date2String(new Date(), "yyyy-MM-dd"));
            demandDao.update(reqPlan);
        } catch (Exception e) {
            e.printStackTrace();
            //return ajaxDoneError("项目启动失败，SVN项目建立成功，启动邮件发送失败：" + e.getMessage());
            BusinessException.throwBusinessException(MsgEnum.ERROR_MAIL_FAIL+e.getMessage());
        }
    }

    /**
     * 判断必填内容不为空 时间人员配置
     * @param reqPlan
     * @return
     */
    private boolean notFinishInfo(DemandDO reqPlan) {
        return StringUtils.isEmpty(reqPlan.getProjectMng()) || StringUtils.isEmpty(reqPlan.getProductMng())
                || StringUtils.isEmpty(reqPlan.getDevpEng()) || StringUtils.isEmpty(reqPlan.getFrontEng())
                || StringUtils.isEmpty(reqPlan.getTestEng()) || StringUtils.isEmpty(reqPlan.getQaMng())
                || StringUtils.isEmpty(reqPlan.getConfigMng()) || StringUtils.isEmpty(reqPlan.getPrdFinshTm())
                || StringUtils.isEmpty(reqPlan.getUatUpdateTm()) || StringUtils.isEmpty(reqPlan.getTestFinshTm())
                || StringUtils.isEmpty(reqPlan.getPreTm()) || StringUtils.isEmpty(reqPlan.getPrdFinshTm());
    }

    /**
     * svn项目建立
     * @param reqTask
     * @param request
     * @return
     */
    private String bulidSvnProjrct(DemandDO reqTask, HttpServletRequest request) {
        // 身份验证
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(SvnConstant.SvnUserName,SvnConstant.SvnPassWord);
        SVNClientManager clientManager = SVNUtil.authSvn(SvnConstant.SvnPath,authManager);
        try {
            String reqNo=reqTask.getReqNo();
            int start=reqNo.indexOf("-")+1;
            String reqMonth=reqNo.substring(start,start+6);
            SVNURL urlDir = SVNURL.parseURIEncoded(SvnConstant.SvnPath + reqMonth);
            //每月文件夹是否存在
            boolean isURLDirExist = SVNUtil.isURLExist(urlDir,authManager);
            if (!isURLDirExist) {
                SVNUtil.makeDirectory(clientManager, urlDir, "创建每月文档");
            }
            String dirNm = reqMonth+"/"+reqNo + "_" + reqTask.getReqNm();
            SVNURL url = SVNURL.parseURIEncoded(SvnConstant.SvnPath + dirNm);
            // 判断项目是否已经建立
            boolean isURLExist = SVNUtil.isURLExist(url,authManager);
            if (isURLExist) {
                return "";
            }
            SVNUtil.makeDirectory(clientManager, url, "项目启动创建文件夹");
            String path = SvnConstant.ProjectTemplatePath;
            File file = new File(path);
            // 导入文件夹
            SVNUtil.importDirectory(clientManager, file, url, "项目启动创建子文件夹", true);
        } catch (Exception e) {
            return "项目启动创建子文件夹失败，" + e.getMessage();
        }
        return "";
    }

    /**
     * 项目启动邮件内容
     * @param reqTask
     * @return
     */
    public String genProjectStartContent(DemandDO reqTask) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy年MM月dd日");
        StringBuilder content = new StringBuilder();
        content.append(reqTask.getReqNo() + "_" + reqTask.getReqNm() + "项目启动。人员以及计划安排如下，请各位知悉：");
        content.append("<br/>");
        content.append("人员计划如下：");
        content.append("<br/>");
        content.append("&nbsp;&nbsp;项目经理：" + reqTask.getProjectMng());
        content.append("<br/>");
        content.append("&nbsp;&nbsp;产品经理：" + reqTask.getProductMng());
        content.append("<br/>");
        content.append("&nbsp;&nbsp;后台开发：" + reqTask.getDevpEng());
        content.append("<br/>");
        content.append("&nbsp;&nbsp;前端开发：" + reqTask.getFrontEng());
        content.append("<br/>");
        content.append("&nbsp;&nbsp;测试人员：" + reqTask.getTestEng());
        content.append("<br/>");
        content.append("&nbsp;&nbsp;QA人员：" + reqTask.getQaMng());
        content.append("<br/>");
        content.append("&nbsp;&nbsp;配置人员：" + reqTask.getConfigMng());
        content.append("<br/>");
        content.append("<br/>");
        content.append("实施计划如下：");
        content.append("<br/>");
        try {
            String prdFinshTm = sdf
                    .format(DateUtils.parseDate(reqTask.getPrdFinshTm(), new String[] { "yyyy-MM-dd" }));
            String uatUpdateTm = sdf
                    .format(DateUtils.parseDate(reqTask.getUatUpdateTm(), new String[] { "yyyy-MM-dd" }));
            String testFinishTm = sdf
                    .format(DateUtils.parseDate(reqTask.getTestFinshTm(), new String[] { "yyyy-MM-dd" }));
            String preTm = sdf.format(DateUtils.parseDate(reqTask.getPreTm(), new String[] { "yyyy-MM-dd" }));
            String oprFisnTm = sdf
                    .format(DateUtils.parseDate(reqTask.getExpPrdReleaseTm(), new String[] { "yyyy-MM-dd" }));
            content.append("&nbsp;&nbsp;1、需求定稿时间：" + prdFinshTm);
            content.append("<br/>");
            content.append("&nbsp;&nbsp;2、UAT更新测试：" + uatUpdateTm);
            content.append("<br/>");
            content.append("&nbsp;&nbsp;3、UAT测试完成时间：" + uatUpdateTm + "—" + testFinishTm);
            content.append("<br/>");
            content.append("&nbsp;&nbsp;4、预投产验证：" + preTm + "—" + oprFisnTm);
            content.append("<br/>");
            content.append("&nbsp;&nbsp;5、投产时间：" + oprFisnTm);
        } catch (ParseException e) {
            return "时间转换错误," + e.getMessage();
        }
        return content.toString();
    }

    /**
     * 发送邮件
     * @param sendTo
     * @param copyTo
     * @param content
     * @param subject
     * @param attachFiles
     * @return
     */
    @Override
    public String sendMail(String sendTo, String copyTo, String content, String subject, Vector<File> attachFiles) {
        // 邮件去重
        sendTo = BaseUtil.distinctStr(sendTo, ";");
        if (StringUtils.isNotBlank(copyTo)) {
            copyTo = BaseUtil.distinctStr(copyTo, ";");
        }
        // 记录邮箱信息
        MailFlowDO bnb = new MailFlowDO(subject, Constant.P_EMAIL_NAME, sendTo, content);

        // 发邮件通知
        MailSenderInfo mailInfo = new MailSenderInfo();
        // 设置邮件服务器类型
        mailInfo.setMailServerHost("smtp.qiye.163.com");
        //设置端口号
        mailInfo.setMailServerPort("25");
        //设置是否验证
        mailInfo.setValidate(true);
        //设置用户名、密码、发送人地址
        mailInfo.setUserName(Constant.P_EMAIL_NAME);
        // 您的邮箱密码
        mailInfo.setPassword(Constant.P_EMAIL_PSWD);
        mailInfo.setFromAddress(Constant.P_EMAIL_NAME);
        sendTo=sendTo.replaceAll("；", ";");
        mailInfo.setToAddress(sendTo.split(";"));
        if (StringUtils.isNotBlank(copyTo)) {
            copyTo=copyTo.replaceAll("；", ";");
            mailInfo.setCcs(copyTo.split(";"));
        }
        if (null != attachFiles) {
            mailInfo.setFile(attachFiles);
        }
        mailInfo.setSubject(subject);
        mailInfo.setContent(content);
        // 这个类主要来发送邮件
        SimpleMailSender sms = new SimpleMailSender();

        boolean isSend = sms.sendHtmlMail(mailInfo);// 发送html格式
        if (isSend) {
            operationProductionDao.addMailFlow(bnb);
        } else {
            return "邮件发送失败";
        }
        return null;

    }
    /**
     * 存量变更
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void changeReq(String req_impl_mon){
//        boolean flag = this.authenticationUser();
//        if(flag){
        try {
            // 找到实施月份为本月、需求状态为未完成的状态、非取消和暂停的需求
            List<DemandDO> list = demandDao.findUnFinishReq(req_impl_mon);
            //获取下个月时间
            SimpleDateFormat simpleDateFormatMonth = new SimpleDateFormat("yyyy-MM");
            Date month = simpleDateFormatMonth.parse(req_impl_mon);
            Calendar c = Calendar.getInstance();
            c.setTime(month);
            c.add(Calendar.MONTH, 1);
            String last_month = simpleDateFormatMonth.format(c.getTime());
            //获取登录用户ID
            String update_user = SecurityUtils.getLoginUserId();
            for (int i = 0; i < list.size(); i++) {
                DemandDO demand = list.get(i);
                // 需求类型变为存量
                demand.setReqType("02");
                // 需求实施月份，转为下个月
                demand.setReqImplMon(last_month);
                // 月初阶段等于需求当前阶段
                demand.setPreMonPeriod(demand.getPreCurPeriod());
                //月初备注置空
                demand.setMonRemark("");
                //月底备注置空
                demand.setEndMonRemark("");
                demand.setEndFeedbackTm("");
                // 工作量已录入总量
                int inputWorkLoad = demand.getInputWorkload() + demand.getMonInputWorkload();
                demand.setInputWorkload(inputWorkLoad);
                demand.setRemainWorkload(demand.getTotalWorkload() - inputWorkLoad);
                // 本月录入，计入上月录入
                demand.setLastInputWorkload(demand.getMonInputWorkload());
                // 本月录入0
                demand.setMonInputWorkload(0);
                // 更新人，更新时间
                demand.setUpdateUser(update_user);
                demand.setUpdateTime(new Date());

                DemandDO vo = new DemandDO();
                vo.setReqNm(demand.getReqNm());
                vo.setReqNo(demand.getReqNo());
                vo.setReqImplMon(demand.getReqImplMon());
                List<DemandDO> dem = demandDao.getReqTaskByUKImpl(vo);
                if (dem.size() == 0) {
                    String reqInnerSeq = demand.getReqInnerSeq();
                    demand.setReqInnerSeq(getNextInnerSeq());
                    demandDao.insertStockReq(demand);
                    SyncJira(demand, reqInnerSeq);

                }else {
                    demand.setReqInnerSeq(dem.get(0).getReqInnerSeq());
                    demandDao.updateStockReq(demand);
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("存量需求转存失败：" + e.getMessage());
            //"存量需求转存失败" + e.getMessage();
            BusinessException.throwBusinessException(MsgEnum.ERROR_FAIL_CHANGE+e.getMessage());
        }
//        }else{
//            //无权限使用该功能
//            BusinessException.throwBusinessException(MsgEnum.ERROR_NOT_PRIVILEGE);
//        }
    }

    /**
     * 需求重新启动
     * @param  ids 需求内部编号
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void rebooting(List<String> ids){
        try {
            //获取当前月时间
            Date date = new Date();
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM");
            String month = df.format(date);
            System.err.println("当前月份："+month);
            //获取登录用户ID
            String update_user = SecurityUtils.getLoginUserId();
            for (int i = 0; i < ids.size(); i++) {
                //根据内部编号查询需求信息
                DemandDO demand = BeanUtils.copyPropertiesReturnDest(new DemandDO(), reqPlanService.findById(ids.get(i)));
                System.err.println("需求状态：="+demand.getReqSts());
                //判断需求是否为取消，暂停需求，不是则跳过
                if(!REQSUSPEND.equals(demand.getReqSts())&&!REQCANCEL.equals(demand.getReqSts())){
                    System.err.println("此需求不是异常需求");
                    continue;
                }
                //需求状态变为进行中
                demand.setReqSts("20");
                // 需求类型变为存量
                demand.setReqType("02");
                // 需求实施月份，变更为当前月
                demand.setReqImplMon(month);
                // 月初阶段等于需求当前阶段
                demand.setPreMonPeriod(demand.getPreCurPeriod());
                //月初备注置空
                demand.setMonRemark("");
                //月底备注置空
                demand.setEndMonRemark("");
                demand.setEndFeedbackTm("");
                // 工作量已录入总量
                int inputWorkLoad = demand.getInputWorkload() + demand.getMonInputWorkload();
                demand.setInputWorkload(inputWorkLoad);
                demand.setRemainWorkload(demand.getTotalWorkload() - inputWorkLoad);
                // 本月录入，计入上月录入
                demand.setLastInputWorkload(demand.getMonInputWorkload());
                // 本月录入0
                demand.setMonInputWorkload(0);
                // 更新人，更新时间
                demand.setUpdateUser(update_user);
                demand.setUpdateTime(new Date());

                DemandDO vo = new DemandDO();
                vo.setReqNm(demand.getReqNm());
                vo.setReqNo(demand.getReqNo());
                vo.setReqImplMon(demand.getReqImplMon());
                //判断该需求是否当前月已存在
                List<DemandDO> dem = demandDao.getReqTaskByUKImpl(vo);
                //如果当前月需求不存在，则新增，否则更新需求信息
                if (dem.size() == 0) {
                    String reqInnerSeq = demand.getReqInnerSeq();
                    demand.setReqInnerSeq(getNextInnerSeq());
                    demandDao.insertStockReq(demand);
                    SyncJira(demand, reqInnerSeq);
                } else {
                    demand.setReqInnerSeq(dem.get(0).getReqInnerSeq());
                    demandDao.updateStockReq(demand);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("需求查询启动：" + e.getMessage());
            BusinessException.throwBusinessException("异常需求查询启动失败:" + e.getMessage());
        }
    }
    private void SyncJira(DemandDO demand, String reqInnerSeq) {
        DemandJiraDO demandJiraDO = demandJiraDao.get(reqInnerSeq);
        //若已存在对应jira任务，则更新jira关联表
        if(JudgeUtils.isNotNull(demandJiraDO)){
            demandJiraDO.setReqInnerSeq(demand.getReqInnerSeq());
            demandJiraDO.setCreatTime(LocalDateTime.now());
            demandJiraDao.insert(demandJiraDO);
        }
        //更新需求状态历史表
        DemandStateHistoryDO demandStateHistoryDO = new DemandStateHistoryDO();
        demandStateHistoryDO.setReqInnerSeq(demand.getReqInnerSeq());
        String reqSts = reqTaskService.reqStsCheck(demand.getReqSts());
        demandStateHistoryDO.setReqSts(reqSts);
        demandStateHistoryDO.setRemarks("存量变更录入");
        demandStateHistoryDO.setReqNo(demand.getReqNo());
        demandStateHistoryDO.setReqNm(demand.getReqNm());
        //获取当前操作员
        demandStateHistoryDO.setCreatUser(SecurityUtils.getLoginName());
        demandStateHistoryDO.setCreatTime(LocalDateTime.now());
        //登记需求状态历史表
        demandStateHistoryDao.insert(demandStateHistoryDO);
    }


    /**
     * 用户身份验证
     */
    public boolean authenticationUser(){
        //获取登录用户名
        String currentUser =  SecurityUtils.getLoginName();
        if (currentUser.equals("田群") || currentUser.equals("吴暇")) {
            return true;
        }
        return false;
    }
    /**
     * 查找最大内部用户号
     */
    public DemandDO getMaxInnerSeq() {
        return demandDao.getMaxInnerSeq();
    }

    public String getNextInnerSeq() {
        // 从最大的值往后排
        DemandDO reqTask = getMaxInnerSeq();
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
    /**
     *文档上传
     */
    @Override
    public void uploadProjrctFile(ProjectStartBO reqDTO, MultipartFile[] files ,HttpServletRequest request){
        String uploadPeriod = reqDTO.getUploadPeriod();
        String innerReqSeq = reqDTO.getReqInnerSeq();
        DemandDO reqPlan = demandDao.get(innerReqSeq);
        if (null == reqPlan) {
            //return ajaxDoneError("文档上传失败：找不到需求相关信息，无法进行上传!");
            BusinessException.throwBusinessException("文档上传失败：找不到需求相关信息，无法进行上传!");
        }
        int period = 0;
        int curPeriod = 0;
        try {
            String reqPeriod = dictionaryService.findFieldName("REQ_PEROID", reqPlan.getPreCurPeriod());
            period = Integer.parseInt(uploadPeriod) + 10;
            curPeriod = Integer.parseInt(reqPeriod);
        } catch (Exception e) {
            //return ajaxDoneError("文档上传失败：请选择相应需求阶段进行文件上传!");
            BusinessException.throwBusinessException("文档上传失败：请选择相应需求阶段进行文件上传!");
        }
        String reqNo=reqPlan.getReqNo();
        if(StringUtils.isBlank(reqNo)){
            //return ajaxDoneError("文档上传失败：需求编号不能为空!");
            BusinessException.throwBusinessException("文档上传失败：需求编号不能为空!");
        }
        int start=reqNo.indexOf("-")+1;
        String reqMonth=reqNo.substring(start,start+6);

        String monthDir= com.cmpay.lemon.monitor.utils.Constant.PROJECTDOC_PATH+reqMonth;
        if(!(new File(monthDir).exists())){
            FileUtils.createDirectory(monthDir);
        }
        String directoryName = reqMonth+"/"+reqNo+"_" + reqPlan.getReqNm();
        String svnRoot = SvnConstant.SvnPath + directoryName;
        // 查看本地是否checkout
        String localSvnPath = com.cmpay.lemon.monitor.utils.Constant.PROJECTDOC_PATH + directoryName;
        String checOutMsg = checkOutSvnDir(directoryName, svnRoot, localSvnPath);
        if (!StringUtils.isEmpty(checOutMsg)) {
            //return ajaxDoneError(checOutMsg);
            BusinessException.throwBusinessException(checOutMsg);
        }
        // 获取阶段中文名
        String periodChName = "";
        String nowTime = DateUtil.date2String(new Date(), "yyyy-MM-dd");
        switch (uploadPeriod) {
            case ReqPeriodConstants.REQ_CONFIRM:
                periodChName = "产品";
                svnRoot = svnRoot + "/产品文档";
                localSvnPath = localSvnPath + "/产品文档/";
                reqPlan.setActPrdUploadTm(nowTime);
                break;
            case ReqPeriodConstants.TECH_DOC_CONFIRM:
                periodChName = "开发技术";
                svnRoot = svnRoot + "/开发技术文档/";
                localSvnPath = localSvnPath + "/开发技术文档/";
                reqPlan.setActWorkloadUploadTm(nowTime);
                break;
            case ReqPeriodConstants.FINISH_SIT_TEST:
                periodChName = "sit测试";
                svnRoot = svnRoot + "/开发技术文档/";
                localSvnPath = localSvnPath + "/开发技术文档/";
                reqPlan.setActSitUploadTm(nowTime);
                break;
            case ReqPeriodConstants.TEST_CASE_CONFIRM:
                svnRoot = svnRoot + "/测试文档/";
                localSvnPath = localSvnPath + "/测试文档/";
                reqPlan.setActTestCasesUploadTm(nowTime);
                break;
            case ReqPeriodConstants.FINISH_UAT_TEST:
                svnRoot = svnRoot + "/测试文档/";
                localSvnPath = localSvnPath + "/测试文档/";
                reqPlan.setActUatUploadTm(nowTime);
                break;
            case ReqPeriodConstants.FINISH_PRE_TEST:
                svnRoot = svnRoot + "/预投产投产文档/";
                localSvnPath = localSvnPath + "/预投产投产文档/";
                reqPlan.setActPreUploadTm(nowTime);
                break;
            case ReqPeriodConstants.FINISH_PRD:
                svnRoot = svnRoot + "/预投产投产文档/";
                localSvnPath = localSvnPath + "/预投产投产文档/";
                reqPlan.setActProductionUploadTm(nowTime);
                break;
            default:
                break;
        }
        // 上传文档到本地SVN工作空间
        Map<String, Object> map = null;
        try {
            //更新文档上传时间
            updateExtraTm(reqPlan);
            map = commitFile(files, svnRoot, localSvnPath,directoryName, reqPlan, request);
        } catch (Exception e) {
            // return ajaxDoneError(e.getMessage());
            BusinessException.throwBusinessException(e.getMessage());
        }
        String message = (String) map.get("message");
        if (!StringUtils.isBlank(message)) {
            //return ajaxDoneError(message);
            BusinessException.throwBusinessException(message);
        }
        Vector<File> attachFiles = (Vector<File>) map.get("files");
        if (StringUtils.equals(uploadPeriod, ReqPeriodConstants.REQ_CONFIRM) || StringUtils.equals(uploadPeriod,ReqPeriodConstants.TECH_DOC_CONFIRM)
                || StringUtils.equals(uploadPeriod, ReqPeriodConstants.FINISH_SIT_TEST)) {
            // 下发文档邮件
            String sendTo = reqDTO.getSendTo();
            String copyTo = reqDTO.getCopyTo();
            if (StringUtils.isBlank(sendTo)) {
                //return ajaxDoneError("文档上传成功，邮件发送失败，收件人必填，多个“;”分割!");
                BusinessException.throwBusinessException("文档上传成功，邮件发送失败，收件人必填，多个“;”分割!");
            }
            String currentUser =  SecurityUtils.getLoginName();
            String  subject= reqPlan.getReqNo() + "_" + reqPlan.getReqNm() + "_" + "需求" + periodChName + "文档";
            String content = "您好！<br/> &nbsp;&nbsp;附件是" + subject + "，请帮忙尽快上传到电子工单系统，谢谢！";
            String msg = reqPlanService.sendMail(sendTo, copyTo, content, subject+"-"+currentUser, attachFiles);
            if (StringUtils.isNotEmpty(msg)) {
                //return ajaxDoneError("文档上传成功，邮件发送失败");
                BusinessException.throwBusinessException("文档上传成功，邮件发送失败");
            }
        }
        // period：下拉框阶段值+10  curPeriod：需求当前阶段，上传文档的状态+10大于需求当前阶段时更新状态到下一个阶段
        if (!ReqPeriodConstants.FINISH_PRD.equals(uploadPeriod) && (period > curPeriod)) {
            //但状态为测试时，变为开发编码阶段
            if(period==60||period==70){
                period=80;
            }
            String preCurPeriod = period + "";
            reqPlan.setPreCurPeriod(preCurPeriod);
            demandDao.updatePreCurPeriod(reqPlan);
        }
    }

    private String checkOutSvnDir(String directoryName, String svnRoot, String localSvnPath) {
        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(SvnConstant.SvnUserName,SvnConstant.SvnPassWord);
        SVNClientManager clientManager = null;
        File localSvnDir = new File(localSvnPath);
        if (!localSvnDir.exists()) {
            clientManager = SVNUtil.authSvn(svnRoot, authManager);
            try {
                if(!SVNUtil.isURLExist(SVNURL.parseURIEncoded(svnRoot),authManager)){
                    return "文档上传失败:请先进行项目启动!";
                }
                SVNUtil.checkout(clientManager, SVNURL.parseURIEncoded(svnRoot), SVNRevision.HEAD, new File(localSvnPath), SVNDepth.INFINITY);
            } catch (SVNException e) {
                return "文档上传失败：SVN检出失败!";
            }
        }
        return "";
    }

    /**
     * 文档上传svn
     * @param files 需求文档
     * @param svnRoot
     * @param localSvnPath
     * @param directoryName
     * @param reqTask
     * @param request
     * @return
     */
    private Map<String, Object> commitFile(MultipartFile[] files, String svnRoot, String localSvnPath, String directoryName,DemandDO reqTask,
                                           HttpServletRequest request) {
        SVNClientManager clientManager;
        String importFilePath = null;
        Map<String, Object> map = new HashMap<String, Object>();
        Vector<File> attachFiles = new Vector<File>();
        if (files != null && files[0].getSize() != 0) {
            // 处理多文件上传
            File fl = null;
            String[] attachFileNames = null;
            for (MultipartFile importfile : files) {
                String loacalpath=localSvnPath;
                String svnPath=svnRoot;
                if (!importfile.isEmpty()) {
                    try {
                        String fileName = importfile.getOriginalFilename();
                        if(!fileName.startsWith(reqTask.getReqNo()+"_"+reqTask.getReqNm())){
                            map.put("message", "文档提交到SVN失败：文件名与需求名不一致，请检查");
                            return map;
                        }
                        if(fileName.contains("评审表")){
                            loacalpath= com.cmpay.lemon.monitor.utils.Constant.PROJECTDOC_PATH + directoryName + "/评审文档/";
                            svnPath = SvnConstant.SvnPath+directoryName + "/评审文档/";
                        }
                        // 文件保存路径
                        importFilePath = loacalpath + fileName;
                        // 转存文件
                        fl = new File(importFilePath);
                        attachFileNames = fl.getAbsolutePath().split(";");
                        for (int i = 0; i < attachFileNames.length; i++) {
                            if ("\\".equals(File.separator)){
                                attachFileNames[i].replace("/", "\\");
                            }
                            else if ("/".equals(File.separator)) {
                                attachFileNames[i].replace("\\", "/");
                            }
                        }
                        // 保存本地svn
                        importfile.transferTo(fl);
                        File newWordLod =null;
                        //功能点解析
                        if (fileName.contains("原子功能点评估表(内部考核)")||fileName.contains("原子功能点评估表（内部考核）")) {
                            String  msg=saveWordLoad(reqTask, map, importFilePath);
                            if(StringUtils.isNotBlank(msg)){
                                map.put("message",msg);
                                return map;
                            }
                            //转换成基地对应的功能点
                            String newfileName=fileName.replaceAll("原子功能点评估表(内部考核)", "原子功能点评估表(电子工单)").replaceAll("原子功能点评估表（内部考核）", "原子功能点评估表(电子工单)");
                            msg=copyWorLoadFile(importFilePath,request,loacalpath+newfileName);
                            if(StringUtils.isNotEmpty(msg)){
                                map.put("message",msg);
                                return map;
                            }
                            //将装换的功能点添加到附件中
                            newWordLod=new File(loacalpath+newfileName);
                            attachFiles.add(newWordLod);
                        }
                        if (!fileName.contains("内部考核")||!fileName.contains("原子功能点评估表（内部考核）")) {
                            // 文件添加到需要发送的邮件组去
                            attachFiles.add(fl);
                        }
                        // 上传到SVN
                        ISVNAuthenticationManager authManager = SVNWCUtil.createDefaultAuthenticationManager(SvnConstant.SvnUserName,SvnConstant.SvnPassWord);
                        clientManager = SVNUtil.authSvn(svnPath,authManager);
                        clientManager.getWCClient().doCleanup(new File(loacalpath));
                        clientManager.getDiffClient().setIgnoreExternals(true);
                        SVNUtil.checkVersiondDirectory(clientManager,fl);
                        SVNUtil.commit(clientManager, fl, true, "文档提交");
                        if(newWordLod!=null){
                            SVNUtil.checkVersiondDirectory(clientManager,newWordLod);
                            SVNUtil.commit(clientManager, newWordLod, true, "文档提交");
                        }
                        SVNUtil.update(clientManager, new File(loacalpath), SVNRevision.HEAD, SVNDepth.INFINITY);
                    } catch (Exception e) {
                        map.put("message", "文档提交到SVN失败："+e.getMessage());
                        return map;
                    }
                }
            }
        }
        map.put("files", attachFiles);
        return map;
    }

    private String saveWordLoad(DemandDO reqTask, Map<String, Object> map, String filePath)
            throws Exception {
        // 从excel得出CR编号，各部门占比，总工作量
        String[] params = paserWorkLoad(filePath);
        if (StringUtils.isNotBlank(params[0])) {
            return params[0];
        }
        // 需求编号
        String crNum = params[1];
        // 各部门占比
        String deptInfo = params[2];
        // 总工作量
        String totalWorkload = params[3];
        if (!crNum.equals(reqTask.getReqNo())) {
            return "功能点附件中需求编号有误！";
        }
        if (StringUtils.isBlank(deptInfo)) {
            return "各部门占比不能为空！";
        }
        if (StringUtils.isBlank(totalWorkload)) {
            return "功能点合计不能为空！";
        }
        int totWork=0;
        try{
            totWork=Double.valueOf(totalWorkload).intValue();
        }catch(Exception e){
            return "功能点合计不为数字";
        }
        //占比
        Map<String,String> result=checkDeptRate(totWork,deptInfo,reqTask);
        String msg=result.get("message");
        if(!StringUtils.isBlank(msg)){
            return msg;
        }
        // 更新工作量信息
        try {
            reqTask.setTotalWorkload(totWork);
            reqTask.setLeadDeptPro(result.get("leadDeptRate"));
            reqTask.setCoorDeptPro(result.get("coorDeptRate"));
            reqTask.setLeadDeptWorkload(result.get("leadDpetWorkLoad"));
            reqTask.setCoorDeptWorkload(result.get("coorDpetWorkLoad"));
            //已录入总量 新增的时候默认为0
            reqTask.setInputWorkload(0);
            reqTask.setLastInputWorkload(0);
            reqTask.setRemainWorkload(totWork);
            reqTask.setMonInputWorkload(0);
            planDao.updateReqWorkLoad(reqTask);
        } catch (NumberFormatException e) {
            return "功能点导入失败："+e.getMessage();
        }
        return null;
    }

    /**
     *
     */
    public String[] paserWorkLoad(String filePath) throws Exception {
        String [] params = new String[4];
        boolean isExcel2003 = filePath.endsWith(".xls");
        boolean isExcel2007 = filePath.endsWith(".xlsx");
        org.apache.poi.ss.usermodel.Workbook wb = null;
        try {
            if (isExcel2003) {
                wb = new HSSFWorkbook(new FileInputStream(new File(filePath)));
                int sheetNum = wb.getNumberOfSheets();
                if (sheetNum!=3) {
                    params[0]=("导入文件不符合要求，功能点标准模板为3个Sheet，但导入文件为"+sheetNum+"个Sheet！");
                    return params;
                }
                HSSFSheet sheet = (HSSFSheet) wb.getSheetAt(0);
                params = parseHSSFSheetExcel(sheet);
            } else if(isExcel2007) {
                wb = new XSSFWorkbook(new FileInputStream(new File(filePath)));
                int sheetNum = wb.getNumberOfSheets();
                if (sheetNum!=3) {
                    params[0]=("导入文件不符合要求，功能点标准模板为3个Sheet，但导入文件为"+sheetNum+"个Sheet！");
                    return params;
                }
                XSSFSheet sheet = (XSSFSheet) wb.getSheetAt(0);
                params = parseXSSFSheetExcel(sheet);
            } else {
                params[0]=("导入文件不符合要求！");
                return params;
            }
        } catch (FileNotFoundException e) {
            throw new Exception(e.getMessage());
        } catch (IOException e) {
            throw new Exception(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            throw new Exception(e.getMessage());
        }finally {
            if (wb != null) {
                try {
                    wb.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return params;
    }

    //针对2003版本
    private String[] parseHSSFSheetExcel(HSSFSheet sheet) throws Exception {
        String [] params = new String[3];
        int  rows  = sheet.getPhysicalNumberOfRows();
        // 检查文件行数，若少于2行则提示错误
        if (rows < 8) {
            params[0]=("导入文件文件格式有误");
            return params;
        }
        //获取cr编号
        HSSFRow rowCR   = sheet.getRow(1);
        HSSFCell cellCR = rowCR.getCell(3);
        params[1]=cellCR != null?cellCR.getStringCellValue():null;

        //获取部门占比
        HSSFRow rowDept   = sheet.getRow(3);
        HSSFCell cellDept = rowDept.getCell(1);
        params[2]=cellDept != null?cellDept.getStringCellValue():null;

        //工作量
        HSSFRow rowWL   = sheet.getRow(7);
        HSSFCell cellWL = rowWL.getCell(10);
        params[3]=cellWL != null?""+cellWL.getNumericCellValue():null;

        LOGGER.info("cr+部门占比+工作量："+params[0]+":"+params[1]+":"+params[2]);

        return params;
    }

    //针对2007版本
    private String [] parseXSSFSheetExcel(XSSFSheet sheet) throws Exception {

        String [] params = new String[4];
        int  rows  = sheet.getPhysicalNumberOfRows();

        // 检查文件行数，若少于2行则提示错误
        if (rows < 8) {
            params[0]=("导入文件文件格式有误");
            return params;
        }
        //获取cr编号
        XSSFRow rowCR   = sheet.getRow(1);
        XSSFCell cellCR = rowCR.getCell(3);
        params[1]=cellCR != null?cellCR.getStringCellValue():null;

        //获取部门占比
        XSSFRow rowDept   = sheet.getRow(3);
        XSSFCell cellDept = rowDept.getCell(1);
        params[2]=cellDept != null?cellDept.getStringCellValue():null;

        //工作量
        XSSFRow rowWL   = sheet.getRow(7);
        XSSFCell cellWL = rowWL.getCell(10);
        params[3]=cellWL != null?""+cellWL.getNumericCellValue():null;

        LOGGER.info("cr+部门占比+工作量："+params[0]+":"+params[1]+":"+params[2]);

        return params;

    }

    /**
     *工作量检查
     */
    public Map<String, String> checkDeptRate(int totWork, String deptInfo, DemandDO demand) {
        deptInfo = deptInfo.replaceAll("：", ":").replaceAll("；", ";");
        Map<String, String> map = new HashMap<>();
        if (deptInfo.indexOf(":") < 0) {
            map.put("message", "【工作量占比(配合部门投入占比)】格式有误，正确格式为：【银行合作研发部：80%;移动业务研发部：20%】，  请修改后重新导入！");
            return map;
        }
        String[] deptrates = deptInfo.split(";");
        BigDecimal totalRate = new BigDecimal(0.0);
        String coorDeptRate = "";
        String leadDeptRate = "";
        String leadDpetWorkLoad = "";
        String coorDpetWorkLoad = "";
        Set<String> leadDeptName=new HashSet<>();
        Set<String> coorDeptName=new HashSet<>();
        for (int i = 0; i < deptrates.length; i++) {
            String perRate = deptrates[i];
            if (StringUtils.isNotEmpty(perRate)) {
                String[] detailRate = perRate.split(":");
                if (detailRate.length != 2) {
                    map.put("message", "【工作量占比(配合部门投入占比)】格式有误，正确格式为：【银行合作研发部：80%;移动业务研发部：20%】，  请修改后重新导入！");
                    return map;
                }
                String rate = detailRate[1];
                rate = rate.replaceAll("%", "");
                if (StringUtils.isEmpty(rate)) {
                    map.put("message", "【工作量占比(配合部门投入占比)】格式有误，正确格式为：【银行合作研发部：80%;移动业务研发部：20%】，  请修改后重新导入！");
                    return map;
                } else {
                    totalRate = totalRate.add(new BigDecimal(Double.valueOf(rate))).setScale(2,
                            BigDecimal.ROUND_HALF_EVEN);
                }
                try {
                    if (StringUtils.isBlank(dictionaryService.findFieldName("DEV_DEPT", detailRate[0]))) {
                        map.put("message", "不存在部门："+detailRate[0]);
                        return map;
                    }
                } catch (Exception e) {
                    map.put("message", "不存在部门："+detailRate[0]);
                    return map;
                }
                if (detailRate[0].equals(demand.getDevpLeadDept())) {
                    leadDeptName.add(detailRate[0]);
                    // 主导部门
                    leadDeptRate = detailRate[0] + ":" + detailRate[1] + ";";
                    leadDpetWorkLoad = leadDpetWorkLoad + detailRate[0] + ":"
                            + new BigDecimal(totWork * Double.valueOf(rate) / 100).setScale(2,
                            BigDecimal.ROUND_HALF_EVEN)
                            + ";";
                } else {
                    coorDeptName.add(detailRate[0]);
                    coorDeptRate = coorDeptRate + detailRate[0] + ":" + detailRate[1] + ";";
                    coorDpetWorkLoad = coorDpetWorkLoad + detailRate[0] + ":"
                            + new BigDecimal(totWork * Double.valueOf(rate) / 100).setScale(2,
                            BigDecimal.ROUND_HALF_EVEN)
                            + ";";
                }
            }
        }
        if (totalRate.compareTo(new BigDecimal(100.0)) != 0) {
            // 返回值 -1 小于 0 等于 1 大于
            map.put("message", "【工作量占比(配合部门投入占比)】合计应等于100%，  请修改后重新导入！");
            return map;
        }
        int size=leadDeptName.size()+coorDeptName.size();
        if(deptrates.length!=size){
            map.put("message", "存在重复的配合部门信息请检查！");
            return map;
        }
        String coorDept=demand.getDevpCoorDept();
        if(StringUtils.isNotBlank(coorDept)){
            String[] coorDeptArr=coorDept.split(",");
            for (int i = 0; i < coorDeptArr.length; i++) {
                if ("产品研究部".equals(coorDeptArr[i]) || "产品测试部".equals(coorDeptArr[i])) {
                    continue;
                }
                if (!coorDeptName.contains(coorDeptArr[i])) {
                    map.put("message", "配合部门占比信息有误请检查！");
                    return map;
                }
            }
        }

        map.put("leadDeptRate", leadDeptRate);
        map.put("coorDeptRate", coorDeptRate);
        map.put("leadDpetWorkLoad", leadDpetWorkLoad);
        map.put("coorDpetWorkLoad", coorDpetWorkLoad);
        map.put("remainWordkLoad", String.valueOf(totWork-demand.getInputWorkload()));
        return map;
    }
    /**
     * 原子功能点文档转换
     */
    public String copyWorLoadFile(String importFilePath,HttpServletRequest request,String loacalpath) {
        String tempPath= "/home/devadm/template/excelTemplate/原子功能点评估表_导入使用.xlsx";
        //读取相关信息
        Map<String, Object> resMap;
        try {
            resMap = paserCopyWorkLoad(importFilePath);
            String[] paramsHead=(String[]) resMap.get("paramHead");
            if(StringUtils.isNotEmpty(paramsHead[0])){
                return paramsHead[0];
            }
            // 需求编号
            String crNum = paramsHead[1];
            // 总工作量
            String totalWorkload = paramsHead[3];
            String reqName=paramsHead[4];
            String prdLine=paramsHead[5];
            String reqDesc=paramsHead[6];
            //读取List数据
            String attachFileNames[] = tempPath.split(";");
            for (int i = 0; i < attachFileNames.length; i++) {
                if ("\\".equals(File.separator))
                    attachFileNames[i].replace("/", "\\");
                else if ("/".equals(File.separator))
                    attachFileNames[i].replace("\\", "/");
            }
            TemplateExportParams param = new TemplateExportParams(tempPath,true);
            param.setColForEach(true);
            Map<String, Object> mapHead = new HashMap<String, Object>();
            mapHead.put("reqNo",crNum);
            mapHead.put("reqName", reqName);
            mapHead.put("prdLine", prdLine);
            mapHead.put("reqDesc", reqDesc);
            mapHead.put("totalWorkload", totalWorkload);
            // 构造数据
            List<Map<String, Object>> listMap =(List<Map<String, Object>>) resMap.get("dataList");
            mapHead.put("totalNum", listMap.size());
            mapHead.put("dataList", listMap);
            String message=GenWorkLoadExcelUtil.createXLSX(mapHead, tempPath, loacalpath);
            return message;
            //打开excel报错问题修复
        } catch (Exception e1) {
            e1.printStackTrace();
            return "功能点转化错误："+e1.getMessage();
        }
    }

    /**
     * copy原子功能点文档
     * @param filePath
     * @return
     * @throws Exception
     */
    private static Map<String,Object> paserCopyWorkLoad(String filePath) throws Exception {
        Map<String, Object> resMap = new HashMap<>();
        String [] params = new String[4];
        boolean isExcel2003 = filePath.endsWith(".xls");
        boolean isExcel2007 = filePath.endsWith(".xlsx");
        org.apache.poi.ss.usermodel.Workbook wb = null;
        try {
            FileInputStream in=	new FileInputStream(new File(filePath));
            if (isExcel2003) {//针对2003版本
                wb = new HSSFWorkbook(in);
                int sheetNum = wb.getNumberOfSheets();
                if (sheetNum!=3) {
                    params[0]=("导入文件不符合要求，功能点标准模板为3个Sheet，但导入文件为"+sheetNum+"个Sheet！");
                    resMap.put("params", params);
                    return resMap;
                }
                HSSFSheet sheet = (HSSFSheet) wb.getSheetAt(0);
                resMap = parseCopyHSSFSheetExcel(sheet);

            } else if(isExcel2007) {//针对2007版本
                wb = new XSSFWorkbook(in);
                int sheetNum = wb.getNumberOfSheets();
                if (sheetNum!=3) {
                    params[0]=("导入文件不符合要求，功能点标准模板为3个Sheet，但导入文件为"+sheetNum+"个Sheet！");
                    resMap.put("params", params);
                    return resMap;
                }
                XSSFSheet sheet = (XSSFSheet) wb.getSheetAt(0);
                resMap = parseCopyXSSFSheetExcel(sheet);
            } else {
                params[0]=("导入文件不符合要求！");
                resMap.put("params", params);
                return resMap;
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }finally{
            if(wb!=null){
                try {
                    wb.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return resMap;
    }

    //针对2003版本
    private static Map<String,Object> parseCopyHSSFSheetExcel(HSSFSheet sheet) throws Exception {
        Map<String,Object > resMap=new HashMap<String,Object>();
        String [] params = new String[6];
        int  rows  = sheet.getPhysicalNumberOfRows();
        // 检查文件行数，若少于2行则提示错误
        if (rows < 8) {
            params[0]=("导入文件文件格式有误");
            resMap.put("params", params);
            return resMap;
        }
        HSSFRow rowCR   = sheet.getRow(1);
        //需求名称
        HSSFCell cellNm= rowCR.getCell(1);
        params[4]=cellNm != null?cellNm.getStringCellValue():null;

        //所属产品
        HSSFCell cellPrdLine= rowCR.getCell(9);
        params[5]=cellPrdLine != null?cellPrdLine.getStringCellValue():null;

        //获取cr编号
        HSSFCell cellCR = rowCR.getCell(3);
        params[1]=cellCR != null?cellCR.getStringCellValue():null;

        //获取部门占比
        HSSFRow rowDept   = sheet.getRow(3);
        HSSFCell cellDept = rowDept.getCell(1);
        params[2]=cellDept != null?cellDept.getStringCellValue():null;

        //需求描述
        HSSFRow rowReqDesc   = sheet.getRow(4);
        HSSFCell cellReqDesc = rowReqDesc.getCell(1);
        params[6]=cellReqDesc != null?cellReqDesc.getStringCellValue():null;

        //工作量
        HSSFRow rowWL   = sheet.getRow(7);
        HSSFCell cellWL = rowWL.getCell(10);
        params[3]=cellWL != null?""+cellWL.getNumericCellValue():null;
        resMap.put("paramHead", params);

        List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
        for (int i = 7; i < rows; i++) {
            HSSFRow row = sheet.getRow(i);
            if (row == null)
                break;
            // 原子功能点名称
            Map<String,Object > map=new HashMap<String, Object>();
            String bussNm = row.getCell(0).getStringCellValue();
            map.put("bussNm", bussNm);
            if(StringUtils.isBlank(bussNm)){
                resMap.put("dataList", listMap);
                return resMap;
            }
            String workLoadName = row.getCell(1)==null?"":row.getCell(1).getStringCellValue();
            map.put("workLoadName",workLoadName);
            String wordType=row.getCell(2)==null?"":row.getCell(2).getStringCellValue();
            map.put("wordType", wordType);
            String modifyType=row.getCell(3)==null?"":row.getCell(3).getStringCellValue();
            map.put("modifyType", modifyType);
            String comlexType=row.getCell(4)==null?"":row.getCell(4).getStringCellValue();
            map.put("comlexType", comlexType);
            String isConn=row.getCell(5)==null?"":row.getCell(5).getStringCellValue();
            map.put("isConn", isConn);
            String workLoadNameComb=row.getCell(6)==null?"":row.getCell(6).getStringCellValue();
            map.put("workLoadNameComb", workLoadNameComb);
            String atomCode=row.getCell(7)==null?"":row.getCell(7).getStringCellValue();
            map.put("atomCode", atomCode);
            String cellNum=Double.valueOf(row.getCell(8)==null?0:row.getCell(8).getNumericCellValue()).intValue()+"";
            map.put("cellNum", cellNum);
            String cellWord=Double.valueOf(row.getCell(9)==null?0:row.getCell(9).getNumericCellValue()).intValue()+"";
            map.put("cellWord", cellWord);
            String workLoad=Double.valueOf(params[3]).intValue()+"";
            map.put("totalWorkLoad",workLoad);
            listMap.add(map);
        }
        resMap.put("dataList", listMap);
        return resMap;
    }

    //针对2007版本
    private static Map<String,Object> parseCopyXSSFSheetExcel(XSSFSheet sheet) throws Exception {
        Map<String,Object > resMap=new HashMap<String,Object>();
        String [] params = new String[7];
        int  rows  = sheet.getPhysicalNumberOfRows();

        // 检查文件行数，若少于2行则提示错误
        if (rows < 8) {
            params[0]=("导入文件文件格式有误");
            resMap.put("params", params);
            return resMap;
        }

        XSSFRow rowCR   = sheet.getRow(1);
        //获取cr编号
        XSSFCell cellCR = rowCR.getCell(3);
        params[1]=cellCR != null?cellCR.getStringCellValue():null;

        //获取部门占比
        XSSFRow rowDept   = sheet.getRow(3);
        XSSFCell cellDept = rowDept.getCell(1);
        params[2]=cellDept != null?cellDept.getStringCellValue():null;

        //需求名称
        XSSFCell cellNm= rowCR.getCell(1);
        params[4]=cellNm != null?cellNm.getStringCellValue():null;

        //所属产品
        XSSFCell cellPrdLine= rowCR.getCell(9);
        params[5]=cellPrdLine != null?cellPrdLine.getStringCellValue():null;

        //需求描述
        XSSFRow rowReqDesc   = sheet.getRow(4);
        XSSFCell cellReqDesc = rowReqDesc.getCell(1);
        params[6]=cellReqDesc != null?cellReqDesc.getStringCellValue():null;

        //工作量
        XSSFRow rowWL   = sheet.getRow(7);
        XSSFCell cellWL = rowWL.getCell(10);
        params[3]=cellWL != null?""+cellWL.getNumericCellValue():null;
        resMap.put("paramHead", params);

        List<Map<String, Object>> listMap = new ArrayList<Map<String, Object>>();
        for (int i = 7; i < rows; i++) {
            Row row = sheet.getRow(i);
            if (row == null){
                break;
            }
            // 原子功能点名称
            Map<String,Object > map=new HashMap<String, Object>();
            String bussNm = row.getCell(0)==null?"":row.getCell(0).getStringCellValue();
            if(StringUtils.isBlank(bussNm)){
                resMap.put("dataList", listMap);
                return resMap;
            }
            map.put("bussNm", bussNm);
            String workLoadName = row.getCell(1)==null?"":row.getCell(1).getStringCellValue();
            map.put("workLoadName",workLoadName);
            String wordType=row.getCell(2)==null?"":row.getCell(2).getStringCellValue();
            map.put("wordType", wordType);
            String modifyType=row.getCell(3)==null?"":row.getCell(3).getStringCellValue();
            map.put("modifyType", modifyType);
            String comlexType=row.getCell(4)==null?"":row.getCell(4).getStringCellValue();
            map.put("comlexType", comlexType);
            String isConn=row.getCell(5)==null?"":row.getCell(5).getStringCellValue();
            map.put("isConn", isConn);
            String workLoadNameComb=row.getCell(6)==null?"":row.getCell(6).getStringCellValue();
            map.put("workLoadNameComb", workLoadNameComb);
            String atomCode=row.getCell(7)==null?"":row.getCell(7).getStringCellValue();
            map.put("atomCode", atomCode);

            String cellNum=Double.valueOf(row.getCell(8)==null?0:row.getCell(8).getNumericCellValue()).intValue()+"";
            map.put("cellNum", cellNum);
            String cellWord=Double.valueOf(row.getCell(9)==null?0:row.getCell(9).getNumericCellValue()).intValue()+"";
            map.put("cellWord", cellWord);
            String workLoad=Double.valueOf(params[3]).intValue()+"";
            map.put("totalWorkLoad",workLoad);
            listMap.add(map);
        }
        resMap.put("dataList", listMap);
        return resMap;

    }

    /**
     * 更新文档上传时间
     * @param bean
     */
    public void updateExtraTm(DemandDO bean) {
        List<DemandDO> list = demandDao.getExtraTm(bean);
        if (list.size() == 0) {
            demandDao.insertExtraTm(bean);
        }else {
            bean.setProId(list.get(0).getProId());
            if (StringUtils.isNotBlank(list.get(0).getProjectStartTm())) {
                bean.setProjectStartTm(null);
            }
            if (StringUtils.isNotBlank(list.get(0).getActPrdUploadTm())) {
                bean.setActPrdUploadTm(null);
            }
            if (StringUtils.isNotBlank(list.get(0).getActWorkloadUploadTm())) {
                bean.setActWorkloadUploadTm(null);
            }
            if (StringUtils.isNotBlank(list.get(0).getActSitUploadTm())) {
                bean.setActSitUploadTm(null);
            }
            if (StringUtils.isNotBlank(list.get(0).getActTestCasesUploadTm())) {
                bean.setActTestCasesUploadTm(null);
            }
            if (StringUtils.isNotBlank(list.get(0).getActUatUploadTm())) {
                bean.setActUatUploadTm(null);
            }
            if (StringUtils.isNotBlank(list.get(0).getActPreUploadTm())) {
                bean.setActPreUploadTm(null);
            }
            if (StringUtils.isNotBlank(list.get(0).getActProductionUploadTm())) {
                bean.setActProductionUploadTm(null);
            }
            demandDao.updateExtraTm(bean);
        }
    }
    /**
     * 获取当前需求阶段
     */
    @Override
    public String getReqPeriod(String preCurPeriod) {
        String reqPeriod = preCurPeriod;
        if("".equals(reqPeriod)||reqPeriod==null){
            return "";
        }
        if (new Integer(reqPeriod) <= REQCONFIRM) {
            reqPeriod = "30";
        }else if (new Integer(reqPeriod) <= TECHDOCCONFIRM) {
            reqPeriod = "50";
        }else if (new Integer(reqPeriod) <= TESTCASECONFIRM) {
            reqPeriod = "70";
        }else if (new Integer(reqPeriod) <= FINISHSITTEST) {
            reqPeriod = "110";
        }else if (new Integer(reqPeriod) <= FINISHUATTEST) {
            reqPeriod = "140";
        }else if (new Integer(reqPeriod) <= FINISHPRETEST) {
            reqPeriod = "160";
        }else if (new Integer(reqPeriod) <= FINISHPRD) {
            reqPeriod = "180";
        }
        return reqPeriod;
    }

}
