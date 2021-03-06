package com.cmpay.lemon.monitor.utils;

import com.cmpay.lemon.monitor.bo.AutomatedProductionBO;
import com.cmpay.lemon.monitor.entity.AutomatedProductionRegistrationDO;
import io.restassured.response.Response;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;

import static io.restassured.RestAssured.given;

/**
 * Created by zouxin on 2018/8/24.
 * 存储公共方法
 */
public class BaseUtil {


    /**
     * 时间转换
     */
    private static SimpleDateFormat simpleDateFormat = null;

    public static void main(String args[]) {
        AutomatedProductionBO automatedProductionBO = new AutomatedProductionBO();
        automatedProductionBO.setProPkgName("包名");
        automatedProductionBO.setEnv("0");
        automatedProductionBO.setProNumber("投产编号");
        int code =0;
        int i=0;
        System.err.println(automatedProductionBO.getTest());
        while(true) {
            try {
                Response response = given()
                        .header("Content-Type", "application/json")
                        .header("charset", "utf-8")
                        .body(automatedProductionBO.getTest())
                        .post("https://127.0.0.1:6005/v1/monitoringui/preproduction/callback");
                response.prettyPrint();
                code = response.getStatusCode();
                break;
            } catch (Throwable e) {
                i++;
                if(i>1) {
                    System.err.println("接口调用失败");
                    return;
                }
            }
        }
        System.err.println(code);
        //登记自动化
        AutomatedProductionRegistrationDO automatedProductionRegistrationDO = new AutomatedProductionRegistrationDO();
        automatedProductionRegistrationDO.setCreatTime(LocalDateTime.now());
        automatedProductionRegistrationDO.setEnv(automatedProductionBO.getEnv());
        automatedProductionRegistrationDO.setPronumber(automatedProductionBO.getProNumber());

        if( code ==200){
            System.err.println("成功");
        } else if(code==450){
            //450则包格式错误
            System.err.println("投产包异常");
        }else{
            //其他错误
            System.err.println("其他");
        }
    }

    /**
     * 字符串去重
     *
     * @param compareStr 去重字符串
     * @param splitReg   分隔符
     * @return
     */
    public static String distinctStr(String compareStr, String splitReg) {
        String tmpStr = "";
        for (String str : compareStr.split(splitReg)) {
            if (tmpStr.indexOf(str) < 0) {
                tmpStr += splitReg + str;
            }
        }
        return tmpStr.substring(1, tmpStr.length());
    }


    /**
     * 按照指定格式将其时间转换成字符串
     *
     * @param date      时间
     * @param formatStr 转换格式
     * @return
     */
    public static String transformDateToStr(Date date, String formatStr) {
        simpleDateFormat = new SimpleDateFormat(formatStr);
        return simpleDateFormat.format(date);
    }

    /**
     * 按照指定格式将其当前时间转换成字符串
     * 默认格式转换
     *
     * @return
     */
    public static String transformDateToStr() {
        simpleDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return simpleDateFormat.format(new Date());
    }

    public static String transformDateToStr(String formatStr) {
        simpleDateFormat = new SimpleDateFormat(formatStr);
        return simpleDateFormat.format(new Date());
    }


    /**
     * 字符串转化成Date类型
     * 默认支持格式为yyyy-MM-dd
     */
    public static Date transformStrToDate(String str) {
        Date date = null;
        simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            date = simpleDateFormat.parse(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }

    public static Date transformStrToDate(String str, String formatStr) {
        Date date = null;
        simpleDateFormat = new SimpleDateFormat(formatStr);
        try {
            date = simpleDateFormat.parse(str);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return date;
    }
}