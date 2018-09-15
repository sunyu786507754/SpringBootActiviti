package com.example.demo.reposity;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.pojo.LeavePojo;

public interface LeaveReposity extends JpaRepository<LeavePojo,Integer>{

}
