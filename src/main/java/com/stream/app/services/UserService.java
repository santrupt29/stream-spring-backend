package com.stream.app.services;

import com.stream.app.entities.Users;

public interface UserService {

    public Users register(Users user);

    public String verify(Users user);
}
