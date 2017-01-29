package se.plushogskolan.restcaseservice.service;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Properties;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import se.plushogskolan.restcaseservice.exception.NotFoundException;
import se.plushogskolan.restcaseservice.exception.UnauthorizedException;
import se.plushogskolan.restcaseservice.exception.WebInternalErrorException;
import se.plushogskolan.restcaseservice.model.AccessBean;
import se.plushogskolan.restcaseservice.model.Admin;
import se.plushogskolan.restcaseservice.repository.AdminRepository;
import se.plushogskolan.restcaseservice.repository.FacebookApi;
import se.plushogskolan.restcaseservice.repository.SocialMedia;

@Service
public class AdminService {

	private final long EXPIRATION_TIME_REFRESH = 7;
	private final long EXPIRATION_TIME_ACCESS = 20;
	private final int ITERATIONS = 10000;

	private AdminRepository adminRepository;

	private SocialMedia socialMediaApi;

	@Autowired
	public AdminService(AdminRepository adminRepository, SocialMedia socialMedia) {
		this.adminRepository = adminRepository;
		this.socialMediaApi = socialMedia;
	}

	public Admin save(String username, String password) {
		Admin admin = createAdmin(username, password);
		try {
			return adminRepository.save(admin);
		} catch (DataAccessException e) {
			throw new WebInternalErrorException("Could not save admin");
		}
	}

	public AccessBean login(String username, String password) {
		Admin admin;
		try {
			admin = adminRepository.findByUsername(username);
		} catch (DataAccessException e) {
			throw new WebInternalErrorException("Internal error");
		}
		if (admin != null) {
			if (authenticateLogin(admin, password)) {

				admin.setRefreshToken(generateRefreshToken());
				admin.setTimestamp(generateRefreshTimestamp());
				admin = adminRepository.save(admin);

				return new AccessBean(generateAccessToken(admin), admin.getRefreshToken());
			} else
				throw new UnauthorizedException("Invalid login");
		} else
			throw new NotFoundException("User does not exist");
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

	public String authenticateFacebookToken(String socialMediaToken) {

		String response;
		JSONObject jsonObject;

		try {

			response = socialMediaApi.getUser(socialMediaToken);

			jsonObject = new JSONObject(response);

		} catch (IOException e) {
			throw new WebInternalErrorException("Internal error");
		}

		return generateAccessToken(isUserFacebookAuthenticated(jsonObject));

	}

	public String generateNewAccessToken(String accessToken, String refreshToken) {

		if (accessToken != null) {

			Admin admin = findAdminByRefreshToken(refreshToken);

			accessToken = new String(accessToken.substring("Bearer ".length()));

			try {

				Jwts.parser().require("adm", true).setSigningKey(getSecret()).parseClaimsJws(accessToken);

			} catch (ExpiredJwtException e) {

				return generateAccessToken(admin);

			} catch (JwtException e) {
				throw new UnauthorizedException("Access token could not be verified");
			}

			return generateAccessToken(admin);

		} else {
			throw new UnauthorizedException("Authorization header not found or empty");
		}
	}

	private Admin isUserFacebookAuthenticated(JSONObject jsonObject) {

		if (jsonObject.get("error") != null) {

			JSONObject errorObject = jsonObject.getJSONObject("error");

			throw new UnauthorizedException(errorObject.getString("message"));
		}

		if (jsonObject.getBoolean("verified")) {
			Admin admin;

			try {

				admin = adminRepository.findByUsername(jsonObject.getString("username"));

			} catch (DataAccessException e) {
				throw new WebInternalErrorException("Internal error");
			}

			if (admin != null) {
				return admin;
			} else {
				throw new NotFoundException("Admin could not be found");
			}

		} else
			throw new UnauthorizedException("Facebook token could not be verified");

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

	private boolean authenticateLogin(Admin admin, String password) {
		return Arrays.equals(generateHash(password, admin.getSalt()), admin.getHashedPassword());
	}

	private String generateAccessToken(Admin admin) {

		String jwtToken = Jwts.builder().setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
				.claim("usn", admin.getUsername()).setExpiration(generateAccessTimestamp()).claim("adm", true)
				.signWith(SignatureAlgorithm.HS256, getSecret()).compact();

		return jwtToken;
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[32];
		SecureRandom random = new SecureRandom();
		random.nextBytes(bytes);
		return new String(Base64.getEncoder().encode(bytes));
	}

	private Date generateRefreshTimestamp() {

		LocalDateTime date = LocalDateTime.now().plusDays(EXPIRATION_TIME_REFRESH);

		return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	}

	private Date generateAccessTimestamp() {

		LocalDateTime date = LocalDateTime.now().plusMinutes(EXPIRATION_TIME_ACCESS);

		return Date.from(date.atZone(ZoneId.systemDefault()).toInstant());
	}

	private Admin findAdminByRefreshToken(String refreshToken) {
		try {

			Admin admin = adminRepository.findByRefreshToken(refreshToken);

			if (admin != null) {
				return admin;
			} else {
				throw new NotFoundException("Admin could not be found");
			}
		} catch (DataAccessException e) {
			throw new NotFoundException("Admin could not be found");
		}
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
