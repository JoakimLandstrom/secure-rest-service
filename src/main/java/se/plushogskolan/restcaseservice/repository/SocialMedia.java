package se.plushogskolan.restcaseservice.repository;

import java.io.IOException;

public interface SocialMedia {

	public String getUser(String socialMediaToken) throws IOException;
	
}
