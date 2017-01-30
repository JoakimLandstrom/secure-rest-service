package se.plushogskolan.restcaseservice.repository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.springframework.stereotype.Component;

import se.plushogskolan.restcaseservice.exception.ExternalApiException;
import se.plushogskolan.restcaseservice.properties.ReadProperty;

@Component
public final class FacebookApi implements SocialMedia {

	private URLConnection getConnection(String connectionUrl) throws IOException {

		URL url = new URL(connectionUrl);

		return url.openConnection();
	}

	@Override
	public String authenticateUser(String token) throws ExternalApiException {
		try {
			URLConnection urlConnection = getConnection(generateAccesstokenUrl(token));

			if (urlConnection instanceof HttpURLConnection) {
				HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

				int statusCode = httpURLConnection.getResponseCode();

				if (statusCode != 200) {
					return readInputStream(httpURLConnection.getErrorStream());
				}
			}

			return readInputStream(urlConnection.getInputStream());
		} catch (IOException e) {
			throw new ExternalApiException("Couldnt authenticate user");
		}
	}

	public String getUserInfo(String token) throws ExternalApiException {
		try {
			URLConnection urlConnection = getConnection(generateDebugUrl(token));

			if (urlConnection instanceof HttpURLConnection) {
				HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

				int statusCode = httpURLConnection.getResponseCode();

				if (statusCode != 200) {
					return readInputStream(httpURLConnection.getErrorStream());
				}
			}

			return readInputStream(urlConnection.getInputStream());
		} catch (IOException e) {
			throw new ExternalApiException("Could not get user info");
		}
	}

	private String generateAccesstokenUrl(String facebookCode) throws IOException {

		StringBuilder builder = new StringBuilder();

		builder.append("https://graph.facebook.com/v2.8/oauth/access_token?client_id=").append(getClientId())
				.append("&redirect_uri=http://localhost:8080/login&client_secret=").append(getClientSecret())
				.append("&code=").append(facebookCode);

		return builder.toString();
	}

	private String generateDebugUrl(String access_token) throws IOException {

		StringBuilder builder = new StringBuilder();

		builder.append("https://graph.facebook.com/debug_token?input_token=").append(access_token)
				.append("&access_token=").append(getAppAccessToken());

		return builder.toString();
	}

	private String getAppAccessToken() throws IOException {

		StringBuilder builder = new StringBuilder();

		builder.append("https://graph.facebook.com/oauth/access_token?client_id=").append(getClientId())
				.append("&client_secret=").append(getClientSecret()).append("&grant_type=client_credentials");

		URLConnection urlConnection = getConnection(builder.toString());

		if (urlConnection instanceof HttpURLConnection) {
			HttpURLConnection httpURLConnection = (HttpURLConnection) urlConnection;

			int statusCode = httpURLConnection.getResponseCode();

			if (statusCode != 200) {
				throw new IOException();
			}
		}

		String inputStream = readInputStream(urlConnection.getInputStream());

		inputStream = inputStream.substring("access_token=".length());

		return inputStream;
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

	private String getClientId() throws IOException {

		return ReadProperty.readProperty("appid");
	}

	private String getClientSecret() throws IOException {

		return ReadProperty.readProperty("appsecret");
	}
}
