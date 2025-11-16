package com.gorap.rideservice.auth;

import com.gorap.rideservice.entity.User;
import com.gorap.rideservice.constants.Role;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;

public class UserPrincipal implements UserDetails {
	private static final long serialVersionUID = 1L;

	private UUID id;
	private String userName;
	private String email;
	private String phoneNumber;
	private String address;
	private String profilePic;

	@JsonIgnore
	private String password;

	private Collection<? extends GrantedAuthority> authorities;

	public UserPrincipal(UUID id, String userName, String email, String phoneNumber, String address, String password,
			Collection<? extends GrantedAuthority> authorities, String profilePic) {
		this.id = id;
		this.userName = userName;
		this.email = email;
		this.phoneNumber = phoneNumber;
		this.address = address;
		this.password = password;
		this.authorities = authorities;
		this.profilePic = profilePic;
	}

	/**
	 * Factory method to create UserPrincipal from your User entity
	 */
	public static UserPrincipal create(User user) {
		// Convert single Role enum to Spring Security authority
		// Your Role.USER becomes "ROLE_USER" as expected by Spring Security
		GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + user.getRole().name());

		return new UserPrincipal(user.getId(), // UUID id
				user.getUserName(), // userName (your field name)
				user.getEmail(), // email
				user.getPhoneNumber(), // phoneNumber
				user.getAddress(), // address
				user.getPassword(), // password
				Collections.singletonList(authority), // Single role as list
				user.getProfilePic()
		);
	}

	public UUID getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public String getAddress() {
		return address;
	}

	public Role getUserRole() {
		// Extract role from authority (remove "ROLE_" prefix)
		String roleName = authorities.iterator().next().getAuthority().substring(5);
		return Role.valueOf(roleName);
	}

	@Override
	public String getUsername() {
		return userName;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}
	
	public String getProfilePic() {
		return profilePic;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true; // You can add logic based on your requirements
	}

	@Override
	public boolean isAccountNonLocked() {
		return true; // You can add logic based on your requirements
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true; // You can add logic based on your requirements
	}

	@Override
	public boolean isEnabled() {
		return true; // You can add logic based on your requirements
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		UserPrincipal that = (UserPrincipal) o;
		return Objects.equals(id, that.id);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id);
	}
}