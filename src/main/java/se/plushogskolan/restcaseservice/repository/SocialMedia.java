package se.plushogskolan.restcaseservice.repository;

import java.io.IOException;

public interface SocialMedia {

	public String authenticateUser(String token) throws IOException;
	
}
