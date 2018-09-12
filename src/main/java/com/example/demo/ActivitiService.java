package com.example.demo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.activiti.engine.HistoryService;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.identity.User;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("activitiService")
public class ActivitiService {
	@Autowired
    private RuntimeService runtimeService;
	@Autowired
    private TaskService taskService;
	@Autowired
	private HistoryService historyService;
	@Autowired
	private IdentityService identityService;
	
	
	//发起请假申请
	public void submitApply(String userName, String reason) {
		
		identityService.setAuthenticatedUserId(userName);
		
		//开始流程
		ProcessInstance processInstance = 
				runtimeService.startProcessInstanceByKey("MyProcess1");
		// 查询当前任务
        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        // 申明任务
        taskService.claim(currentTask.getId(), userName);

        Map<String, Object> vars = new HashMap<>();
        vars.put("userName", userName);
        vars.put("reason", reason);
        vars.put("applyDate", new Date());
        // 完成任务
        taskService.complete(currentTask.getId(), vars);
	}
	//获取我发起的申请
	public List<Map<String, Object>> loadMyApply(HttpSession session) {
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
		List<Map<String, Object>> list=new ArrayList<>();
		String userName=session.getAttribute("userName").toString();
		List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().startedBy(userName).list();		
		
		
		for(ProcessInstance pi:instanceList) {
			String reason = runtimeService.getVariable(pi.getId(), "reason", String.class);
			Date date=runtimeService.getVariable(pi.getId(), "applyDate", Date.class);
			Map<String,Object> m=new HashMap<String,Object>();
			m.put("reason", reason);
			m.put("userName", userName);
			m.put("applyDate", sdf.format(date));
			
			List<IdentityLink> identityLinkList=taskService.getIdentityLinksForTask("16");
			StringBuffer sb=new StringBuffer();
			for(IdentityLink il:identityLinkList) {
	            if (sb.length() > 0) {//该步即不会第一位有逗号，也防止最后一位拼接逗号！
	                sb.append(",");
	            }
	            sb.append(il.getUserId());
			}
			m.put("audit", sb.toString());
			
			list.add(m);
		}
		
		return list;
	}
	//待我审核的请假
	public List<Map<String, Object>> holdOnMyAudit(HttpSession session) {
		String userName=session.getAttribute("userName").toString();
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
		List<Task> taskList = taskService.createTaskQuery().taskCandidateUser(userName)
                .orderByTaskCreateTime().desc().list();
		List<Map<String,Object>> list=new ArrayList<>();
		for (Task task : taskList) {
			String instanceId = task.getProcessInstanceId();
			ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
			
			String reason = runtimeService.getVariable(instance.getId(), "reason", String.class);
			Date date=runtimeService.getVariable(instance.getId(), "applyDate", Date.class);
			
			Map<String,Object> m=new HashMap<String,Object>();
			m.put("reason", reason);
			m.put("userName", userName);
			m.put("applyDate", sdf.format(date));
			
			list.add(m);
		}
		return list;
	}
	
	
	
}
