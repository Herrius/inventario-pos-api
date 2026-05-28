package com.enrique.inventario.catalog;

import com.enrique.inventario.catalog.dto.CategoryRequest;
import com.enrique.inventario.catalog.dto.CategoryResponse;
import com.enrique.inventario.common.ConflictException;
import com.enrique.inventario.common.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> findAll() {
        return repository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse findById(Long id) {
        return toResponse(load(id));
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        if (repository.existsByNombre(request.nombre())) {
            throw new ConflictException("Ya existe una categoría con nombre '" + request.nombre() + "'.");
        }
        Category saved = repository.save(new Category(request.nombre()));
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = load(id);
        // Permitir mantener el mismo nombre, pero rechazar si otro registro lo usa.
        if (repository.existsByNombreAndIdNot(request.nombre(), id)) {
            throw new ConflictException("Ya existe otra categoría con nombre '" + request.nombre() + "'.");
        }
        category.setNombre(request.nombre());
        return toResponse(category);
    }

    @Transactional
    public void delete(Long id) {
        Category category = load(id);
        // El borrado puede fallar si la categoría tiene productos (ON DELETE RESTRICT
        // en la FK). Se traduce a 409 en el handler; aquí no hacemos pre-validación
        // explícita para no introducir una race condition.
        repository.delete(category);
    }

    private Category load(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada: id=" + id));
    }

    private CategoryResponse toResponse(Category c) {
        return new CategoryResponse(c.getId(), c.getNombre(), c.getCreatedAt());
    }
}
