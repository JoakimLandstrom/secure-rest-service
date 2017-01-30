package se.plushogskolan.restcaseservice.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import se.plushogskolan.restcaseservice.exception.ExternalApiException;
import se.plushogskolan.restcaseservice.exception.UnauthorizedException;
import se.plushogskolan.restcaseservice.exception.WebInternalErrorException;
import se.plushogskolan.restcaseservice.model.Admin;
import se.plushogskolan.restcaseservice.properties.ReadProperty;
import se.plushogskolan.restcaseservice.repository.AdminRepository;
import se.plushogskolan.restcaseservice.repository.FacebookApi;

@Service
public class FacebookAdminService {

	private final long EXPIRATION_TIME_ACCESS = 20;

	private AdminRepository adminRepository;

	private FacebookApi facebookApi;

	@Autowired
	public FacebookAdminService(AdminRepository adminRepository, FacebookApi facebookApi) {
		this.adminRepository = adminRepository;
		this.facebookApi = facebookApi;
	}

	public void createAdmin(String userid, String username) {

		try {
			Admin admin = new Admin(username);
			admin.setUserId(userid);

			adminRepository.save(admin);

		} catch (DataAccessException e) {
			throw new WebInternalErrorException("Could not save admin");
		}
	}

	public boolean authenticateToken(String token) {

		if (token != null) {

			token = new String(token.substring("Bearer ".length()));

			try {

				Jwts.parser().require("admin", true).setSigningKey(ReadProperty.readProperty("secret"))
						.parseClaimsJws(token);

			} catch (ExpiredJwtException e) {
				throw new UnauthorizedException("Access token has run out");
			} catch (JwtException e) {
				throw new UnauthorizedException("Access token could not be verified");
			} catch (IOException e) {
				throw new WebInternalErrorException("Internal error");
			}

			return true;
		} else {
			throw new UnauthorizedException("Authorization header not found or empty");
		}
	}

	public String authenticateFacebookUser(String facebookToken) {

		try {
			JSONObject userAccess = new JSONObject(facebookApi.authenticateUser(facebookToken));
			String userid = getUserId(facebookApi.getUserInfo(userAccess.getString("access_token")));

			Admin admin = adminRepository.findByUserId(userid);
			admin.setToken(userAccess.getString("access_token"));
			adminRepository.save(admin);

			return generateAccessToken(getFacebookUser(userid));
		} catch (ExternalApiException | JSONException | DataAccessException e) {
			throw new WebInternalErrorException("Internal error");
		}
	}

	public String refreshToken(String jwt) {

		jwt = jwt.substring("Bearer ".length());
		String username = getUsernameFromJwt(jwt);

		try {
			Admin admin = adminRepository.findByUsername(username);

			if (admin != null) {

				String adminInfo = facebookApi.getUserInfo(admin.getToken());

				if (isValid(adminInfo)) {

					return generateAccessToken(admin);
				} else {
					throw new UnauthorizedException("Token is not valid, login again");
				}
			} else {
				throw new UnauthorizedException("Admin doest not exist");
			}

		} catch (DataAccessException | ExternalApiException e) {
			throw new WebInternalErrorException("Internal error");
		}
	}

	private String getUsernameFromJwt(String jwt) {

		if (hasExpired(jwt)) {
			Base64.Decoder decoder = Base64.getUrlDecoder();
			String[] parts = jwt.split("\\.");

			String payload = new String(decoder.decode(parts[1]));

			JSONObject jsonObject = new JSONObject(payload);
			return jsonObject.getString("user");
		} else {
			throw new UnauthorizedException("Malformed access token");
		}
	}

	private boolean hasExpired(String jwt) {

		try {
			Jwts.parser().require("admin", true).setSigningKey(ReadProperty.readProperty("secret")).parseClaimsJws(jwt);
			return true;
		} catch (ExpiredJwtException e) {
			return true;
		} catch (JwtException e) {
			throw new UnauthorizedException("Malformed access token");
		} catch (IOException e) {
			throw new WebInternalErrorException("Internal error");
		}
	}

	private boolean isValid(String jsonString) {
		try {
			JSONObject jsonObject = new JSONObject(jsonString).getJSONObject("data");

			return jsonObject.getBoolean("is_valid");

		} catch (JSONException e) {
			throw new UnauthorizedException("Facebook user could not be verified");
		}
	}

	private String getUserId(String jsonString) {

		try {

			JSONObject jsonObject = new JSONObject(jsonString).getJSONObject("data");
			String userid = jsonObject.getString("user_id");

			if (userid != null) {
				return userid;
			} else {
				throw new UnauthorizedException("No user id");
			}
		} catch (JSONException e) {
			throw new UnauthorizedException("Facebook user could not be verified");
		}

	}

	private Admin getFacebookUser(String userid) {

		try {
			Admin admin = adminRepository.findByUserId(userid);

			if (admin != null) {
				return admin;
			} else {
				throw new UnauthorizedException("Admin doest not exist");
			}
		} catch (DataAccessException e) {
			throw new WebInternalErrorException("Internal error");
		}
	}

	private String generateAccessToken(Admin admin) {

		try {
			String jwtToken = Jwts.builder().setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
					.claim("user", admin.getUsername()).setExpiration(generateAccessTimestamp()).claim("admin", true)
					.signWith(SignatureAlgorithm.HS256, ReadProperty.readProperty("secret")).compact();

			return jwtToken;

		} catch (IOException e) {
			throw new WebInternalErrorException("Internal error");
		}

	}

	private Date generateAccessTimestamp() {

		LocalDateTime date = LocalDateTime.now().plusMinutes(EXPIRATION_TIME_ACCESS);

		return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	}
}
