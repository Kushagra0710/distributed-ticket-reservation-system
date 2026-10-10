package com.ticketing.ticket_reservation.service.impl;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ticketing.ticket_reservation.constants.ApplicationConstants;
import com.ticketing.ticket_reservation.dto.AuthRequest;
import com.ticketing.ticket_reservation.dto.AuthResponse;
import com.ticketing.ticket_reservation.dto.GoogleAuthRequest;
import com.ticketing.ticket_reservation.dto.RegisterRequest;
import com.ticketing.ticket_reservation.entity.AuthProvider;
import com.ticketing.ticket_reservation.entity.Role;
import com.ticketing.ticket_reservation.entity.User;
import com.ticketing.ticket_reservation.repository.UserRepository;
import com.ticketing.ticket_reservation.security.JwtUtils;
import com.ticketing.ticket_reservation.exception.EmailAlreadyRegisteredException;
import com.ticketing.ticket_reservation.service.interfaces.AuthService;

import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AuthServiceImpl implements AuthService{
    
    @Value ("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override 
    @Transactional 
    public void register(RegisterRequest request){
        String email = request.getEmail().trim();
        if(userRepository.existsByEmail(email)){
            throw new EmailAlreadyRegisteredException("Email is already registered");
        }

        User user = User.builder().fullName(request.getFullName().trim())
                    .email(email)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.USER)
                    .provider(AuthProvider.LOCAL)
                    .build();

        userRepository.save(user);
    }

    @Override 
    @Transactional (readOnly = true)
    public AuthResponse login(AuthRequest request){
        User user = userRepository.findByEmail(request.getEmail().trim())
                    .orElseThrow(()-> new BadCredentialsException("Invalid email or password"));
        if (!user.isActive()) {
            throw new BadCredentialsException("This account is disabled.");
        }

        //Guard against Google users trying to login with a local password
        if(user.getProvider()==AuthProvider.GOOGLE){
            throw new BadCredentialsException("This account uses Google Sign-In. Please log in with Google.");
        }

        if(!passwordEncoder.matches(request.getPassword(),user.getPassword())){
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getEmail(),user.getRole().name());

        return AuthResponse.builder()
            .token(token)
                .userName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    @Override 
    @Transactional 
    public AuthResponse googleLogin(GoogleAuthRequest request){
        try{
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(),new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();
            
            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if(idToken == null){
                throw new BadCredentialsException("Invalid or expired Google ID token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // Find the user or automatically register them if its their first time logging in 
            User user = userRepository.findByEmail(email).orElseGet(()->{
                User newUser = User.builder()
                        .fullName(name!=null? name: "Google User")
                        .email(email)
                        .provider(AuthProvider.GOOGLE)
                        .role(Role.USER)
                        .build();

                    return userRepository.save(newUser);
            });

            if (!user.isActive()) {
                throw new BadCredentialsException("This account is disabled.");
            }

            if(user.getProvider()==AuthProvider.LOCAL){
                throw new EmailAlreadyRegisteredException("Email already registered. Please log in with your password.");
            }

            String jwt = jwtUtils.generateToken(user.getEmail(),user.getRole().name());
            return AuthResponse.builder()
                        .token(jwt)
                        .userName(user.getFullName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .build();

        }catch(BadCredentialsException | EmailAlreadyRegisteredException e){
            throw e;
        }catch(IllegalArgumentException e){
            throw e;
        }catch(Exception e){
            throw new RuntimeException("Google authentication verification failed",e);
        }
    }
}
