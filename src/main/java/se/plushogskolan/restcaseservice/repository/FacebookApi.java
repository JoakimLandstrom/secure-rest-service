package se.plushogskolan.restcaseservice.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.stereotype.Component;

@Component
public final class FacebookApi implements SocialMedia {

	private static final String URL = "https://graph.facebook.com/me?access_token=";
	
	private static final String URL = "https://graph.facebook.com/oauth/access_token?grant_type=fb_exchange_token&client_id=1733194800342742&client_secret=308089725406fd9222b83f837f518104&fb_exchange_token=EAAYoVNxmptYBAIWYsXI0GsvbdihuEt7e3l2PRgeaZAoUw9Uqk1Bh6YHzoJFy2KcUzsbsZCh2thhcZBeZCH2RFOJ3RZCZCQZCraiU5rmCYRx9oqImTDXDkHiCcWdaB1WBlU3CxtMpLrZCU2qjStotl62SKEEUZCidrYpT67iZBB4vkgMQZDZD";
	

	@Override
	public String getUser(String socialMediaToken) throws IOException {

		URLConnection urlConnection = getConnection(socialMediaToken);

		if (urlConnection instanceof HttpURLConnection) {
			HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

			int statusCode = httpURLConnection.getResponseCode();

			if (statusCode != 200) {
				return readInputStream(httpURLConnection.getErrorStream());
			}			
		}
		
		return readInputStream(urlConnection.getInputStream());
	}

	private URLConnection getConnection(String fbToken) throws IOException {

		StringBuilder builder = new StringBuilder();

		builder.append(URL).append(fbToken);

		URL url = new URL(builder.toString());

		return url.openConnection();
	}

	private String readInputStream(InputStream inputStream) throws IOException {

		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		StringBuilder builder = new StringBuilder();

		String line = "";

		while ((line = reader.readLine()) != null) {
			builder.append(line).append("\n");
		}

		reader.close();

		return builder.toString();
	}
	
	private String generateUrl(){
		
		StringBuilder builder = new StringBuilder();
	}

}
