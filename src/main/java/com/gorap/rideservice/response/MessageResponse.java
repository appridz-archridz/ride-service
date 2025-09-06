package com.gorap.rideservice.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MessageResponse {
    
    private String message;
    private boolean success;
    
    public MessageResponse(String message) {
        this.message = message;
        this.success = true;
    }
    
    public MessageResponse(String message, boolean success) {
        this.message = message;
        this.success = success;
    }
}
