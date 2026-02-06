package com.stream.app.services.Impl;

import com.stream.app.entities.Users;
import com.stream.app.repositories.UserRepo;
import com.stream.app.services.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImplementation implements UserService {

    private UserRepo repo;
    AuthenticationManager authManager;
    public UserServiceImplementation(UserRepo repo, AuthenticationManager authManager) {
        this.authManager = authManager;
        this.repo = repo;
    }






    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    @Override
    public Users register(Users user) {
        user.setPassword(encoder.encode(user.getPassword()));
        return repo.save(user);
    }

    @Override
    public String verify(Users user) {
        return "";
    }
}
