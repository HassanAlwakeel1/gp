package com.example.Quizapp.services.impl;

import com.example.Quizapp.dto.*;
import com.example.Quizapp.entities.Role;
import com.example.Quizapp.entities.Token;
import com.example.Quizapp.entities.TokenType;
import com.example.Quizapp.entities.User;
import com.example.Quizapp.repository.TokenRepository;
import com.example.Quizapp.repository.UserRepository;
import com.example.Quizapp.services.AuthenticationService;
import com.example.Quizapp.services.JWTService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;

@Service
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    private final UserRepository userRepository;

    private final PasswordEncoder passwordEncoder;

    private final AuthenticationManager authenticationManager;

    private final JWTService jwtService;

    private final TokenRepository tokenRepository;

    public JwtAuthenticationResponse signup(SignUpRequest signUpRequest){
        User user = new User();
        user.setPhone(signUpRequest.getPhone());
        user.setUsername(signUpRequest.getUsername());
        user.setRole(signUpRequest.getRole());
        user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));

        var savedUser = userRepository.save(user);
        var jwtToken = jwtService.generateToken(user);
        var jwtRefreshToken = jwtService.generateRefreshToken(new HashMap<>(),user);
        saveUserToken(savedUser, jwtToken);

        return JwtAuthenticationResponse.builder()
                .token(jwtToken)
                .refreshToken(jwtRefreshToken)
                .build();
    }

    private void saveUserToken(User user, String jwtToken) {
        var token = Token.builder()
                .user(user)
                .token(jwtToken)
                .tokenType(TokenType.BEARER)
                .revoked(false)
                .expired(false)
                .build();
        tokenRepository.save(token);
    }

    public JwtAuthenticationResponse signin(SignInRequest signinRequest){
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(signinRequest.getUsername(),
                signinRequest.getPassword()));

        var user=userRepository.findByUsername(signinRequest.getUsername()).orElseThrow(()-> new IllegalArgumentException("Invalid Username or password"));

        var jwt = jwtService.generateToken(user);

        var refreshToken = jwtService.generateRefreshToken(new HashMap<>(), user);

        JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
        jwtAuthenticationResponse.setToken(jwt);
        jwtAuthenticationResponse.setRefreshToken(refreshToken);
        revokeAllUserTokens(user);
        saveUserToken(user, jwt);

        return jwtAuthenticationResponse;
    }

    public void revokeAllUserTokens(User user){
        var validUserTokens = tokenRepository.findAllValidTokensByUser(user.getId());
        if(validUserTokens.isEmpty())
            return;
        validUserTokens.forEach(
                token -> {
                    token.setExpired(true);
                    token.setRevoked(true);
                }
        );
        tokenRepository.saveAll(validUserTokens);
    }

    public JwtAuthenticationResponse refreshToken(RefreshTokenRequest refreshTokenRequest)  {
        String userEmail = jwtService.extractUserName(refreshTokenRequest.getToken());
        User user = userRepository.findByUsername(userEmail).orElseThrow();
        if(jwtService.isTokenValid(refreshTokenRequest.getToken(),user)){
            var jwt = jwtService.generateToken(user);

            JwtAuthenticationResponse jwtAuthenticationResponse = new JwtAuthenticationResponse();
            jwtAuthenticationResponse.setToken(jwt);
            jwtAuthenticationResponse.setRefreshToken(refreshTokenRequest.getToken());

            return jwtAuthenticationResponse;
        }
        return null;
    }
}
