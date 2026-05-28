package com.enrique.inventario.catalog;

import com.enrique.inventario.catalog.dto.ProductRequest;
import com.enrique.inventario.catalog.dto.ProductResponse;
import com.enrique.inventario.common.ConflictException;
import com.enrique.inventario.common.NotFoundException;
import com.enrique.inventario.common.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(Long categoryId, String q, Pageable pageable) {
        // El repo espera "" para "sin filtro" (ver Javadoc en ProductRepository.search).
        String qSafe = (q == null) ? "" : q;
        return PageResponse.from(productRepository.search(categoryId, qSafe, pageable).map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: id=" + id));
        return toResponse(product);
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ConflictException("Ya existe un producto con sku '" + request.sku() + "'.");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada: id=" + request.categoryId()));
        Product product = new Product(
                request.sku(), request.nombre(), request.precio(), category, request.minStock());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = productRepository.findByIdWithCategory(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: id=" + id));
        if (productRepository.existsBySkuAndIdNot(request.sku(), id)) {
            throw new ConflictException("Ya existe otro producto con sku '" + request.sku() + "'.");
        }
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new NotFoundException("Categoría no encontrada: id=" + request.categoryId()));
        product.setSku(request.sku());
        product.setNombre(request.nombre());
        product.setPrecio(request.precio());
        product.setCategory(category);
        if (request.minStock() != null) product.setMinStock(request.minStock());
        return toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Producto no encontrado: id=" + id));
        // El borrado puede fallar si el producto tiene movimientos/ventas (FK
        // restrictivas en M3/M4). Hoy aún no existen esas tablas; se irá
        // endureciendo a medida que se agreguen las relaciones.
        productRepository.delete(product);
    }

    private ProductResponse toResponse(Product p) {
        Category c = p.getCategory();
        return new ProductResponse(
                p.getId(), p.getSku(), p.getNombre(), p.getPrecio(),
                c.getId(), c.getNombre(),
                p.getStockActual(), p.getMinStock(), p.getCreatedAt());
    }
}
