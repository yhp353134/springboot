package com.yu.boot.utils.email.javaEmail;


import java.util.ArrayList;
import java.util.List;

public class TestEmail {

	public static void main(String[] args) {
		// 邮件主题
		String title = "邮件主题";
		// 邮件正文
		String htmlContent = "邮件内容";
		// 收件人
		List<String> receivers = new ArrayList<>();
		receivers.add("yhp353134@163.com");
		receivers.add("yuhp@belink.com");
		/*// 附件
		String fileName1 = "附件路径1";
		File file1 = new File(fileName1);
		String fileName2 = "附件路径2";
		File file2 = new File(fileName2);
		List<File> fileList = new ArrayList<File>();
		fileList.add(file1);
		fileList.add(file2);*/
		// 执行发送
		new SendMail().sendEmail(title, htmlContent, receivers, null);
	}

}
