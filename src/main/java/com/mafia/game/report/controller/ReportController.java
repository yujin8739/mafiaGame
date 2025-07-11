package com.mafia.game.report.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.mafia.game.member.model.vo.Member;
import com.mafia.game.report.model.service.ReportService;
import com.mafia.game.report.model.vo.Report;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;

@Controller
@RequestMapping("/report")
@Slf4j
public class ReportController {

	@Autowired
	private ReportService reportService;

	/**
	 * 신고 폼 페이지
	 */
	@GetMapping("/form")
	public String reportForm(@RequestParam(required = false) String reportedName, Model model, HttpSession session) {

		// 로그인 확인
		Member loginUser = (Member) session.getAttribute("loginUser");
		if (loginUser == null) {
			return "redirect:/login/view";
		}

		// URL 파라미터로 신고 대상이 전달된 경우 모델에 추가
		if (reportedName != null && !reportedName.isEmpty()) {
			model.addAttribute("reportedName", reportedName);
		}

		return "report/reportForm";
	}
	
	
	/**
	 * 신고하기 (AJAX)
	 */
	@PostMapping("/submit")
	@ResponseBody
	public Map<String, Object> submitReport(@RequestParam String reportedName,
											@RequestParam String reason,
											HttpSession session) {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			// 로그인 사용자 확인
			Member loginUser = (Member) session.getAttribute("loginUser");
			if (loginUser == null) {
				response.put("success", false);
				response.put("message", "로그인이 필요합니다.");
				return response;
			}

			// 자기 자신 신고 방지
			if (loginUser.getUserName().equals(reportedName)) {
				response.put("success", false);
				response.put("message", "자기 자신을 신고할 수 없습니다.");
				return response;
			}

			// 중복 신고 확인
			boolean isDuplicate = reportService.checkDuplicateReport(loginUser.getUserName(), reportedName);
			if (isDuplicate) {
				response.put("success", false);
				response.put("message", "이미 신고한 사용자입니다.");
				return response;
			}

			// 신고 등록
			Report report = Report.builder()
					.reporterId(loginUser.getUserName())
					.reportedName(reportedName)
					.reason(reason)
					.build();

			int result = reportService.insertReport(report);

			if (result > 0) {
				response.put("success", true);
				response.put("message", "신고가 접수되었습니다.");
				log.info("신고 접수 완료 - 신고자: {}, 신고대상: {}", loginUser.getUserName(), reportedName);
			} else {
				response.put("success", false);
				response.put("message", "신고 접수에 실패했습니다.");
			}

		} catch (Exception e) {
			log.error("신고 처리 중 오류 발생", e);
			response.put("success", false);
			response.put("message", "서버 오류가 발생했습니다.");
		}

		return response;
	}

	/**
	 * 내 신고 내역 조회
	 */
	@GetMapping("/myReports")
	public String myReports(HttpSession session, Model model) {
		
		// 로그인 확인
		Member loginUser = (Member) session.getAttribute("loginUser");
		if (loginUser == null) {
			return "redirect:/login/view";
		}

		try {
			// 내 신고 내역 조회
			ArrayList<Report> reportList = reportService.getMyReportList(loginUser.getUserName());
			model.addAttribute("reportList", reportList);
			
			log.info("신고 내역 조회 - 사용자: {}, 건수: {}", loginUser.getUserName(), reportList.size());
			
		} catch (Exception e) {
			log.error("신고 내역 조회 중 오류 발생", e);
			model.addAttribute("msg", "신고 내역을 불러오는데 실패했습니다.");
		}

		return "report/reportList";
	}

	/**
	 * 신고 삭제 (취소)
	 */
	@PostMapping("/delete")
	@ResponseBody
	public Map<String, Object> deleteReport(@RequestParam int reportId, HttpSession session) {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			// 로그인 확인
			Member loginUser = (Member) session.getAttribute("loginUser");
			if (loginUser == null) {
				response.put("success", false);
				response.put("message", "로그인이 필요합니다.");
				return response;
			}

			// 신고 정보 확인
			Report report = reportService.getReportById(reportId);
			if (report == null) {
				response.put("success", false);
				response.put("message", "해당 신고를 찾을 수 없습니다.");
				return response;
			}

			if (!report.getReporterId().equals(loginUser.getUserName())) {
				response.put("success", false);
				response.put("message", "본인이 신고한 내용만 삭제할 수 있습니다.");
				return response;
			}

			// 신고 삭제
			int result = reportService.deleteReport(reportId);
			
			if (result > 0) {
				response.put("success", true);
				response.put("message", "신고가 취소되었습니다.");
				log.info("신고 취소 완료 - 신고번호: {}, 사용자: {}", reportId, loginUser.getUserName());
			} else {
				response.put("success", false);
				response.put("message", "신고 취소에 실패했습니다.");
			}

		} catch (Exception e) {
			log.error("신고 삭제 중 오류 발생", e);
			response.put("success", false);
			response.put("message", "서버 오류가 발생했습니다.");
		}

		return response;
	}

	/**
	 * 중복신고확인
	 */
	@GetMapping("/checkDuplicate")
	@ResponseBody
	public Map<String, Object> checkDuplicateReport(@RequestParam String reportedName, HttpSession session) {
		
		Map<String, Object> response = new HashMap<>();
		
		try {
			Member loginUser = (Member) session.getAttribute("loginUser");
			if (loginUser == null) {
				response.put("isDuplicate", false);
				response.put("message", "로그인이 필요합니다.");
				return response;
			}

			boolean isDuplicate = reportService.checkDuplicateReport(loginUser.getUserName(), reportedName);
			response.put("isDuplicate", isDuplicate);
			
			if (isDuplicate) {
				response.put("message", "이미 신고한 사용자입니다.");
			}

		} catch (Exception e) {
			log.error("중복 신고 확인 중 오류 발생", e);
			response.put("isDuplicate", false);
			response.put("message", "확인 중 오류가 발생했습니다.");
		}

		return response;
	}
}