package com.cmpay.lemon.monitor.bo;

import com.cmpay.lemon.framework.page.PageInfo;

import java.util.ArrayList;
import java.util.List;

public class ErcdmgErrorComditionRspBO {
    List<ErcdmgErrorComditionBO> ercdmgErrorComditionBOList;
    PageInfo<ErcdmgErrorComditionBO> pageInfo;

    List<ErcdmgUpdmgnBO> ercdmgUpdmgnBOList;
    PageInfo<ErcdmgUpdmgnBO> updmgnBOPageInfo;
    //人员信息
    List<ErcdmgPordUserBO> ercdmgPordUserDTOList;

    public List<ErcdmgErrorComditionBO> getErcdmgErrorComditionBOList() {
        return ercdmgErrorComditionBOList;
    }

    public void setErcdmgErrorComditionBOList(List<ErcdmgErrorComditionBO> ercdmgErrorComditionBOList) {
        this.ercdmgErrorComditionBOList = ercdmgErrorComditionBOList;
    }

    public PageInfo<ErcdmgErrorComditionBO> getPageInfo() {
        return pageInfo;
    }

    public void setPageInfo(PageInfo<ErcdmgErrorComditionBO> pageInfo) {
        this.pageInfo = pageInfo;
    }

    public List<ErcdmgPordUserBO> getErcdmgPordUserDTOList() {
        return ercdmgPordUserDTOList;
    }

    public void setErcdmgPordUserDTOList(List<ErcdmgPordUserBO> ercdmgPordUserDTOList) {
        this.ercdmgPordUserDTOList = ercdmgPordUserDTOList;
    }

    public List<ErcdmgUpdmgnBO> getErcdmgUpdmgnBOList() {
        return ercdmgUpdmgnBOList;
    }

    public void setErcdmgUpdmgnBOList(List<ErcdmgUpdmgnBO> ercdmgUpdmgnBOList) {
        this.ercdmgUpdmgnBOList = ercdmgUpdmgnBOList;
    }

    public PageInfo<ErcdmgUpdmgnBO> getUpdmgnBOPageInfo() {
        return updmgnBOPageInfo;
    }

    public void setUpdmgnBOPageInfo(PageInfo<ErcdmgUpdmgnBO> updmgnBOPageInfo) {
        this.updmgnBOPageInfo = updmgnBOPageInfo;
    }

    @Override
    public String toString() {
        return "ErcdmgErrorComditionRspBO{" +
                "ercdmgErrorComditionBOList=" + ercdmgErrorComditionBOList +
                ", pageInfo=" + pageInfo +
                ", ercdmgUpdmgnBOList=" + ercdmgUpdmgnBOList +
                ", updmgnBOPageInfo=" + updmgnBOPageInfo +
                ", ercdmgPordUserDTOList=" + ercdmgPordUserDTOList +
                '}';
    }
}
