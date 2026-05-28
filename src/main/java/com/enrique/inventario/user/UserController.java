package com.enrique.inventario.user;

import com.enrique.inventario.user.dto.UserResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    // Perfil del usuario dueño del token. El "sub" del JWT es su email.
    // @PreAuthorize demuestra la autorización por rol: requiere que la authority
    // ROLE_CLIENTE/ROLE_ADMIN (mapeada desde el claim "role") esté presente.
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CLIENTE', 'ADMIN')")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getProfile(jwt.getSubject());
    }
}
