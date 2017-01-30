package se.plushogskolan.restcaseservice.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.ext.Provider;

import org.springframework.beans.factory.annotation.Autowired;

import se.plushogskolan.restcaseservice.exception.UnauthorizedException;
import se.plushogskolan.restcaseservice.service.AdminService;

@Provider
public final class RequestFilter implements ContainerRequestFilter {

	@Autowired
	private AdminService adminService;

	@Override
	public void filter(ContainerRequestContext requestContext) {
		
		String token = requestContext.getHeaderString("Authorization");
		String resource = requestContext.getUriInfo().getRequestUri().getRawPath().substring(0, "/login".length());
		
		if ("/login".equals(resource)) {} 
		else if(adminService.authenticateToken(token)) {}
		else
			throw new UnauthorizedException("Unauthorized");
	}
}
