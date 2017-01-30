package se.plushogskolan.restcaseservice.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
	public Response authenticate(AuthBean authBean){
		
		String access_token = "";
		
		if(authBean.getFacebook_token() != null)
			access_token =  adminService.authenticateFacebookUser(authBean.getFacebook_token());
		
		AccessBean accessBean = new AccessBean(access_token);
		
		return Response.ok(accessBean).build();
	}
}
