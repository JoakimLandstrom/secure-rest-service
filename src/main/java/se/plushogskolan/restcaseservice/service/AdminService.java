package se.plushogskolan.restcaseservice.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

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
import se.plushogskolan.restcaseservice.repository.AdminRepository;
import se.plushogskolan.restcaseservice.repository.FacebookApi;

@Service
public class AdminService {

	private final long EXPIRATION_TIME_ACCESS = 20;
	private final int ITERATIONS = 10000;

	private AdminRepository adminRepository;

	private FacebookApi facebookApi;

	@Autowired
	public AdminService(AdminRepository adminRepository, FacebookApi facebookApi) {
		this.adminRepository = adminRepository;
		this.facebookApi = facebookApi;
	}

	public Admin save(String username, String password) {
		Admin admin = createAdmin(username, password);
		try {
			return adminRepository.save(admin);
		} catch (DataAccessException e) {
			throw new WebInternalErrorException("Could not save admin");
		}
	}

	public boolean authenticateToken(String token) {

		if (token != null) {

			token = new String(token.substring("Bearer ".length()));

			try {

				Jwts.parser().require("adm", true).setSigningKey(getSecret()).parseClaimsJws(token);

			} catch (ExpiredJwtException e) {
				throw new UnauthorizedException("Access token has run out");
			} catch (JwtException e) {
				throw new UnauthorizedException("Access token could not be verified");
			}

			return true;
		} else {
			throw new UnauthorizedException("Authorization header not found or empty");
		}
	}

	public String authenticateFacebookUser(String facebookToken) {

		String response;
		JSONObject jsonObject;

		try {

			response = facebookApi.getUser(facebookToken);

			jsonObject = new JSONObject(response);

		} catch (IOException | JSONException e) {
			throw new WebInternalErrorException("Internal error");
		}

		Admin admin = isUserFacebookAuthenticated(jsonObject);

		admin.setToken(facebookToken);

		adminRepository.save(admin);

		return generateAccessToken(admin);
	}

	private Admin isUserFacebookAuthenticated(JSONObject jsonObject) {

		Admin admin;

		if (jsonObjectContainsAdmin(jsonObject)) {

			try {
				admin = adminRepository.findByUsername(jsonObject.getString("name"));
			} catch (DataAccessException e) {
				throw new WebInternalErrorException("Internal error");
			}
		} else {
			throw new UnauthorizedException("Facebook token could not be verified");
		}

		if (admin != null) {
			return admin;
		} else {
			throw new UnauthorizedException("Admin doest not exist");
		}
	}

	private boolean jsonObjectContainsAdmin(JSONObject jsonObject) {

		try {
			jsonObject.getString("name");
		} catch (JSONException e) {
			throw new UnauthorizedException(jsonObject.getJSONObject("error").getString("message"));
		}

		return true;
	}

	private Admin createAdmin(String username, String password) {
		byte[] salt = generateSalt(password);
		byte[] hash = generateHash(password, salt);
		return new Admin(hash, username, salt);
	}

	private byte[] generateSalt(String password) {
		byte[] bytes = new byte[32 - password.length()];
		SecureRandom random = new SecureRandom();
		random.nextBytes(bytes);
		return Base64.getEncoder().encode(bytes);
	}

	private byte[] generateHash(String arg, byte[] salt) {
		byte[] hashToReturn = null;
		char[] password = arg.toCharArray();
		PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, 256);
		SecretKeyFactory factory;
		try {
			factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512");
			hashToReturn = factory.generateSecret(spec).getEncoded();
		} catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
			throw new WebInternalErrorException("Internal error");
		}

		return hashToReturn;
	}

	private String generateAccessToken(Admin admin) {

		String jwtToken = Jwts.builder().setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
				.claim("usn", admin.getUsername()).setExpiration(generateAccessTimestamp()).claim("adm", true)
				.signWith(SignatureAlgorithm.HS256, getSecret()).compact();

		return jwtToken;
	}

	private Date generateAccessTimestamp() {

		LocalDateTime date = LocalDateTime.now().plusMinutes(EXPIRATION_TIME_ACCESS);

		return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	}

	private String getSecret() {

		Properties prop = new Properties();
		InputStream input = null;

		try {

			input = new FileInputStream("src/main/resources/application.properties");

			prop.load(input);

			String property = prop.getProperty("secret");

			return property;

		} catch (IOException e) {
			throw new WebInternalErrorException("Internal error");
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					throw new WebInternalErrorException("Internal error");
				}
			}
		}

	}

}
