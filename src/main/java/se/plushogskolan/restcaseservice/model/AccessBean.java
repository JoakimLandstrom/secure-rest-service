package se.plushogskolan.restcaseservice.model;

public final class AccessBean {

	private String access_token;

	public AccessBean(String access_token){
		this.access_token = access_token;
	}
	
	public String getAccess_token() {
		return access_token;
	}
}
