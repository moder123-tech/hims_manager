/*
 * @ClassName WorkingHoursDO
 * @Description 
 * @version 1.0
 * @Date 2020-04-30 15:59:10
 */
package com.cmpay.lemon.monitor.entity;

import com.cmpay.framework.data.BaseDO;
import com.cmpay.lemon.framework.annotation.DataObject;

@DataObject
public class WorkingHoursDO extends BaseDO {
    /**
     * @Fields jiraworklogkey 关联jira日志主键ID
     */
    private String jiraworklogkey;

    public String getSelectTime() {
        return selectTime;
    }

    public void setSelectTime(String selectTime) {
        this.selectTime = selectTime;
    }

    /**
     * @Fields issuekey 子任务key
     */
    private String issuekey;
    /**
     * @Fields timespnet 用时
     */
    private String timespnet;
    /**
     * @Fields subtaskname 子任务名称
     */
    private String subtaskname;
    /**
     * @Fields assignmentDepartment 任务归属部门
     */
    private String assignmentDepartment;
    /**
     * @Fields name 操作人name
     */
    private String name;
    /**
     * @Fields displayname 操作人中文名
     */
    private String displayname;
    /**
     * @Fields devpLeadDept 员工归属部门
     */
    private String devpLeadDept;
    /**
     * @Fields comment 工作备注
     */
    private String comment;
    /**
     * @Fields createdtime 录入时间
     */
    private String createdtime;
    /**
     * @Fields startedtime 日志开始时间
     */
    private String startedtime;
    /**
     * @Fields updatedtime 修改日志时间
     */
    private String updatedtime;
    // 总公时
    private String sumTime;
    private String selectTime;

    public String getJiraworklogkey() {
        return jiraworklogkey;
    }

    public void setJiraworklogkey(String jiraworklogkey) {
        this.jiraworklogkey = jiraworklogkey;
    }

    public String getIssuekey() {
        return issuekey;
    }

    public void setIssuekey(String issuekey) {
        this.issuekey = issuekey;
    }

    public String getTimespnet() {
        return timespnet;
    }

    public void setTimespnet(String timespnet) {
        this.timespnet = timespnet;
    }

    public String getSubtaskname() {
        return subtaskname;
    }

    public void setSubtaskname(String subtaskname) {
        this.subtaskname = subtaskname;
    }

    public String getAssignmentDepartment() {
        return assignmentDepartment;
    }

    public void setAssignmentDepartment(String assignmentDepartment) {
        this.assignmentDepartment = assignmentDepartment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayname() {
        return displayname;
    }

    public void setDisplayname(String displayname) {
        this.displayname = displayname;
    }

    public String getDevpLeadDept() {
        return devpLeadDept;
    }

    public void setDevpLeadDept(String devpLeadDept) {
        this.devpLeadDept = devpLeadDept;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getCreatedtime() {
        return createdtime;
    }

    public void setCreatedtime(String createdtime) {
        this.createdtime = createdtime;
    }

    public String getStartedtime() {
        return startedtime;
    }

    public void setStartedtime(String startedtime) {
        this.startedtime = startedtime;
    }

    public String getUpdatedtime() {
        return updatedtime;
    }

    public void setUpdatedtime(String updatedtime) {
        this.updatedtime = updatedtime;
    }

    public String getSumTime() {
        return sumTime;
    }

    public void setSumTime(String sumTime) {
        this.sumTime = sumTime;
    }
}