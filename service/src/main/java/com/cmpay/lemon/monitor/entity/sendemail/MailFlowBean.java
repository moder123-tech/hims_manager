package com.cmpay.lemon.monitor.entity.sendemail;

import java.util.Date;

public class MailFlowBean {
    
	private int mail_serial_number;
	private String mail_subject;
	private String mail_sender;
	private String mail_receiver;
	private String mail_annex;
	private String mail_send_content;
	private Date flow_time;
	private String employeeEmail;
    private String emailPassword;
    private String employeeName;
	
	public MailFlowBean() {
	}

	public MailFlowBean(String mailSubject, String mailSender,
                        String mailReceiver, String mailSendContent) {
		super();
		mail_subject = mailSubject;
		mail_sender = mailSender;
		mail_receiver = mailReceiver;
		mail_send_content = mailSendContent;
	}
	
	public MailFlowBean(String mailSubject, String mailSender,
                        String mailReceiver, String mailAnnex, String mailSendContent) {
		super();
		mail_subject = mailSubject;
		mail_sender = mailSender;
		mail_receiver = mailReceiver;
		mail_annex = mailAnnex;
		mail_send_content = mailSendContent;
	}
	
	

	public String getEmployeeName() {
		return employeeName;
	}

	public void setEmployeeName(String employeeName) {
		this.employeeName = employeeName;
	}

	public String getEmployeeEmail() {
		return employeeEmail;
	}

	public void setEmployeeEmail(String employeeEmail) {
		this.employeeEmail = employeeEmail;
	}

	public String getEmailPassword() {
		return emailPassword;
	}

	public void setEmailPassword(String emailPassword) {
		this.emailPassword = emailPassword;
	}

	public Date getFlow_time() {
		return flow_time;
	}

	public void setFlow_time(Date flowTime) {
		flow_time = flowTime;
	}

	public int getMail_serial_number() {
		return mail_serial_number;
	}

	public void setMail_serial_number(int mailSerialNumber) {
		mail_serial_number = mailSerialNumber;
	}

	public String getMail_subject() {
		return mail_subject;
	}

	public void setMail_subject(String mailSubject) {
		mail_subject = mailSubject;
	}

	public String getMail_sender() {
		return mail_sender;
	}

	public void setMail_sender(String mailSender) {
		mail_sender = mailSender;
	}

	public String getMail_receiver() {
		return mail_receiver;
	}

	public void setMail_receiver(String mailReceiver) {
		mail_receiver = mailReceiver;
	}

	public String getMail_annex() {
		return mail_annex;
	}

	public void setMail_annex(String mailAnnex) {
		mail_annex = mailAnnex;
	}

	public String getMail_send_content() {
		return mail_send_content;
	}

	public void setMail_send_content(String mailSendContent) {
		mail_send_content = mailSendContent;
	}
	
	
	
}
