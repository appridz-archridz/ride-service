package com.gorap.rideservice.util;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseModel<T> {

    private String statusCode;

    private String message;
    
    private boolean success; 

    private T data;
    
    
}
