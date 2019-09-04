/*
 * @ClassName LogType
 * @Description
 * @version 1.0
 * @Date 2019-01-16 16:31:48
 */
package com.cmpay.lemon.monitor.dto;


import com.cmpay.framework.data.response.GenericRspDTO;

/**
 * 项目启动dto
 */
public class ProjectStartRspDTO extends GenericRspDTO {
    private String req_inner_seq;
    private String req_no;
    private String req_nm;
    private String sendTo;
    private String copyTo;
    private String pre_cur_period;
    private String uploadPeriod;
    private DictionaryRspDTO dictionaryRspDTO;

    public ProjectStartRspDTO(String req_inner_seq, String req_no, String req_nm, String sendTo, String copyTo, String pre_cur_period, String uploadPeriod, DictionaryRspDTO dictionaryRspDTO) {
        this.req_inner_seq = req_inner_seq;
        this.req_no = req_no;
        this.req_nm = req_nm;
        this.sendTo = sendTo;
        this.copyTo = copyTo;
        this.pre_cur_period = pre_cur_period;
        this.uploadPeriod = uploadPeriod;
        this.dictionaryRspDTO = dictionaryRspDTO;
    }

    public DictionaryRspDTO getDictionaryRspDTO() {
        return dictionaryRspDTO;
    }

    public void setDictionaryRspDTO(DictionaryRspDTO dictionaryRspDTO) {
        this.dictionaryRspDTO = dictionaryRspDTO;
    }

    public String getReq_inner_seq() {
        return req_inner_seq;
    }

    public void setReq_inner_seq(String req_inner_seq) {
        this.req_inner_seq = req_inner_seq;
    }

    public String getReq_no() {
        return req_no;
    }

    public void setReq_no(String req_no) {
        this.req_no = req_no;
    }

    public String getReq_nm() {
        return req_nm;
    }

    public void setReq_nm(String req_nm) {
        this.req_nm = req_nm;
    }

    public String getSendTo() {
        return sendTo;
    }

    public void setSendTo(String sendTo) {
        this.sendTo = sendTo;
    }

    public String getCopyTo() {
        return copyTo;
    }

    public void setCopyTo(String copyTo) {
        this.copyTo = copyTo;
    }

    public String getPre_cur_period() {
        return pre_cur_period;
    }

    public void setPre_cur_period(String pre_cur_period) {
        this.pre_cur_period = pre_cur_period;
    }

    public String getUploadPeriod() {
        return uploadPeriod;
    }

    public void setUploadPeriod(String uploadPeriod) {
        this.uploadPeriod = uploadPeriod;
    }

    @Override
    public String toString() {
        return "ProjectStartRspDTO{" +
                "req_inner_seq='" + req_inner_seq + '\'' +
                ", req_no='" + req_no + '\'' +
                ", req_nm='" + req_nm + '\'' +
                ", sendTo='" + sendTo + '\'' +
                ", copyTo='" + copyTo + '\'' +
                ", pre_cur_period='" + pre_cur_period + '\'' +
                ", uploadPeriod='" + uploadPeriod + '\'' +
                ", dictionaryRspDTO=" + dictionaryRspDTO +
                '}';
    }

    public ProjectStartRspDTO(String req_inner_seq, String req_no, String req_nm, String sendTo, String copyTo, String pre_cur_period, String uploadPeriod) {
        this.req_inner_seq = req_inner_seq;
        this.req_no = req_no;
        this.req_nm = req_nm;
        this.sendTo = sendTo;
        this.copyTo = copyTo;
        this.pre_cur_period = pre_cur_period;
        this.uploadPeriod = uploadPeriod;
    }

    public ProjectStartRspDTO() {
    }
}