package com.mafia.game.common.template;

import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.Reply;

public class BadgeSetUtil {
	
	public static Board setBadgeUrl(Board board) {
		int rankPoint = board.getRankPoint();
		if(0 <= rankPoint && rankPoint < 500) {
			board.setProfileUrl("/godDaddy_etc/badge/iron.png");
		}else if(500 <= rankPoint && rankPoint < 1200) {
			board.setProfileUrl("/godDaddy_etc/badge/thug.png");
		}else if(1200 <= rankPoint && rankPoint < 2000) {
			board.setProfileUrl("/godDaddy_etc/badge/agent.png");
		}else if(2000 <= rankPoint && rankPoint < 3000) {
			board.setProfileUrl("/godDaddy_etc/badge/underBoss.png");
		}else if(3000 <= rankPoint) {
			board.setProfileUrl("/godDaddy_etc/badge/boss.png");
		}
		
		return board;
	}
	
	public static Reply setBadgeUrl(Reply reply) {
		int rankPoint = reply.getRankPoint();
		if(0 <= rankPoint && rankPoint < 500) {
			reply.setProfileUrl("/godDaddy_etc/badge/iron.png");
		}else if(500 <= rankPoint && rankPoint < 1200) {
			reply.setProfileUrl("/godDaddy_etc/badge/thug.png");
		}else if(1200 <= rankPoint && rankPoint < 2000) {
			reply.setProfileUrl("/godDaddy_etc/badge/agent.png");
		}else if(2000 <= rankPoint && rankPoint < 3000) {
			reply.setProfileUrl("/godDaddy_etc/badge/underBoss.png");
		}else if(3000 <= rankPoint) {
			reply.setProfileUrl("/godDaddy_etc/badge/boss.png");
		}
		
		return reply;
	}
	
	public static BoardFile setBadgeUrl(BoardFile file) {
		int rankPoint = file.getRankPoint();
		if(0 <= rankPoint && rankPoint < 500) {
			file.setProfileUrl("/godDaddy_etc/badge/iron.png");
		}else if(500 <= rankPoint && rankPoint < 1200) {
			file.setProfileUrl("/godDaddy_etc/badge/thug.png");
		}else if(1200 <= rankPoint && rankPoint < 2000) {
			file.setProfileUrl("/godDaddy_etc/badge/agent.png");
		}else if(2000 <= rankPoint && rankPoint < 3000) {
			file.setProfileUrl("/godDaddy_etc/badge/underBoss.png");
		}else if(3000 <= rankPoint) {
			file.setProfileUrl("/godDaddy_etc/badge/boss.png");
		}
		
		return file;
	}
	
}
