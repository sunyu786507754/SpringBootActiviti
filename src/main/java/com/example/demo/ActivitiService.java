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
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricVariableInstance;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Comment;
import org.activiti.engine.task.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.pojo.LeavePojo;
import com.example.demo.reposity.LeaveReposity;

@Service("activitiService")
public class ActivitiService {
	@Autowired
	private LeaveReposity leaveReposity;
	
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
		LeavePojo lp=new LeavePojo();
		lp.setApplyName(userName);
		lp.setApplyReason(reason);
		lp.setApplyDate(new Date());
		
		leaveReposity.save(lp);
		
		String businessKey="BusinessKey_Leave_"+lp.getId();
		
		Map<String,Object> variables = new HashMap<String, Object>();
		variables.put("applyUser", userName);
		
		identityService.setAuthenticatedUserId(userName);
//		
		//开始流程
		ProcessInstance processInstance = 
				runtimeService.startProcessInstanceByKey("leaveProcess",businessKey,variables);
		// 查询当前任务
        Task currentTask = taskService.createTaskQuery().processInstanceId(processInstance.getId()).singleResult();
        // 申明任务
        taskService.claim(currentTask.getId(), userName);
//
//        Map<String, Object> vars = new HashMap<>();
//        vars.put("userName", userName);
//        vars.put("reason", reason);
//        vars.put("applyDate", sdf.format(new Date()));
        // 完成任务
        taskService.complete(currentTask.getId());
	}
	//获取我发起的申请
	public List<Map<String, Object>> loadMyApply(HttpSession session) {
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<Map<String, Object>> list=new ArrayList<>();
		String userName=session.getAttribute("userName").toString();
		List<ProcessInstance> instanceList = runtimeService.createProcessInstanceQuery().startedBy(userName).list();		
		for(ProcessInstance pi:instanceList) {
			// 查询当前任务
	        Task currentTask = taskService.createTaskQuery().processInstanceId(pi.getId()).singleResult();
	        String businessKey=pi.getBusinessKey();
	        String primaryId=businessKey.substring(18);
	        LeavePojo lp=leaveReposity.findById(Integer.valueOf(primaryId)).get() ;
	        
			Map<String,Object> m=new HashMap<String,Object>();
			m.put("applyName", lp.getApplyName());//申请人
			m.put("applyReason", lp.getApplyReason());//申请理由
			m.put("applyDate", sdf.format(lp.getApplyDate()));//申请日期
			m.put("audit", currentTask.getAssignee());//待审人
			
			list.add(m);
		}
		
		return list;
	}
	//待我审核的请假
	public List<Map<String, Object>> holdOnMyAudit(HttpSession session) {
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String userName=session.getAttribute("userName").toString();
		List<Task> taskList = taskService.createTaskQuery().taskAssignee(userName)
                .orderByTaskCreateTime().desc().list();
		List<Map<String,Object>> list=new ArrayList<>();
		for (Task task : taskList) {
			String instanceId = task.getProcessInstanceId();
			ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(instanceId).singleResult();
			String businessKey=instance.getBusinessKey();
	        String primaryId=businessKey.substring(18);
	        LeavePojo lp=leaveReposity.findById(Integer.valueOf(primaryId)).get() ;
	        
			Map<String,Object> m=new HashMap<String,Object>();
			m.put("applyName", lp.getApplyName());//申请人
			m.put("applyReason", lp.getApplyReason());//申请理由
			m.put("applyDate", sdf.format(lp.getApplyDate()));//申请日期
			
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
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String userName=session.getAttribute("userName").toString();
		List<HistoricProcessInstance> hisProInstance = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey("leaveProcess").startedBy(userName).finished()
                .orderByProcessInstanceEndTime().desc().list();
		List<Map<String,Object>> list=new ArrayList<>();
		for(HistoricProcessInstance hpi:hisProInstance) {

			String businessKey=hpi.getBusinessKey();
	        String primaryId=businessKey.substring(18);
	        LeavePojo lp=leaveReposity.findById(Integer.valueOf(primaryId)).get() ;
	        
	        Map<String,Object> m=new HashMap<String,Object>();
	        
	        m.put("applyName", lp.getApplyName());//申请人
			m.put("applyReason", lp.getApplyReason());//申请理由
			m.put("applyDate", sdf.format(lp.getApplyDate()));//申请日期
			
			m.put("instanceId", hpi.getId());
			m.put("businessKey", businessKey);
			list.add(m);
		}
		return list;
	}
	/**
	 * 审批记录详细
	 * @param instanceId
	 * @return
	 */
	public List<Map<String, Object>> detail(String businessKey) {
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		List<Map<String, Object>> l=new ArrayList<>();
		List<HistoricTaskInstance> list=historyService.createHistoricTaskInstanceQuery()
				.processInstanceBusinessKey(businessKey)
				.list();
		for(HistoricTaskInstance ht:list) {

			List<Comment> commentList=taskService.getTaskComments(ht.getId());
			StringBuffer commentBuffer=new StringBuffer();
			for(Comment c:commentList) {
				commentBuffer.append(c.getFullMessage());
			}
			Map<String,Object> m=new HashMap<>();
			m.put("assignee", ht.getAssignee());//审批人
			m.put("comment", commentBuffer.toString());//审批意见
			m.put("auditDate",sdf.format(ht.getEndTime()));//审批时间
			
			l.add(m);
			
		}
		return l;
	}
	
	
	
}
