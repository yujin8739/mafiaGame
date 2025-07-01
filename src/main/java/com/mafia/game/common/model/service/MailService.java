package com.mafia.game.common.model.service;

public interface MailService {
	void sendEmail(String toEmail, String subject, String body);
}
