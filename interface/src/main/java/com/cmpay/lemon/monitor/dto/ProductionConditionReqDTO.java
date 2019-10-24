package com.cmpay.lemon.monitor.dto;

import com.cmpay.framework.data.response.PageableRspDTO;

import java.sql.Date;

/**
 * @author: zhou_xiong
 */
public class ProductionConditionReqDTO extends PageableRspDTO {
    private String proNumber;
    private String proNeed;
    private String proType;
    private Date proDate;
    private Date proDateStart;
    private Date proDateEnd;
    private String applicationDept;
    private String proApplicant;
    private String applicantTel;
    private String proModule;
    private String businessPrincipal;
    private String basePrincipal;
    private String proManager;
    private String proStatus;
    private String isUpDatabase;
    private String isUpStructure;
    private String proOperation;
    private String isRefCerificate;
    private String isAdvanceProduction;
    private String notAdvanceReason;
    private String proAdvanceResult;
    private String notProductionImpact;
    private String identifier;
    private String identifierTel;
    private String proChecker;
    private String checkerTel;
    private String validation;
    private String developmentLeader;
    private String approver;
    private String updateOperator;
    private String remark;
    private String unusualReasonPhrase;
    private String urgentReasonPhrase;
    private String productionDeploymentResult;
    private String isOperationProduction;
    /*
     * 紧急更新
     */
    private String completionUpdate ;
    private String earlyImplementation;
    private String influenceUse ;
    private String influenceUseReason;
    private String influenceUseInf;
    private String operatingTime;

    private String proOperator;
    private String operationType;
    /*
     * 审核邮件
     */
    private String mailRecipient;
    private String mailCopyPerson;
    /*
     * 部门关系
     */
    private String deptName;
    private String deptManagerName;
    private String developmentDept;

    private String proPkgStatus;
    /**
     * 页数
     */
    private int pageNum;
    /**
     * 页面大小
     */
    private int pageSize;

    @Override
    public int getPageNum() {
        return pageNum;
    }

    @Override
    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    @Override
    public int getPageSize() {
        return pageSize;
    }

    @Override
    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public String getProNumber() {
        return proNumber;
    }

    public void setProNumber(String proNumber) {
        this.proNumber = proNumber;
    }

    public String getProNeed() {
        return proNeed;
    }

    public void setProNeed(String proNeed) {
        this.proNeed = proNeed;
    }

    public String getProType() {
        return proType;
    }

    public void setProType(String proType) {
        this.proType = proType;
    }

    public Date getProDate() {
        return proDate;
    }

    public void setProDate(Date proDate) {
        this.proDate = proDate;
    }

    public Date getProDateStart() {
        return proDateStart;
    }

    public void setProDateStart(Date proDateStart) {
        this.proDateStart = proDateStart;
    }

    public Date getProDateEnd() {
        return proDateEnd;
    }

    public void setProDateEnd(Date proDateEnd) {
        this.proDateEnd = proDateEnd;
    }

    public String getApplicationDept() {
        return applicationDept;
    }

    public void setApplicationDept(String applicationDept) {
        this.applicationDept = applicationDept;
    }

    public String getProApplicant() {
        return proApplicant;
    }

    public void setProApplicant(String proApplicant) {
        this.proApplicant = proApplicant;
    }

    public String getApplicantTel() {
        return applicantTel;
    }

    public void setApplicantTel(String applicantTel) {
        this.applicantTel = applicantTel;
    }

    public String getProModule() {
        return proModule;
    }

    public void setProModule(String proModule) {
        this.proModule = proModule;
    }

    public String getBusinessPrincipal() {
        return businessPrincipal;
    }

    public void setBusinessPrincipal(String businessPrincipal) {
        this.businessPrincipal = businessPrincipal;
    }

    public String getBasePrincipal() {
        return basePrincipal;
    }

    public void setBasePrincipal(String basePrincipal) {
        this.basePrincipal = basePrincipal;
    }

    public String getProManager() {
        return proManager;
    }

    public void setProManager(String proManager) {
        this.proManager = proManager;
    }

    public String getProStatus() {
        return proStatus;
    }

    public void setProStatus(String proStatus) {
        this.proStatus = proStatus;
    }

    public String getIsUpDatabase() {
        return isUpDatabase;
    }

    public void setIsUpDatabase(String isUpDatabase) {
        this.isUpDatabase = isUpDatabase;
    }

    public String getIsUpStructure() {
        return isUpStructure;
    }

    public void setIsUpStructure(String isUpStructure) {
        this.isUpStructure = isUpStructure;
    }

    public String getProOperation() {
        return proOperation;
    }

    public void setProOperation(String proOperation) {
        this.proOperation = proOperation;
    }

    public String getIsRefCerificate() {
        return isRefCerificate;
    }

    public void setIsRefCerificate(String isRefCerificate) {
        this.isRefCerificate = isRefCerificate;
    }

    public String getIsAdvanceProduction() {
        return isAdvanceProduction;
    }

    public void setIsAdvanceProduction(String isAdvanceProduction) {
        this.isAdvanceProduction = isAdvanceProduction;
    }

    public String getNotAdvanceReason() {
        return notAdvanceReason;
    }

    public void setNotAdvanceReason(String notAdvanceReason) {
        this.notAdvanceReason = notAdvanceReason;
    }

    public String getProAdvanceResult() {
        return proAdvanceResult;
    }

    public void setProAdvanceResult(String proAdvanceResult) {
        this.proAdvanceResult = proAdvanceResult;
    }

    public String getNotProductionImpact() {
        return notProductionImpact;
    }

    public void setNotProductionImpact(String notProductionImpact) {
        this.notProductionImpact = notProductionImpact;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getIdentifierTel() {
        return identifierTel;
    }

    public void setIdentifierTel(String identifierTel) {
        this.identifierTel = identifierTel;
    }

    public String getProChecker() {
        return proChecker;
    }

    public void setProChecker(String proChecker) {
        this.proChecker = proChecker;
    }

    public String getCheckerTel() {
        return checkerTel;
    }

    public void setCheckerTel(String checkerTel) {
        this.checkerTel = checkerTel;
    }

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

    public String getDevelopmentLeader() {
        return developmentLeader;
    }

    public void setDevelopmentLeader(String developmentLeader) {
        this.developmentLeader = developmentLeader;
    }

    public String getApprover() {
        return approver;
    }

    public void setApprover(String approver) {
        this.approver = approver;
    }

    public String getUpdateOperator() {
        return updateOperator;
    }

    public void setUpdateOperator(String updateOperator) {
        this.updateOperator = updateOperator;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getUnusualReasonPhrase() {
        return unusualReasonPhrase;
    }

    public void setUnusualReasonPhrase(String unusualReasonPhrase) {
        this.unusualReasonPhrase = unusualReasonPhrase;
    }

    public String getUrgentReasonPhrase() {
        return urgentReasonPhrase;
    }

    public void setUrgentReasonPhrase(String urgentReasonPhrase) {
        this.urgentReasonPhrase = urgentReasonPhrase;
    }

    public String getProductionDeploymentResult() {
        return productionDeploymentResult;
    }

    public void setProductionDeploymentResult(String productionDeploymentResult) {
        this.productionDeploymentResult = productionDeploymentResult;
    }

    public String getIsOperationProduction() {
        return isOperationProduction;
    }

    public void setIsOperationProduction(String isOperationProduction) {
        this.isOperationProduction = isOperationProduction;
    }

    public String getCompletionUpdate() {
        return completionUpdate;
    }

    public void setCompletionUpdate(String completionUpdate) {
        this.completionUpdate = completionUpdate;
    }

    public String getEarlyImplementation() {
        return earlyImplementation;
    }

    public void setEarlyImplementation(String earlyImplementation) {
        this.earlyImplementation = earlyImplementation;
    }

    public String getInfluenceUse() {
        return influenceUse;
    }

    public void setInfluenceUse(String influenceUse) {
        this.influenceUse = influenceUse;
    }

    public String getInfluenceUseReason() {
        return influenceUseReason;
    }

    public void setInfluenceUseReason(String influenceUseReason) {
        this.influenceUseReason = influenceUseReason;
    }

    public String getInfluenceUseInf() {
        return influenceUseInf;
    }

    public void setInfluenceUseInf(String influenceUseInf) {
        this.influenceUseInf = influenceUseInf;
    }

    public String getOperatingTime() {
        return operatingTime;
    }

    public void setOperatingTime(String operatingTime) {
        this.operatingTime = operatingTime;
    }

    public String getProOperator() {
        return proOperator;
    }

    public void setProOperator(String proOperator) {
        this.proOperator = proOperator;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getMailRecipient() {
        return mailRecipient;
    }

    public void setMailRecipient(String mailRecipient) {
        this.mailRecipient = mailRecipient;
    }

    public String getMailCopyPerson() {
        return mailCopyPerson;
    }

    public void setMailCopyPerson(String mailCopyPerson) {
        this.mailCopyPerson = mailCopyPerson;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
    }

    public String getDeptManagerName() {
        return deptManagerName;
    }

    public void setDeptManagerName(String deptManagerName) {
        this.deptManagerName = deptManagerName;
    }

    public String getDevelopmentDept() {
        return developmentDept;
    }

    public void setDevelopmentDept(String developmentDept) {
        this.developmentDept = developmentDept;
    }

    public String getProPkgStatus() {
        return proPkgStatus;
    }

    public void setProPkgStatus(String proPkgStatus) {
        this.proPkgStatus = proPkgStatus;
    }

}