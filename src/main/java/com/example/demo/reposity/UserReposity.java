package com.example.demo.reposity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.example.demo.pojo.UserPojo;

public interface UserReposity 
	extends JpaRepository<UserPojo,Integer>,
	JpaSpecificationExecutor<UserPojo>{

}
