package com.cmpay.lemon.monitor.service.impl.production;

import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.BeanUtils;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.common.utils.StringUtils;
import com.cmpay.lemon.framework.page.PageInfo;
import com.cmpay.lemon.framework.security.SecurityUtils;
import com.cmpay.lemon.framework.utils.PageUtils;
import com.cmpay.lemon.monitor.bo.*;
import com.cmpay.lemon.monitor.dao.*;
import com.cmpay.lemon.monitor.entity.Constant;
import com.cmpay.lemon.monitor.entity.*;
import com.cmpay.lemon.monitor.entity.sendemail.*;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import com.cmpay.lemon.monitor.service.SystemUserService;
import com.cmpay.lemon.monitor.service.demand.ReqTaskService;
import com.cmpay.lemon.monitor.service.productTime.ProductTimeService;
import com.cmpay.lemon.monitor.service.production.OperationProductionService;
import com.cmpay.lemon.monitor.utils.*;
import com.cmpay.lemon.monitor.utils.wechatUtil.schedule.BoardcastScheduler;
import com.jcraft.jsch.*;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

/**
 * 投产管理：查询及状态变更
 */
@Service
public class OperationProductionServiceImpl implements OperationProductionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(OperationProductionServiceImpl.class);
    @Autowired
    private IOperationProductionDao operationProductionDao;
    @Autowired
    private IOperationApplicationDao operationApplicationDao;

    @Autowired
    private IProductionPicDao productionPicDao;
    @Autowired
    private ProductTimeService productTimeService;
    @Autowired
    private ReqTaskService reqTaskService;
    @Autowired
    IPermiUserDao permiUserDao;
    @Autowired
    ITPermiDeptDao permiDeptDao;
    @Autowired
    SystemUserService userService;
    @Autowired
    private BoardcastScheduler boardcastScheduler;

    @Override
    public ProductionRspBO find(ProductionBO productionBO) {
        PageInfo<ProductionBO> pageInfo = getPageInfo(productionBO);
        List<ProductionBO> productionBOList = BeanConvertUtils.convertList(pageInfo.getList(), ProductionBO.class);
        ProductionRspBO productionRspBO = new ProductionRspBO();
        productionRspBO.setProductionList(productionBOList);
        productionRspBO.setPageInfo(pageInfo);
        return productionRspBO;
    }
    @Override
    public ScheduleRspBO find1(ScheduleBO scheduleBO){
        PageInfo<ScheduleBO> pageInfo = getPageInfo1(scheduleBO);
        List<ScheduleBO> scheduleBOList = BeanConvertUtils.convertList(pageInfo.getList(), ScheduleBO.class);
        for (int i=0;i<scheduleBOList.size();i++) {
            if(scheduleBOList.get(i).getProNumber().startsWith("REQ") || scheduleBOList.get(i).getProNumber().startsWith("P") || scheduleBOList.get(i).getProNumber().startsWith("FIRE")){

                ProductionDO bean=operationProductionDao.findProductionBean(scheduleBOList.get(i).getProNumber());
                if(bean!=null){
                    scheduleBOList.get(i).setProType(bean.getProType());
                    scheduleBOList.get(i).setIsOperationProduction(bean.getIsOperationProduction());
                }else{
                    scheduleBOList.get(i).setProType("查无此信息");
                    scheduleBOList.get(i).setIsOperationProduction("");
                }
            }
            if(scheduleBOList.get(i).getProNumber().startsWith("SYS-OPR")){
                OperationApplicationDO bean=operationApplicationDao.findBaseOperationalApplicationInfo(scheduleBOList.get(i).getProNumber());
                if(bean!=null){
                    scheduleBOList.get(i).setProType(bean.getSysOperType());
                }else{
                    scheduleBOList.get(i).setProType("查无此信息");
                }
            }
        }
        ScheduleRspBO productionRspBO = new ScheduleRspBO();
        System.err.println(scheduleBOList.size());
        productionRspBO.setScheduleList(scheduleBOList);
        productionRspBO.setPageInfo(pageInfo);
        return productionRspBO;
    }
    private PageInfo<ProductionBO> getPageInfo(ProductionBO productionBO) {
        ProductionDO productionDO = new ProductionDO();
        BeanConvertUtils.convert(productionDO, productionBO);
        PageInfo<ProductionBO> pageInfo = PageUtils.pageQueryWithCount(productionBO.getPageNum(), productionBO.getPageSize(),
                () -> BeanConvertUtils.convertList(operationProductionDao.findPageBreakByCondition(productionDO), ProductionBO.class));
        return pageInfo;
    }
    private PageInfo<ScheduleBO> getPageInfo1(ScheduleBO scheduleBO) {
        ScheduleDO productionDO = new ScheduleDO();
        BeanConvertUtils.convert(productionDO, scheduleBO);
        PageInfo<ScheduleBO> pageInfo = PageUtils.pageQueryWithCount(scheduleBO.getPageNum(), scheduleBO.getPageSize(),
                () -> BeanConvertUtils.convertList(operationProductionDao.findPageBreakBySchedule(productionDO), ScheduleBO.class));
        return pageInfo;
    }
    @Override
    public void exportExcel(HttpServletRequest request, HttpServletResponse response, ProductionBO productionBO){
        ProductionDO productionDO = new ProductionDO();
        BeanConvertUtils.convert(productionDO, productionBO);
        List<ProductionDO> list = operationProductionDao.findExportExcelListByDate(productionDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), ProductionDO.class, list);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // 判断数据
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // 设置excel的文件名称
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
    public void updateAllProduction(HttpServletRequest request, HttpServletResponse response, String taskIdStr){
        //UserPrincipal currentUser = (UserPrincipal) SecurityUtils.getSubject().getPrincipal();
        //获取登录用户名
        String currentUser = userService.getFullname(SecurityUtils.getLoginName());
        //生成流水记录
        ScheduleDO scheduleBean =new ScheduleDO(currentUser);
        String[] pro_number_list=taskIdStr.split("~");
        if(pro_number_list[0].equals("1")){
            //return ajaxDoneError("请填写进行此操作原因");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("请填写进行此操作原因");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if(pro_number_list[0].equals("ytc")){
            if(pro_number_list.length==1){
                //return ajaxDoneError("请选择投产进行操作!");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            for(int i=2;i<pro_number_list.length;i++){
                ProductionDO beanCheck=operationProductionDao.findProductionBean(pro_number_list[i]);
                if(!(beanCheck.getProStatus().equals("投产提出") || (beanCheck.getProStatus().equals("投产待部署") && beanCheck.getProType().equals("救火更新")))){
                    //return ajaxDoneError("当前投产状态的投产信息不可修改!");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("当前投产状态的投产信息不可修改");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
                if(beanCheck.getProductionDeploymentResult().equals("已部署")){
                    //return ajaxDoneError("当前投产预投产已部署，不可重复操作!");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("当前投产预投产已部署，不可重复操作!");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
                ProductionDO bean=operationProductionDao.findProductionBean(pro_number_list[i]);
                bean.setProductionDeploymentResult("已部署");
                operationProductionDao.updateProduction(bean);
                ScheduleDO sBean=new ScheduleDO();
                sBean.setPreOperation(bean.getProStatus());
                ScheduleDO schedule=new ScheduleDO(bean.getProNumber(), currentUser, "预投产已部署", sBean.getPreOperation(), sBean.getPreOperation(), "预投产已提前部署");
                operationProductionDao.insertSchedule(schedule);
            }
            return;
        }
        if(pro_number_list[0].equals("yztg")){
            if(pro_number_list.length==1){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            for(int i=2;i<pro_number_list.length;i++){
                ProductionDO beanCheck=operationProductionDao.findProductionBean(pro_number_list[i]);
                if(!currentUser.equals(beanCheck.getProManager()) && !currentUser.equals(beanCheck.getProApplicant()) && !currentUser.equals(beanCheck.getDevelopmentLeader())){
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("只有负责该投产的产品经理,申请人以及开发负责人才能验证通过!");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
                if(beanCheck.getIsAdvanceProduction().equals("否")){
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("当前投产不做预投产验证,不可操作!");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
                if(beanCheck.getProAdvanceResult().equals("通过")){
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("当前预投产验证已通过,不可重复操作!");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }

                ProductionDO bean=operationProductionDao.findProductionBean(pro_number_list[i]);
                bean.setProAdvanceResult("通过");
                operationProductionDao.updateProduction(bean);
                ScheduleDO sBean=new ScheduleDO();
                sBean.setPreOperation(bean.getProStatus());
                ScheduleDO schedule=new ScheduleDO(bean.getProNumber(), currentUser, "预投产验证通过", sBean.getPreOperation(), sBean.getPreOperation(), "预投产验证已通过");
                operationProductionDao.insertSchedule(schedule);
                //是否预投产验证为“是”时，预投产验证结果为“通过”，需求当前阶段变更为“完成预投产”
                if (bean.getIsAdvanceProduction().equals("是") && bean.getProAdvanceResult().equals("通过")) {
                    DemandDO demand = reqTaskService.findById1(bean.getProNumber());
                    if (!JudgeUtils.isNull(demand)) {
                        demand.setPreCurPeriod("160");
                        DemandBO demandBO =  new DemandBO();
                        BeanUtils.copyPropertiesReturnDest(demandBO, demand);
                        reqTaskService.update(demandBO);
                    }
                }
            }
            return ;//ajaxDoneSuccess("预投产验证通过");
        }
        String pro_status_after = "";
        String pro_status_before = "";
        if (pro_number_list[0].equals("dtc")) {
            if (pro_number_list.length == 1) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            pro_status_before = "投产提出";
            pro_status_after = "投产待部署";
            scheduleBean.setOperationReason("通过");
        }
        else if (pro_number_list[0].equals("dh")) {
            pro_status_before = "投产提出";
            pro_status_after = "投产打回";
            if ((pro_number_list.length == 1) || (pro_number_list.length == 2)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            scheduleBean.setOperationReason(pro_number_list[1]);
        }
        else if (pro_number_list[0].equals("dyz")) {
            if (pro_number_list.length == 1){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            pro_status_before = "投产待部署";
            pro_status_after = "部署完成待验证";
            scheduleBean.setOperationReason("通过");
        } else if (pro_number_list[0].equals("qx")) {
            pro_status_before = "投产待部署";
            pro_status_after = "投产取消";
            if ((pro_number_list.length == 1) || (pro_number_list.length == 2)){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            scheduleBean.setOperationReason(pro_number_list[1]);
        } else if (pro_number_list[0].equals("yzwc")) {
            if (pro_number_list.length == 1){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            pro_status_before = "部署完成待验证";
            pro_status_after = "投产验证完成";
            scheduleBean.setOperationReason("通过");
        } else if (pro_number_list[0].equals("ht")) {
            pro_status_before = "部署完成待验证";
            pro_status_after = "投产回退";
            if ((pro_number_list.length == 1) || (pro_number_list.length == 2)){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            scheduleBean.setOperationReason(pro_number_list[1]);
        }
        scheduleBean.setPreOperation(pro_status_before);
        scheduleBean.setAfterOperation(pro_status_after);
        scheduleBean.setOperationType(pro_status_after);
        for (int i = 2; i < pro_number_list.length; ++i) {
            String status = operationProductionDao.findProductionBean(pro_number_list[i]).getProStatus();
            if (pro_status_after.equals("投产取消")) {
                String applicant = operationProductionDao.findProductionBean(pro_number_list[i]).getProApplicant();
                String pro_manager = operationProductionDao.findProductionBean(pro_number_list[i]).getProManager();
                if ((!(currentUser.equals(applicant))) && (!(currentUser.equals(pro_manager)))) {
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("您不是投产申请人或者负责该投产的产品经理!");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
            }
            if(!(pro_status_before.equals(status) || (pro_status_after.equals("投产打回") && (status.equals("投产待部署")) ) || (pro_status_after.equals("投产回退") && status.equals("投产验证完成")) ||(pro_status_after.equals("投产取消") && (status.equals("投产提出")|| status.equals("投产待部署"))))){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择符合当前操作类型的正确投产状态!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }
        for (int j = 2; j < pro_number_list.length; ++j) {
            String status = operationProductionDao.findProductionBean(pro_number_list[j]).getProStatus();
            scheduleBean.setProNumber(pro_number_list[j]);
            scheduleBean.setPreOperation(status);

            boolean isSend = true;

            if ((((pro_status_before.equals(status)) || ((pro_status_after.equals("投产打回")) && (status.equals("投产待部署"))) || ((pro_status_after.equals("投产回退")) && (status.equals("投产验证完成"))) || ((pro_status_after.equals("投产取消")) && (((status.equals("投产提出")) || (status.equals("投产待部署"))))))) && ((
                    (pro_status_after.equals("投产打回")) || (pro_status_after.equals("投产回退")) || (pro_status_after.equals("投产取消")))))
            {
                MailFlowConditionDO mfva = new MailFlowConditionDO();
                mfva.setEmployeeName(operationProductionDao.findProductionBean(pro_number_list[j]).getProApplicant());
                MailFlowDO mfba = operationProductionDao.searchUserEmail(mfva);
                ProductionDO bean = operationProductionDao.findProductionBean(pro_number_list[j]);
                MailFlowDO bnb = new MailFlowDO("投产不合格结果反馈", "code_review@hisuntech.com", mfba.getEmployeeEmail(), "");

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

                //String[] mailToAddress = mfba.getEmployeeEmail().split(";");
                String[] mailToAddress = {"tu_yi@hisuntech.com","wu_lr@hisuntech.com","huangyan@hisuntech.com"};
                mailInfo.setToAddress(mailToAddress);
                String mess = null;
                if (pro_status_after.equals("投产打回")) {
                    mess = pro_status_after;
                }
                if (pro_status_after.equals("投产回退")) {
                    mess = pro_status_after;
                }
                if (pro_status_after.equals("投产取消")) {
                    mess = pro_status_after;
                }

                mailInfo.setSubject("【" + mess + "通知】");
                mailInfo.setContent("你好:<br/>由于【" + pro_number_list[1] + "】，您的" + pro_number_list[j] + bean.getProNeed() + ",中止投产流程。");

                // 这个类主要来发送邮件
                SimpleMailSender sms = new SimpleMailSender();
                isSend = sms.sendHtmlMail(mailInfo);

                operationProductionDao.addMailFlow(bnb);
            }

            ProductionDO productionBean = operationProductionDao.findProductionBean(pro_number_list[j]);

            if ((((!(productionBean.getProType().equals("正常投产"))) || (!(productionBean.getIsOperationProduction().equals("是"))))) && (productionBean.getProStatus().equals("部署完成待验证")))
            {
                MailFlowConditionDO mfva = new MailFlowConditionDO();
                mfva.setEmployeeName(operationProductionDao.findProductionBean(pro_number_list[j]).getProApplicant());
                MailFlowDO mfba = operationProductionDao.searchUserEmail(mfva);

                MailFlowConditionDO mfwa = new MailFlowConditionDO();
                mfwa.setEmployeeName(operationProductionDao.findProductionBean(pro_number_list[j]).getIdentifier());
                MailFlowDO mfaa = operationProductionDao.searchUserEmail(mfwa);

                List<ProductionDO> bean=new ArrayList<ProductionDO>();
                bean.add(operationProductionDao.findExportExcelList(pro_number_list[j]));
                File file = sendExportExcel_Result(bean);

                MailFlowDO bnb = new MailFlowDO("投产部署完成待验证结果反馈", "code_review@hisuntech.com", mfba.getEmployeeEmail() + ";" + mfaa.getEmployeeEmail(), file.getName(), "");
                MailSenderInfo mailInfo = new MailSenderInfo();
                mailInfo.setMailServerHost("smtp.qiye.163.com");
                mailInfo.setMailServerPort("25");
                mailInfo.setValidate(true);
                mailInfo.setUserName("code_review@hisuntech.com");
                mailInfo.setPassword("hisun@248!@#");
                mailInfo.setFromAddress("code_review@hisuntech.com");

                Vector filesv = new Vector();
                filesv.add(file);
                mailInfo.setFile(filesv);

                //String[] mailToAddress = (mfba.getEmployeeEmail()+";"+mfaa.getEmployeeEmail()).split(";");
                String[] mailToAddress = {"tu_yi@hisuntech.com","wu_lr@hisuntech.com","huangyan@hisuntech.com"};
                mailInfo.setToAddress(mailToAddress);
                StringBuffer sb = new StringBuffer();
                if (productionBean.getProType().equals("救火更新")) {
                    mailInfo.setSubject("【救火更新部署完成待验证结果通知】-" + productionBean.getProNeed() + "-" + productionBean.getProNumber() + "-" + productionBean.getProApplicant());
                    sb.append("<table border='1' style='border-collapse: collapse;background-color: white; white-space: nowrap;'>");
                    sb.append("<tr><td colspan='6' style='text-align: center;font-weight: bold;'>救火更新结果反馈</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>更新标题</td><td colspan='5'>" + productionBean.getProNeed() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>申请部门</td><td>" + productionBean.getApplicationDept() + "</td><td style='font-weight: bold;'>申请人</td><td>" + productionBean.getProApplicant() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getApplicantTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>投产编号</td><td>" + productionBean.getProNumber() + "</td><td style='font-weight: bold;'>复核人</td><td>" + productionBean.getProChecker() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getCheckerTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>验证测试类型</td><td>" + productionBean.getValidation() + "</td><td style='font-weight: bold;'>验证人</td><td>" + productionBean.getIdentifier() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getIdentifierTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>不走正常投产原因</td><td colspan='5'>" + productionBean.getUrgentReasonPhrase() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>当天不投产的影响</td><td colspan='5'>" + productionBean.getNotProductionImpact() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>更新要求完成时间</td><td colspan='5'>" + productionBean.getCompletionUpdate() + "</td></tr>");
                    sb.append("<tr><td colspan='6' style='font-weight: bold;'>如需提前至当日24点前更新，需补充填写以下内容：</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>提前实施原因</td><td colspan='5'>" + productionBean.getEarlyImplementation() + "</td></tr>");
                    sb.append("<tr><td rowspan='2' style='font-weight: bold;' >是否影响客户使用</td><td  rowspan='2'>" + productionBean.getInfluenceUse() + "</td>");
                    sb.append("<td style='font-weight: bold;' >如不影响客户使用，请简要描述原因</td><td colspan='3'>" + productionBean.getInfluenceUseReason() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;' >如影响客户使用，描述具体影响范围</td><td colspan='3'>" + productionBean.getInfluenceUseInf() + "</td></tr>");
                    sb.append(" <tr><td style='font-weight: bold;'>更新时间及预计操作时长</td><td colspan='5'>" + productionBean.getOperatingTime() + "</td></tr></table>");
                }
                if ((productionBean.getProType().equals("正常投产")) && (productionBean.getIsOperationProduction().equals("否")))
                {
                    mailInfo.setSubject("【正常投产(非投产日)部署完成待验证结果通知】-" + productionBean.getProNeed() + "-" + productionBean.getProNumber() + "-" + productionBean.getProApplicant());
                    sb.append("<table border='1' style='border-collapse: collapse;background-color: white; white-space: nowrap;'>");
                    sb.append("<tr><td colspan='6' style='text-align: center;font-weight: bold;'>正常投产(非投产日)结果反馈</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>需求名称及内容简述</td><td colspan='5'>" + productionBean.getProNeed() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>申请部门</td><td>" + productionBean.getApplicationDept() + "</td><td style='font-weight: bold;'>申请人</td><td>" + productionBean.getProApplicant() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getApplicantTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>投产编号</td><td>" + productionBean.getProNumber() + "</td><td style='font-weight: bold;'>复核人</td><td>" + productionBean.getProChecker() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getCheckerTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>验证测试类型</td><td>" + productionBean.getValidation() + "</td><td style='font-weight: bold;'>验证人</td><td>" + productionBean.getIdentifier() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getIdentifierTel() + "</td></tr>");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sb.append("<tr><td style='font-weight: bold;'>计划投产日期</td><td>" + sdf.format(productionBean.getProDate()) + "</td><td style='font-weight: bold;'>产品所属模块</td><td>" + productionBean.getProModule() + "</td>");
                    sb.append("<td style='font-weight: bold;'>基地业务负责人</td><td>" + productionBean.getBusinessPrincipal() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>产品经理</td><td>" + productionBean.getProManager() + "</td><td style='font-weight: bold;'>是否涉及证书</td><td>" + productionBean.getIsRefCerificate() + "</td>");
                    sb.append("<td style='font-weight: bold;'>开发负责人</td><td>" + productionBean.getDevelopmentLeader() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>审批人</td><td>" + productionBean.getApprover() + "</td><td style='font-weight: bold;'>是否需要运维监控</td><td>" + productionBean.getProOperation() + "</td>");
                    if ((productionBean.getUpdateOperator() != null) && (!(productionBean.getUpdateOperator().equals("")))) {
                        sb.append("<td style='font-weight: bold;'>版本更新操作人</td><td>" + productionBean.getUpdateOperator() + "</td></tr>");
                    }
                    else {
                        sb.append("<td style='font-weight: bold;'>版本更新操作人</td><td>暂未录入</td></tr>");
                    }
                    if (productionBean.getIsAdvanceProduction().equals("否")) {
                        sb.append("<tr><td style='font-weight: bold;'>不做预投产验证原因</td><td colspan='5'>" + productionBean.getNotAdvanceReason() + "</td></tr>");
                    }
                    sb.append("<tr><td style='font-weight: bold;'>不走正常投产原因</td><td colspan='5'>" + productionBean.getUnusualReasonPhrase() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>备注 (影响范围,其它补充说明)</td><td colspan='5'>" + productionBean.getRemark() + "</td></tr></table>");
                }

                mailInfo.setContent("你好：<br/>&nbsp;&nbsp;本次投产部署完成，请知悉。谢谢！<br/>" + sb.toString());

                SimpleMailSender sms = new SimpleMailSender();
                isSend = sms.sendHtmlMail(mailInfo);

                operationProductionDao.addMailFlow(bnb);
                if ((file.isFile()) && (file.exists())) {
                    file.delete();
                }
            }
            if ((((!(productionBean.getProType().equals("正常投产"))) || (!(productionBean.getIsOperationProduction().equals("是"))))) && (productionBean.getProStatus().equals("投产验证完成")))
            {
                List bean = new ArrayList();
                bean.add(operationProductionDao.findExportExcelList(pro_number_list[j]));
                File file = sendExportExcel_Result( bean);

                MailFlowDO bnb = new MailFlowDO("投产申请结果反馈", "code_review@hisuntech.com", productionBean.getMailRecipient(), file.getName(), "");

                MailSenderInfo mailInfo = new MailSenderInfo();

                mailInfo.setMailServerHost("smtp.qiye.163.com");

                mailInfo.setMailServerPort("25");

                mailInfo.setValidate(true);

                mailInfo.setUserName("code_review@hisuntech.com");
                mailInfo.setPassword("hisun@248!@#");
                mailInfo.setFromAddress("code_review@hisuntech.com");

                Vector filesv = new Vector();
                filesv.add(file);
                mailInfo.setFile(filesv);

                //String[] mailToAddress = productionBean.getMailRecipient().split(";");
                String[] mailToAddress = {"tu_yi@hisuntech.com","wu_lr@hisuntech.com","huangyan@hisuntech.com"};
                mailInfo.setToAddress(mailToAddress);
                mailInfo.setCcs(productionBean.getMailCopyPerson().split(";"));
                StringBuffer sb = new StringBuffer();
                if (productionBean.getProType().equals("救火更新")) {
                    mailInfo.setSubject("【救火更新投产结果通报】-" + productionBean.getProNeed() + "-" + productionBean.getProNumber() + "-" + productionBean.getProApplicant());
                    sb.append("<table border='1' style='border-collapse: collapse;background-color: white; white-space: nowrap;'>");
                    sb.append("<tr><td colspan='6' style='text-align: center;font-weight: bold;'>救火更新结果反馈</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>更新标题</td><td colspan='5'>" + productionBean.getProNeed() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>申请部门</td><td>" + productionBean.getApplicationDept() + "</td><td style='font-weight: bold;'>申请人</td><td>" + productionBean.getProApplicant() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getApplicantTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>投产编号</td><td>" + productionBean.getProNumber() + "</td><td style='font-weight: bold;'>复核人</td><td>" + productionBean.getProChecker() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getCheckerTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>验证测试类型</td><td>" + productionBean.getValidation() + "</td><td style='font-weight: bold;'>验证人</td><td>" + productionBean.getIdentifier() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getIdentifierTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>不走正常投产原因</td><td colspan='5'>" + productionBean.getUrgentReasonPhrase() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>当天不投产的影响</td><td colspan='5'>" + productionBean.getNotProductionImpact() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>更新要求完成时间</td><td colspan='5'>" + productionBean.getCompletionUpdate() + "</td></tr>");
                    sb.append("<tr><td colspan='6' style='font-weight: bold;'>如需提前至当日24点前更新，需补充填写以下内容：</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>提前实施原因</td><td colspan='5'>" + productionBean.getEarlyImplementation() + "</td></tr>");
                    sb.append("<tr><td rowspan='2' style='font-weight: bold;' >是否影响客户使用</td><td  rowspan='2'>" + productionBean.getInfluenceUse() + "</td>");
                    sb.append("<td style='font-weight: bold;' >如不影响客户使用，请简要描述原因</td><td colspan='3'>" + productionBean.getInfluenceUseReason() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;' >如影响客户使用，描述具体影响范围</td><td colspan='3'>" + productionBean.getInfluenceUseInf() + "</td></tr>");
                    sb.append(" <tr><td style='font-weight: bold;'>更新时间及预计操作时长</td><td colspan='5'>" + productionBean.getOperatingTime() + "</td></tr></table>");
                }
                if ((productionBean.getProType().equals("正常投产")) && (productionBean.getIsOperationProduction().equals("否")))
                {
                    mailInfo.setSubject("【正常投产(非投产日)投产结果通报】-" + productionBean.getProNeed() + "-" + productionBean.getProNumber() + "-" + productionBean.getProApplicant());
                    sb.append("<table border='1' style='border-collapse: collapse;background-color: white; white-space: nowrap;'>");
                    sb.append("<tr><td colspan='6' style='text-align: center;font-weight: bold;'>正常投产(非投产日)结果反馈</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>需求名称及内容简述</td><td colspan='5'>" + productionBean.getProNeed() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>申请部门</td><td>" + productionBean.getApplicationDept() + "</td><td style='font-weight: bold;'>申请人</td><td>" + productionBean.getProApplicant() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getApplicantTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>投产编号</td><td>" + productionBean.getProNumber() + "</td><td style='font-weight: bold;'>复核人</td><td>" + productionBean.getProChecker() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getCheckerTel() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>验证测试类型</td><td>" + productionBean.getValidation() + "</td><td style='font-weight: bold;'>验证人</td><td>" + productionBean.getIdentifier() + "</td>");
                    sb.append("<td style='font-weight: bold;'>联系方式</td><td>" + productionBean.getIdentifierTel() + "</td></tr>");
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                    sb.append("<tr><td style='font-weight: bold;'>计划投产日期</td><td>" + sdf.format(productionBean.getProDate()) + "</td><td style='font-weight: bold;'>产品所属模块</td><td>" + productionBean.getProModule() + "</td>");
                    sb.append("<td style='font-weight: bold;'>基地业务负责人</td><td>" + productionBean.getBusinessPrincipal() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>产品经理</td><td>" + productionBean.getProManager() + "</td><td style='font-weight: bold;'>是否涉及证书</td><td>" + productionBean.getIsRefCerificate() + "</td>");
                    sb.append("<td style='font-weight: bold;'>开发负责人</td><td>" + productionBean.getDevelopmentLeader() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>审批人</td><td>" + productionBean.getApprover() + "</td><td style='font-weight: bold;'>是否需要运维监控</td><td>" + productionBean.getProOperation() + "</td>");
                    if ((productionBean.getUpdateOperator() != null) && (!(productionBean.getUpdateOperator().equals("")))) {
                        sb.append("<td style='font-weight: bold;'>版本更新操作人</td><td>" + productionBean.getUpdateOperator() + "</td></tr>");
                    }
                    else {
                        sb.append("<td style='font-weight: bold;'>版本更新操作人</td><td>暂未录入</td></tr>");
                    }
                    if (productionBean.getIsAdvanceProduction().equals("否")) {
                        sb.append("<tr><td style='font-weight: bold;'>不做预投产验证原因</td><td colspan='5'>" + productionBean.getNotAdvanceReason() + "</td></tr>");
                    }
                    sb.append("<tr><td style='font-weight: bold;'>不走正常投产原因</td><td colspan='5'>" + productionBean.getUnusualReasonPhrase() + "</td></tr>");
                    sb.append("<tr><td style='font-weight: bold;'>备注 (影响范围,其它补充说明)</td><td colspan='5'>" + productionBean.getRemark() + "</td></tr></table>");
                }

                mailInfo.setContent("你好：<br/>&nbsp;&nbsp;本次投产验证完成，请知悉。谢谢！<br/>" + sb.toString());

                SimpleMailSender sms = new SimpleMailSender();
                isSend = sms.sendHtmlMail(mailInfo);

                operationProductionDao.addMailFlow(bnb);
                if ((file.isFile()) && (file.exists())) {
                    file.delete();
                }
            }
            operationProductionDao.insertSchedule(scheduleBean);
            operationProductionDao.updateProduction(new ProductionDO(pro_number_list[j], pro_status_after));

            DemandDO demand = reqTaskService.findById1(pro_number_list[j]);
            if (!JudgeUtils.isNull(demand)) {
                //投产状态为“投产待部署”时，需求当前阶段变更为“待投产”  16
                if (pro_status_after.equals("投产待部署")) {
                    demand.setPreCurPeriod("170");
                    DemandBO demandBO =  new DemandBO();
                    BeanUtils.copyPropertiesReturnDest(demandBO, demand);
                    reqTaskService.update(demandBO);
                }else if (pro_status_after.equals("投产验证完成")) {
                    //投产状态为“投产验证完成”时，需求当前阶段为“已投产”  17
                    demand.setPreCurPeriod("180");
                    demand.setReqSts("50");
                    DemandBO demandBO =  new DemandBO();
                    BeanUtils.copyPropertiesReturnDest(demandBO, demand);
                    reqTaskService.update(demandBO);
                }
            }
            if (!(isSend)) {
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("批量操作成功,但您有邮件没有发送成功,请及时联系系统维护人员!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }
        return ;//ajaxDoneSuccess("批量操作成功");
    }

    @Override
    public String findManagerMailByUserName(List<String> userNames) {
        return operationProductionDao.findManagerMailByUserName(userNames);
    }

    @Override
    public void addMailFlow(MailFlowBean bean) {
        operationProductionDao.insertMailFlow(bean);
    }

    @Override
    public File exportUnusualExcel(
                                   List<ProductionBO> list)  {
        String fileName = "正常投产(非投产日)申请表" + DateUtil.date2String(new Date(), "yyyyMMddhhmmss") + ".xls";
        File file=null;
        try {
            //todo 写死路径
            //String path="D:\\home\\devadm\\temp\\import";
            String path = "/home/devadm/temp/import/";
            String filePath = path + fileName;
            ExcelUnusualListUtil util = new ExcelUnusualListUtil();
            util.createExcel(filePath, list,null);
            file=new File(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }

    @Override
    public File exportUrgentExcel(List<ProductionBO> list){
        String fileName = "救火更新申请表" + DateUtil.date2String(new Date(), "yyyyMMddhhmmss") + ".xls";
        File file=null;
        try {
            //todo 写死路径
            //String path="D:\\home\\devadm\\temp\\import";
            String path = "/home/devadm/temp/import/";
            String filePath = path + fileName;
            ExcelUrgentListUtil util = new ExcelUrgentListUtil();
            util.createExcel(filePath, list,null);
            file=new File(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return file;

    }

    @Override
    public ProductionBO searchProdutionDetail(String proNumber) {
        ProductionBO productionBO=null;
        ProductionDO productionBean = operationProductionDao.findProductionBean(proNumber);
        if(productionBean!=null) {
            productionBO= BeanUtils.copyPropertiesReturnDest(new ProductionBO(), productionBean);
        }
        return productionBO;
    }

    @Override
    public void updateAllProduction(ProductionBO bean) {
        operationProductionDao.updateAllProduction(BeanUtils.copyPropertiesReturnDest(new ProductionDO(), bean));
    }

    @Override
    public void addScheduleBean(ScheduleDO scheduleBean) {
        operationProductionDao.insertSchedule(scheduleBean);
    }

    @Override
    public void addProduction(ProductionBO bean) {
        operationProductionDao.insertProduction(BeanUtils.copyPropertiesReturnDest(new ProductionDO(), bean));
    }

    @Override
    public void addProductionPicBean(ProductionPicDO productionPicDO) {
        productionPicDao.insert(productionPicDO);
    }

    @Override
    public void updateMailGroup(MailGroupBO mailGroupBO) {
        MailGroupDO mailGroupDO = BeanUtils.copyPropertiesReturnDest(new MailGroupDO(), mailGroupBO);
        operationProductionDao.updateMailGroup(mailGroupDO);
    }

    /*
    *投产不及时验证清单发送企业微信
    * */
    //TODO 添加定时
   // @Scheduled
    @Override
    public void productionVerificationIsNotTimely() {
        String date="2019-10-01";
        List<ProductionDO> productionDOList = getProductionVerificationIsNotTimely(date);
        Map<String, Integer> map = new HashMap<>();
        productionDOList.forEach(m->{
            String applicationDept = m.getApplicationDept();
            boolean exist = map.containsKey(applicationDept);
            if(exist){
                map.put(applicationDept,map.get(applicationDept)+1);
            }else{
                map.put(applicationDept,1);
            }
        });

        String body="投产验证不及时清单汇总"+"\n"
            ;
        for(Map.Entry<String, Integer> entry : map.entrySet()){
            String mapKey = entry.getKey();
            Integer mapValue = entry.getValue();
            body=body+ mapKey+":"+mapValue+"条"+"\n";
        }
        body=body+"详情如下";
        SendExcelProductionVerificationIsNotTimely sendExcelProductionVerificationIsNotTimely = new SendExcelProductionVerificationIsNotTimely();
        File file=null;
        try{
            String excel = sendExcelProductionVerificationIsNotTimely.createExcel("D:\\投产验证不及时清单.xls", productionDOList, null);
            file=new File("D:\\投产验证不及时清单.xls");
        }catch (Exception e){
            e.printStackTrace();
        }
        boardcastScheduler.test(body,file);
        file.delete();



    }
    /**
     * @param date 天数
     *  计算天数内投产验证不及时清单
     */
    @Override
    public List<ProductionDO> getProductionVerificationIsNotTimely(String date) {
        ProductionDO productionDO = new ProductionDO();
        try {
        productionDO.setProStatus("部署完成待验证");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Date date1 = sdf.parse(date);
        productionDO.setProDate(new java.sql.Date(date1.getTime()));
        }catch ( Exception e){
            e.printStackTrace();
        }
        return operationProductionDao.getProductionVerificationIsNotTimely(productionDO);
    }


    @Override
    public MailGroupRspBO searchMailGroupList(MailGroupBO mailGroupBO) {
        MailGroupDO mailGroupDO = BeanUtils.copyPropertiesReturnDest(new MailGroupDO(), mailGroupBO);

        PageInfo<MailGroupBO> pageInfo = PageUtils.pageQueryWithCount(mailGroupBO.getPageNum(), mailGroupBO.getPageSize(),
                () -> BeanConvertUtils.convertList(operationProductionDao.findMailGroup(mailGroupDO), MailGroupBO.class));
        List<MailGroupBO> mailGroupBOList = BeanConvertUtils.convertList(pageInfo.getList(), MailGroupBO.class);
        MailGroupRspBO mailGroupRspBO = new MailGroupRspBO();
        mailGroupRspBO.setMailGroupBOList(mailGroupBOList);
        mailGroupRspBO.setPageInfo(pageInfo);
        return mailGroupRspBO;
    }


    public File sendExportExcel_Result(List<ProductionDO> list){
        String fileName = "生产验证结果表" + DateUtil.date2String(new Date(), "yyyyMMddhhmmss") + ".xls";
        File file=null;
        try {
            //String path = "C:\\home\\devadm\\temp\\propkg";
            String path = "/home/devadm/temp/propkg/";
            String filePath = path + fileName;
            SendExcelOperationResultProductionUtil util = new SendExcelOperationResultProductionUtil();
            util.createExcel(filePath, list,null);
            file=new File(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
    public File sendExportExcel_out(List<ProductionDO> list){
        String fileName = "投产记录通报清单" + DateUtil.date2String(new Date(), "yyyyMMddhhmmss") + ".xls";
        File file=null;
        try {
            //String path = "C:\\home\\devadm\\temp\\propkg";
            String path = "/home/devadm/temp/propkg/";
            String filePath = path + fileName;
            SendExcelOperationProductionUtil util = new SendExcelOperationProductionUtil();
            util.createExcel(filePath, list,null);
            file=new File(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
    //根据日期获取礼拜
    public static String testDate(String newtime) {
        String dayNames[] = {"星期日","星期一","星期二","星期三","星期四","星期五","星期六"};
        Calendar c = Calendar.getInstance();// 获得一个日历的实例
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        try {
            c.setTime(sdf.parse(newtime));
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return dayNames[c.get(Calendar.DAY_OF_WEEK)-1];
    }
    //投产清单通报
    @Override
    public void sendGoExport(HttpServletRequest request, HttpServletResponse response, String taskIdStr){
        String[] pro_number_list=taskIdStr.split("~");
        if(pro_number_list[0].equals("1")){
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("请填写必填信息!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if(pro_number_list.length==2){
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产进行操作!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        List<ProductionDO> list=new ArrayList<ProductionDO>();
        StringBuffer sbfStr = new StringBuffer();
        for(int i=2;i<pro_number_list.length;i++){
            ProductionDO bean=operationProductionDao.findExportExcelList(pro_number_list[i]);
            if(!(bean.getProType().equals("正常投产") && bean.getIsOperationProduction().equals("是"))){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产日正常投产类型发送!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if(!bean.getProStatus().equals("投产待部署") && !bean.getProStatus().equals("投产提出")){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产提出或者待部署状态投产发送!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
//			bean.setUpdate_operator("郑景楠、尹均辉 ");
            bean.setUpdateOperator(pro_number_list[0]);
            operationProductionDao.updateAllProduction(bean);
            list.add(bean);
            if(bean.getMailLeader() !=null && !bean.getMailLeader().equals("")){
                if(sbfStr.length()<1){
                    sbfStr.append(bean.getMailLeader());
                }else{
                    sbfStr.append(";"+bean.getMailLeader());
                }
            }
        }
        File file=sendExportExcel_out(list);
        MailGroupDO mp=operationProductionDao.findMailGroupBeanDetail("1");
        MailSenderInfo mailInfo = new MailSenderInfo();
        // 设置邮件服务器类型
        mailInfo.setMailServerHost("smtp.qiye.163.com");
        //设置端口号
        mailInfo.setMailServerPort("25");
        //设置是否验证
        mailInfo.setValidate(true);
        //设置用户名、密码、发送人地址
        mailInfo.setUserName(Constant.P_EMAIL_NAME);
        mailInfo.setPassword(Constant.P_EMAIL_PSWD);// 您的邮箱密码
        mailInfo.setFromAddress(Constant.P_EMAIL_NAME);
        /**
         * 附件
         */
        Vector<File> files = new Vector<File>() ;
        files.add(file) ;
        mailInfo.setFile(files) ;
        /**
         * 收件人邮箱
         */
        String[] mailToAddressDemo = null;
        if(sbfStr !=null && sbfStr.length()>0){
            mailToAddressDemo = ("wang_lu@justinmobile.com;hu_yi@justinmobile.com;fu_yz@justinmobile.com;"+sbfStr.toString()+";"+mp.getMailUser()).split(";");
        }else{
            mailToAddressDemo = ("wang_lu@justinmobile.com;hu_yi@justinmobile.com;fu_yz@justinmobile.com;"+mp.getMailUser()).split(";");
        }

        //收件人去重复
        List<String> result = new ArrayList<String>();
        boolean flag;
        for(int i=0;i<mailToAddressDemo.length;i++){
            flag = false;
            for(int j=0;j<result.size();j++){
                if(mailToAddressDemo[i].equals(result.get(j))){
                    flag = true;
                    break;
                }
            }
            if(!flag){
                result.add(mailToAddressDemo[i]);
            }
        }
        //String[] mailToAddress = (String[]) result.toArray(new String[result.size()]);
        String[] mailToAddress = {"tu_yi@hisuntech.com","wu_lr@hisuntech.com","huangyan@hisuntech.com"};
        mailInfo.setToAddress(mailToAddress);
        mailInfo.setSubject("【投产清单通报】");
        //记录邮箱信息
        MailFlowDO bn=new MailFlowDO("投产清单通报",Constant.P_EMAIL_NAME, mp.getMailUser()+";"+sbfStr, "" ,"");
        //组织发送内容
        StringBuffer sb=new StringBuffer();
        sb.append("<table border ='1' style='width:3000px;border-collapse: collapse;background-color: white;'>");
        sb.append("<tr><th>投产编号</th><th>需求名称及内容简述</th><th>投产类型</th><th>计划投产日期</th>");
        sb.append("<th>申请部门</th><th>投产申请人</th><th>申请人联系方式</th><th>产品所属模块</th><th>业务需求提出人</th>");
        sb.append("<th>基地负责人</th><th>产品经理</th><th>投产状态</th><th>是否更新数据库数据</th><th>是否更新数据库(表)结构</th>");
        sb.append("<th>投产后是否需要运维监控</th><th>是否涉及证书</th><th>是否预投产验证</th><th>不能预投产验证原因</th><th>预投产验证结果</th>");
        sb.append("<th>验证人</th><th>验证人联系方式</th><th>验证复核人</th><th>验证复核人联系方式</th><th>生产验证方式</th>");
        sb.append("<th>开发负责人</th><th>审批人</th><th>版本更新操作人</th><th>备注 (影响范围,其它补充说明)</th></tr>");
        for(int i=2;i<pro_number_list.length;i++){

            ProductionDO bean=operationProductionDao.findExportExcelList(pro_number_list[i]);
            String proNumber = bean.getProNumber();
            if(bean.getProNumber().startsWith("REQ")){
                proNumber = bean.getProNumber().substring(4,bean.getProNumber().length()).toString();
            }
            sb.append("<tr><td>"+proNumber+"</td>");//投产编号
            sb.append("<td >"+bean.getProNeed()+"</td>");//需求名称及内容简述
            sb.append("<td style='white-space: nowrap;'>"+bean.getProType()+"</td>");//投产类型
            // 日期转换
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            if(bean.getProDate()!=null)
                sb.append("<td style='white-space: nowrap;'>"+sdf.format(bean.getProDate())+"</td>");//计划投产日期
            sb.append("<td style='white-space: nowrap;'>"+bean.getApplicationDept()+"</td>");//申请部门
            sb.append("<td style='white-space: nowrap;'>"+bean.getProApplicant()+"</td>");//投产申请人
            sb.append("<td style='white-space: nowrap;'>"+bean.getApplicantTel()+"</td>");//申请人联系方式
            sb.append("<td style='white-space: nowrap;'>"+bean.getProModule()+"</td>");//产品所属模块
            sb.append("<td style='white-space: nowrap;'>"+bean.getBusinessPrincipal()+"</td>");//业务需求提出人
            sb.append("<td style='white-space: nowrap;'>"+bean.getBasePrincipal()+"</td>");//基地负责人
            sb.append("<td style='white-space: nowrap;'>"+bean.getProManager()+"</td>");//产品经理
            sb.append("<td style='white-space: nowrap;'>"+bean.getProStatus()+"</td>");//投产状态
            sb.append("<td >"+bean.getIsUpDatabase()+"</td>");//是否更新数据库数据
            sb.append("<td >"+bean.getIsUpStructure()+"</td>");//是否更新数据库(表)结构
            sb.append("<td >"+bean.getProOperation()+"</td>");//投产后是否需要运维监控
            sb.append("<td >"+bean.getIsRefCerificate()+"</td>");//是否涉及证书
            sb.append("<td >"+bean.getIsAdvanceProduction()+"</td>");//是否预投产验证
            if(bean.getNotAdvanceReason()!=null && !bean.getNotAdvanceReason().equals("")){
                sb.append("<td >"+bean.getNotAdvanceReason()+"</td>");//不能预投产验证原因
            }else{
                sb.append("<td ></td>");//预投产验证结果
            }
            if(bean.getProAdvanceResult()!=null && !bean.getProAdvanceResult().equals("")){
                sb.append("<td >"+bean.getProAdvanceResult()+"</td>");//预投产验证结果
            }else{
                sb.append("<td ></td>");//预投产验证结果
            }
            sb.append("<td style='white-space: nowrap;'>"+bean.getIdentifier()+"</td>");//验证人
            sb.append("<td style='white-space: nowrap;'>"+bean.getIdentifierTel()+"</td>");//验证人联系方式
            sb.append("<td style='white-space: nowrap;'>"+bean.getProChecker()+"</td>");//验证复核人
            sb.append("<td style='white-space: nowrap;'>"+bean.getCheckerTel()+"</td>");//验证复核人联系方式
            sb.append("<td style='white-space: nowrap;'>"+bean.getValidation()+"</td>");//生产验证方式
            sb.append("<td style='white-space: nowrap;'>"+bean.getDevelopmentLeader()+"</td>");//开发负责人
            sb.append("<td style='white-space: nowrap;'>"+bean.getApprover()+"</td>");//审批人
            sb.append("<td style='white-space: nowrap;'>"+bean.getUpdateOperator()+"</td>");
            sb.append("<td style='width:100px;'>"+bean.getRemark()+"</td></tr>");//备注(更新原因及影响范围详细说明)
        }
        sb.append("</table>");
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String format = simpleDateFormat.format(new Date());
        String testDate = testDate(format);
        String change = "计划";
        if(testDate.equals("星期三") || testDate.equals("星期四") || testDate.equals("星期五")){
            change = "最终";
        }
        mailInfo.setContent("大家好!<br/>&nbsp;&nbsp; 以下是"+change+"本周投产清单,烦请需求负责人提前做好投产前的风险评估与评审准备工作。" +
                "本周产品投产更新牵头负责人是"+pro_number_list[1]+",请各生产验证负责人将验证结果反馈给"+pro_number_list[1]+"。无特殊原因，投产后验证工作需在投产当晚完成，请知晓。<br/>如有任何问题请及时反馈与沟通。<br/>"+sb.toString());
        SimpleMailSender sms = new SimpleMailSender();
        boolean isSend=sms.sendHtmlMail(mailInfo);// 发送html格式
        if(isSend){
            operationProductionDao.addMailFlow(bn);
            if(file.isFile() && file.exists()){
                file.delete();
            }
        }else{
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("邮件发送失败!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        return ;//ajaxDoneSuccess("邮件发送成功！");
    }
    // 投产结果通报
    @Override
    public void sendGoExportResult(HttpServletRequest request, HttpServletResponse response, String taskIdStr){
        String[] pro_number_list=taskIdStr.split("~");
        List<ProductionDO> list=new ArrayList<ProductionDO>();
        StringBuffer sbfStr = new StringBuffer();
        for(int i=0;i<pro_number_list.length;i++){
            ProductionDO bean=operationProductionDao.findProductionBean(pro_number_list[i]);
            if(!(bean.getProType().equals("正常投产") && bean.getIsOperationProduction().equals("是"))){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择投产日正常投产类型发送");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            if(!bean.getProStatus().equals("部署完成待验证") && !bean.getProStatus().equals("投产验证完成")){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择部署完成待验证或者投产验证完成的投产状态发送!");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
            list.add(bean);
            if(bean.getMailLeader() !=null && !bean.getMailLeader().equals("")){
                if(sbfStr.length()<1){
                    sbfStr.append(bean.getMailLeader());
                }else{
                    sbfStr.append(";"+bean.getMailLeader());
                }
            }
        }
        File file=sendExportExcel_Result(list);
        MailGroupDO mp=operationProductionDao.findMailGroupBeanDetail("2");

        MailSenderInfo mailInfo = new MailSenderInfo();
        // 设置邮件服务器类型
        mailInfo.setMailServerHost("smtp.qiye.163.com");
        //设置端口号
        mailInfo.setMailServerPort("25");
        //设置是否验证
        mailInfo.setValidate(true);
        //设置用户名、密码、发送人地址
        mailInfo.setUserName(Constant.P_EMAIL_NAME);
        mailInfo.setPassword(Constant.P_EMAIL_PSWD);// 您的邮箱密码
        mailInfo.setFromAddress(Constant.P_EMAIL_NAME);
        /**
         * 附件
         */
        Vector<File> files = new Vector<File>() ;
        files.add(file) ;
        mailInfo.setFile(files) ;
        /**
         * 收件人邮箱
         */
        String[] mailToAddressDemo = null;
        if(sbfStr !=null && sbfStr.length()>0){
            mailToAddressDemo = (sbfStr.toString()+";"+mp.getMailUser()).split(";");
        }else{
            mailToAddressDemo = mp.getMailUser().split(";");
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        mailInfo.setSubject("【投产结果通报】"+sdf.format(new Date())+"产品需求投产验证结果");
        //收件人去重复
        List<String> result = new ArrayList<String>();
        boolean flag;
        for(int i=0;i<mailToAddressDemo.length;i++){
            flag = false;
            for(int j=0;j<result.size();j++){
                if(mailToAddressDemo[i].equals(result.get(j))){
                    flag = true;
                    break;
                }
            }
            if(!flag){
                result.add(mailToAddressDemo[i]);
            }
        }
        //String[] mailToAddress = (String[]) result.toArray(new String[result.size()]);
        String[] mailToAddress = {"tu_yi@hisuntech.com","wu_lr@hisuntech.com","huangyan@hisuntech.com"};
        mailInfo.setToAddress(mailToAddress);
        //记录邮箱信息
        MailFlowDO bn=new MailFlowDO("投产结果通报", Constant.P_EMAIL_NAME, mp.getMailUser()+";"+sbfStr, file.getName() ,"");
        //添加发送内容
        StringBuffer sb=new StringBuffer();
        sb.append("<table border='1' style='border-collapse: collapse;background-color: white; white-space: nowrap;'>");
        sb.append("<tr><th>投产编号</th><th>产品名称</th><th>需求名称及内容简述</th><th>基地负责人</th>");
        sb.append("<th>产品经理</th><th>生产验证方式</th><th>验证结果</th></tr>");
        for (ProductionDO bean : list) {
            String proNumber = bean.getProNumber();
            if(bean.getProNumber().startsWith("REQ")){
                proNumber = bean.getProNumber().substring(4,bean.getProNumber().length()).toString();
            }
            sb.append("<td >"+proNumber+"</td>");//投产编号
            sb.append("<td >"+bean.getProModule()+"</td>");//产品名称
            sb.append("<td >"+bean.getProNeed()+"</td>");//需求名称及内容简述
            sb.append("<td >"+bean.getBasePrincipal()+"</td>");//基地负责人
            sb.append("<td >"+bean.getProManager()+"</td>");//产品经理
            sb.append("<td >"+bean.getValidation()+"</td>");//生产验证方式
            //验证结果
            if(bean.getValidation().equals("当晚验证")){
                if(bean.getProStatus().equals("投产验证完成")){
                    sb.append("<td >验证通过</td></tr>");//验证结果
                }else{
                    sb.append("<td >验证未通过</td></tr>");//验证结果
                }
            }
            if(bean.getValidation().equals("隔日验证")){
                if(bean.getProStatus().equals("投产验证完成")){
                    sb.append("<td >验证通过</td></tr>");//验证结果
                }else{
                    sb.append("<td >隔日验证</td></tr>");//验证结果
                }
            }
            if(bean.getValidation().equals("待业务触发验证")){
                sb.append("<td >待业务触发验证</td></tr>");//验证结果
            }

        }
        sb.append("</table>");
        mailInfo.setContent("各位好：<br/>&nbsp;&nbsp;本周例行投产完成，投产后系统运行稳定、正常，请知悉。谢谢！"+sb.toString());
        // 这个类主要来发送邮件
        SimpleMailSender sms = new SimpleMailSender();
        boolean isSend=sms.sendHtmlMail(mailInfo);// 发送html格式
        operationProductionDao.addMailFlow(bn);
        if(isSend){
            if(file.isFile() && file.exists()){
                file.delete();
            }
        }


        MailGroupDO mpb=operationProductionDao.findMailGroupBeanDetail("3");
        mp.setMailUser(mpb.getMailUser());
        List<List<ProblemDO>> pblist=new ArrayList<List<ProblemDO>>();
        for(int i=0;i<list.size();i++){
            pblist.add(operationProductionDao.findProblemInfo(list.get(i).getProNumber()));
        }
        //获取登录用户名
        String currentUser =  userService.getFullname(SecurityUtils.getLoginName());
        File file2=exportExcel_Nei(list, pblist, currentUser);
        //记录邮箱信息
        MailFlowDO bfn=new MailFlowDO("每周投产通报", Constant.P_EMAIL_NAME, mp.getMailUser(), file.getName() ,"");
        //String[] mailToAddresss = mp.getMailUser().split(";");
        String[] mailToAddresss = {"tu_yi@hisuntech.com","wu_lr@hisuntech.com","huangyan@hisuntech.com"};
        mailInfo.setToAddress(mailToAddresss);
        /**
         * 附件
         */
        Vector<File> file1 = new Vector<File>() ;
        file1.add(file2) ;
        mailInfo.setFile(file1) ;
        mailInfo.setSubject("【每周投产通报"+sdf.format(new Date())+"】");
        mailInfo.setContent("各位好！<br/>&nbsp;&nbsp;本周例行投产已完成,详情请参见附件<br/><br/>");
        boolean isSends=sms.sendHtmlMail(mailInfo);// 发送html格式
        if(isSends){
            operationProductionDao.addMailFlow(bfn);
            if(file2.isFile() && file2.exists()){
                file2.delete();
            }
        }else{
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("邮件发送失败!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        return ;//ajaxDoneSuccess("邮件发送成功!");
    }
    public File exportExcel_Nei(List<ProductionDO> list,List<List<ProblemDO>> proBeanList,String userName){
        String fileName = "每周投产通报" + DateUtil.date2String(new Date(), "yyyyMMddhhmmss") + ".xls";
        File file=null;
        try {
            //String path = "C:\\home\\devadm\\temp\\propkg";
            String path = "/home/devadm/temp/propkg/";
            String filePath = path + fileName;
            SendExcelOperationResultProblemUtil util = new SendExcelOperationResultProblemUtil();
            util.createExcel(filePath, list,null,proBeanList,userName);
            file=new File(filePath);
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e) {
            e.printStackTrace();
        }
        return file;
    }
    // 投产包检查
    @Override
    public String proPkgCheck(HttpServletRequest request, HttpServletResponse response, String taskIdStr){
        if(taskIdStr==null||taskIdStr=="") {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择需要检查的投产记录!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        String[] pro_list = taskIdStr.split("~");
        String result="";
        String s1 = "";
        String s2 = "";
        try {
            StringBuffer command=new StringBuffer();
            ProductionDO bean = null;
            if (pro_list.length>0) {
                command.append("cd ~/tomcat/webapps/hims/hckeck/\n");
                for(String s:pro_list){
                    bean = operationProductionDao.findProductionBean(s);
                    if (bean.getProPkgStatus().equals("待上传"))
                        s1+=s+",";
                    else
                        command.append("cp ~/home/devadm/temp/propkg/"+s+"/"+bean.getProPkgName()
                                +" ~/tomcat/webapps/hims/hckeck/ver/\n");
                }
                if(!s1.equals("")){
                    s1 = s1.substring(0, s1.length()-1);
                    s1 = s1 +"投产包未上传";
                }
                command.append("sh c_cross.sh");
                //s2 = execCommand(command.toString());

                Map<String,String> map = execCommand(command.toString());
                String succFlag=map.get("succFlag");

                if ("1".equals(succFlag))
                    s2="检查通过";
                else
                    s2=map.get("result");

            }
        }catch(Exception e){
            e.printStackTrace();
        }
        if (s1.equals("")) {
            result = s2;
        }
        if (!s1.equals("")&&s2.equals("检查通过")) {
            result = s1;
        }
        if (!s1.equals("")&&!s2.equals("检查通过")){
            result = s1+"</br>"+s2;
        }
        return result;
    }
    public Map execCommand(String command){

        Map<String,String> map=new HashMap<String, String>();
        JSch jsch = new JSch();
        Session session = null;
        Channel channel = null;

        String succFlag="0";
        String result="检查失败";

        try{
            session = jsch.getSession("devadm", "10.9.10.116", 22);
            session.setPassword("devadm@hisun");
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(command);
            channel.setInputStream(null);
            ((ChannelExec) channel).setErrStream(System.err);
            channel.connect();
            InputStream in = channel.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, "GB18030"));

            /**
             * 与版本管理约定：
             *      投产检查脚本（内部考核系统）请按如下约定处理：
             成功只输出一行：  0
             失败输出两行： 第一行为：1  ，第二行为失败信息
             提醒信息输出2行：第一行为：2  ，第二行为提醒信息（系统默认检查通过，仅是提醒）
             *
             */
            String temp=reader.readLine();

            if ("0".equals(temp)){  //成功
                succFlag="1";
                result="检查通过";
            }else{
                if("1".equals(temp)){ 	//成功，但有警告
                    succFlag="1";
                    result="检查通过,但有警告:";
                }
                if("2".equals(temp)){	//失败
                    succFlag="0";
                    result="检查失败";
                }

                while((temp=reader.readLine())!=null){
                    result += temp + "</br>";
                }


            }
        } catch (JSchException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            channel.disconnect();
            session.disconnect();
        }
        map.put("succFlag", succFlag);
        map.put("result", result);
        return map;
    }
    @Override
    public  void doProductionDetailDownload(HttpServletRequest request, HttpServletResponse response, String taskIdStr)throws Exception{
        String[] pro_number_list=taskIdStr.split("~");
        if(pro_number_list.length == 0){
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("请先查出需要导出的投产变更明细记录!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        int[] pro_number_li = new int[pro_number_list.length];
        for(int i=0;i<pro_number_list.length;i++){
            pro_number_li[i]=Integer.parseInt(pro_number_list[i]);
        }
        List<ScheduleDO> list=new ArrayList<ScheduleDO>();
        for(int i=0;i<pro_number_li.length;i++){
            list.add(operationProductionDao.findOperationExcelList(pro_number_li[i]));
        }
        exportOperationExcel(response,request, list);
    }
    public void exportOperationExcel(HttpServletResponse response,HttpServletRequest request,
                                     List<ScheduleDO> list) throws Exception {
        String fileName = "投产操作明细表" + DateUtil.date2String(new Date(), "yyyyMMddhhmmss") + ".xls";
        OutputStream os = null;
        response.reset();
        try {
            //String path = "C:\\home\\devadm\\temp\\propkg";
            String path = "/home/devadm/temp/propkg/";
            String filePath = path + fileName;
            ExcelOperationDetailUtil util = new ExcelOperationDetailUtil();
            String createFile = util.createExcel(filePath, list,null);
            //告诉浏览器允许所有的域访问
            //注意 * 不能满足带有cookie的访问,Origin 必须是全匹配
            //resp.addHeader("Access-Control-Allow-Origin", "*");
            //解决办法通过获取Origin请求头来动态设置
            String origin = request.getHeader("Origin");
            if (StringUtils.isNotBlank(origin)) {
                response.addHeader("Access-Control-Allow-Origin", origin);
            }
            //允许带有cookie访问
            response.addHeader("Access-Control-Allow-Credentials", "true");
            //告诉浏览器允许跨域访问的方法
            response.addHeader("Access-Control-Allow-Methods", "*");
            //告诉浏览器允许带有Content-Type,header1,header2头的请求访问
            //resp.addHeader("Access-Control-Allow-Headers", "Content-Type,header1,header2");
            //设置支持所有的自定义请求头
            String headers = request.getHeader("Access-Control-Request-Headers");
            if (StringUtils.isNotBlank(headers)) {
                response.addHeader("Access-Control-Allow-Headers", headers);
            }
            //告诉浏览器缓存OPTIONS预检请求1小时,避免非简单请求每次发送预检请求,提升性能
            response.addHeader("Access-Control-Max-Age", "3600");
            response.setContentType("application/octet-stream; charset=utf-8");
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + fileName);
            try {
                os = response.getOutputStream();
                os.write(org.apache.commons.io.FileUtils.readFileToByteArray(new File(filePath)));
                os.flush();
            } catch (IOException e) {
                throw e;
            } finally {
                if (os != null) {
                    try {
                        os.close();
                    } catch (IOException e) {
                        throw e;
                    }
                }
            }
            // 删除文件
            new File(createFile).delete();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            MsgEnum.ERROR_CUSTOM.setMsgInfo("投产操作明细报表导出失败");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        } catch (Exception e) {
            e.printStackTrace();
            MsgEnum.ERROR_CUSTOM.setMsgInfo("投产操作明细报表导出失败");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }

    }





    public Vector<File> setVectorFile(MultipartFile file, Vector<File> files, ProductionBO bean){
        //todo 先改成本机写死路径

       // String path="D:\\home\\devadm\\temp\\import";
        String path = "/home/devadm/temp/import/";
        String fileName = file.getOriginalFilename();
        File tmp_file = new File(path + File.separator + bean.getProNumber() + "_" + fileName);
        try {
            if (!tmp_file.exists()) {
                file.transferTo(new File(path + File.separator + bean.getProNumber() + "_" + fileName));
                this.addProductionPicBean(new ProductionPicDO(bean.getProNumber(), fileName, path));
            } else {
                file.transferTo(new File(path + File.separator + bean.getProNumber() + "_" + fileName));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        files.add(tmp_file);
        return  files;
    }

    //
    public MsgEnum productionInput(MultipartFile file, Boolean isApproveProduct, ProductionBO bean) {
        bean.setProStatus("投产提出");
        System.err.println(bean.toString());
        boolean isSend = false;
        //后台判断数据
        if (!bean.getProNumber().matches(".*[a-zA-z].*")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 您的投产编号填写错误");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProNumber() == null || bean.getProNumber().equals("")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 您的投产编号不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProType().equals("救火更新")) {
            if (bean.getOperatingTime() == null || bean.getOperatingTime().equals("")|| bean.getOperatingTime().equals("undefined")) {
                MsgEnum.ERROR_IMPORT.setMsgInfo(" 更新预计操作时长不能为空");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }
        }
        if (bean.getProNeed() == null || bean.getProNeed().equals("")|| bean.getProNeed().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 需求名不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProType() == null || bean.getProType().equals("")|| bean.getProType().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 投产类型不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProDate() == null || bean.getProDate().equals("")|| bean.getProDate().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 投产日期不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getApplicationDept() == null || bean.getApplicationDept().equals("")|| bean.getApplicationDept().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 申请部门不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProApplicant() == null || bean.getProApplicant().equals("")|| bean.getProApplicant().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 申请人不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getApplicantTel() == null || bean.getApplicantTel().equals("")|| bean.getApplicantTel().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 申请人联系方式不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProModule() == null || bean.getProModule().equals("")|| bean.getProModule().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 产品模块不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getBusinessPrincipal() == null || bean.getBusinessPrincipal().equals("")|| bean.getBusinessPrincipal().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 基地业务负责人不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getBasePrincipal() == null || bean.getBasePrincipal().equals("")|| bean.getBasePrincipal().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 基地技术负责人不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProManager() == null || bean.getProManager().equals("")|| bean.getProManager().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 产品经理不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getIsUpDatabase() == null || bean.getIsUpDatabase().equals("")|| bean.getIsUpDatabase().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 是否更新数据库不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getIsUpStructure() == null || bean.getIsUpStructure().equals("")|| bean.getIsUpStructure().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 是否更新数据库表不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProOperation() == null || bean.getProOperation().equals("")|| bean.getProOperation().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 是否需要运维监控不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getIsRefCerificate() == null || bean.getIsRefCerificate().equals("")|| bean.getIsRefCerificate().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 是否涉及证书不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getIsAdvanceProduction() == null || bean.getIsAdvanceProduction().equals("")|| bean.getIsAdvanceProduction().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 是否预投产验证不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getIsAdvanceProduction().equals("否")) {
            if (bean.getNotAdvanceReason() == null || bean.getNotAdvanceReason().equals("")|| bean.getNotAdvanceReason().equals("undefined")) {
                MsgEnum.ERROR_IMPORT.setMsgInfo(" 不做预投产验证原因不能为空");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }
        }
        if (bean.getIsAdvanceProduction().equals("是")) {
            if (bean.getProAdvanceResult() == null || bean.getProAdvanceResult().equals("")|| bean.getProAdvanceResult().equals("undefined")) {
                MsgEnum.ERROR_IMPORT.setMsgInfo(" 预投产验证结果不能为空");
                BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
            }
        }
        if (bean.getIdentifier() == null || bean.getIdentifier().equals("")|| bean.getIdentifier().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 验证人不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getIdentifierTel() == null || bean.getIdentifierTel().equals("")|| bean.getIdentifierTel().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 验证人联系方式不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getProChecker() == null || bean.getProChecker().equals("")|| bean.getProChecker().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 复核人不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getCheckerTel() == null || bean.getCheckerTel().equals("")|| bean.getCheckerTel().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 复核人手机号码不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getValidation() == null || bean.getValidation().equals("")|| bean.getValidation().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 生成验证方式不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getDevelopmentLeader() == null || bean.getDevelopmentLeader().equals("")|| bean.getDevelopmentLeader().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 开发负责人不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getApprover() == null || bean.getApprover().equals("")|| bean.getApprover().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 审批人不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }
        if (bean.getRemark() == null || bean.getRemark().equals("")|| bean.getRemark().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 备注不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }

        if (bean.getMailLeader() == null || bean.getMailLeader().equals("")|| bean.getMailLeader().equals("undefined")) {
            MsgEnum.ERROR_IMPORT.setMsgInfo(" 开发负责人邮箱不能为空");
            BusinessException.throwBusinessException(MsgEnum.ERROR_IMPORT);
        }

        //发邮件通知
        MailSenderInfo mailInfo = new MailSenderInfo();
        // 设置邮件服务器类型
        mailInfo.setMailServerHost("smtp.qiye.163.com");
        //设置端口号
        mailInfo.setMailServerPort("25");
        //设置是否验证
        mailInfo.setValidate(true);
        //设置用户名、密码、发送人地址
        mailInfo.setUserName(Constant.P_EMAIL_NAME);
        mailInfo.setPassword(Constant.P_EMAIL_PSWD);// 您的邮箱密码
        mailInfo.setFromAddress(Constant.P_EMAIL_NAME);
        SendEmailConfig config = new SendEmailConfig();
        //投产日正常投产，是否超时11点需要审批的投产
        if (isApproveProduct && bean.getProType().equals("正常投产") && bean.getIsOperationProduction().equals("是")) {
            // 设置投产录入状态
            bean.setProStatus("投产待审批");
            //收件人
            List<String> receiver_users = new ArrayList<String>();
            //添加申请人部门经理邮箱地址
            receiver_users.add(bean.getProApplicant());
            receiver_users.add(bean.getDevelopmentLeader());
            String receiver_mail = bean.getMailLeader()+";"+this.findManagerMailByUserName(receiver_users) + ";" + config.getNormalMailTo(false)+";wu_lr@hisuntech.com";
            //todo 收件人需要添加两人必选先注释 先用自己的邮件代替
            //+";tian_qun@hisuntech.com;huang_jh@hisuntech.com";
            // 邮件去重
            receiver_mail = BaseUtil.distinctStr(receiver_mail, ";");
            //记录邮箱信息
            MailFlowBean bnb = new MailFlowBean("【投产录入审批申请】", Constant.P_EMAIL_NAME, receiver_mail, "");
            bean.setMailRecipient(receiver_mail);
            //todo 抄送人需要添加两人必选先注释 先用自己的邮件代替
            //bean.setMailCopyPerson("tian_qun@hisuntech.com;huang_jh@hisuntech.com");
            bean.setMailCopyPerson("wu_lr@hisuntech.com");
            mailInfo.setToAddress(receiver_mail.split(";"));
            mailInfo.setSubject("【投产录入审批申请】-" + bean.getProNeed() + "-" + bean.getProNumber() + "-" + bean.getProApplicant());
            mailInfo.setContent("武金艳、肖铧：<br/>&nbsp;&nbsp;由于超过正常投产录入时间，投产无法正常录入，现申请投产审批，烦请审批！");
            // 这个类主要来发送邮件
            SimpleMailSender sms = new SimpleMailSender();
            isSend = sms.sendHtmlMail(mailInfo);// 发送html格式
            this.addMailFlow(bnb);
        }
        //正常投产；投产日投产；不投产验证
        if (bean.getProType().equals("正常投产") && bean.getIsOperationProduction().equals("是") && bean.getIsAdvanceProduction().equals("否")) {
            //添加各人员部门经理邮箱地址
            //收件人
            List<String> receiver_users = new ArrayList<String>();
            //添加申请人部门经理邮箱地址
            receiver_users.add(bean.getProApplicant());
            //添加产品部门经理邮箱地址
            receiver_users.add(bean.getProManager());
            //添加开发负责人部门经理邮箱地址
            receiver_users.add(bean.getDevelopmentLeader());
            String receiver_mail = this.findManagerMailByUserName(receiver_users) + ";" + config.getNormalMailTo(false);
            // 邮件去重
            receiver_mail = BaseUtil.distinctStr(receiver_mail, ";");
            //投产信息记录邮箱
            bean.setMailRecipient(receiver_mail);
            //记录邮箱信息
            mailInfo.setToAddress(receiver_mail.split(";"));
            mailInfo.setSubject("【预投产不验证申请】-" + bean.getProNeed() + "-" + bean.getProNumber() + "-" + bean.getProApplicant());
            mailInfo.setContent("武金艳、肖铧：<br/>&nbsp;&nbsp;由于" + bean.getNotAdvanceReason() + "，预投产无法验证，现申请预投产不验证，烦请审批！");
            // 这个类主要来发送邮件
            SimpleMailSender sms = new SimpleMailSender();
            isSend = sms.sendHtmlMail(mailInfo);// 发送html格式
            this.addMailFlow(new MailFlowBean("【预投产不验证申请】", Constant.P_EMAIL_NAME, receiver_mail, ""));
        }
        //非投产日正常投产
        if (bean.getIsOperationProduction() != null && bean.getProType().equals("正常投产") && bean.getIsOperationProduction().equals("否")) {
            List<ProductionBO> unusuaList = new ArrayList<ProductionBO>();
            unusuaList.add(bean);
            File unusualFile = this.exportUnusualExcel( unusuaList);
            // 附件
            Vector<File> filesv = new Vector<File>();
            // 添加附件信息
            if (!file.isEmpty()) {
                filesv = this.setVectorFile(file,filesv,bean);
            }
            filesv.add(unusualFile);
            mailInfo.setFile(filesv);
            // 收件人邮箱
            List<String> receiver_users = new ArrayList<String>();
            List<String> copy_users = new ArrayList<String>();
            //添加申请人部门经理邮箱地址
            receiver_users.add(bean.getProApplicant());
            //添加部门经理邮箱地址
            copy_users.add(bean.getProApplicant());
            boolean is_advance_production = true;
            // 是否投产验证
            if (bean.getIsAdvanceProduction().equals("否")) {
                is_advance_production = false;
                //添加产品部门经理邮箱地址
                receiver_users.add(bean.getProManager());
                //添加开发负责人部门经理邮箱地址
                receiver_users.add(bean.getDevelopmentLeader());
            }
            //相关人员邮件
            String receiver_mail = this.findManagerMailByUserName(receiver_users) + ";" + config.getAbnormalMailTo(is_advance_production) + ";" + bean.getMailRecipient();
            //系统配置邮件、救火更新收件人邮件
            //收件人去重
            receiver_mail = BaseUtil.distinctStr(receiver_mail, ";");
            //抄送人邮箱
            String copy_mail = this.findManagerMailByUserName(copy_users) + ";" + config.getAbnormalMailCopy();
            if (bean.getMailCopyPerson() != null && !bean.getMailCopyPerson().equals("")) {
                copy_mail += ";" + bean.getMailCopyPerson();
            }
            //去重抄送人邮箱
            copy_mail = BaseUtil.distinctStr(copy_mail, ";");
            //投产信息记录邮箱
            bean.setMailRecipient(receiver_mail);
            bean.setMailCopyPerson(copy_mail);
            mailInfo.setToAddress(receiver_mail.split(";"));
            mailInfo.setCcs(copy_mail.split(";"));
            mailInfo.setSubject("【正常投产(非投产日)审核】-" + bean.getProNeed() + "-" + bean.getProNumber() + "-" + bean.getProApplicant());
            //拼接邮件内容
            mailInfo.setContent("各位领导好:<br/>&nbsp;&nbsp;本次投产申请详细内容请参见下表<br/>烦请审批，谢谢！<br/>" + EmailConfig.setProEmailContent(bean));
            // 这个类主要来发送邮件
            SimpleMailSender sms = new SimpleMailSender();
            isSend = sms.sendHtmlMail(mailInfo);// 发送html格式
            this.addMailFlow(new MailFlowBean("【正常投产(非投产日)审核】", Constant.P_EMAIL_NAME, receiver_mail, unusualFile.getName(), "wu_lr@hisuntech.com"));
            if (unusualFile.isFile() && unusualFile.exists()) {
                unusualFile.delete();
            }
        }
        //救火更新
        if (bean.getProType().equals("救火更新")) {
            bean.setIsOperationProduction("");
            bean.setProStatus("投产待部署");
            List<ProductionBO> list = new ArrayList<ProductionBO>();
            list.add(bean);


            List<String> receiver_users = new ArrayList<String>();
            List<String> copy_users = new ArrayList<String>();

            // 添加申请人
            receiver_users.add(bean.getProApplicant());
            // 添加部门经理 抄送
            copy_users.add(bean.getProApplicant());
            // 附件
            Vector<File> files = new Vector<File>();
            File file_fire = this.exportUrgentExcel(list);
            // 添加救火附件
            if (!file.isEmpty()) {
                files = this.setVectorFile(file,files,bean);
            }
            files.add(file_fire);
            mailInfo.setFile(files);
            // 收件人邮箱
            String base_receiver_mail = bean.getMailRecipient();
            if (bean.getIsAdvanceProduction().equals("是")) {
                base_receiver_mail +=  config.getFireMailTo(true);
            } else {
                receiver_users.add(bean.getProManager());
                receiver_users.add(bean.getDevelopmentLeader());
                base_receiver_mail +=   bean.getMailLeader() + ";" + config.getFireMailTo(false);
            }
            //去重
            base_receiver_mail=base_receiver_mail +";"+ this.findManagerMailByUserName(receiver_users);
            String receiver_mail = BaseUtil.distinctStr(base_receiver_mail, ";");
            // 抄送人邮箱
            String copy_mail = this.findManagerMailByUserName(copy_users) + ";" + config.getFireMailCopy();
            if (bean.getMailCopyPerson() != null && !bean.getMailCopyPerson().equals("")) {
                copy_mail += ";" + bean.getMailCopyPerson();
            }
            copy_mail = BaseUtil.distinctStr(copy_mail, ";");

            //记录邮箱信息
            bean.setMailRecipient(receiver_mail);
            bean.setMailCopyPerson(copy_mail);
            //记录邮箱信息
            mailInfo.setToAddress(receiver_mail.split(";"));
            mailInfo.setCcs(copy_mail.split(";"));
            //保存抄送人
//	          bean.setMail_copy_person(mailCopySum);
            mailInfo.setSubject("【救火更新审核】-" + bean.getProNeed() + "-" + bean.getProNumber() + "-" + bean.getProApplicant());

            mailInfo.setContent("各位领导好:<br/>&nbsp;&nbsp;本次投产申请详细内容请参见下表<br/>烦请审批，谢谢！<br/>" + EmailConfig.setFireEmailContent(bean));
            // 这个类主要来发送邮件
            SimpleMailSender sms = new SimpleMailSender();
            isSend = sms.sendHtmlMail(mailInfo);// 发送html格式
            this.addMailFlow(new MailFlowBean("【救火更新审核】", Constant.P_EMAIL_NAME, receiver_mail, "", ""));
            if (file_fire != null && file_fire.isFile() && file_fire.exists()) {
                file_fire.delete();
            }
        }


        // 将其原先的lis循环查找相同pro_number编号的投产信息 更新为查找一条记录是否存在
        ProductionBO productionBean = this.searchProdutionDetail(bean.getProNumber());
        bean.setProductionDeploymentResult("未部署");
        if (productionBean != null) {
            this.updateAllProduction(bean);

            //生成流水记录
            ScheduleDO scheduleBean = new ScheduleDO(bean.getProNumber(), userService.getFullname(SecurityUtils.getLoginName()), "重新录入", productionBean.getProStatus(), bean.getProStatus(), "无");
            this.addScheduleBean(scheduleBean);
            //是否预投产验证为“否”时，需求当前阶段变更为“完成预投产”
            if (bean.getIsAdvanceProduction().equals("否")) {
                DemandBO vo = new DemandBO();
                vo.setReqNo(bean.getProNumber());
                List<DemandBO> list = reqTaskService.getReqTaskByUK(vo);
                if (list.size() != 0) {
                    DemandBO demand = list.get(0);
                    demand.setPreCurPeriod("160");
                    reqTaskService.updatePreCurPeriod(demand);
                }
            }
            if (!isSend) {
                MsgEnum.CUSTOMSUCCESS.setMsgInfo(" 投产已录入,但您有邮件没有发送成功,请及时联系系统维护人员!");
                return   MsgEnum.CUSTOMSUCCESS;
            }
            MsgEnum.CUSTOMSUCCESS.setMsgInfo("投产已重新提交");
            return   MsgEnum.CUSTOMSUCCESS;
        }

        this.addProduction(bean);
        //生成流水记录
        ScheduleDO scheduleBean= new ScheduleDO(bean.getProNumber(),  userService.getFullname(SecurityUtils.getLoginName()), "录入", "", bean.getProStatus(), "无");
        this.addScheduleBean(scheduleBean);
        //是否预投产验证为“否”时，需求当前阶段变更为“完成预投产”
        if (bean.getIsAdvanceProduction().equals("否")) {
            DemandBO vo = new DemandBO();
            vo.setReqNo(bean.getProNumber());
            List<DemandBO> list = reqTaskService.getReqTaskByUK(vo);
            if (list.size() != 0) {
                DemandBO DemandBO = list.get(0);
                DemandBO.setPreCurPeriod("160");
                reqTaskService.updatePreCurPeriod(DemandBO);
            }
        }
        if (!isSend) {
            //自定义类型成功
            MsgEnum.CUSTOMSUCCESS.setMsgInfo(" 投产已录入,但您有邮件没有发送成功,请及时联系系统维护人员!");
            return   MsgEnum.CUSTOMSUCCESS;
        }
        return MsgEnum.SUCCESS;
    }

    /**
     * 投产包上传
     * @param file
     */
    public void doBatchImport(MultipartFile file,String reqNumber) {
        if (file.isEmpty()) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("请选择上传文件!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        String currentUser =  userService.getFullname(SecurityUtils.getLoginName());
        ProductionDO bean = null;
        bean = operationProductionDao.findProductionBean(reqNumber);
//        if(!currentUser.equals(bean.getProApplicant())&&!currentUser.equals(bean.getDevelopmentLeader())){
//            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
//            MsgEnum.ERROR_CUSTOM.setMsgInfo("只有负责投产的申请提出人或开发负责人才能上传投产包!");
//            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
//        }
        if(bean.getProType().equals("正常投产")){

            if(bean.getIsOperationProduction()!=null && !bean.getIsOperationProduction().equals("")){
                if(bean.getProDate()!=null && !bean.getProDate().equals("")){
                    SimpleDateFormat smf = new SimpleDateFormat("yyyyMMdd");
                    SimpleDateFormat smft = new SimpleDateFormat("yyyyMMddHHmmss");
                    String nowStr = smft.format(new Date());;
                    if(bean.getIsOperationProduction().equals("是")){
                        String config_time = productTimeService.findProductTimeByID(1);
                        String pro_date = smf.format(bean.getProDate()) + config_time.replace(":", "") + "00";//
//							 String pro_date = smf.format(bean.getPro_date())+"230000";
                        if(Long.parseLong(nowStr) >= Long.parseLong(pro_date)){
                            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                            MsgEnum.ERROR_CUSTOM.setMsgInfo("正常投产日投产必须在计划投产日"+config_time+"之前上传投产包");
                            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                        }
                    }else{
                        String config_time = productTimeService.findProductTimeByID(2);
                        String pro_date = smf.format(bean.getProDate()) + config_time.replace(":", "") + "00";
                        if(Long.parseLong(nowStr) >= Long.parseLong(pro_date)){
                            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                            MsgEnum.ERROR_CUSTOM.setMsgInfo("正常投产日投产必须在计划投产日"+config_time+"之前上传投产包");
                            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                        }
                    }
                }else{
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                    MsgEnum.ERROR_CUSTOM.setMsgInfo("计划投产日字段不能为空");
                    BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
                }
            }else{
                MsgEnum.ERROR_CUSTOM.setMsgInfo("");
                MsgEnum.ERROR_CUSTOM.setMsgInfo("是否正常投产日投产字段不能为空");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }
        //File fileDir = new File("C:\\home\\devadm\\temp\\propkg\\" + reqNumber);
        File fileDir = new File("/home/devadm/temp/upload/propkg/" + reqNumber);
        File filePath = new File(fileDir.getPath()+"/"+file.getOriginalFilename());
        if(fileDir.exists()){
            File[] oldFile = fileDir.listFiles();
            for(File o:oldFile) o.delete();
        }else{
            fileDir.mkdir();
        }
        try {
            file.transferTo(filePath);
        } catch (IllegalStateException e) {
            e.printStackTrace();
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("文件上传失败");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        } catch (IOException e) {
            e.printStackTrace();
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("文件上传失败");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }

        bean.setProPkgTime(new Timestamp(new Date().getTime())); //待修复BUG
        bean.setProPkgName(file.getOriginalFilename());
        StringBuffer command = new StringBuffer();
        command.append("cd ~/tomcat/webapps/hims/hckeck/\n");
        command.append("cp ~/home/devadm/temp/upload/propkg/" + reqNumber + "/" + bean.getProPkgName()
                + " ~/tomcat/webapps/hims/hckeck/ver/\n");
        command.append("sh zck.sh " + bean.getProPkgName() + "\n");
        Map<String,String> map = execCommand(command.toString());
        String succFlag=map.get("succFlag");
        String result=map.get("result");
        if ("1".equals(succFlag)){
            bean.setProPkgStatus("检查通过");
        }
        else{
            bean.setProPkgStatus("待上传");
        }
        operationProductionDao.updateProduction(bean);
    }

    @Override
    public List<ProblemBO> findProblemInfo(String proNumber) {
        List<ProblemBO> problemBOS = new LinkedList<>();
        List<ProblemDO> problemDOS=operationProductionDao.findProblemInfo(proNumber);
        problemDOS.forEach(m->
                problemBOS.add(BeanUtils.copyPropertiesReturnDest(new ProblemBO(), m))
        );
        //确保listc长度为5
        while (problemBOS.size()<5){
            problemBOS.add(new ProblemBO());
        }
        return problemBOS;
    }

    @Override
    public void updateProblem(ProblemBO problemBO) {
        ProblemDO problemDO = BeanUtils.copyPropertiesReturnDest(new ProblemDO(), problemBO);
        operationProductionDao.updateProblem(problemDO);
    }

    @Override
    public void deleteProblemInfo(String proNumber1) {
        operationProductionDao.deleteProblemInfo(proNumber1);
    }

    @Override
    public void insertProblemInfo(ProblemBO problemBO) {
        ProblemDO problemDO = BeanUtils.copyPropertiesReturnDest(new ProblemDO(), problemBO);
        operationProductionDao.insertProblemInfo(problemBO);
    }


    /**
     * 问题录入
     * 按照老系统照搬写下来的，设计的有点不合理 后期可以考虑把参数改成链表
     *
     * @param questionInputReqBO
     */
    @Override
    public void questionInput(QuestionInputReqBO questionInputReqBO) {
        //问题一
        if(questionInputReqBO.getProNumber1()!=null && !questionInputReqBO.getProNumber1().equals("")){
            if( questionInputReqBO.getQuestionOne() !=null && ! questionInputReqBO.getQuestionOne().equals("")){
                ProblemBO proBean=new ProblemBO(Integer.parseInt(questionInputReqBO.getProNumber1()),questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionOne());
                this.updateProblem(proBean);
            }
            else{
                this.deleteProblemInfo(questionInputReqBO.getProNumber1());
            }
        }else{
            if(questionInputReqBO.getQuestionOne()!=null && !questionInputReqBO.getQuestionOne().equals("")){
                ProblemBO proBean=new ProblemBO(questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionOne());
                this.insertProblemInfo(proBean);
            }
        }
        //问题二
        if(questionInputReqBO.getProNumber2()!=null && !questionInputReqBO.getProNumber2().equals("")){
            if( questionInputReqBO.getQuestionTwo() !=null && ! questionInputReqBO.getQuestionTwo().equals("")){
                ProblemBO proBean=new ProblemBO(Integer.parseInt(questionInputReqBO.getProNumber2()),questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionTwo());
                this.updateProblem(proBean);
            }
            else{
                this.deleteProblemInfo(questionInputReqBO.getProNumber2());
            }
        }else{
            if(questionInputReqBO.getQuestionTwo()!=null && !questionInputReqBO.getQuestionTwo().equals("")){
                ProblemBO proBean=new ProblemBO(questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionTwo());
                this.insertProblemInfo(proBean);
            }
        }
        //问题三
        if(questionInputReqBO.getProNumber3()!=null && !questionInputReqBO.getProNumber3().equals("")){
            if( questionInputReqBO.getQuestionThree() !=null && ! questionInputReqBO.getQuestionThree().equals("")){
                ProblemBO proBean=new ProblemBO(Integer.parseInt(questionInputReqBO.getProNumber3()),questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionThree());
                this.updateProblem(proBean);
            }
            else{
                this.deleteProblemInfo(questionInputReqBO.getProNumber3());
            }
        }else{
            if(questionInputReqBO.getQuestionThree()!=null && !questionInputReqBO.getQuestionThree().equals("")){
                ProblemBO proBean=new ProblemBO(questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionThree());
                this.insertProblemInfo(proBean);
            }
        }
        //问题四
        if(questionInputReqBO.getProNumber4()!=null && !questionInputReqBO.getProNumber4().equals("")){
            if( questionInputReqBO.getQuestionFour() !=null && ! questionInputReqBO.getQuestionFour().equals("")){
                ProblemBO proBean=new ProblemBO(Integer.parseInt(questionInputReqBO.getProNumber4()),questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionFour());
                this.updateProblem(proBean);
            }
            else{
                this.deleteProblemInfo(questionInputReqBO.getProNumber4());
            }
        }else{
            if(questionInputReqBO.getQuestionFour()!=null && !questionInputReqBO.getQuestionFour().equals("")){
                ProblemBO proBean=new ProblemBO(questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionFour());
                this.insertProblemInfo(proBean);
            }
        }
        //问题五
        if(questionInputReqBO.getProNumber5()!=null && !questionInputReqBO.getProNumber5().equals("")){
            if( questionInputReqBO.getQuestionFive() !=null && ! questionInputReqBO.getQuestionFive().equals("")){
                ProblemBO proBean=new ProblemBO(Integer.parseInt(questionInputReqBO.getProNumber5()),questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionFive());
                this.updateProblem(proBean);
            }
            else{
                this.deleteProblemInfo(questionInputReqBO.getProNumber5());
            }
        }else{
            if(questionInputReqBO.getQuestionFive()!=null && !questionInputReqBO.getQuestionFive().equals("")){
                ProblemBO proBean=new ProblemBO(questionInputReqBO.getProNumber(), questionInputReqBO.getQuestionFive());
                this.insertProblemInfo(proBean);
            }
        }
    }
    //投产包下载
    @Override
    public void pkgDownload(HttpServletRequest request, HttpServletResponse response, String proNumber){
        response.reset();
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
           // File fileDir=new File(request.getSession().getServletContext().getRealPath("/") + RELATIVE_PATH +proNumber);
            File fileDir = new File("/home/devadm/temp/upload/propkg/" + proNumber);
            File[] pkgFile=fileDir.listFiles();
            File fileSend=null;
            if(pkgFile!=null&&pkgFile.length>0){
                fileSend = pkgFile[0];
            }
            response.setHeader("Content-Disposition", "attachment; filename="  + new String(fileSend.getName().getBytes(Constant.CHARSET_GB2312), Constant.CHARSET_ISO8859));
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            //告诉浏览器允许所有的域访问
            //注意 * 不能满足带有cookie的访问,Origin 必须是全匹配
            //resp.addHeader("Access-Control-Allow-Origin", "*");
            //解决办法通过获取Origin请求头来动态设置
            String origin = request.getHeader("Origin");
            if (StringUtils.isNotBlank(origin)) {
                response.addHeader("Access-Control-Allow-Origin", origin);
            }
            //允许带有cookie访问
            response.addHeader("Access-Control-Allow-Credentials", "true");
            //告诉浏览器允许跨域访问的方法
            response.addHeader("Access-Control-Allow-Methods", "*");
            //告诉浏览器允许带有Content-Type,header1,header2头的请求访问
            //resp.addHeader("Access-Control-Allow-Headers", "Content-Type,header1,header2");
            //设置支持所有的自定义请求头
            String headers = request.getHeader("Access-Control-Request-Headers");
            if (StringUtils.isNotBlank(headers)) {
                response.addHeader("Access-Control-Allow-Headers", headers);
            }
            //告诉浏览器缓存OPTIONS预检请求1小时,避免非简单请求每次发送预检请求,提升性能
            response.addHeader("Access-Control-Max-Age", "3600");
            response.setContentType("application/octet-stream; charset=utf-8");
            output.write(org.apache.commons.io.FileUtils.readFileToByteArray(fileSend));
            bufferedOutPut.flush();
        } catch (Exception e) {
            e.printStackTrace();
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public MailFlowDO searchUserEmail(MailFlowConditionDO mailFlowConditionDO) {
        return operationProductionDao.searchUserEmail(mailFlowConditionDO);
    }

    @Override
    public ProductionDO findDeptManager(String deptName) {
        return operationProductionDao.findDeptManager(deptName);
    }



    //投产审批
    @Override
    public void approval(String proNumber){
        //获取登录用户名
        String currentUser =  userService.getFullname(SecurityUtils.getLoginName());
        ProductionDO bean = operationProductionDao.findProductionBean(proNumber);
        String proStatus = bean.getProStatus();
        bean.setProStatus("投产提出");
        operationProductionDao.updateProduction(bean);
        ScheduleDO sBean=new ScheduleDO();
        sBean.setPreOperation(bean.getProStatus());
        //生成流水记录
        ScheduleDO schedule=new ScheduleDO(bean.getProNumber(), currentUser, "投产审批", proStatus, bean.getProStatus(), "投产信息更新");
        operationProductionDao.insertSchedule(schedule);
    }
    // 投产详情修改
    @Override
    public void updateProductionBean(ProductionBO productionBO){
        //获取登录用户名
        String currentUser =  userService.getFullname(SecurityUtils.getLoginName());
        if (!(productionBO.getProStatus().equals("投产提出") || (productionBO.getProStatus().equals("投产待部署") && productionBO.getProType().equals("救火更新")))) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("当前投产状态的投产信息不可修改!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        if (!(productionBO.getProApplicant().equals(currentUser))) {
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo("非当前投产申请人不得更改信息!");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        ProductionDO bean = BeanUtils.copyPropertiesReturnDest(new ProductionDO(), productionBO);
        operationProductionDao.updateAllProduction(bean);
        ScheduleDO sBean=new ScheduleDO();
        sBean.setPreOperation(bean.getProStatus());
        //生成流水记录
        ScheduleDO schedule=new ScheduleDO(bean.getProNumber(), currentUser, "修改", bean.getProStatus(), bean.getProStatus(), "投产信息更新");
        operationProductionDao.insertSchedule(schedule);
    }

    //邮件补发
    @Override
    public void reissueMail(MultipartFile file, ProductionBO bean) {
        //当前操作员姓名
        PermiUserDO permiUserDO = new PermiUserDO();
        String loginName = SecurityUtils.getLoginName();
        permiUserDO.setUserId(loginName);
        System.err.println(loginName);
        List<PermiUserDO> permiUserDOS = permiUserDao.find(permiUserDO);
        if(permiUserDOS.isEmpty()){
            MsgEnum.ERROR_CUSTOM.setMsgInfo("操作人信息未同步，请联系管理员");
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        String userName = permiUserDOS.get(0).getUserName();

        ProductionDO productionDO = operationProductionDao.findProductionBean(bean.getProNumber());
        //判断操作人是否是该投产申请人，若不是则判断是不是部门经理
        if(!userName.equals(productionDO.getProApplicant())){

            TPermiDeptDO permiDeptDO = new TPermiDeptDO();
            permiDeptDO.setDeptName(productionDO.getApplicationDept());
            //获得开发主导部门查询该部门部门经理
            List<TPermiDeptDO> tPermiDeptDOS = permiDeptDao.find(permiDeptDO);
            String deptManagerName = tPermiDeptDOS.get(0).getDeptManagerName();
            if(!deptManagerName.equals(productionDO.getProApplicant())){
                MsgEnum.ERROR_CUSTOM.setMsgInfo("只有申请人本人和申请部门部门经理才有权操作");
                BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
            }
        }

        ProductionBO productionBO = BeanUtils.copyPropertiesReturnDest(new ProductionBO(), productionDO);
        productionBO.setMailRecipient(bean.getMailRecipient());
        productionBO.setMailCopyPerson(bean.getMailCopyPerson());
        this.productionInput( file,false,  productionBO);
    }


}
