package com.example.Quizapp.dto;

import com.example.Quizapp.entities.Role;
import lombok.Data;

@Data
public class SignUpRequest {

    private String phone;

    private String username;

    private String password;

    private Role role;
}
