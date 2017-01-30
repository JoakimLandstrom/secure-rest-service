package se.plushogskolan.restcaseservice.model;

import javax.persistence.Column;
import javax.persistence.Entity;

import se.plushogskolan.casemanagement.model.AbstractEntity;

@Entity
public class Admin extends AbstractEntity {

	@Column(unique=true)
	private String username;
	
	@Column(unique=true)
	private String token;
	
	@Column(unique=true)
	private String userId;
	
	protected Admin(){
	}
	
	public Admin(String username){
		this.username = username;
	}

	public String getUsername() {
		return username;
	}
	
	public String getToken() {
		return token;
	}
	
	public String getUserId() {
		return userId;
	}
	
	public void setUserId(String userid) {
		this.userId = userid;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
