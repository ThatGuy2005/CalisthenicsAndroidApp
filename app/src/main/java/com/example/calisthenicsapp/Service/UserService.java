package com.example.calisthenicsapp.Service;

import com.example.calisthenicsapp.CoreComponents.User;
import com.example.calisthenicsapp.Repos.UserRepository;
import com.example.calisthenicsapp.Verificate.UserValidator;

import at.favre.lib.crypto.bcrypt.BCrypt;

public class UserService {
    public UserService(UserRepository repo) {
        this.userRepository = repo;
    }
    public boolean login(String username, String password) {
        User user = this.userRepository.getOne(username);

        if(user == null)
            return false;

        boolean ok = BCrypt.verifyer()
                .verify(password.toCharArray(), user.getPasswordHash()).verified;

        if(ok) {

            currentUser = user;
            return true;

        } else {

          return false;

        }
    }

    public boolean signUp(String username, String email, String password,
                    Double height, Double weight, String gender, String mode) {
        if(!UserValidator.isValidUsername(username)) return false;
        if(!UserValidator.isValidEmail(email)) return false;
        if(!UserValidator.isValidPassword(password)) return false;
        String hashedPassword = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        return this.userRepository.insert(new User(
                username,email,hashedPassword,height,weight,gender,mode
        ));
    }

    UserRepository userRepository;
    User currentUser;
}
