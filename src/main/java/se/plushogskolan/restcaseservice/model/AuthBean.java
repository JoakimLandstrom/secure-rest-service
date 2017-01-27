package se.plushogskolan.restcaseservice.model;

public final class AuthBean {
	
	private String username;
	
	private String password;
	
	private String refresh_token;
	
	private AuthBean(String username, String password, String refresh_token){
		this.username = username;
		this.password = password;
		this.refresh_token = refresh_token;
	}
	
	private AuthBean(){
		this.username = null;
		this.password = null;
		this.refresh_token = null;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getRefresh_token() {
		return refresh_token;
	}

}
