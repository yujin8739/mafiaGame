package com.mafia.game.report.model.service;

import java.util.ArrayList;

import com.mafia.game.report.model.vo.Report;

public interface ReportService {

	// 신고 등록
	int insertReport(Report report);

	// 내 신고 내역 조회
	ArrayList<Report> getMyReportList(String reporterId);

	// 신고 상세 조회
	Report getReportById(int reportId);

	// 중복 신고 확인
	boolean checkDuplicateReport(String reporterId, String reportedName);

	// 신고 삭제 (취소)
	int deleteReport(int reportId);

	// 특정 사용자가 받은 신고 개수 조회
	int getReportCountByReported(String reportedName);

}