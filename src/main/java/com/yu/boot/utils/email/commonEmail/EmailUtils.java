package com.yu.boot.utils.email.commonEmail;


import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

public class EmailUtils {

	/**
	 * 日志输出
	 **/
	Logger log = LoggerFactory.getLogger(getClass());

	public boolean send(Mail mail) {
		log.info("发送邮件开始，请求参数为={}", mail);
		// 发送email
		HtmlEmail email = new HtmlEmail();
		try {
			// 163的如下："smtp.163.com"
			email.setHostName(mail.getHost());
			// 字符编码集的设置
			email.setCharset(Mail.ENCODEING);
			// 收件人的邮箱
			email.addTo(mail.getReceiver());
			// 抄送人
			List<String> ccList = mail.getCcAddress();
			if (null != ccList && 0 < ccList.size()) {
				for (int i = 0; i < ccList.size(); i++) {
					email.addCc(ccList.get(i));
				}
			}
			// 发送人的邮箱
			email.setFrom(mail.getSender(), mail.getName());
			// 如果需要认证信息的话，设置认证：用户名-密码。分别为发件人在邮件服务器上的注册名称和密码
			email.setAuthentication(mail.getUsername(), mail.getPassword());
			// 要发送的邮件主题
			email.setSubject(mail.getSubject());
			// 要发送的信息，由于使用了HtmlEmail，可以在邮件内容中使用HTML标签
			email.setMsg(mail.getMessage());
			// 发送
			email.send();
			if (log.isDebugEnabled()) {
				log.info("邮件发送结束,发送人={}   接收人={}  抄送人={}  已经发送成功", mail.getSender(), mail.getReceiver(), mail.getCcAddress());
			}
			return true;
		} catch (EmailException e) {
			e.printStackTrace();
			log.info("发送人={}  接收人={}  抄送人={}  发送异常", mail.getSender(), mail.getReceiver(), mail.getCcAddress(), e);
		}
		return false;
	}

}
