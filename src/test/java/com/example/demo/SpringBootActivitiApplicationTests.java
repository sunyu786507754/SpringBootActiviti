package com.example.demo;

import java.util.List;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringBootActivitiApplicationTests {

	@Autowired
    private RuntimeService runtimeService;
	@Autowired
    private TaskService taskService;
	@Test
	public void contextLoads() {
//		ProcessInstance processInstance=runtimeService.startProcessInstanceByKey("HelloWorldKey");
//		System.out.println("流程实例id======="+processInstance.getId());
//		System.out.println("流程定义id======="+processInstance.getDeploymentId());
		
		
		// 查询并且返回任务即可  
	    List<Task> taskList=taskService.createTaskQuery()
	    		.processDefinitionKey("HelloWorldKey")
	    		.taskAssignee("张三").list();
	    for(Task task:taskList){  
	        System.out.println("任务ID:"+task.getId());  
	        System.out.println("任务名称："+task.getName());  
	        System.out.println("任务创建时间："+task.getCreateTime());  
	        System.out.println("任务委派人："+task.getAssignee());  
	        System.out.println("流程实例ID:"+task.getProcessInstanceId());  
	    }  
		
	}

}
