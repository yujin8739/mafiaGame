package com.mafia.game.report.model.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mafia.game.report.model.dao.ReportDao;
import com.mafia.game.report.model.vo.Report;

@Service
@Transactional
public class ReportServiceImpl implements ReportService {

	@Autowired
	private SqlSessionTemplate sqlSession;
	
	@Autowired
	private ReportDao reportDao;

	@Override
	public int insertReport(Report report) {
		return reportDao.insertReport(sqlSession, report);
	}

	@Override
	public ArrayList<Report> getMyReportList(String reporterId) {
		return reportDao.selectMyReportList(sqlSession, reporterId);
	}

	@Override
	public Report getReportById(int reportId) {
		return reportDao.selectReportById(sqlSession, reportId);
	}

	@Override
	public boolean checkDuplicateReport(String reporterId, String reportedName) {
		Map<String, String> params = new HashMap<>();
		params.put("reporterId", reporterId);
		params.put("reportedName", reportedName);
		
		int count = reportDao.checkDuplicateReport(sqlSession, params);
		return count > 0; // 0보다 크면 중복
	}

	@Override
	public int deleteReport(int reportId) {
		return reportDao.deleteReport(sqlSession, reportId);
	}

	@Override
	public int getReportCountByReported(String reportedName) {
		return reportDao.countReportsByReported(sqlSession, reportedName);
	}

}