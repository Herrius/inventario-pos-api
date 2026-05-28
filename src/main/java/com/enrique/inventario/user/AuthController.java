package com.enrique.inventario.user;

import com.enrique.inventario.user.dto.LoginRequest;
import com.enrique.inventario.user.dto.LoginResponse;
import com.enrique.inventario.user.dto.RegisterRequest;
import com.enrique.inventario.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    public AuthController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    // El controller solo recibe y responde; la lógica está en el service.
    // @Valid dispara la validación del DTO; 201 Created es el código correcto al crear.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    // 200 OK con el token (el login no crea un recurso, por eso no es 201).
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
