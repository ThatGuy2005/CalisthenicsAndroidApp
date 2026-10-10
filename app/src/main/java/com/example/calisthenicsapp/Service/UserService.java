package com.example.calisthenicsapp.Service;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.UserRepository;

public class UserService {
    public UserService(UserRepository repo) {
        this.userRepository = repo;
    }
    public boolean login(String username, String passwordHash) {
        User user = this.userRepository.getOne(username);
        if(user == null)
            return false;
        if(user.getPasswordHash().equals(passwordHash)) {
            currentUser = user;
            return true;
        } else {
          return false;
        }
    }
    UserRepository userRepository;
    User currentUser;
}
