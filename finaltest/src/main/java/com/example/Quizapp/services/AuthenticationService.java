package com.example.Quizapp.services;

import com.example.Quizapp.dto.*;
import com.example.Quizapp.entities.User;

public interface AuthenticationService {
    JwtAuthenticationResponse signup(SignUpRequest signUpRequest);

    JwtAuthenticationResponse signin(SignInRequest signinRequest);

    JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest);
}
