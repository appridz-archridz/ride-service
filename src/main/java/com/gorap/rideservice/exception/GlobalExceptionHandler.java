package com.gorap.rideservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.gorap.rideservice.util.ResponseModel;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(RecordNotFoundException.class)
	public ResponseEntity<ResponseModel<String>> handleRecordNotFoundException(RecordNotFoundException ex) {
		String errorMessage = "Record not found: " + ex.getMessage();
		ResponseModel<String> responseModel = new ResponseModel<>();
		responseModel.setMessage(errorMessage);
		responseModel.setStatusCode(HttpStatus.NOT_FOUND.toString());
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(responseModel);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ResponseModel<String>> handleAllExceptions(Exception ex) {
		String errorMessage = "An unexpected error occurred: " + ex.getMessage();
		ResponseModel<String> responseModel = new ResponseModel<>();
		responseModel.setMessage(errorMessage);
		responseModel.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR.toString());
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(responseModel);
	}

}
