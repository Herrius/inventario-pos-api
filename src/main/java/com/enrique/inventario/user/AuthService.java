package com.enrique.inventario.user;

import com.enrique.inventario.security.TokenService;
import com.enrique.inventario.user.dto.LoginRequest;
import com.enrique.inventario.user.dto.LoginResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        // Mismo error tanto si el email no existe como si la clave falla: no
        // revelar cuáles emails están registrados (evita enumeración de usuarios).
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        String token = tokenService.issue(user.getEmail(), user.getRole().name());
        return new LoginResponse(token, "Bearer", tokenService.getExpirySeconds());
    }
}
