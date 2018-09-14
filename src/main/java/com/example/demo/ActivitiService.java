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
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricProcessInstance;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
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
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd");
		Map<String,Object> variables = new HashMap<String, Object>();
		variables.put("applyUser", userName);
		identityService.setAuthenticatedUserId(userName);
		
		//开始流程
		ProcessInstance processInstance = 
				runtimeService.startProcessInstanceByKey("ProcessDemo20180912",variables);
		// 查询当前任务
        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        // 申明任务
        taskService.claim(currentTask.getId(), userName);

        Map<String, Object> vars = new HashMap<>();
        vars.put("userName", userName);
        vars.put("reason", reason);
        vars.put("applyDate", sdf.format(new Date()));
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
			String date=runtimeService.getVariable(pi.getId(), "applyDate", String.class);
			// 查询当前任务
	        Task currentTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
			Map<String,Object> m=new HashMap<String,Object>();
			m.put("reason", reason);
			m.put("userName", userName);
			m.put("applyDate", date);
			m.put("audit", currentTask.getAssignee());
			
			list.add(m);
		}
		
		return list;
	}
	//待我审核的请假
	public List<Map<String, Object>> holdOnMyAudit(HttpSession session) {
		String userName=session.getAttribute("userName").toString();
		List<Task> taskList = taskService.createTaskQuery().taskAssignee(userName)
                .orderByTaskCreateTime().desc().list();
		List<Map<String,Object>> list=new ArrayList<>();
		for (Task task : taskList) {
			String instanceId = task.getProcessInstanceId();
			ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
			String applyUser=runtimeService.getVariable(instance.getId(), "userName", String.class);
			String reason = runtimeService.getVariable(instance.getId(), "reason", String.class);
			String date=runtimeService.getVariable(instance.getId(), "applyDate", String.class);
			
			Map<String,Object> m=new HashMap<String,Object>();
			m.put("reason", reason);
			m.put("applyUser", applyUser);
			m.put("applyDate", date);
			m.put("taskId", task.getId());
			m.put("instanceId", instanceId);
			list.add(m);
		}
		return list;
	}
	/**
	 * 同意
	 * @param taskId
	 * @param instanceId
	 */
	public void agree(String taskId, String instanceId) {
		taskService.addComment(taskId, instanceId, "同意");
		taskService.complete(taskId);
	}
	/**
	 * 我申请过的记录
	 * @param session
	 * @return
	 */
	public List<Map<String, Object>> loadAlreadyMyApply(HttpSession session) {
		String userName=session.getAttribute("userName").toString();
		List<HistoricProcessInstance> hisProInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("ProcessDemo20180912").startedBy(userName).finished()
                .orderByProcessInstanceEndTime().desc().list();
		List<Map<String,Object>> list=new ArrayList<>();
		for(HistoricProcessInstance hpi:hisProInstance) {
			Map<String,Object> m=new HashMap<String,Object>();
			
			List<HistoricVariableInstance> varInstanceList = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hpi.getId()).list();
			for(HistoricVariableInstance hi:varInstanceList) {
				m.put(hi.getVariableName(), hi.getValue());
			}
			m.put("instanceId", hpi.getId());
			list.add(m);
		}
		return list;
	}
	/**
	 * 审批记录详细
	 * @param instanceId
	 * @return
	 */
	public List<Map<String, Object>> detail(String instanceId) {
		List<Map<String, Object>> l=new ArrayList<>();
		//获取历史活动
		List<HistoricProcessInstance> list = historyService
				.createHistoricProcessInstanceQuery()
				.processDefinitionId(instanceId)//流程定义ID
				.list();
		StringBuffer comment=new StringBuffer();
		//获取添加的批注
		for(HistoricProcessInstance hpi:list) {
			Map<String,Object> m=new HashMap<String,Object>();
			
			List<HistoricVariableInstance> varInstanceList = historyService.createHistoricVariableInstanceQuery()
                    .processInstanceId(hpi.getId()).list();
			for(HistoricVariableInstance hi:varInstanceList) {
				m.put(hi.getVariableName(), hi.getValue());
			}
			
			
			List<HistoricTaskInstance> htiList = historyService.createHistoricTaskInstanceQuery()//历史任务表查询
					.processInstanceId(hpi.getId())//使用流程实例ID查询
					.list();
			for(HistoricTaskInstance hti:htiList){
				//任务ID
				String htaskId = hti.getId();
				//获取批注信息
				List<Comment> taskList = taskService.getTaskComments(htaskId);//对用历史完成后的任务ID
				for(Comment c:taskList) {
					comment.append(c.getFullMessage());
				}
			}
			m.put("comment", comment.toString());
			
			l.add(m);
			
		}
			
		return l;
	}
	
	
	
}
