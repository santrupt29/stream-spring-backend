package com.stream.app.controllers;

import com.stream.app.entities.Users;
import com.stream.app.services.UserService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@CrossOrigin("*")
public class UserController {
    private final UserService service;


    @PostMapping("/register")
    public Users register(@RequestBody Users user) {
        return service.register(user);
    }


    @PostMapping("/login")
    public String login(@RequestBody Users user) {
        return service.verify(user);
    }
}
