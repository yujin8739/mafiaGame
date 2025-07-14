package com.mafia.game.board.model.service;

import java.util.ArrayList;
import java.util.HashMap;

import com.mafia.game.board.model.vo.Notice;
import com.mafia.game.common.model.vo.PageInfo;

public interface NoticeService {

	int noticeCount(HashMap<String, String> noticeMap);

	ArrayList<Notice> noticeList(HashMap<String, String> noticeMap, PageInfo pi);

	Notice selectNotice(int noticeNo);

	void increaseCount(int noticeNo);

	int deleteNotice(int noticeNo);

	int updateNotice(Notice notice);

	int writeNotice(Notice notice);
	
}
