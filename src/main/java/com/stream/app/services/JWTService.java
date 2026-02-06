package com.stream.app.services;

import org.springframework.security.core.userdetails.UserDetails;

import java.util.HashMap;
import java.util.Map;

public interface JWTService {

    public String generateToken(String email);

    public String extractEmail(String token);

    public boolean validateToken(String token, UserDetails userDetails);
}
