package com.example.expenses.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
//@RequiredArgsConstructor
public class NotificationService {

	private final JavaMailSender mailSender;
	
	private final String from;
	
	public NotificationService(@Value("${app.mail.from}") String from, JavaMailSender mailSender) {
		this.from = from;
		this.mailSender = mailSender;
	}
	
	public void notifySubmitted(String to, long expenseId, String traceId) {
		send(to,
				"[Expenses]申請が提出されました",
				"expenseId=" + expenseId + "\ntraceId=" + traceId + "\n");
	}
	
	public void notifyApproved(String to, long expenseId, String traceId) {
		send(to,
				"[Expenses]申請が承認されました",
				"expenseId=" + expenseId + "\ntraceId=" + traceId + "\n");
	}
	
	public void notifyRejected(String to, long expenseId, String reason, String traceId) {
		send(to,
				"[Expenses]申請が却下されました",
				"expenseId=" + expenseId + "\nreason=" + reason + "\ntraceId=" + traceId + "\n");
	}
	
	private void send(String to, String suject, String text) {
		var msg = new SimpleMailMessage();
		msg.setFrom(from);
		msg.setTo(to);
		msg.setSubject(suject);
		msg.setText(text);
		mailSender.send(msg);
	}
	
	
}
