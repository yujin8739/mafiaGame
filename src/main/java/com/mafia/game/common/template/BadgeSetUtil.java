package com.mafia.game.common.template;

import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.Reply;

public class BadgeSetUtil {
	
	public static Board setBadgeUrl(Board board) {
		int rankPoint = board.getRankPoint();
		if(0 <= rankPoint && rankPoint < 500) {
			board.setProfileUrl("/images/badge/iron.png");
		}else if(500 <= rankPoint && rankPoint < 1200) {
			board.setProfileUrl("/images/badge/thug.png");
		}else if(1200 <= rankPoint && rankPoint < 2000) {
			board.setProfileUrl("/images/badge/agent.png");
		}else if(2000 <= rankPoint && rankPoint < 3000) {
			board.setProfileUrl("/images/badge/underBoss.png");
		}else if(3000 <= rankPoint) {
			board.setProfileUrl("/images/badge/boss.png");
		}
		
		return board;
	}
	
	public static Reply setBadgeUrl(Reply reply) {
		int rankPoint = reply.getRankPoint();
		if(0 <= rankPoint && rankPoint < 500) {
			reply.setProfileUrl("/images/badge/iron.png");
		}else if(500 <= rankPoint && rankPoint < 1200) {
			reply.setProfileUrl("/images/badge/thug.png");
		}else if(1200 <= rankPoint && rankPoint < 2000) {
			reply.setProfileUrl("/images/badge/agent.png");
		}else if(2000 <= rankPoint && rankPoint < 3000) {
			reply.setProfileUrl("/images/badge/underBoss.png");
		}else if(3000 <= rankPoint) {
			reply.setProfileUrl("/images/badge/boss.png");
		}
		
		return reply;
	}
	
	public static BoardFile setBadgeUrl(BoardFile file) {
		int rankPoint = file.getRankPoint();
		if(0 <= rankPoint && rankPoint < 500) {
			file.setProfileUrl("/images/badge/iron.png");
		}else if(500 <= rankPoint && rankPoint < 1200) {
			file.setProfileUrl("/images/badge/thug.png");
		}else if(1200 <= rankPoint && rankPoint < 2000) {
			file.setProfileUrl("/images/badge/agent.png");
		}else if(2000 <= rankPoint && rankPoint < 3000) {
			file.setProfileUrl("/images/badge/underBoss.png");
		}else if(3000 <= rankPoint) {
			file.setProfileUrl("/images/badge/boss.png");
		}
		
		return file;
	}
	
}
