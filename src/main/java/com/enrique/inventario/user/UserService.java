package com.enrique.inventario.user;

import com.enrique.inventario.user.dto.RegisterRequest;
import com.enrique.inventario.user.dto.UserResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Inyección por constructor: dependencias explícitas y fáciles de testear.
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        // Regla de negocio: no permitir emails duplicados.
        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }
        // Por defecto, los usuarios registrados desde la API pública son CAJERO.
        // Para crear un ADMIN se promueve manualmente en la base (ver DEPLOY.md).
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()), // guardamos el HASH, nunca el texto plano
                Role.CAJERO
        );
        User saved = userRepository.save(user);
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException(email));
        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
    }
}
