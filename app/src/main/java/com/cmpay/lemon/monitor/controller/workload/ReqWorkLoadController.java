package com.cmpay.lemon.monitor.controller.workload;

import com.cmpay.framework.data.request.GenericDTO;
import com.cmpay.framework.data.response.GenericRspDTO;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.BeanUtils;
import com.cmpay.lemon.common.utils.StringUtils;
import com.cmpay.lemon.framework.data.NoBody;
import com.cmpay.lemon.monitor.bo.*;
import com.cmpay.lemon.monitor.constant.MonitorConstants;
import com.cmpay.lemon.monitor.dto.*;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import com.cmpay.lemon.monitor.service.demand.ReqPlanService;
import com.cmpay.lemon.monitor.service.workload.ReqWorkLoadService;
import com.cmpay.lemon.monitor.utils.BeanConvertUtils;
import com.cmpay.lemon.monitor.utils.DateUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static com.cmpay.lemon.monitor.constant.MonitorConstants.FILE;
import static com.cmpay.lemon.monitor.utils.FileUtils.doWrite;

/**
 * @author: zhou_xiong
 * 需求周反馈
 */
@RestController
@RequestMapping(value = MonitorConstants.REQWORK_PATH)
public class ReqWorkLoadController {
    @Autowired
    private ReqPlanService reqPlanService;
    @Autowired
    private ReqWorkLoadService reqWorkLoadService;

    /**
     * 分页需求列表
     *
     * @param reqDTO
     * @return
     */
    @RequestMapping("/list")
    public GenericRspDTO<DemandRspDTO> getUserInfoPage(@RequestBody DemandReqDTO reqDTO) {
        if(reqDTO.getReqStartDate()!=null && !reqDTO.getReqStartDate().equals("") && (reqDTO.getReqEndDate()==null || reqDTO.getReqEndDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqStartDate());
        }
        if(reqDTO.getReqEndDate()!=null && !reqDTO.getReqEndDate().equals("") && (reqDTO.getReqStartDate()==null || reqDTO.getReqStartDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqEndDate());
        }
        DemandBO demandBO = BeanUtils.copyPropertiesReturnDest(new DemandBO(), reqDTO);
        DemandRspBO demandRspBO = reqWorkLoadService.findDemand(demandBO);
        DemandRspDTO rspDTO = new DemandRspDTO();
        rspDTO.setDemandDTOList(BeanConvertUtils.convertList(demandRspBO.getDemandBOList(), DemandDTO.class));
        rspDTO.setPageNum(demandRspBO.getPageInfo().getPageNum());
        rspDTO.setPages(demandRspBO.getPageInfo().getPages());
        rspDTO.setTotal(demandRspBO.getPageInfo().getTotal());
        rspDTO.setPageSize(demandRspBO.getPageInfo().getPageSize());
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, rspDTO);
    }

    /**
     * 存量变更
     *
     * @return
     */
    @RequestMapping("/changeReq")
    public GenericRspDTO changeReq(@RequestBody DemandReqDTO reqDTO) {
        System.out.println(reqDTO.getReqImplMon());
        reqWorkLoadService.changeReq(reqDTO.getReqImplMon());
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, NoBody.class);
    }
    /**
     * 已完成但工作量未完全录入需求存量变更
     *
     * @return
     */
    @RequestMapping("/changesInLegacyWorkload")
    public GenericRspDTO changesInLegacyWorkload(@RequestBody DemandReqDTO reqDTO) {
        System.out.println(reqDTO.getReqImplMon());
        reqPlanService.changesInLegacyWorkload(reqDTO.getReqImplMon());
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, NoBody.class);
    }
    /**
     * 模板下载
     *
     * @return
     * @throws IOException
     */
    @PostMapping("/template/download")
    public GenericRspDTO<NoBody> downloadTmp(GenericDTO<NoBody> req, HttpServletResponse response) {
        doWrite("static/workLoad.xlsx", response);
        return GenericRspDTO.newSuccessInstance();
    }
    /**
     * 工作量导入
     *
     * @return
     */
    @PostMapping("/batch/import")
    public GenericRspDTO<NoBody> batchImport(HttpServletRequest request, GenericDTO<NoBody> req) {
        MultipartFile file = ((MultipartHttpServletRequest) request).getFile(FILE);
        reqWorkLoadService.doBatchImport(file);
        return GenericRspDTO.newSuccessInstance();
    }
    // 基地工作量导出
    @RequestMapping("/goExportForJd")
    public void goExportForJd(@RequestBody DemandReqDTO reqDTO,HttpServletRequest request,HttpServletResponse response) {
        if(reqDTO.getReqStartDate()!=null && !reqDTO.getReqStartDate().equals("") && (reqDTO.getReqEndDate()==null || reqDTO.getReqEndDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqStartDate());
        }
        if(reqDTO.getReqEndDate()!=null && !reqDTO.getReqEndDate().equals("") && (reqDTO.getReqStartDate()==null || reqDTO.getReqStartDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqEndDate());
        }
        DemandBO demandBO = new DemandBO();
        BeanConvertUtils.convert(demandBO, reqDTO);
        reqWorkLoadService.exportExcel(request,response, demandBO, "1", "基地工作量");
    }
    // 各部门工作量月统计明细报表导出
    @RequestMapping("/goExportDetlForDevp")
    public void goExportDetlForDevp(@RequestBody DemandReqDTO reqDTO,HttpServletRequest request,HttpServletResponse response) {
        if(reqDTO.getReqStartDate()!=null && !reqDTO.getReqStartDate().equals("") && (reqDTO.getReqEndDate()==null || reqDTO.getReqEndDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqStartDate());
        }
        if(reqDTO.getReqEndDate()!=null && !reqDTO.getReqEndDate().equals("") && (reqDTO.getReqStartDate()==null || reqDTO.getReqStartDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqEndDate());
        }
        DemandBO demandBO = new DemandBO();
        BeanConvertUtils.convert(demandBO, reqDTO);
        reqWorkLoadService.exportExcel(request,response, demandBO, "2", "基地工作量");
    }
    // 各部门工作量月统计明细报表导出
    @RequestMapping("/goExportCountForDevp")
    public GenericRspDTO<NoBody> goExportCountForDevp(@RequestBody DemandReqDTO reqDTO,HttpServletRequest request,HttpServletResponse response) {
        if(reqDTO.getReqStartDate()!=null && !reqDTO.getReqStartDate().equals("") && (reqDTO.getReqEndDate()==null || reqDTO.getReqEndDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqStartDate());
        }
        if(reqDTO.getReqEndDate()!=null && !reqDTO.getReqEndDate().equals("") && (reqDTO.getReqStartDate()==null || reqDTO.getReqStartDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqEndDate());
        }
        DemandBO demandBO = new DemandBO();
        BeanConvertUtils.convert(demandBO, reqDTO);
        reqWorkLoadService.exportExcel(request,response, demandBO, "3", "基地工作量");
        return GenericRspDTO.newSuccessInstance();
    }
    // 各二级部门工作量月统计明细报表导出
    @RequestMapping("/goExportCountForDevp2")
    public GenericRspDTO<NoBody> goExportCountForDevp2(@RequestBody DemandReqDTO reqDTO,HttpServletRequest request,HttpServletResponse response) {
        if(reqDTO.getReqStartDate()!=null && !reqDTO.getReqStartDate().equals("") && (reqDTO.getReqEndDate()==null || reqDTO.getReqEndDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqStartDate());
        }
        if(reqDTO.getReqEndDate()!=null && !reqDTO.getReqEndDate().equals("") && (reqDTO.getReqStartDate()==null || reqDTO.getReqStartDate().equals(""))){
            reqDTO.setReqImplMon(reqDTO.getReqEndDate());
        }
        DemandBO demandBO = new DemandBO();
        BeanConvertUtils.convert(demandBO, reqDTO);
        reqWorkLoadService.goExportCountForDevp2(request,response, demandBO, "3");
        return GenericRspDTO.newSuccessInstance();
    }

    /**
     * 查询主导部门工作量占比
     *
     */
    @GetMapping("/getPerDeptWorkLoad1")
    public GenericRspDTO<DictionaryRspDTO> getPerDeptWorkLoad1(@RequestParam("reqInnerSeq") String reqInnerSeq, GenericDTO<NoBody> req) {
        DemandBO bean = reqPlanService.findById(reqInnerSeq);
        DictionaryBO dictionaryBO = new DictionaryBO();
        String leadDeptPro = bean.getLeadDeptPro();
        List<DictionaryBO> leadDeptWorkLoads = new ArrayList<>();
        if (StringUtils.isNotEmpty(leadDeptPro)) {
            String leadDepts[] = leadDeptPro.split(";");
            for (int i = 0; i < leadDepts.length; i++) {
                if (StringUtils.isNotEmpty(leadDepts[i])) {
                    String perLeadDept[] = leadDepts[i].split(":");
                    if (perLeadDept.length == 2) {
                        DictionaryBO leadDeptWorkLoad = new DictionaryBO();
                        leadDeptWorkLoad.setDeptName(perLeadDept[0]);
                        leadDeptWorkLoad.setDeptRate(perLeadDept[1].replaceAll("%", ""));
                        leadDeptWorkLoads.add(leadDeptWorkLoad);
                    }
                }
            }
        } else {
            //判断配合部门是否为空
            String leadPdet = bean.getDevpLeadDept();
            if (StringUtils.isNotEmpty(leadPdet)) {
                if (StringUtils.isEmpty(bean.getDevpCoorDept())){
                    DictionaryBO coDeptWorkLoad = new DictionaryBO();
                    coDeptWorkLoad.setDeptName(leadPdet);
                    coDeptWorkLoad.setDeptRate("100");
                    leadDeptWorkLoads.add(coDeptWorkLoad);
                    if (bean.getTotalWorkload() == 0 || "".equals(bean.getTotalWorkload())){
                        bean.setLeadDeptWorkload(leadPdet+":0.00;");
                    }else {
                        bean.setLeadDeptWorkload(leadPdet+":"+String.format("%.2f",Double.valueOf(bean.getTotalWorkload()))+";");
                    }
                }else{
                    DictionaryBO coDeptWorkLoad = new DictionaryBO();
                    coDeptWorkLoad.setDeptName(leadPdet);
                    leadDeptWorkLoads.add(coDeptWorkLoad);
                    //model.addAttribute("leadDeptWorkLoads", leadDeptWorkLoads);
                }
            }
        }
        List<DictionaryBO> dictionaryBOList = leadDeptWorkLoads;
        DictionaryRspDTO dictionaryRspDTO = new DictionaryRspDTO();
        dictionaryRspDTO.setDictionaryDTOList(BeanConvertUtils.convertList(dictionaryBOList, DictionaryDTO.class));
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, dictionaryRspDTO);
    }
    /**
     * 查询配合部门工作量占比
     *
     */
    @GetMapping("/getPerDeptWorkLoad2")
    public GenericRspDTO<DictionaryRspDTO> getPerDeptWorkLoad2(@RequestParam("reqInnerSeq") String reqInnerSeq, GenericDTO<NoBody> req) {
        DemandBO bean = reqPlanService.findById(reqInnerSeq);
        DictionaryBO dictionaryBO = new DictionaryBO();
        String leadDeptPro = bean.getLeadDeptPro();
        List<DictionaryBO> leadDeptWorkLoads = new ArrayList<>();
        String coorDeptPro = bean.getCoorDeptPro();
        List<DictionaryBO> coorDeptWorkLoads = new ArrayList<>();
        if (StringUtils.isNotEmpty(coorDeptPro)) {
            String coorDepts[] = coorDeptPro.split(";");
            for (int i = 0; i < coorDepts.length; i++) {
                if (StringUtils.isNotEmpty(coorDepts[i])) {
                    String perCoorDept[] = coorDepts[i].split(":");
                    if (perCoorDept.length == 2) {
                        DictionaryBO coDeptWorkLoad = new DictionaryBO();
                        coDeptWorkLoad.setDeptName(perCoorDept[0]);
                        coDeptWorkLoad.setDeptRate(perCoorDept[1].replaceAll("%", ""));
                        coorDeptWorkLoads.add(coDeptWorkLoad);
                    }
                }
            }
        } else {
            String coorDept = bean.getDevpCoorDept();
            if (StringUtils.isNotEmpty(coorDept)) {
                String coorDepts[] = coorDept.split(",");
                for (int i = 0; i < coorDepts.length; i++) {
                    DictionaryBO coDeptWorkLoad = new DictionaryBO();
                    coDeptWorkLoad.setDeptName(coorDepts[i]);
                    coorDeptWorkLoads.add(coDeptWorkLoad);
                }
            }
        }

        List<DictionaryBO> dictionaryBOList = coorDeptWorkLoads;
        DictionaryRspDTO dictionaryRspDTO = new DictionaryRspDTO();
        dictionaryRspDTO.setDictionaryDTOList(BeanConvertUtils.convertList(dictionaryBOList, DictionaryDTO.class));
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, dictionaryRspDTO);
    }
    @RequestMapping("/checkDeptRate")
    public GenericRspDTO<WorkLoadRspDTO> checkDeptRate(@RequestBody DemandReqDTO reqDTO) {
        DemandBO demandBO = BeanUtils.copyPropertiesReturnDest(new DemandBO(), reqDTO);
        Map<String, String> map =reqWorkLoadService.checkDeptRate1(demandBO);
        if(StringUtils.isNotEmpty(map.get("message"))){
            MsgEnum.ERROR_CUSTOM.setMsgInfo("");
            MsgEnum.ERROR_CUSTOM.setMsgInfo(MsgEnum.ERROR_WORK_IMPORT.getMsgInfo() + map.get("message"));
            BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
        }
        WorkLoadRspDTO rspDTO = new WorkLoadRspDTO();
        rspDTO.setRemainWorkload(Double.parseDouble(map.get("remainWordkLoad")));
        rspDTO.setLeadDeptWorkload(map.get("leadDpetWorkLoad"));
        rspDTO.setCoorDeptWorkload(map.get("coorDpetWorkLoad"));
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, rspDTO);
    }
    /**
     * 更新需求信息
     *
     * @param
     * @return
     */
    @RequestMapping("/update")
    public GenericRspDTO updateReqWorkLoad(@RequestBody DemandDTO demandDTO) {
        DemandBO demandBO = BeanUtils.copyPropertiesReturnDest(new DemandBO(), demandDTO);
        reqWorkLoadService.update(demandBO);
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, NoBody.class);
    }

    /*
     *  修改月份工作量录入状态
     */
    @RequestMapping("/updateWorkloadEntryStatus")
    public GenericRspDTO updateWorkloadEntryStatus(@RequestBody UpdateReqWorkLoadDTO updateReqWorkLoadDTO) {
        WorkloadLockedStateBO workloadLockedStateBO = BeanUtils.copyPropertiesReturnDest(new WorkloadLockedStateBO(), updateReqWorkLoadDTO);
        reqWorkLoadService.updateWorkloadEntryStatus(workloadLockedStateBO);
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, NoBody.class);
    }

    /*
     *  获取月份工作量录入状态
     */
    @RequestMapping("/getWorkloadEntryStatus")
    public GenericRspDTO<WorkLoadDTO> getWorkloadEntryStatus(@RequestBody GetReqWorkLoadDTO getReqWorkLoadDTO) {
        WorkloadLockedStateBO workloadLockedStateBO = new WorkloadLockedStateBO();
        workloadLockedStateBO.setEntrymonth(getReqWorkLoadDTO.getEntrymonth());
        workloadLockedStateBO = reqWorkLoadService.getWorkloadEntryStatus(workloadLockedStateBO);
        WorkLoadDTO workLoadDTO = BeanUtils.copyPropertiesReturnDest(new WorkLoadDTO(), workloadLockedStateBO);
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, workLoadDTO);
    }
    /*
     *  修改月份工作量录入状态
     */
    @RequestMapping("/updateFeedbackEntryStatus")
    public GenericRspDTO updateFeedbackEntryStatus(@RequestBody UpdateReqWorkLoadDTO updateReqWorkLoadDTO) {
        WorkloadLockedStateBO workloadLockedStateBO = BeanUtils.copyPropertiesReturnDest(new WorkloadLockedStateBO(), updateReqWorkLoadDTO);
        reqWorkLoadService.updateFeedbackEntryStatus(workloadLockedStateBO);
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, NoBody.class);
    }

    /*
     *  获取月份工作量录入状态
     */
    @RequestMapping("/getFeedbackEntryStatus")
    public GenericRspDTO<WorkLoadDTO> getFeedbackEntryStatus(@RequestBody GetReqWorkLoadDTO getReqWorkLoadDTO) {
        WorkloadLockedStateBO workloadLockedStateBO = new WorkloadLockedStateBO();
        workloadLockedStateBO.setEntrymonth(getReqWorkLoadDTO.getEntrymonth());
        workloadLockedStateBO = reqWorkLoadService.getFeedbackEntryStatus(workloadLockedStateBO);
        WorkLoadDTO workLoadDTO = BeanUtils.copyPropertiesReturnDest(new WorkLoadDTO(), workloadLockedStateBO);
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, workLoadDTO);
    }

    /**
     * 分页需求列表
     *
     * @param reqDTO
     * @return
     */
    @RequestMapping("/supportWorkloadfindList")
    public GenericRspDTO<SupportWorkloadRspDTO> supportWorkloadfindList(@RequestBody SupportWorkloadReqDTO reqDTO) {
        if((reqDTO.getStartTime() != null && !reqDTO.getStartTime().equals(""))&&(reqDTO.getEndTime()==null || reqDTO.getEndTime().equals(""))){
            reqDTO.setCompletiontime(reqDTO.getStartTime());
        }
        if((reqDTO.getStartTime() == null|| reqDTO.getStartTime().equals(""))&&(reqDTO.getEndTime()!=null && !reqDTO.getEndTime().equals(""))){
            reqDTO.setCompletiontime(reqDTO.getEndTime());
        }
        SupportWorkloadBO supportWorkloadBO = BeanUtils.copyPropertiesReturnDest(new SupportWorkloadBO(), reqDTO);
        SupportWorkloadRspBO supportWorkloadRspBO = reqWorkLoadService.supportWorkloadfindList(supportWorkloadBO);
        SupportWorkloadRspDTO rspDTO = new SupportWorkloadRspDTO();
        rspDTO.setSupportWorkloadDTOList(BeanConvertUtils.convertList(supportWorkloadRspBO.getSupportWorkloadBOList(), SupportWorkloadDTO.class));
        rspDTO.setPageNum(supportWorkloadRspBO.getPageInfo().getPageNum());
        rspDTO.setPages(supportWorkloadRspBO.getPageInfo().getPages());
        rspDTO.setTotal(supportWorkloadRspBO.getPageInfo().getTotal());
        rspDTO.setPageSize(supportWorkloadRspBO.getPageInfo().getPageSize());
        return GenericRspDTO.newInstance(MsgEnum.SUCCESS, rspDTO);
    }

    /**
     * 支撑工作量导入
     *
     * @return
     */
    @PostMapping("/supportWorkloadDown")
    public GenericRspDTO<NoBody> supportWorkloadDown(HttpServletRequest request, GenericDTO<NoBody> req) {
        MultipartFile file = ((MultipartHttpServletRequest) request).getFile(FILE);
        reqWorkLoadService.supportWorkloadDown(file);
        return GenericRspDTO.newSuccessInstance();
    }

    /**
     * 支撑工作量导出
     *
     * @return
     * @throws IOException
     */
    @RequestMapping("/download")
    public GenericRspDTO<NoBody> download(@RequestBody SupportWorkloadReqDTO reqDTO, HttpServletResponse response) {
        if((reqDTO.getStartTime() != null && !reqDTO.getStartTime().equals(""))&&(reqDTO.getEndTime()==null || reqDTO.getEndTime().equals(""))){
            reqDTO.setProcessstartdate(reqDTO.getStartTime());
        }
        if((reqDTO.getStartTime() == null|| reqDTO.getStartTime().equals(""))&&(reqDTO.getEndTime()!=null && !reqDTO.getEndTime().equals(""))){
            reqDTO.setProcessstartdate(reqDTO.getEndTime());
        }
        SupportWorkloadBO supportWorkloadBO = BeanUtils.copyPropertiesReturnDest(new SupportWorkloadBO(), reqDTO);
        reqWorkLoadService.getDownload(response, supportWorkloadBO);
        return GenericRspDTO.newSuccessInstance();
    }

    // 各一级部门支撑工作量月统计明细报表导出
    @RequestMapping("/supportWorkloadCountForDevp")
    public GenericRspDTO<NoBody> supportWorkloadCountForDevp(@RequestBody SupportWorkloadReqDTO reqDTO,HttpServletRequest request,HttpServletResponse response) {
        SupportWorkloadBO supportWorkloadBO = BeanUtils.copyPropertiesReturnDest(new SupportWorkloadBO(), reqDTO);
        if (StringUtils.isBlank(supportWorkloadBO.getReqImplMon())) {
            supportWorkloadBO.setReqImplMon(DateUtil.date2String(new Date(), "yyyy-MM"));
        }
        reqWorkLoadService.supportWorkloadCountForDevp(request,response, supportWorkloadBO);
        return GenericRspDTO.newSuccessInstance();
    }
    // 各二级部门支撑工作量月统计明细报表导出
    @RequestMapping("/supportWorkloadCountForDevp2")
    public GenericRspDTO<NoBody> supportWorkloadCountForDevp2(@RequestBody SupportWorkloadReqDTO reqDTO,HttpServletRequest request,HttpServletResponse response) {
        SupportWorkloadBO supportWorkloadBO = BeanUtils.copyPropertiesReturnDest(new SupportWorkloadBO(), reqDTO);
        if (StringUtils.isBlank(supportWorkloadBO.getReqImplMon())) {
            supportWorkloadBO.setReqImplMon(DateUtil.date2String(new Date(), "yyyy-MM"));
        }
        reqWorkLoadService.supportWorkloadCountForDevp2(request,response, supportWorkloadBO);
        return GenericRspDTO.newSuccessInstance();
    }
}
