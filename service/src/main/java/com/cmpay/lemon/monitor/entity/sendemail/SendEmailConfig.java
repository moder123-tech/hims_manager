package com.cmpay.lemon.monitor.entity.sendemail;
import com.cmpay.lemon.common.Env;
import com.cmpay.lemon.common.exception.BusinessException;
import com.cmpay.lemon.framework.utils.LemonUtils;
import com.cmpay.lemon.monitor.enums.MsgEnum;

import java.util.Properties;

/**
 * description：
 * @author wang_hui
 * @date 2017年5月8日
 */
public class SendEmailConfig {
	private String env;
	private  static final String DEV="dev";
	//todo 先改成发给自己
	private String devMail="wu_lr@hisuntech.com";
	//private String devMail="wu_lr@hisuntech.com";
	public static final String SEND_MAIL = "sendmail.properties";
	/**
	 *
	 * 类名：读取发送邮件sendmail.properties
	 * 描述：getParamValue
	 * @author 方小勇
	 * @date 2018年2月2日 下午2:17:40
	 */
	public static String getParamValue(String paramKey){
		Properties prop = new Properties();
		 try{
		     prop.load(SendEmailConfig.class.getClassLoader().getResourceAsStream(SEND_MAIL));
		     System.out.println(prop.getProperty(paramKey));
		 }
		 catch(Exception e){
		     System.out.println(e);
		 }
	    return  prop.getProperty(paramKey);
	}

	public SendEmailConfig(){
		//以前是从配置文件获取,重构没有引入配置文件
		if(LemonUtils.getEnv().equals(Env.SIT)) {
			this.env= "PRO";
		} else if(LemonUtils.getEnv().equals(Env.DEV)) {
			this.env= DEV;
		}else {
			MsgEnum.ERROR_CUSTOM.setMsgInfo("");
			MsgEnum.ERROR_CUSTOM.setMsgInfo("当前配置环境路径有误，请尽快联系管理员!");
			BusinessException.throwBusinessException(MsgEnum.ERROR_CUSTOM);
		}


		/*Properties p;
		try {
			p = PropertiesUtils.loadProperties("set.properties");
			this.env = p.getProperty("mail_env");
		} catch (IOException e) {
			e.printStackTrace();
		}*/
	};
	/**
	 *
	 * description : 正常投产发送   （含不做预投产邮件发送者）
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @param isPre 是否预投产
	 * @return
	 */
	public String getNormalMailTo(boolean isPre){
		if(env.equals(DEV)) {
			return devMail;
		}else if(env.equals("PRO")){
			if(isPre) {
				return "version_it@hisuntech.com;huang_jh@hisuntech.com";
			}
			else {
				String paramValue = "version_it@hisuntech.com;huang_jh@hisuntech.com;wujinyan@hisuntech.com;xiao_hua@hisuntech.com;dong_jm@hisuntech.com";
				return paramValue;//预投产不做验证
			}
		}
		return "";
		/*if(env.equals("DEV")) {
			return devMail;
		}else if(env.equals("PRO")){
			if(isPre)
				return "version_it@hisuntech.com;huang_jh@hisuntech.com";//预投产做验证
			//预投产不做验证
			else return "version_it@hisuntech.com;huang_jh@hisuntech.com;wujinyan@hisuntech.com;xiao_hua@hisuntech.com;dong_jm@hisuntech.com";
		}
		return "";*/
	}
	/**
	 *
	 * description : 正常投产抄送  --没有用到！！！
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @return
	 */
	public String getNormalMailCopy(){
		return "";
	}

	/**
	 *
	 * description : 非正常投产发送
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @param isPre 是否预投产
	 * @return
	 */
	public String getAbnormalMailTo(boolean isPre){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			if(isPre) {return "version_it@hisuntech.com;huang_jh@hisuntech.com";}
			else {return "version_it@hisuntech.com;huang_jh@hisuntech.com;dong_jm@hisuntech.com";}
		}
		return "";
	}

	/**
	 *
	 * description : 非正常投产抄送
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @return
	 */
	public String getAbnormalMailCopy(){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			return "PR_GMO@hisuntech.com;deng_shj@hisuntech.com;li_zhj@hisuntech.com;li_xn@hisuntech.com;version_it@hisuntech.com;PE_PLAT@hisuntech.com";
		}
		return "";
	}

	/**
	 *
	 * description : 救火更新发送
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @param isPre 是否预投产
	 * @return
	 */
	public String getFireMailTo(boolean isPre){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			if(isPre) {
				return "version_it@hisuntech.com;huang_jh@hisuntech.com";
			}
			else {
				return "version_it@hisuntech.com;huang_jh@hisuntech.com;dong_jm@hisuntech.com";
			}
		}
		return "";
	}

	/**
	 *
	 * description : 救火更新抄送
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @param
	 * @return
	 */
	public String getFireMailCopy(){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			return "PR_GMO@hisuntech.com;deng_shj@hisuntech.com;li_zhj@hisuntech.com;li_xn@hisuntech.com;version_it@hisuntech.com;PE_PLAT@hisuntech.com";
		}
		return "";
	}

	/**
	 *
	 * description : 系统操作发送
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @return
	 */
	public String getExcuteMailTo(){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			return "version_it@hisuntech.com;huang_jh@hisuntech.com;deng_shj@hisuntech.com";
		}
		return "";
	}

	/**
	 *
	 * description : 系统操作抄送
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @return
	 */
	public String getExcuteMailCopy(){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			return "PR_GMO@hisuntech.com;li_zhj@hisuntech.com;li_xn@hisuntech.com;version_it@hisuntech.com;PE_PLAT@hisuntech.com";
		}
		return "";
	}

	/**
	 *
	 * description : DBA人员
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @return
	 */
	public String getDbaMailTo(){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			return "xiong_hl@hisuntech.com;liuxiangquan@hisuntech.com";
		}
		return "";
	}

	/**
	 *
	 * description : SQL审批人员
	 * @author wang_hui
	 * 2017年5月8日
	 *
	 * @return
	 */
	public String getSqlMailTo(){
		if(env.equals(DEV)){
			return devMail;
		}else if(env.equals("PRO")){
			return "deng_shj@hisuntech.com;wujinyan@hisuntech.com;xiao_hua@hisuntech.com";
		}
		return "";
	}


    public String[] getErroCodeMailTo()
    {
        if(env.equals(DEV)){
			return (new String[] {
					devMail
			});
		}else if(env.equals("PRO")){
			return (new String[] {
					"dong_jm@hisuntech.com", "wujinyan@hisuntech.com", "xiao_hua@hisuntech.com", "liu_dm@hisuntech.com", "liujia3@hisuntech.com", "liu_sch@hisuntech.com"
			});
		}
		return null;

    }

}
