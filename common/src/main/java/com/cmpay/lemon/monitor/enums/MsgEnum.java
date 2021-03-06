package com.cmpay.lemon.monitor.enums;

import com.cmpay.lemon.common.AlertCapable;
import com.cmpay.lemon.common.utils.StringUtils;

/**
 * @author: zhou_xiong
 */
public enum MsgEnum implements AlertCapable {

    /**
     * 成功
     */
    SUCCESS("MON00000", "交易成功"),
    /**
     * 成功
     */
    CUSTOMSUCCESS("MON00000", ""),
    /**
     * 失败
     */
    LOGIN_DATA_BIND_FAILED("MON00001", "登录数据解析失败"),
    /**
     * 失败
     */
    LOGIN_ACCOUNT_OR_PASSWORD_ERROR("MON00002", "账户或密码错误"),
    /**
     * 失败
     */
    LOGIN_SESSION_EXPIRE("MON00003", "session已失效，请重新登录"),
    /**
     * 失败
     */
    LOGIN_FAILED("MON00004", "登录失败"),
    /**
     * 失败
     */
    SUPER_ADMIN_CANNNOT_DELETE("MON00005", "超级管理员不能被删除"),
    /**
     * 失败
     */
    ORGIN_PASSWORD_NOT_CORRECT("MON00006", "原密码错误"),
    /**
     * 失败
     */
    USER_DISABLED("MON00007", "该用户已被禁用,不可登陆"),
    USER_DISABLED2("MON00513", "该用户已被禁用,不可再修改密码，请联系系统管理员"),

    /**
     * 失败
     */
    DB_UPDATE_FAILED("MON00100", "数据更新失败"),
    /**
     * 失败
     */
    DB_DELETE_FAILED("MON00101", "数据删除失败"),
    /**
     * 失败
     */
    DB_INSERT_FAILED("MON00102", "数据新增失败"),
    /**
     * 失败
     */
    DB_FIND_FAILED("MON00103", "数据查询失败"),
    /**
     * 失败
     */
    DB_CANNOT_DELETE("MON00104", "整体项不允许删除"),
    /**
     * 失败
     */
    ERROR_REQ_NO("MON00105", "需求任务提交失败:需求编号有误！"),
    /**
     * 失败
     */
    NON_UNIQUE("MON00106", "需求任务提交失败,已存在需求名或需求编号相同的记录！"),
    /**
     * 失败
     */
    NULL_REMARK("MON00107", "需求任务提交失败:需求状态为取消或暂停时，月初备注信息不能为空！"),
    /**
     * 失败
     */
    ERROR_DEVP("MON00108", "需求任务提交失败:配合部门不能和主导部门相同！"),
    /**
     * 失败
     */
    ERROR_IMPORT("MON00109", "需求任务导入失败:"),
    /**
     * 失败
     */
    ERROR_WORK_IMPORT("MON00110", "工作量导入失败:"),
    /**
     * 失败
     */
    ERROR_CUSTOM("MON00111", ""),
    /**
     * 失败
     */
    ERROR_WORK_UPDATE("MON00112", "工作量更新失败:"),
    /**
     * 失败
     */
    MENU_NAME_CANNOT_NULL("MON00200", "菜单名称不能为空"),
    /**
     * 失败
     */
    PARENT_MENU_CANNOT_NULL("MON00201", "上级菜单不能为空"),
    /**
     * 失败
     */
    PARENT_MENU_MUSTBE_CATALOG("MON00203", "上级菜单只能为目录类型"),
    /**
     * 失败
     */
    PARENT_MENU_MUSTBE_MENU("MON00204", "上级菜单只能为菜单类型"),
    /**
     * 失败
     */
    SYSTEM_MENU_CANNOT_DELETE("MON00205", "系统菜单不允许删除"),
    /**
     * 失败
     */
    DELETE_SUBMENU_OR_BUTTON_FIRST("MON00206", "请先删除子菜单或按钮"),
    /**
     * 失败
     */
    FILE_NAME_NOT_EXIST("MON00304", "文件名不存在"),
    /**
     * 失败
     */
    WRITE_FILE_ERROR("MON00306", "读写时异常"),
    /**
     * 失败
     */
    BATCH_IMPORT_FAILED("MON00307", "批量导入失败,请稍后再试"),
    /**
     * 失败
     */
    SEARCH_FAILED("MON00401", "搜索日志失败,请稍后再试"),
    /**
     * 失败
     */
    REQUESTID_IS_BLANK("MON00402", "日志号为空"),
    /**
     * 失败
     */
    ERROR_SENDT0_ISBLANK("MON00501", "项目启动失败，收件人必填，多个“;”分割!"),
    /**
     * 失败
     */
    ERROR_PLAN_NULL("MON00502", "项目启动失败，找不到该需求对应信息!"),
    /**
     * 失败
     */
    ERROR_REQNO_REQNM_ISBLANK("MON00503", "项目启动失败，需求编号和需求名称不能为空!"),
    /**
     * 失败
     */
    ERROR_NOT_PROJECTMNG("MON00504", "项目启动失败，只有该需求的项目经理和产品经理才能进行项目启动"),
    /**
     * 失败
     */
    ERROR_NOT_FINISHINFO("MON00505", "项目启动失败:人员或时间配置不完善"),
    /**
     * 失败
     */
    ERROR_NOT_SVN("MON00506", "项目启动失败:"),
    /**
     * 失败
     */
    ERROR_NOT_SVNBULID("MON00507", "项目启动失败，SVN项目建立失败："),
    /**
     * 失败
     */
    ERROR_MAIL_FAIL("MON00508", "项目启动失败，SVN项目建立成功，启动邮件发送失败："),
    /**
     * 失败
     */
    ERROR_NOT_PRIVILEGE("MON00509", "无权限使用该功能"),
    /**
     * 失败
     */
    WECHAT_QUERY_FAILED("MON00501", "向企业微信请求失败"),
    /**
     * 导出文件失败
     */
    EXCEL_EXPORT_FAILURE("MON00511", "导出文件失败"),
    /**
     * 失败
     */

    ERROR_FAIL_CHANGE("MON00510", "存量需求转存失败:"),
    /**
     * 失败
     */

    ERROR_FAIL_USER_STATUS("MON00512", "密码错误次数已超限，账号已禁用！解封请联系系统管理员（黄佳海）重置密码！:");

    private String msgCd;
    private String msgInfo;

    MsgEnum(String msgCd, String msgInfo) {
        this.msgCd = msgCd;
        this.msgInfo = msgInfo;
    }

    public void setMsgCd(String msgCd) {
        this.msgCd = msgCd;
    }

    public void setMsgInfo(String msgInfo) {
        this.msgInfo = msgInfo;
    }

    @Override
    public String getMsgCd() {
        return msgCd;
    }

    @Override
    public String getMsgInfo() {
        return msgInfo;
    }

    public static MsgEnum getEnum(String msgCd) {
        for (MsgEnum m : MsgEnum.values()) {
            if (StringUtils.equals( m.msgCd, msgCd )) {
                return m;
            }
        }
        return null;
    }
}
