package se.plushogskolan.restcaseservice.service;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.impl.crypto.MacProvider;
import se.plushogskolan.restcaseservice.exception.NotFoundException;
import se.plushogskolan.restcaseservice.exception.UnauthorizedException;
import se.plushogskolan.restcaseservice.exception.WebInternalErrorException;
import se.plushogskolan.restcaseservice.model.AccessBean;
import se.plushogskolan.restcaseservice.model.Admin;
import se.plushogskolan.restcaseservice.repository.AdminRepository;

@Service
public class AdminService {

	private final long EXPIRATION_TIME = 300;
	private final int ITERATIONS = 10000;
	
	private AdminRepository adminRepository;
	
	private Key key = MacProvider.generateKey();

	@Autowired
	public AdminService(AdminRepository adminRepository) {
		this.adminRepository = adminRepository;
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
				String token = generateToken(admin);
				admin.setToken(token);
				admin.setTimestamp(generateTimestamp());
				admin = adminRepository.save(admin);
				return new AccessBean(admin.getToken(), admin.getTimestamp().toString());
			} else
				throw new UnauthorizedException("Invalid login");
		} else
			throw new NotFoundException("User does not exist");
	}

	public boolean authenticateToken(String token) {
		if (token != null) {
			token = new String(token.substring("Bearer ".length()));
			Admin admin;
			try {
				admin = adminRepository.findByToken(token);
			} catch (DataAccessException e) {
				throw new WebInternalErrorException("Internal error");
			}
			if (admin == null)
				throw new UnauthorizedException("Token not found");
			else if (admin.getTimestamp().isBefore(LocalDateTime.now())) {
				throw new UnauthorizedException("Token has run out");
			} else
				return true;
		} else
			throw new UnauthorizedException("No authorization header found");
	}

	public void updateTokenTimestamp(String token) {
		if (token != null) {
			token = new String(token.substring("Bearer".length()));
			try {
				Admin admin = adminRepository.findByToken(token);
				adminRepository.updateTimestampById(admin.getId(), generateTimestamp());
			} catch (DataAccessException e) {
				throw new WebInternalErrorException("Internal error");
			}
		}
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

	private String generateToken(Admin admin) {

		String jwtToken = Jwts.builder().setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
				.claim("usn", admin.getUsername()).claim("exp", getDateFromLocalDate(admin.getTimestamp()))
				.claim("adm", true).signWith(SignatureAlgorithm.HS256, key).compact();

		return jwtToken;
	}

	private LocalDateTime generateTimestamp() {
		return LocalDateTime.now().plusSeconds(EXPIRATION_TIME);
	}

	private Date getDateFromLocalDate(LocalDateTime dateTime) {

		Instant instant = dateTime.atZone(ZoneId.systemDefault()).toInstant();

		return Date.from(instant);
	}

}
