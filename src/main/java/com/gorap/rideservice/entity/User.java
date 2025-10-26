package com.gorap.rideservice.entity;

import java.sql.Timestamp;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.GenericGenerator;

import com.gorap.rideservice.constants.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users",schema="identity")
@Getter
@Setter
public class User {
	
	
	@Id
    @GenericGenerator(name = "uuid", strategy = "uuid2")
    @GeneratedValue(generator = "uuid")
    private UUID id;
    
	@Column(name="user_name")
    private String userName;
	
	@Column(name="password")
    private String password;
    
    @Column(name="email")
    private String email;
    
    @Column(name = "deviceName")
    private String deviceName;
    
    @Column(name="role")
    @Enumerated(EnumType.STRING)
    private Role role;
    
    @Column(name="phone_number")
    private String phoneNumber;
    
    @Column(name="address")
    private String address;
    
    @Column(name = "created_on")
    @CreationTimestamp
    private Timestamp createdOn;
    
    @Column(name = "modified_on")
    private Timestamp modifiedOn;
    
    @Column(name = "profile_pic")
    private String profilePic;

}
