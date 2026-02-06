package com.stream.app.services.Impl;

import com.stream.app.entities.Users;
import com.stream.app.repositories.UserRepo;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
public class MyUserDetailsServiceImplementation implements UserDetailsService {

    UserRepo repo;

    public MyUserDetailsServiceImplementation(UserRepo repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {
        return repo.findByEmail(email) // Better to return an Optional<Users>, prevents NullPointerException
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }
}










//        Users user = repo.findByEmail(email);
//        if(user == null) {
//            System.out.println("User not found");
//            throw new UsernameNotFoundException("User not found");
//        }
//        return user;
