/*
 * @ClassName DemandEaseDevelopmentDO
 * @Description
 * @version 1.0
 * @Date 2020-08-31 14:33:51
 */
package com.cmpay.lemon.monitor.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.cmpay.framework.data.BaseDO;
import com.cmpay.lemon.framework.annotation.DataObject;

@DataObject
public class DemandEaseDevelopmentDO extends BaseDO {
    @Excel(name = "实施月份")
    private String reqImplMon;
    @Excel(name = "一级主导团队")
    private String firstLevelOrganization;
    @Excel(name = "产品管理部门")
    private String productmanagementdepartment;
    /**
     * @Fields costdepartment 成本管理部门
     */
    @Excel(name = "成本管理部门")
    private String costdepartment;
    /**
     * @Fields secondlevelorganization 二级主导团队
     */

    /**
     * @Fields documentnumber 文号
     */
    @Excel(name = "文号")
    private String documentnumber;
    /**
     * @Fields processstartdate 流程开始日期
     */
    @Excel(name = "流程开始日期")
    private String processstartdate;
    /**
     * @Fields developmentowner 开发负责人
     */
    @Excel(name = "开发负责人")
    private String developmentowner;
    /**
     * @Fields supportingmanufacturers 支撑厂家
     */
    @Excel(name = "支撑厂家")
    private String supportingmanufacturers;
    /**
     * @Fields supportingmanufacturerproducts 支撑厂家产品
     */
    @Excel(name = "支撑厂家产品")
    private String supportingmanufacturerproducts;
    /**
     * @Fields cuttype 裁剪类型
     */
    @Excel(name = "裁剪类型")
    private String cuttype;
    /**
     * @Fields demandtheme 需求主题
     */
    @Excel(name = "需求主题")
    private String demandtheme;
    /**
     * @Fields requirementdescription 需求描述
     */
    @Excel(name = "需求描述")
    private String requirementdescription;
    /**
     * @Fields developmentworkloadassess 开发工作量评估
     */
    @Excel(name = "开发工作量评估")
    private String developmentworkloadassess;
    /**
     * @Fields developmentworkload 开发工作量
     */
    @Excel(name = "开发工作量")
    private String developmentworkload;
    /**
     * @Fields commissioningdate 投产日期
     */
    @Excel(name = "投产日期")
    private String commissioningdate;
    /**
     * @Fields acceptor 验收人
     */
    @Excel(name = "验收人")
    private String acceptor;
    /**
     * @Fields acceptancedate 验收日期
     */
    @Excel(name = "验收日期")
    private String acceptancedate;

    /**
     * @Fields remark 备注
     */
    @Excel(name = "备注")
    private String remark;
    private String startTime;
    private String endTime;
    private String secondlevelorganization;
    private String startTime2;
    private String endTime2;

    public String getStartTime2() {
        return startTime2;
    }

    public void setStartTime2(String startTime2) {
        this.startTime2 = startTime2;
    }

    public String getEndTime2() {
        return endTime2;
    }

    public void setEndTime2(String endTime2) {
        this.endTime2 = endTime2;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getReqImplMon() {
        return reqImplMon;
    }

    public void setReqImplMon(String reqImplMon) {
        this.reqImplMon = reqImplMon;
    }

    public String getFirstLevelOrganization() {
        return firstLevelOrganization;
    }

    public void setFirstLevelOrganization(String firstLevelOrganization) {
        this.firstLevelOrganization = firstLevelOrganization;
    }

    public String getDocumentnumber() {
        return documentnumber;
    }

    public void setDocumentnumber(String documentnumber) {
        this.documentnumber = documentnumber;
    }

    public String getProcessstartdate() {
        return processstartdate;
    }

    public void setProcessstartdate(String processstartdate) {
        this.processstartdate = processstartdate;
    }

    public String getDevelopmentowner() {
        return developmentowner;
    }

    public void setDevelopmentowner(String developmentowner) {
        this.developmentowner = developmentowner;
    }

    public String getSupportingmanufacturers() {
        return supportingmanufacturers;
    }

    public void setSupportingmanufacturers(String supportingmanufacturers) {
        this.supportingmanufacturers = supportingmanufacturers;
    }

    public String getSupportingmanufacturerproducts() {
        return supportingmanufacturerproducts;
    }

    public void setSupportingmanufacturerproducts(String supportingmanufacturerproducts) {
        this.supportingmanufacturerproducts = supportingmanufacturerproducts;
    }

    public String getCuttype() {
        return cuttype;
    }

    public void setCuttype(String cuttype) {
        this.cuttype = cuttype;
    }

    public String getDemandtheme() {
        return demandtheme;
    }

    public void setDemandtheme(String demandtheme) {
        this.demandtheme = demandtheme;
    }

    public String getRequirementdescription() {
        return requirementdescription;
    }

    public void setRequirementdescription(String requirementdescription) {
        this.requirementdescription = requirementdescription;
    }

    public String getCommissioningdate() {
        return commissioningdate;
    }

    public void setCommissioningdate(String commissioningdate) {
        this.commissioningdate = commissioningdate;
    }

    public String getAcceptancedate() {
        return acceptancedate;
    }

    public void setAcceptancedate(String acceptancedate) {
        this.acceptancedate = acceptancedate;
    }

    public String getAcceptor() {
        return acceptor;
    }

    public void setAcceptor(String acceptor) {
        this.acceptor = acceptor;
    }

    public String getDevelopmentworkloadassess() {
        return developmentworkloadassess;
    }

    public void setDevelopmentworkloadassess(String developmentworkloadassess) {
        this.developmentworkloadassess = developmentworkloadassess;
    }

    public String getDevelopmentworkload() {
        return developmentworkload;
    }

    public void setDevelopmentworkload(String developmentworkload) {
        this.developmentworkload = developmentworkload;
    }

    public String getCostdepartment() {
        return costdepartment;
    }

    public void setCostdepartment(String costdepartment) {
        this.costdepartment = costdepartment;
    }

    public String getSecondlevelorganization() {
        return secondlevelorganization;
    }

    public void setSecondlevelorganization(String secondlevelorganization) {
        this.secondlevelorganization = secondlevelorganization;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getProductmanagementdepartment() {
        return productmanagementdepartment;
    }

    public void setProductmanagementdepartment(String productmanagementdepartment) {
        this.productmanagementdepartment = productmanagementdepartment;
    }

    @Override
    public String toString() {
        return "DemandEaseDevelopmentDO{" +
                "reqImplMon='" + reqImplMon + '\'' +
                ", firstLevelOrganization='" + firstLevelOrganization + '\'' +
                ", productmanagementdepartment='" + productmanagementdepartment + '\'' +
                ", costdepartment='" + costdepartment + '\'' +
                ", documentnumber='" + documentnumber + '\'' +
                ", processstartdate='" + processstartdate + '\'' +
                ", developmentowner='" + developmentowner + '\'' +
                ", supportingmanufacturers='" + supportingmanufacturers + '\'' +
                ", supportingmanufacturerproducts='" + supportingmanufacturerproducts + '\'' +
                ", cuttype='" + cuttype + '\'' +
                ", demandtheme='" + demandtheme + '\'' +
                ", requirementdescription='" + requirementdescription + '\'' +
                ", commissioningdate='" + commissioningdate + '\'' +
                ", acceptancedate='" + acceptancedate + '\'' +
                ", acceptor='" + acceptor + '\'' +
                ", developmentworkloadassess='" + developmentworkloadassess + '\'' +
                ", developmentworkload='" + developmentworkload + '\'' +
                ", remark='" + remark + '\'' +
                ", startTime='" + startTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", secondlevelorganization='" + secondlevelorganization + '\'' +
                '}';
    }
}
