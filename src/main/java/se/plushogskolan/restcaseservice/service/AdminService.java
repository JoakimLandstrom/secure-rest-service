package se.plushogskolan.restcaseservice.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
import se.plushogskolan.restcaseservice.exception.UnauthorizedException;
import se.plushogskolan.restcaseservice.exception.WebInternalErrorException;
import se.plushogskolan.restcaseservice.model.Admin;
import se.plushogskolan.restcaseservice.properties.ReadProperty;
import se.plushogskolan.restcaseservice.repository.AdminRepository;
import se.plushogskolan.restcaseservice.repository.FacebookApi;

@Service
public class AdminService {

	private final long EXPIRATION_TIME_ACCESS = 20;

	private AdminRepository adminRepository;

	private FacebookApi facebookApi;

	@Autowired
	public AdminService(AdminRepository adminRepository, FacebookApi facebookApi) {
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

				Jwts.parser().require("adm", true).setSigningKey(ReadProperty.readProperty("secret"))
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

		JSONObject userAccess;
		String userid;

		try {
			userAccess = new JSONObject(facebookApi.authenticateUser(facebookToken));
			userid = getUserId(facebookApi.getUserInfo(userAccess.getString("access_token")));
			
			Admin admin = adminRepository.findByUserId(userid);
			admin.setToken(userAccess.getString("access_token"));
			adminRepository.save(admin);

		} catch (IOException | JSONException | DataAccessException e) {
			e.printStackTrace();
			throw new WebInternalErrorException("Internal error");
		}

		return generateAccessToken(getFacebookUser(userid));
	}

	private String getUserId(String jsonString) {

		String userid;
		JSONObject jsonObject = new JSONObject(jsonString).getJSONObject("data");

		try {
			userid = jsonObject.getString("user_id");
		} catch (JSONException e) {
			throw new UnauthorizedException("Facebook user could not be verified");
		}

		if (userid != null) {
			return userid;
		} else {
			throw new UnauthorizedException("No user id");
		}
	}

	private Admin getFacebookUser(String userid) {

		Admin admin;

		try {
			admin = adminRepository.findByUserId(userid);
		} catch (DataAccessException e) {
			throw new WebInternalErrorException("Internal error");
		}

		if (admin != null) {
			return admin;
		} else {
			throw new UnauthorizedException("Admin doest not exist");
		}
	}

	private String generateAccessToken(Admin admin) {
		String jwtToken;

		try {
			jwtToken = Jwts.builder().setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
					.claim("usn", admin.getUsername()).setExpiration(generateAccessTimestamp()).claim("adm", true)
					.signWith(SignatureAlgorithm.HS256, ReadProperty.readProperty("secret")).compact();

		} catch (IOException e) {
			throw new WebInternalErrorException("Internal error");
		}

		return jwtToken;
	}

	private Date generateAccessTimestamp() {

		LocalDateTime date = LocalDateTime.now().plusMinutes(EXPIRATION_TIME_ACCESS);

		return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	}
}
