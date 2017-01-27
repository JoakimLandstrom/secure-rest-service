package se.plushogskolan.restcaseservice.service;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import se.plushogskolan.restcaseservice.exception.NotFoundException;
import se.plushogskolan.restcaseservice.exception.UnauthorizedException;
import se.plushogskolan.restcaseservice.exception.WebInternalErrorException;
import se.plushogskolan.restcaseservice.model.AccessBean;
import se.plushogskolan.restcaseservice.model.Admin;
import se.plushogskolan.restcaseservice.repository.AdminRepository;

@Service
public class AdminService {

	private final long EXPIRATION_TIME_REFRESH = 7l;
	private final long EXPIRATION_TIME_ACCESS = 20l;
	private final int ITERATIONS = 10000;

	private AdminRepository adminRepository;

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
		// if (token != null) {
		// token = new String(token.substring("Bearer ".length()));
		// Admin admin;
		// try {
		// admin = adminRepository.findByRefreshToken(token);
		// } catch (DataAccessException e) {
		// throw new WebInternalErrorException("Internal error");
		// }
		// if (admin == null)
		// throw new UnauthorizedException("Token not found");
		// else if (admin.getTimestamp().isBefore(LocalDateTime.now())) {
		// throw new UnauthorizedException("Token has run out");
		// } else
		// return true;
		// } else
		// throw new UnauthorizedException("No authorization header found");
		//

		if (token != null) {

			token = new String(token.substring("Bearer ".length()));

			try {

				Jws<Claims> claims = Jwts.parser().require("adm", true).setSigningKey("fisk").parseClaimsJws(token);

			} catch (ExpiredJwtException e) {
				throw new UnauthorizedException("JWT has run out");
			} catch (JwtException e) {
				throw new UnauthorizedException("JWT could not be verified");
			}

			return true;
		} else {
			throw new UnauthorizedException("Authorization header not found or empty");
		}

	}

	public void updateTokenTimestamp(String token) {
		if (token != null) {
			token = new String(token.substring("Bearer ".length()));
			try {
				Admin admin = adminRepository.findByRefreshToken(token);
				admin.setTimestamp(generateRefreshTimestamp());
				adminRepository.save(admin);
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

	private String generateAccessToken(Admin admin) {

		String jwtToken = Jwts.builder().setHeaderParam("alg", "HS256").setHeaderParam("typ", "JWT")
				.claim("usn", admin.getUsername()).setExpiration(generateAccessTimestamp()).claim("adm", true)
				.signWith(SignatureAlgorithm.HS256, "fisk").compact();

		return jwtToken;
	}

	private String generateRefreshToken() {
		byte[] bytes = new byte[32];
		SecureRandom random = new SecureRandom();
		random.nextBytes(bytes);
		return new String(Base64.getEncoder().encode(bytes));
	}

	private LocalDateTime generateRefreshTimestamp() {

		return LocalDateTime.now().plusDays(EXPIRATION_TIME_REFRESH);
	}

	private Date generateAccessTimestamp() {

		LocalDateTime date = LocalDateTime.now().plusMinutes(EXPIRATION_TIME_ACCESS);

		return Date.from(date.toInstant(ZoneOffset.UTC));
	}
}
