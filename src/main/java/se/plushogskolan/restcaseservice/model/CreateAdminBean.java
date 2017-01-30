package se.plushogskolan.restcaseservice.model;

public final class CreateAdminBean {

	private String facebook_id;

	private String username;

	private CreateAdminBean(String facebook_id) {
		this.facebook_id = facebook_id;
	}

	private CreateAdminBean() {
		this.facebook_id = null;
		this.username = null;
	}

	public String getFacebook_id() {
		return facebook_id;
	}

	public String getUsername() {
		return username;
	}
}
