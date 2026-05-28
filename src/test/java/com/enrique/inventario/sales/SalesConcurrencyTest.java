package com.enrique.inventario.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.enrique.inventario.TestcontainersConfiguration;
import com.enrique.inventario.catalog.Category;
import com.enrique.inventario.catalog.CategoryRepository;
import com.enrique.inventario.catalog.Product;
import com.enrique.inventario.catalog.ProductRepository;
import com.enrique.inventario.inventory.StockMovementRepository;
import com.enrique.inventario.inventory.StockMovementService;
import com.enrique.inventario.inventory.StockMovementType;
import com.enrique.inventario.user.Role;
import com.enrique.inventario.user.User;
import com.enrique.inventario.user.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * EL test de portafolio: N cajeros que venden la última unidad de stock al
 * mismo tiempo. Solo una venta debe tener éxito; las demás deben fallar con 409.
 *
 * Esto demuestra que {@code @Version} (optimistic locking) + el handler global
 * funcionan en condiciones reales. Sin esta protección habría sobreventa
 * (negative stock o entries duplicadas → caos contable en producción).
 *
 * Defensible en entrevista: corre un Postgres real (Testcontainers), levanta
 * el contexto Spring completo (filter chain, JWT, JPA) y dispara N requests
 * concurrentes contra HTTP local — no mocks.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
class SalesConcurrencyTest {

    private static final int CONCURRENT_REQUESTS = 10;

    @LocalServerPort int port;

    @Autowired CategoryRepository categoryRepository;
    @Autowired ProductRepository productRepository;
    @Autowired UserRepository userRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired SaleRepository saleRepository;
    @Autowired StockMovementRepository movementRepository;
    @Autowired StockMovementService stockMovementService;
    @Autowired JdbcTemplate jdbc;

    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void tenParallelSalesOfLastUnit_oneWinsRestFail() throws Exception {
        // Reset del estado heredado del contexto compartido entre tests.
        jdbc.execute("TRUNCATE stock_movement, sale_items, sales, products, categories RESTART IDENTITY CASCADE");
        jdbc.execute("DELETE FROM users WHERE email IN ('cajero-conc@test.com','admin-conc@test.com')");

        Category cat = categoryRepository.save(new Category("Test"));
        Product product = productRepository.save(
                new Product("CONC-001", "Concurrency Test", new BigDecimal("10.00"), cat, 0));

        // Admin para la entrada de stock.
        userRepository.save(new User("admin-conc@test.com", passwordEncoder.encode("admin"), Role.ADMIN));
        stockMovementService.appendMovement(
                product.getId(), StockMovementType.ENTRADA, 1,
                "Stock para test concurrencia", "admin-conc@test.com", null);

        userRepository.save(new User("cajero-conc@test.com", passwordEncoder.encode("pass"), Role.CAJERO));

        // Sanity check: stock = 1
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStockActual()).isEqualTo(1);

        String token = login("cajero-conc@test.com", "pass");

        ExecutorService exec = Executors.newFixedThreadPool(CONCURRENT_REQUESTS);
        CountDownLatch ready = new CountDownLatch(CONCURRENT_REQUESTS);
        CountDownLatch fire = new CountDownLatch(1);

        var tasks = new java.util.ArrayList<Future<Integer>>();
        String body = json.writeValueAsString(
                Map.of("items", List.of(Map.of("productId", product.getId(), "cantidad", 1))));

        for (int i = 0; i < CONCURRENT_REQUESTS; i++) {
            tasks.add(exec.submit((Callable<Integer>) () -> {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + port + "/v1/sales"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + token)
                        .POST(BodyPublishers.ofString(body))
                        .build();
                ready.countDown();
                fire.await();                // todos disparan a la misma señal
                return http.send(req, BodyHandlers.discarding()).statusCode();
            }));
        }

        ready.await(10, TimeUnit.SECONDS);
        fire.countDown();                    // ¡fuego!

        var statuses = new java.util.ArrayList<Integer>();
        for (Future<Integer> f : tasks) statuses.add(f.get(15, TimeUnit.SECONDS));
        exec.shutdown();

        long success = statuses.stream().filter(s -> s == 201).count();
        long conflicts = statuses.stream().filter(s -> s == 409).count();

        System.out.println("[concurrency] statuses=" + statuses);
        assertThat(success).as("Solo 1 venta debe tener éxito").isEqualTo(1);
        assertThat(conflicts).as("El resto debe haber fallado con 409").isEqualTo(CONCURRENT_REQUESTS - 1);

        // Estado final consistente: 0 stock, 1 venta, 1 SALIDA — cero sobreventa.
        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getStockActual()).isZero();
        assertThat(saleRepository.count()).isEqualTo(1);
        long salidas = movementRepository.findAll().stream()
                .filter(m -> m.getTipo() == StockMovementType.SALIDA)
                .count();
        assertThat(salidas).isEqualTo(1);
    }

    private String login(String email, String password) throws Exception {
        String body = json.writeValueAsString(Map.of("email", email, "password", password));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/v1/auth/login"))
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> resp = http.send(req, BodyHandlers.ofString());
        assertThat(resp.statusCode()).isEqualTo(200);
        return json.readTree(resp.body()).get("accessToken").asText();
    }
}
