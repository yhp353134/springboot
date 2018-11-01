package com.yu.boot.utils.email.commonEmail;


import java.util.ArrayList;

public class TestEmail {

	public static void main(String[] args) {
		Mail mail = new Mail();
		mail.setHost("smtp.163.com"); // 设置邮件服务器,如果不用163的,自己找找看相关的
		mail.setSender("yhp_develop_test@163.com");  //发件人
		mail.setUsername("yhp_develop_test"); // 登录账号,一般都是和邮箱名一样吧
		mail.setPassword("test123"); // 发件人邮箱的登录密码/ 或者是授权码
		mail.setReceiver("yhp353134@163.com"); // 接收人
		mail.setSubject("账户余额通知");
		mail.setMessage("我发送了，发送次数");
		mail.setCcAddress(new ArrayList<String>() {{
			add("yuhp@belink.com");
		}});
		new EmailUtils().send(mail);
	}
}
