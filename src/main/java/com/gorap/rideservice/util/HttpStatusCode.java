package com.gorap.rideservice.util;

import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpStatus;

import lombok.Builder;

@Configuration
@Builder
public class HttpStatusCode {

	public HttpStatus getHttpStatusFromCode(String statusCode) {
		switch (statusCode) {
		case "200 OK":
			return HttpStatus.OK;
		case "201 CREATED":
			return HttpStatus.CREATED;
		case "400 BAD_REQUEST":
			return HttpStatus.BAD_REQUEST;
		case "401 UNAUTHORIZED":
			return HttpStatus.UNAUTHORIZED;
		case "403 FORBIDDEN":
			return HttpStatus.FORBIDDEN;
		case "404 NOT_FOUND":
			return HttpStatus.NOT_FOUND;
		case "409 CONFLICT":
			return HttpStatus.CONFLICT;
		case "500 INTERNAL_SERVER_ERROR":
			return HttpStatus.INTERNAL_SERVER_ERROR;
		default:
			return HttpStatus.OK;
		}
	}
}
