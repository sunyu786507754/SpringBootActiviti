package com.example.demo;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ActitiviController {
	@Autowired
	private ActivitiService activitiService;
	@GetMapping("/index")
	public String startProcess() {
		return "index";
	}
	@GetMapping("/apply/{userName}")
	public String apply(@PathVariable("userName")String userName,HttpSession session) {
		session.setAttribute("userName", userName);
		return "apply";
	}
	/**
	 * 发起申请
	 * @param userName
	 * @param option
	 * @return
	 */
	@PostMapping("/submitApply")
	@ResponseBody
	public String submitApply(
			@RequestParam("userName")String userName,
			@RequestParam("reason")String reason) {
		
		activitiService.submitApply(userName,reason);
		return "ok";
	}
	/**
	 * 获取我申请的记录
	 * @return
	 */
	@GetMapping("/loadMyApply")
	@ResponseBody
	public List<Map<String,Object>> loadMyApply(HttpSession session) {
		List<Map<String,Object>> list=activitiService.loadMyApply(session);
		return list;
	}
	/**
	 * 待我审核的请假
	 * @param session
	 * @return
	 */
	@GetMapping("/holdOnMyAudit")
	@ResponseBody
	public List<Map<String,Object>> holdOnMyAudit(HttpSession session){
		List<Map<String,Object>> list=activitiService.holdOnMyAudit(session);
		return list;
	}
	
}
