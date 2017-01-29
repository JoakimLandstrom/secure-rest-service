package se.plushogskolan.restcaseservice.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;

import se.plushogskolan.casemanagement.model.AbstractEntity;

@Entity
public class Admin extends AbstractEntity {

	@Column(unique=true)
	private String username;
	
	@Column(unique=true, columnDefinition = "LONGBLOB")
	private byte[] salt;
	
	private byte[] hashedPassword;
	
	private Date timestamp;
	
	@Column(unique=true)
	private String token;
	
	protected Admin(){
	}
	
	public Admin(byte[] password, String username, byte[] salt){
		this.hashedPassword = password;
		this.username = username;
		this.salt = salt;
	}

	public String getUsername() {
		return username;
	}
	
	public byte[] getSalt() {
		return salt;
	}
	
	public byte[] getHashedPassword() {
		return hashedPassword;
	}
	
	public Date getTimestamp() {
		
		Date date = Date.from(timestamp.toInstant());
		
		return date;
	}
	
	public String getToken() {
		return token;
	}
	
	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
}
