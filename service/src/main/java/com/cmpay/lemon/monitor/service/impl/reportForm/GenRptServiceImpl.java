package com.cmpay.lemon.monitor.service.impl.reportForm;


import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.common.utils.JudgeUtils;
import com.cmpay.lemon.monitor.bo.GenerateReportBO;
import com.cmpay.lemon.monitor.dao.IDiningReportDao;
import com.cmpay.lemon.monitor.entity.DiningReportDO;
import com.cmpay.lemon.monitor.service.reportForm.GenRptService;
import com.cmpay.lemon.monitor.utils.GenRptUtils;
import com.cmpay.lemon.monitor.utils.PrimaryDatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.cmpay.lemon.monitor.enums.MsgEnum;
import javax.annotation.Resource;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 2020/6/29
 *
 * @author: hu_hx
 */
@Service
public class GenRptServiceImpl implements GenRptService {
    private static Logger logger = LoggerFactory.getLogger(GenRptServiceImpl.class);

    @Resource
    private PrimaryDatabaseConfig primaryDatabaseConfig;
    @Resource
    private IDiningReportDao diningReportDao;

    @Value("${report.reportFilePath:}")
    private String reportFilePath;
    @Value("${report.licUrl:}")
    private String licUrl;
    @Value("${report.dataSourceName:}")
    private String dataSourceName;
    private static final String REPORT_EXCEL = "xlsx";
    private static final String REPORT_PDF = "pdf";
    private static final String STR_ONE = "1";

    @Override
    public File genRaqRpt(GenerateReportBO generateReportBO) {
        if (JudgeUtils.isEmpty(generateReportBO.getReportId())){
            BusinessException.throwBusinessException(MsgEnum.EXCEL_EXPORT_FAILURE);
        }
        DiningReportDO diningReport = diningReportDao.get(generateReportBO.getReportId());
//        DiningReportDO diningReport = new DiningReportDO();
//        diningReport.setReportId(generateReportBO.getReportId());
//        diningReport.setRaqPath("C:\\home\\devadm\\report\\raq\\RPX-Z-20012_HBST.raq");
//        diningReport.setReportSts("0");
//        diningReport.setReportFile("?????????????????????");
        if (JudgeUtils.equals(STR_ONE,diningReport.getReportSts())){
            BusinessException.throwBusinessException(MsgEnum.EXCEL_EXPORT_FAILURE);
        }

        if (!new File(diningReport.getRaqPath()).exists()) {
            logger.error( "???????????????????????????:{}",generateReportBO.getReportId());
            BusinessException.throwBusinessException(MsgEnum.EXCEL_EXPORT_FAILURE);
        }

        String reportStyle = generateReportBO.getReportStyle();
        generateReportBO.setRaqPath(diningReport.getRaqPath());
        generateReportBO.setReportStyle(REPORT_EXCEL);
        generateReportBO.setParamArray(generateReportBO.getRaqArgs());
        generateReportBO.setLicUrl(licUrl);
        generateReportBO.setFileSavePath(reportFilePath+diningReport.getReportFile()+"."+generateReportBO.getReportStyle());
        generateReportBO.setDataSourceName(dataSourceName);

        //?????????????????????
        generateReportBO.setDataBaseDriver(primaryDatabaseConfig.getDriverClassName());
        generateReportBO.setDataBaseUrl(primaryDatabaseConfig.getUrl());
        generateReportBO.setDataBaseUserName(primaryDatabaseConfig.getUsername());
        generateReportBO.setUserPassword(primaryDatabaseConfig.getPassword());
        logger.info("???????????????:"+generateReportBO.getDataBaseUrl());
        logger.info("raqArgs???????????????"+ Arrays.asList(generateReportBO.getRaqArgs()));
        if (JudgeUtils.isBlankAny(generateReportBO.getDataBaseDriver(),generateReportBO.getDataBaseUrl(),
                generateReportBO.getDataBaseUserName(),generateReportBO.getUserPassword())){
            logger.error( "?????????????????????:{}",generateReportBO.getRaqPath());
            BusinessException.throwBusinessException(MsgEnum.EXCEL_EXPORT_FAILURE);
        }

        //??????????????????
        boolean result = GenRptUtils.genRaqRpt(generateReportBO);
        if (result){
            File excelFile = new File(generateReportBO.getFileSavePath());
//            if (JudgeUtils.equals(REPORT_PDF,reportStyle)){
//                File pdfFile =new File(reportFilePath+diningReport.getReportFile()+"."+REPORT_PDF);
//                try {
//                    ExcelToPdfUtils.excelToPdf(excelFile,pdfFile);
//                    return pdfFile;
//                } catch (Exception e) {
//                    logger.error("excelToPdf??????:",e);
//                }
//            }
            return excelFile;
        }else {
            BusinessException.throwBusinessException(MsgEnum.EXCEL_EXPORT_FAILURE);
        }
        return null;
    }

    @Override
    public String splitString(List<String> string) {
        String splitString = "";
        if (string.size()>0){
            for (int i = 0;i<string.size();i++){
                splitString = splitString + "'"+string.get(i)+"'";
                if (i!=(string.size()-1)){
                    splitString = splitString+",";
                }
            }
        }
        return splitString;
    }
}

