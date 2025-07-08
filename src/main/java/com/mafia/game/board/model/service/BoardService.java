package com.mafia.game.board.model.service;

import java.util.ArrayList;
import java.util.HashMap;

import com.mafia.game.board.model.vo.Board;
import com.mafia.game.board.model.vo.BoardFile;
import com.mafia.game.board.model.vo.Reply;
import com.mafia.game.common.model.vo.PageInfo;

public interface BoardService {

	int listCount(HashMap<String, String> filterMap);

	ArrayList<Board> boardList(HashMap<String, String> filterMap, PageInfo pi);

	Board loungeBoardDetail(int boardNo);

	int writeLoungeBoard(Board board,BoardFile file);

	int increaseCount(int boardNo);

	int uploadReply(Reply reply, BoardFile file);

	ArrayList<Reply> getReplyList(int boardNo);

	int deleteReply(Reply reply);

	Reply selectReply(int replyNo);

}
