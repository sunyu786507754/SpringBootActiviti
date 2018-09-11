package com.example.demo;

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
import org.activiti.engine.runtime.ProcessInstance;
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
		List<Map<String, Object>> list=new ArrayList<>();
		String userName=session.getAttribute("userName").toString();
		List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().startedBy(userName).list();		
		for(ProcessInstance pi:instanceList) {
			String reason = runtimeService.getVariable(pi.getId(), "reason", String.class);

			Map<String,Object> m=new HashMap<String,Object>();
			m.put("reason", reason);
			m.put("userName", userName);
			
			list.add(m);
		}
		
		return list;
	}
	
	
	
}
