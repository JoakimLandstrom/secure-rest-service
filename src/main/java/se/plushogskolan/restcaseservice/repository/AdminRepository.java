package se.plushogskolan.restcaseservice.repository;

import org.springframework.data.repository.PagingAndSortingRepository;

import se.plushogskolan.restcaseservice.model.Admin;

public interface AdminRepository extends PagingAndSortingRepository<Admin, Long> {

	public Admin findByUsername(String username);
	
	public Admin findByToken(String token);
		
}
