package se.plushogskolan.restcaseservice.model;

public final class AuthBean {
	
	private String facebookToken;
	
	private AuthBean(String facebookToken){
		this.facebookToken = facebookToken;
	}
	
	private AuthBean(){
		this.facebookToken = null;
	}
	
	public String getFacebookToken() {
		return facebookToken;
	}
}
