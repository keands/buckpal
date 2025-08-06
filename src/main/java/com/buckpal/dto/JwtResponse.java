package com.buckpal.dto;

public class JwtResponse {
    
    private String token;
    private String type = "Bearer";
    private String email;
    private String firstName;
    private String lastName;
    
    public JwtResponse(String accessToken, String email, String firstName, String lastName) {
        this.token = accessToken;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }
    
    public String getAccessToken() { return token; }
    public void setAccessToken(String accessToken) { this.token = accessToken; }
    
    public String getTokenType() { return type; }
    public void setTokenType(String tokenType) { this.type = tokenType; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}