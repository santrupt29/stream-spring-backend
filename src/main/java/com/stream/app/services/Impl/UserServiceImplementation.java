package com.stream.app.services.Impl;

import com.stream.app.entities.Users;
import com.stream.app.repositories.UserRepo;
import com.stream.app.services.JWTService;
import com.stream.app.services.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImplementation implements UserService {

    private UserRepo repo;
    AuthenticationManager authManager;
    private JWTService jwtService;
    public UserServiceImplementation(UserRepo repo, AuthenticationManager authManager, JWTService jwtService) {
        this.authManager = authManager;
        this.repo = repo;
        this.jwtService = jwtService;
    }

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public Users register(Users user) {
        user.setPassword(encoder.encode(user.getPassword()));
        return repo.save(user);
    }

    @Override
    public String verify(Users user) {
        Authentication authentication = authManager.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));
        if(authentication.isAuthenticated()) return jwtService.generateToken(user.getUsername());
        return "Failure";
    }
}
