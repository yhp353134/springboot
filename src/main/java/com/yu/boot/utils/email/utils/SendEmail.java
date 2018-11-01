package com.yu.boot.utils.email.utils;

import com.alibaba.fastjson.JSONObject;
import com.yu.boot.utils.email.commonEmail.EmailUtils;
import com.yu.boot.utils.email.commonEmail.Mail;
import com.yu.boot.utils.email.utils.java.SendMail;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @Description: 发送邮件
 * @Author: yhp
 * @Date: 2018/11/1
 */
public class SendEmail {

	/**
	 * 日志输出
	 **/
	static Logger log = LoggerFactory.getLogger(SendEmail.class);

	/**
	 * 测试，参数名称不可以修改
	 */
	public static void main(String[] args) {
		// 发送邮件的参数，调用在SendEmailNewService里面，提供成了公共的服务
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("host", "smtp.163.com"); // 协议 163邮箱的协议
		jsonObject.put("sender", "yhp_develop_test@163.com"); // 发送人
		jsonObject.put("userName", "test"); // 邮箱账户名，如果没有 就是跟邮箱一样
		jsonObject.put("password", "test123"); // 发送人密码，若是开通授权码，则填写授权码
		jsonObject.put("receiver", "cc@163.com"); // 接受人
		jsonObject.put("subject", "账户余额不足通知");
		jsonObject.put("content", "邮件测试内容");
		jsonObject.put("cList", "aa@belink.com|bb@qq.com"); // 抄送人
		jsonObject.put("fileList", null); // 竖线分开
		jsonObject.put("type", null); // 默认不传，若需要传附件，就需要传value等于mail // jsonObject.put("type", "mail");
		sendCommonEmail(jsonObject); // common.email方式发送
		sendJavaEmail(jsonObject); // java.mail方式发送
	}

	/**
	 * 使用commons-email发送邮件，但是此方式不支持多媒体的发送
	 **/
	public static Boolean sendCommonEmail(JSONObject jsonObject) {
		try {
			Mail mail = new Mail();
			mail.setHost(MapUtils.getString(jsonObject, "host")); //协议
			mail.setSender(MapUtils.getString(jsonObject, "sender"));  //发件人
			mail.setUsername(MapUtils.getString(jsonObject, "userName")); // 登录账号,一般都是和邮箱名一样吧
			mail.setPassword(MapUtils.getString(jsonObject, "password")); // 发件人邮箱的登录密码或者授权码
			mail.setReceiver(MapUtils.getString(jsonObject, "receiver")); // 接收人
			mail.setSubject(MapUtils.getString(jsonObject, "subject")); // 主题
			mail.setMessage(MapUtils.getString(jsonObject, "content")); // 发送内容
			String cList = MapUtils.getString(jsonObject, "cList"); //抄送人，多个用竖线分割
			if (StringUtils.isNotBlank(cList)) {
				String[] ccList = cList.split("\\|");
				List<String> ccListArr = new ArrayList<>();
				for (int z = 0; z < ccList.length; z++) {
					ccListArr.add(ccList[z]);
				}
				mail.setCcAddress(ccListArr);
			}
			return new EmailUtils().send(mail);
		} catch (Exception e) {
			log.error("发送邮件异常", e);
		}
		return false;
	}

	/**
	 * 使用java.email发送邮件，支持附件的发送
	 **/
	public static Boolean sendJavaEmail(JSONObject jsonObject) {
		try {
			log.info("发送邮件开始，请求参数为={}", jsonObject);
			String host = MapUtils.getString(jsonObject, "host");
			String userName = MapUtils.getString(jsonObject, "sender");
			String passwrd = MapUtils.getString(jsonObject, "password");
			// 邮件主题
			String title = MapUtils.getString(jsonObject, "subject");
			// 邮件正文
			String htmlContent = MapUtils.getString(jsonObject, "content");
			// 收件人
			String receiverUser = MapUtils.getString(jsonObject, "receiver");
			String cList = MapUtils.getString(jsonObject, "cList"); //抄送人，多个用竖线分割
			List<String> receivers = new ArrayList<>();
			receivers.add(receiverUser);
			// 有抄送人，需要循环添加到里面
			if (StringUtils.isNotBlank(cList)) {
				String[] ccList = cList.split("\\|");
				for (int z = 0; z < ccList.length; z++) {
					receivers.add(ccList[z]);
				}
			}
			// 附件
			String fileList = MapUtils.getString(jsonObject, "fileList"); //附件地址，多个用竖线分割
			List<File> fileAddList = new ArrayList<>();
			if (StringUtils.isNotBlank(fileList)) {
				String[] fileArr = fileList.split("\\|");
				for (int m = 0; m < fileArr.length; m++) {
					File tmpFile = new File(fileArr[m]);
					fileAddList.add(tmpFile);
				}
			}
			new SendMail().sendEmail(host, userName, passwrd, title, htmlContent, receivers, fileAddList);
			log.info("邮件发送结束");
			return true;
		} catch (Exception e) {
			log.error("发送邮件异常", e);
		}
		return false;
	}

}
