package com.cmpay.lemon.monitor.service.impl.defects;


import cn.afterturn.easypoi.excel.ExcelExportUtil;
import cn.afterturn.easypoi.excel.entity.ExportParams;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.framework.page.PageInfo;
import com.cmpay.lemon.framework.utils.PageUtils;
import com.cmpay.lemon.monitor.bo.*;
import com.cmpay.lemon.monitor.dao.*;
import com.cmpay.lemon.monitor.entity.*;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import com.cmpay.lemon.monitor.service.defects.DefectsService;
import com.cmpay.lemon.monitor.utils.BeanConvertUtils;
import com.cmpay.lemon.monitor.utils.DateUtil;
import com.cmpay.lemon.monitor.utils.ReadExcelUtils;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.beans.Transient;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@Service
public class DefectsServiceImpl  implements DefectsService {
    @Autowired
    private IProductionDefectsExtDao productionDefectsDao;
    @Autowired
    private ISmokeTestFailedCountExtDao smokeTestFailedCountDao;
    @Autowired
    private ISmokeTestRegistrationExtDao smokeTestRegistrationDao;
    @Autowired
    private IZenQuestiontExtDao zenQuestiontExtDao;
    @Autowired
    private IOnlineDefectExtDao onlineDefectExtDao;
    @Autowired
    private IUserExtDao iUserDao;
    @Autowired
    private IDefectDetailsExtDao defectDetailsDao;
    @Autowired
    private IOrganizationStructureDao iOrganizationStructureDao;

    @Override
    public ProductionDefectsRspBO findDefectAll(ProductionDefectsBO productionDefectsBO) {
        PageInfo<ProductionDefectsBO> pageInfo = getProductionDefectsPageInfo(productionDefectsBO);
        List<ProductionDefectsBO> productionDefectsBOList = BeanConvertUtils.convertList(pageInfo.getList(), ProductionDefectsBO.class);

        ProductionDefectsRspBO productionDefectsRspBO = new ProductionDefectsRspBO();
        productionDefectsRspBO.setProductionDefectsBOList(productionDefectsBOList);
        productionDefectsRspBO.setPageInfo(pageInfo);
        return productionDefectsRspBO;
    }

    @Override
    public SmokeTestRegistrationRspBO smokeTestFailedQuery(SmokeTestRegistrationBO smokeTestRegistrationBO) {
        PageInfo<SmokeTestRegistrationBO> pageInfo = getProductionDefectsPageInfo2(smokeTestRegistrationBO);
        List<SmokeTestRegistrationBO> productionDefectsBOList = BeanConvertUtils.convertList(pageInfo.getList(), SmokeTestRegistrationBO.class);

        SmokeTestRegistrationRspBO smokeTestRegistrationRspBO = new SmokeTestRegistrationRspBO();
        smokeTestRegistrationRspBO.setSmokeTestRegistrationBOList(productionDefectsBOList);
        smokeTestRegistrationRspBO.setPageInfo(pageInfo);
        return smokeTestRegistrationRspBO;
    }

    private PageInfo<ProductionDefectsBO> getProductionDefectsPageInfo(ProductionDefectsBO productionDefectsBO) {
        ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
        BeanConvertUtils.convert(productionDefectsDO, productionDefectsBO);
        PageInfo<ProductionDefectsBO> pageInfo = PageUtils.pageQueryWithCount(productionDefectsBO.getPageNum(), productionDefectsBO.getPageSize(),
                () -> BeanConvertUtils.convertList(productionDefectsDao.findList(productionDefectsDO), ProductionDefectsBO.class));
        return pageInfo;
    }

    private PageInfo<SmokeTestRegistrationBO> getProductionDefectsPageInfo2(SmokeTestRegistrationBO smokeTestRegistrationBO) {
        SmokeTestRegistrationDO smokeTestRegistrationDO = new SmokeTestRegistrationDO();
        BeanConvertUtils.convert(smokeTestRegistrationDO, smokeTestRegistrationBO);
        PageInfo<SmokeTestRegistrationBO> pageInfo = PageUtils.pageQueryWithCount(smokeTestRegistrationBO.getPageNum(), smokeTestRegistrationBO.getPageSize(),
                () -> BeanConvertUtils.convertList(smokeTestRegistrationDao.findList(smokeTestRegistrationDO), SmokeTestRegistrationBO.class));
        return pageInfo;
    }

    //??????
    @Override
    public void getDownloadTest(HttpServletResponse response, SmokeTestRegistrationBO smokeTestRegistrationBO) {
        SmokeTestRegistrationDO smokeTestRegistrationDO = new SmokeTestRegistrationDO();
        BeanConvertUtils.convert(smokeTestRegistrationDO, smokeTestRegistrationBO);
        List<SmokeTestRegistrationDO> demandDOList = smokeTestRegistrationDao.findList(smokeTestRegistrationDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), SmokeTestRegistrationDO.class, demandDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "productionDefectsDO_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }
    //??????
    @Override
    public void getDownload(HttpServletResponse response, ProductionDefectsBO productionDefectsBO) {
        ProductionDefectsDO productionDefectsDO = new ProductionDefectsDO();
        BeanConvertUtils.convert(productionDefectsDO, productionDefectsBO);
        List<ProductionDefectsDO> demandDOList = productionDefectsDao.findList(productionDefectsDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), ProductionDefectsDO.class, demandDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "productionDefectsDO_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
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
    public void zennDataImport(MultipartFile file){
        File f = null;
        LinkedList<ZenQuestiontDO> zenQuestiontDOLinkedList = new LinkedList<>();
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
//                `bugNumber` varchar(255) COLLATE utf8_bin NOT NULL DEFAULT '' COMMENT 'BUG??????',
//                        `belongProducts` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `belongModule` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `belongProject` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `relatedDemand` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `relatedTask` varchar(2555) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `bugTitle` varchar(2555) COLLATE utf8_bin DEFAULT NULL COMMENT 'Bug??????',
//                        `keyword` varchar(2048) COLLATE utf8_bin DEFAULT NULL COMMENT '?????????',
//                        `severity` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `priority` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '?????????',
//                        `bugType` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT 'Bug??????',
//                        `operatingSystem` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `browser` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '?????????',
//                        `repeatSteps` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `bugStatus` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT 'Bug??????',
//                        `expirationDate` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `activateNumber` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `whetherConfirm` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `carbonCopy` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '?????????',
//                        `creator` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `createdDate` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `affectsVersion` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `assigned` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '?????????',
//                        `assignedDate` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `solver` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '?????????',
//                        `solution` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `solveVersion` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `solveDate` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `shutPerson` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `shutDate` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `repetitionId` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '??????ID',
//                        `relatedBug` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '??????Bug',
//                        `relatedCase` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `lastReviser` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '???????????????',
//                        `changedDate` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '????????????',
//                        `accessory` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '??????',
//                        `secondlevelorganization` varchar(255) COLLATE utf8_bin DEFAULT NULL COMMENT '??????????????????',
                ZenQuestiontDO zenQuestiontDO = new ZenQuestiontDO();
                //?????????????????????????????????double ??????.0
                if (!JudgeUtils.isEmpty(map.get(i).get(0).toString().trim())) {
                    zenQuestiontDO.setBugnumber((int)Double.parseDouble(map.get(i).get(0).toString().trim())+"");
                }
                zenQuestiontDO.setBelongproducts(map.get(i).get(1).toString().trim());
                zenQuestiontDO.setBelongmodule(map.get(i).get(2).toString().trim());
                zenQuestiontDO.setBelongproject(map.get(i).get(3).toString().trim());
                zenQuestiontDO.setRelateddemand(map.get(i).get(4).toString().trim());
                zenQuestiontDO.setRelatedtask(map.get(i).get(5).toString().trim());
                zenQuestiontDO.setBugtitle(map.get(i).get(6).toString().trim());
                zenQuestiontDO.setKeyword(map.get(i).get(7).toString().trim());
                if (!JudgeUtils.isEmpty(map.get(i).get(8).toString().trim())) {
                    zenQuestiontDO.setSeverity((int)Double.parseDouble(map.get(i).get(8).toString().trim())+"");
                }
                if (!JudgeUtils.isEmpty(map.get(i).get(9).toString().trim())) {
                    zenQuestiontDO.setPriority((int)Double.parseDouble(map.get(i).get(9).toString().trim())+"");
                }
                zenQuestiontDO.setBugtype(map.get(i).get(10).toString().trim());
                zenQuestiontDO.setOperatingsystem(map.get(i).get(11).toString().trim());
                zenQuestiontDO.setBrowser(map.get(i).get(12).toString().trim());
                zenQuestiontDO.setRepeatsteps(map.get(i).get(13).toString().trim());
                zenQuestiontDO.setBugstatus(map.get(i).get(14).toString().trim());
                //????????????
                if (map.get(i).get(15) instanceof String) {
                    zenQuestiontDO.setExpirationdate(map.get(i).get(15).toString().trim());
                }
                if (map.get(i).get(15) instanceof Date) {
                    Date date = (Date)map.get(i).get(15);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    zenQuestiontDO.setExpirationdate(dt.trim());
                }
                if (!JudgeUtils.isEmpty(map.get(i).get(16).toString().trim())) {
                    zenQuestiontDO.setActivatenumber((int)Double.parseDouble(map.get(i).get(16).toString().trim())+"");
                }
                zenQuestiontDO.setWhetherconfirm(map.get(i).get(17).toString().trim());
                zenQuestiontDO.setCarboncopy(map.get(i).get(18).toString().trim());
                zenQuestiontDO.setCreator(map.get(i).get(19).toString().trim());
                //????????????
                if (map.get(i).get(20) instanceof String) {
                    zenQuestiontDO.setCreateddate(map.get(i).get(20).toString().trim());
                }
                if (map.get(i).get(20) instanceof Date) {
                    Date date = (Date)map.get(i).get(20);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    zenQuestiontDO.setCreateddate(dt.trim());
                }
                zenQuestiontDO.setAffectsversion(map.get(i).get(21).toString().trim());
                zenQuestiontDO.setAssigned(map.get(i).get(22).toString().trim());
                //????????????
                if (map.get(i).get(23) instanceof String) {
                    zenQuestiontDO.setAssigneddate(map.get(i).get(23).toString().trim());
                }
                if (map.get(i).get(23) instanceof Date) {
                    Date date = (Date)map.get(i).get(23);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    zenQuestiontDO.setAssigneddate(dt.trim());
                }
                zenQuestiontDO.setSolver(map.get(i).get(24).toString().trim());
                zenQuestiontDO.setSolution(map.get(i).get(25).toString().trim());
                zenQuestiontDO.setSolveversion(map.get(i).get(26).toString().trim());
                //????????????
                if (map.get(i).get(27) instanceof String) {
                    zenQuestiontDO.setSolvedate(map.get(i).get(27).toString().trim());
                }
                if (map.get(i).get(27) instanceof Date) {
                    Date date = (Date)map.get(i).get(27);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    zenQuestiontDO.setSolvedate(dt.trim());
                }
                zenQuestiontDO.setShutperson(map.get(i).get(28).toString().trim());
                //????????????
                if (map.get(i).get(29) instanceof String) {
                    zenQuestiontDO.setShutdate(map.get(i).get(29).toString().trim());
                }
                if (map.get(i).get(29) instanceof Date) {
                    Date date = (Date)map.get(i).get(29);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    zenQuestiontDO.setShutdate(dt.trim());
                }
                zenQuestiontDO.setRepetitionid(map.get(i).get(30).toString().trim());
                zenQuestiontDO.setRelatedbug(map.get(i).get(31).toString().trim());
                zenQuestiontDO.setRelatedcase(map.get(i).get(32).toString().trim());
                zenQuestiontDO.setLastreviser(map.get(i).get(33).toString().trim());
                //????????????
                if (map.get(i).get(34) instanceof String) {
                    zenQuestiontDO.setChangeddate(map.get(i).get(34).toString().trim());
                }
                if (map.get(i).get(34) instanceof Date) {
                    Date date = (Date)map.get(i).get(34);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                    String dt = simpleDateFormat.format(date);
                    zenQuestiontDO.setChangeddate(dt.trim());
                }
                zenQuestiontDO.setAccessory(map.get(i).get(35).toString().trim());
                // ???????????????????????????????????????????????????
                if(JudgeUtils.isNotBlank(zenQuestiontDO.getCreator())){
                    UserDO userDO = new UserDO();
                    userDO.setFullname(zenQuestiontDO.getCreator());
                    List<UserDO> userDOS = iUserDao.find(userDO);
                    if(!userDOS.isEmpty()){
                        if("??????????????????".equals(userDOS.get(0).getDepartment())){
                            zenQuestiontDO.setTest(true);
                        }else {
                            zenQuestiontDO.setTest(false);
                        }
                    }else{
                        zenQuestiontDO.setTest(false);
                    }
                }
                //??????????????????
                //???????????????????????????????????????????????????????????????
                // ?????????????????????????????????????????????
                if(JudgeUtils.isNotBlank(zenQuestiontDO.getSolver())){
                    UserDO userDO = new UserDO();
                    userDO.setFullname(zenQuestiontDO.getSolver());
                    List<UserDO> userDOS = iUserDao.find(userDO);
                    if(!userDOS.isEmpty()){
                        zenQuestiontDO.setSecondlevelorganization(userDOS.get(0).getDepartment());
                        OrganizationStructureDO organizationStructureDO = new OrganizationStructureDO();
                        organizationStructureDO.setSecondlevelorganization(userDOS.get(0).getDepartment());
                        List<OrganizationStructureDO> organizationStructureDOList =  iOrganizationStructureDao.find(organizationStructureDO);
                        if (organizationStructureDOList!=null && organizationStructureDOList.size()>0){
                            // ??????????????????
                            zenQuestiontDO.setFirstLevelOrganization(organizationStructureDOList.get(0).getFirstlevelorganization());
                        }
                    }else{
                        zenQuestiontDO.setSecondlevelorganization("");
                        zenQuestiontDO.setFirstLevelOrganization(null);
                    }
                }
                zenQuestiontDOLinkedList.add(zenQuestiontDO);
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
        zenQuestiontDOLinkedList.forEach(m -> {
            ZenQuestiontDO zenQuestiontDO1 = new ZenQuestiontDO();
            //??????????????????????????????
            zenQuestiontDO1.setBugnumber(m.getBugnumber());
            List<ZenQuestiontDO> zenQuestiontDOList = zenQuestiontExtDao.find(zenQuestiontDO1);
            if (JudgeUtils.isEmpty(zenQuestiontDOList)) {
                zenQuestiontExtDao.insert(m);
            } else {
                zenQuestiontExtDao.update(m);
            }
            // ????????????????????????????????????????????????
            if(m.isTest()){
                //?????????????????????
                if(!"????????????".equals(m.getSolution()) && !"??????BUG".equals(m.getSolution()) ){
                    DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                    defectDetailsDO.setJireKey(m.getBugnumber());
                    defectDetailsDO.setEpicKey(m.getBugnumber());
                    defectDetailsDO.setDefectName(m.getBugtitle());
                    defectDetailsDO.setRegistrationDate(m.getCreateddate()+" 00:00:01");
                    defectDetailsDO.setTestNumber(0);
                    defectDetailsDO.setDefectDetails(m.getRepeatsteps());
                    defectDetailsDO.setDefectRegistrant(m.getCreator());
                    defectDetailsDO.setSolution(m.getSolution());
                    if("?????????".equals(m.getBugstatus()) || "?????????".equals(m.getBugstatus()) ){
                        defectDetailsDO.setDefectStatus("??????");
                    }else{
                        defectDetailsDO.setDefectStatus("?????????");
                    }
                    DefectDetailsDO defectDetailsDO1 = defectDetailsDao.get(defectDetailsDO.getJireKey());
                    if (JudgeUtils.isNull(defectDetailsDO1)) {
                        defectDetailsDao.insert(defectDetailsDO);
                    } else {
                        defectDetailsDao.update(defectDetailsDO);
                    }
                }
            }
            //???????????????????????????
            if(m.getSecondlevelorganization()!=null && m.getSecondlevelorganization()!=""){
                //?????????????????????
                if(!"????????????".equals(m.getSolution()) && !"????????????".equals(m.getSolution())
                        && !"????????????".equals(m.getSolution()) && !"??????BUG".equals(m.getSolution()) ){
                    DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
                    defectDetailsDO.setJireKey(m.getBugnumber());
                    defectDetailsDO.setEpicKey(m.getBugnumber());
                    defectDetailsDO.setDefectName(m.getBugtitle());
                    defectDetailsDO.setDefectsDepartment(m.getSecondlevelorganization());
                    defectDetailsDO.setRegistrationDate(m.getCreateddate()+" 00:00:00");
                    defectDetailsDO.setTestNumber(0);
                    defectDetailsDO.setDefectDetails(m.getRepeatsteps());
                    defectDetailsDO.setDefectRegistrant(m.getCreator());
                    defectDetailsDO.setAssignee(m.getSolver());
                    defectDetailsDO.setSolution(m.getSolution());
                    defectDetailsDO.setFirstlevelorganization(m.getFirstLevelOrganization());
                    defectDetailsDO.setProblemHandler(m.getSolver());
                    defectDetailsDO.setProblemHandlerDepartment(m.getSecondlevelorganization());
                    if("?????????".equals(m.getBugstatus()) || "?????????".equals(m.getBugstatus()) ){
                        defectDetailsDO.setDefectStatus("??????");
                    }else{
                        defectDetailsDO.setDefectStatus("?????????");
                    }
                    DefectDetailsDO defectDetailsDO1 = defectDetailsDao.get(defectDetailsDO.getJireKey());
                    if (JudgeUtils.isNull(defectDetailsDO1)) {
                        defectDetailsDao.insert(defectDetailsDO);
                    } else {
                        defectDetailsDao.update(defectDetailsDO);
                    }
                }
            }
        });
    }

    @Override
    public ZenQuestiontRspBO zenQuestiontFindList(ZenQuestiontBO zenQuestiontBO){
        PageInfo<ZenQuestiontBO> pageInfo = getPageInfo1(zenQuestiontBO);
        List<ZenQuestiontBO> zenQuestiontBOList = BeanConvertUtils.convertList(pageInfo.getList(), ZenQuestiontBO.class);
        ZenQuestiontRspBO zenQuestiontRspBO = new ZenQuestiontRspBO();
        zenQuestiontRspBO.setZenQuestiontBOList(zenQuestiontBOList);
        zenQuestiontRspBO.setPageInfo(pageInfo);
        return zenQuestiontRspBO;
    }
    private PageInfo<ZenQuestiontBO>  getPageInfo1(ZenQuestiontBO zenQuestiontBO) {
        ZenQuestiontDO zenQuestiontDO = new ZenQuestiontDO();
        BeanConvertUtils.convert(zenQuestiontDO, zenQuestiontBO);
        PageInfo<ZenQuestiontBO> pageInfo = PageUtils.pageQueryWithCount(zenQuestiontBO.getPageNum(), zenQuestiontBO.getPageSize(),
                () -> BeanConvertUtils.convertList(zenQuestiontExtDao.findList(zenQuestiontDO), ZenQuestiontBO.class));
        return pageInfo;
    }

    @Override
    public void downloadZenQuestiont(HttpServletResponse response,ZenQuestiontBO zenQuestiontBO){
        ZenQuestiontDO zenQuestiontDO = new ZenQuestiontDO();
        BeanConvertUtils.convert(zenQuestiontDO, zenQuestiontBO);
        List<ZenQuestiontDO> demandDOList = zenQuestiontExtDao.findList(zenQuestiontDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), ZenQuestiontDO.class, demandDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "productionDefectsDO_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }
    @Override
    public OnlineDefectRspBO onlineDefectFindList(OnlineDefectBO onlineDefectBO){
        PageInfo<OnlineDefectBO> pageInfo = getOnlineDefectPageInfo(onlineDefectBO);
        List<OnlineDefectBO> zenQuestiontBOList = BeanConvertUtils.convertList(pageInfo.getList(), OnlineDefectBO.class);
        OnlineDefectRspBO onlineDefectRspBO = new OnlineDefectRspBO();
        onlineDefectRspBO.setOnlineDefectBOList(zenQuestiontBOList);
        onlineDefectRspBO.setPageInfo(pageInfo);
        return onlineDefectRspBO;
    }
    private PageInfo<OnlineDefectBO>  getOnlineDefectPageInfo(OnlineDefectBO onlineDefectBO) {
        OnlineDefectDO onlineDefectDO = new OnlineDefectDO();
        BeanConvertUtils.convert(onlineDefectDO, onlineDefectBO);
        PageInfo<OnlineDefectBO> pageInfo = PageUtils.pageQueryWithCount(onlineDefectBO.getPageNum(), onlineDefectBO.getPageSize(),
                () -> BeanConvertUtils.convertList(onlineDefectExtDao.findList(onlineDefectDO), OnlineDefectBO.class));
        return pageInfo;
    }
    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = RuntimeException.class)
    public void onlineDefectImport(MultipartFile file){
        File f = null;
        LinkedList<OnlineDefectDO> onlineDefectDOLinkedList = new LinkedList<>();
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
                OnlineDefectDO onlineDefectDO = new OnlineDefectDO();
                //?????????????????????????????????double ??????.0
                if (!JudgeUtils.isEmpty(map.get(i).get(0).toString().trim())) {
                    onlineDefectDO.setReqImplMon((int)Double.parseDouble(map.get(i).get(0).toString().trim())+"");
                }
                onlineDefectDO.setFirstlevelorganization(map.get(i).get(1).toString().trim());
                onlineDefectDO.setIsAssess(map.get(i).get(2).toString().trim());
                onlineDefectDO.setNotAssessCause(map.get(i).get(3).toString().trim());
                onlineDefectDO.setDocumentNumber(map.get(i).get(4).toString().trim());
                onlineDefectDO.setProcessStatus(map.get(i).get(5).toString().trim());
                //????????????
                if (map.get(i).get(6) instanceof String) {
                    onlineDefectDO.setProcessStartDate(map.get(i).get(6).toString().trim());
                }
                if (map.get(i).get(6) instanceof Date) {
                    Date date = (Date)map.get(i).get(6);
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String dt = simpleDateFormat.format(date);
                    onlineDefectDO.setProcessStartDate(dt.trim());
                }
                onlineDefectDO.setDefectProposer(map.get(i).get(7).toString().trim());
                onlineDefectDO.setDefectTheme(map.get(i).get(8).toString().trim());
                onlineDefectDO.setDefrctDescription(map.get(i).get(9).toString().trim());
                onlineDefectDO.setDevelopmentLeader(map.get(i).get(10).toString().trim());
                onlineDefectDO.setProductLeader(map.get(i).get(11).toString().trim());
                onlineDefectDO.setManufacturers(map.get(i).get(12).toString().trim());
                onlineDefectDO.setManufacturersProduct(map.get(i).get(13).toString().trim());
                onlineDefectDO.setQuestionCause(map.get(i).get(14).toString().trim());
                onlineDefectDO.setQuestionType(map.get(i).get(15).toString().trim());
                onlineDefectDO.setSolution(map.get(i).get(16).toString().trim());
                onlineDefectDOLinkedList.add(onlineDefectDO);
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
        onlineDefectDOLinkedList.forEach(m -> {
            OnlineDefectDO zenQuestiontDO1 = new OnlineDefectDO();
            //??????????????????????????????
            zenQuestiontDO1.setDocumentNumber(m.getDocumentNumber());
            List<OnlineDefectDO> zenQuestiontDOList = onlineDefectExtDao.find(zenQuestiontDO1);
            if (JudgeUtils.isEmpty(zenQuestiontDOList)) {
                onlineDefectExtDao.insert(m);
            } else {
                m.setId(zenQuestiontDOList.get(0).getId());
                onlineDefectExtDao.update(m);
            }
        });
    }
    @Override
    public void onlineDefectDownloadt(HttpServletResponse response,OnlineDefectBO onlineDefectBO){
        OnlineDefectDO zenQuestiontDO = new OnlineDefectDO();
        BeanConvertUtils.convert(zenQuestiontDO, onlineDefectBO);
        List<OnlineDefectDO> demandDOList = onlineDefectExtDao.findList(zenQuestiontDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), OnlineDefectDO.class, demandDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "productionDefectsDO_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }

    @Override
    public void internalDefectDownload(HttpServletResponse response,DefectDetailsBO defectDetailsBO){
        DefectDetailsDO zenQuestiontDO = new DefectDetailsDO();
        BeanConvertUtils.convert(zenQuestiontDO, defectDetailsBO);
        List<DefectDetailsDO> demandDOList = defectDetailsDao.findDefect(zenQuestiontDO);
        Workbook workbook = ExcelExportUtil.exportExcel(new ExportParams(), DefectDetailsDO.class, demandDOList);
        try (OutputStream output = response.getOutputStream();
             BufferedOutputStream bufferedOutPut = new BufferedOutputStream(output)) {
            // ????????????
            if (workbook == null) {
                BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
            }
            // ??????excel???????????????
            String excelName = "productionDefectsDO_" + DateUtil.date2String(new Date(), "yyyyMMddHHmmss") + ".xls";
            response.setHeader(CONTENT_DISPOSITION, "attchement;filename=" + excelName);
            response.setHeader(ACCESS_CONTROL_EXPOSE_HEADERS, CONTENT_DISPOSITION);
            workbook.write(bufferedOutPut);
            bufferedOutPut.flush();
        } catch (IOException e) {
            BusinessException.throwBusinessException(MsgEnum.BATCH_IMPORT_FAILED);
        }
    }
    @Override
    public DefectDetailsRspBO internalDefectInquiry(DefectDetailsBO defectDetailsBO){
        PageInfo<DefectDetailsBO> pageInfo = getinternalDefectPageInfo(defectDetailsBO);
        List<DefectDetailsBO> defectDetailsBOList = BeanConvertUtils.convertList(pageInfo.getList(), DefectDetailsBO.class);
        DefectDetailsRspBO onlineDefectRspBO = new DefectDetailsRspBO();
        onlineDefectRspBO.setDefectDetailsBos(defectDetailsBOList);
        onlineDefectRspBO.setPageInfo(pageInfo);
        return onlineDefectRspBO;
    }
    private PageInfo<DefectDetailsBO>  getinternalDefectPageInfo(DefectDetailsBO defectDetailsBO) {
        DefectDetailsDO defectDetailsDO = new DefectDetailsDO();
        BeanConvertUtils.convert(defectDetailsDO, defectDetailsBO);
        PageInfo<DefectDetailsBO> pageInfo = PageUtils.pageQueryWithCount(defectDetailsBO.getPageNum(), defectDetailsBO.getPageSize(),
                () -> BeanConvertUtils.convertList(defectDetailsDao.findDefect(defectDetailsDO), DefectDetailsBO.class));
        return pageInfo;
    }
}
