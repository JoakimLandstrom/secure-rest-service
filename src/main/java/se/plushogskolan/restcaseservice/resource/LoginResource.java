package se.plushogskolan.restcaseservice.resource;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import se.plushogskolan.restcaseservice.model.AccessBean;
import se.plushogskolan.restcaseservice.model.CreateAdminBean;
import se.plushogskolan.restcaseservice.service.FacebookAdminService;

@Component
@Path("login/facebook")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public final class LoginResource {

	@Autowired
	private FacebookAdminService adminService;

	@GET
	public Response authenticateFacebookUser(@QueryParam("code") String facebookAccesstoken) {

		// sent from
		// https://www.facebook.com/v2.8/dialog/oauth?client_id=1733194800342742&redirect_uri=http://localhost:8080/login/facebook

		AccessBean accessBean = new AccessBean(adminService.authenticateFacebookUser(facebookAccesstoken));

		return Response.ok(accessBean).build();
	}
	
	@GET
	@Path("refresh")
	public Response refreshToken(@HeaderParam("Authorization") String authorization){
	
		AccessBean accessBean = new AccessBean(adminService.refreshToken(authorization));
		
		return Response.ok(accessBean).build();
	}

	@POST
	public Response createAdmin(CreateAdminBean adminBean) {

		adminService.createAdmin(adminBean.getFacebook_id(), adminBean.getUsername());

		return Response.ok().build();
	}
}
