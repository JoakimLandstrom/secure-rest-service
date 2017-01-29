package se.plushogskolan.restcaseservice.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.stereotype.Component;

@Component
public final class FacebookApi implements SocialMedia {
	
	private static final String URL =  "https://graph.facebook.com/me?access_token=";

	@Override
	public String getUser(String socialMediaToken) throws IOException{
		
		URLConnection urlConnection = getConnection(socialMediaToken);
		
		BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
		
		StringBuilder builder = new StringBuilder();
		
		String line = "";
		
		while((line = reader.readLine()) != null){
			builder.append(line).append("\n");
		}
		
		reader.close();
		
		return builder.toString();
	}
	
	private URLConnection getConnection(String fbToken) throws IOException{
		
		StringBuilder builder = new StringBuilder();
		
		builder.append(URL).append(fbToken);
		
		URL url = new URL(builder.toString());
		
		return url.openConnection();
	}
	

}
