package com.cmpay.lemon.monitor.service.defects;


import com.cmpay.lemon.monitor.bo.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;

public  interface DefectsService {

    ProductionDefectsRspBO findDefectAll(ProductionDefectsBO productionDefectsBO);

    SmokeTestRegistrationRspBO smokeTestFailedQuery(SmokeTestRegistrationBO smokeTestRegistrationBO);

    void getDownload(HttpServletResponse response,ProductionDefectsBO productionDefectsBO);
    void getDownloadTest(HttpServletResponse response,SmokeTestRegistrationBO smokeTestRegistrationBO);
    void downloadZenQuestiont(HttpServletResponse response,ZenQuestiontBO zenQuestiontBO);
    void onlineDefectDownloadt(HttpServletResponse response,OnlineDefectBO onlineDefectBO);
    void internalDefectDownload(HttpServletResponse response,DefectDetailsBO defectDetailsBO);
    void zennDataImport(MultipartFile file);
    ZenQuestiontRspBO zenQuestiontFindList(ZenQuestiontBO zenQuestiontBO);
    OnlineDefectRspBO onlineDefectFindList(OnlineDefectBO onlineDefectBO);
    DefectDetailsRspBO internalDefectInquiry(DefectDetailsBO defectDetailsBO);

    void onlineDefectImport(MultipartFile file);

}
