package se.plushogskolan.restcaseservice.model;

public final class AuthBean {
	
	private String facebook_token;
	
	private AuthBean(String facebookToken){
		this.facebook_token = facebookToken;
	}
	
	private AuthBean(){
		this.facebook_token = null;
	}
	
	public String getFacebook_token() {
		return facebook_token;
	}
}
