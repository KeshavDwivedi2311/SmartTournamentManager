package com.sports.SmartSport.user.DTO;

import lombok.Data;

@Data
public class SignUpRequest {
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String fullName;
}
