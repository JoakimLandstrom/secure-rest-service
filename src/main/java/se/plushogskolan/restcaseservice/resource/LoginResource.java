package se.plushogskolan.restcaseservice.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import se.plushogskolan.restcaseservice.exception.UnauthorizedException;
import se.plushogskolan.restcaseservice.model.AccessBean;
import se.plushogskolan.restcaseservice.model.AuthBean;
import se.plushogskolan.restcaseservice.service.AdminService;

@Component
@Path("login")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class LoginResource {
	
	@Autowired
	private AdminService adminService;
	
	@POST
	public Response login(AuthBean credentials){
		
		if(credentials.getPassword() == null || credentials.getUsername() == null)
			throw new UnauthorizedException("Missing username or password");
		
		AccessBean accessBean = adminService.login(credentials.getUsername(), credentials.getPassword());
		
		return Response.ok(accessBean).build();
	}
	
	@POST
	@Path("refresh")
	public Response getNewAccessToken(AuthBean authBean, @HeaderParam("Authorization") String access_token){
		
		String refresh_token = authBean.getRefresh_token();
		
		if(refresh_token == null)
			throw new UnauthorizedException("Missing refresh token");
		
		String accessToken = adminService.generateNewAccessToken(access_token, refresh_token);
		
		AccessBean accessBean = new AccessBean(accessToken, refresh_token);
		
		return Response.ok(accessBean).build();
	}
	
	//just to create an admin
	@POST
	@Path("new")
	public Response createAdmin(AuthBean credentials){
		
		if(credentials.getPassword() == null || credentials.getUsername() == null)
			throw new UnauthorizedException("Missing username or password");
		
		adminService.save(credentials.getUsername(), credentials.getPassword());
		
		return Response.ok().build();
	}

}
