package com.mafia.game.report.model.dao;

import java.util.ArrayList;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.mafia.game.report.model.vo.Report;

@Repository
public class ReportDao {

	// 신고 등록
	public int insertReport(SqlSessionTemplate sqlSession, Report report) {
		return sqlSession.insert("reportMapper.insertReport", report);
	}

	// 내 신고 내역 조회
	public ArrayList<Report> selectMyReportList(SqlSessionTemplate sqlSession, String reporterId) {
		return (ArrayList)sqlSession.selectList("reportMapper.selectMyReportList", reporterId);
	}

	// 신고 상세 조회
	public Report selectReportById(SqlSessionTemplate sqlSession, int reportId) {
		return sqlSession.selectOne("reportMapper.selectReportById", reportId);
	}

	// 중복 신고 확인
	public int checkDuplicateReport(SqlSessionTemplate sqlSession, Map<String, String> params) {
		return sqlSession.selectOne("reportMapper.checkDuplicateReport", params);
	}

	// 신고 삭제
	public int deleteReport(SqlSessionTemplate sqlSession, int reportId) {
		return sqlSession.delete("reportMapper.deleteReport", reportId);
	}

	// 특정 사용자가 받은 신고 개수 조회
	public int countReportsByReported(SqlSessionTemplate sqlSession, String reportedName) {
		return sqlSession.selectOne("reportMapper.countReportsByReported", reportedName);
	}

	// 전체 신고 목록 조회 (관리자용)
	public ArrayList<Report> selectAllReports(SqlSessionTemplate sqlSession) {
		return (ArrayList)sqlSession.selectList("reportMapper.selectAllReports");
	}

	// 신고 개수 조회 (페이징용)
	public int selectReportCount(SqlSessionTemplate sqlSession, String reporterId) {
		return sqlSession.selectOne("reportMapper.selectReportCount", reporterId);
	}

}