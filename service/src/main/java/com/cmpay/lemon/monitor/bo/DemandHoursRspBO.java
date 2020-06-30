package com.cmpay.lemon.monitor.bo;


import java.util.ArrayList;
import java.util.List;

/**
 * @author: zhou_xiong
 */
public class DemandHoursRspBO {
    private String sum ;
    private List<String> listSum ;
    private List<String> stringList = new ArrayList<>();

    public List<String> getStringList() {
        return stringList;
    }

    public void setStringList(List<String> stringList) {
        this.stringList = stringList;
    }

    public String getSum() {
        return sum;
    }

    public void setSum(String sum) {
        this.sum = sum;
    }

    public List<String> getListSum() {
        return listSum;
    }

    public void setListSum(List<String> listSum) {
        this.listSum = listSum;
    }

    @Override
    public String toString() {
        return "DemandHoursRspBO{" +
                "sum='" + sum + '\'' +
                ", listSum=" + listSum +
                ", stringList=" + stringList +
                '}';
    }
}