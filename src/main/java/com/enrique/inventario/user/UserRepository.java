package com.enrique.inventario.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data deriva la query del nombre del método. Útil para login (buscar
    // por email) y para validar en el registro que el email no exista ya.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
