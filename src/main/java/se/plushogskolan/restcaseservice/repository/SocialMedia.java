package se.plushogskolan.restcaseservice.repository;

import se.plushogskolan.restcaseservice.exception.ExternalApiException;

public interface SocialMedia {

	public String authenticateUser(String token) throws ExternalApiException;
	
}
