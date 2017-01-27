package se.plushogskolan.restcaseservice.model;

import java.time.LocalDateTime;

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
	
	private LocalDateTime timestamp;
	
	@Column(unique=true)
	private String refreshToken;
	
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
	
	public LocalDateTime getTimestamp() {
		
		return LocalDateTime.from(timestamp);
	}
	
	public String getRefreshToken() {
		return refreshToken;
	}
	
	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
	
	public void setRefreshToken(String token) {
		this.refreshToken = token;
	}
}
