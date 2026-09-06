package com.eastwest9.orderinventory.order.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.eastwest9.orderinventory.inventory.domain.Inventory;
import com.eastwest9.orderinventory.inventory.domain.InventoryChangeType;
import com.eastwest9.orderinventory.inventory.domain.InventoryHistory;
import com.eastwest9.orderinventory.inventory.exception.InsufficientInventoryException;
import com.eastwest9.orderinventory.inventory.repository.InventoryHistoryRepository;
import com.eastwest9.orderinventory.inventory.repository.InventoryRepository;
import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.member.repository.MemberRepository;
import com.eastwest9.orderinventory.order.dto.OrderCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderItemCreateRequestDto;
import com.eastwest9.orderinventory.order.service.OrderService;
import com.eastwest9.orderinventory.product.domain.Product;
import com.eastwest9.orderinventory.product.domain.ProductStatus;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import com.eastwest9.orderinventory.product.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest(properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true",
        "spring.datasource.hikari.maximum-pool-size=50"
})
@ActiveProfiles("mysql-integration")
@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrderInventoryConcurrencyIntegrationTest {

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
    }

    @Autowired
    private OrderService orderService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private InventoryHistoryRepository inventoryHistoryRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void 동시_주문에서_조건부_원자적_차감의_재고_정합성을_보장한다() throws InterruptedException {
        // Given: fixture 트랜잭션은 worker 시작 전에 커밋된다.
        int initialQuantity = 20;
        int requestCount = 50;
        Long memberId = memberRepository.save(new Member(UUID.randomUUID() + "@example.com", null, "동시 주문 테스트 회원")).getId();
        Long variantId = createVariant(initialQuantity);
        Queue<Long> successfulOrderIds = new ConcurrentLinkedQueue<>();
        Queue<InsufficientInventoryException> insufficientFailures = new ConcurrentLinkedQueue<>();
        Queue<Throwable> unexpectedFailures = new ConcurrentLinkedQueue<>();
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requestCount);
        ExecutorService executor = Executors.newFixedThreadPool(requestCount);

        // When: 각 worker는 DTO를 직접 만들고 실제 서비스 트랜잭션을 호출한다.
        try {
            for (int i = 0; i < requestCount; i++) {
                executor.submit(() -> {
                    ready.countDown();
                    try {
                        if (!start.await(30, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("동시 시작 대기 시간 초과");
                        }
                        OrderCreateRequestDto request = new OrderCreateRequestDto(memberId, List.of(new OrderItemCreateRequestDto(variantId, 1)));
                        successfulOrderIds.add(orderService.createOrder(request).id());
                    } catch (InsufficientInventoryException exception) {
                        insufficientFailures.add(exception);
                    } catch (Throwable exception) {
                        if (exception instanceof InterruptedException) {
                            Thread.currentThread().interrupt();
                        }
                        unexpectedFailures.add(exception);
                    } finally {
                        done.countDown();
                    }
                });
            }
            assertThat(ready.await(30, TimeUnit.SECONDS)).as("모든 worker 준비").isTrue();
            start.countDown();
            assertThat(done.await(60, TimeUnit.SECONDS)).as("모든 주문 완료").isTrue();
        } finally {
            start.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).as("executor 종료").isTrue();
        }

        // Then: 조회 범위를 이번 회원과 SKU로 제한하고 커밋된 DB 상태를 검증한다.
        int finalQuantity = inventoryRepository.findByProductVariant_Id(variantId).orElseThrow().getQuantity();
        List<Long> orderIds = jdbcTemplate.queryForList("SELECT order_id FROM shop_order WHERE member_id = ? AND order_status = 'CREATED'", Long.class, memberId);
        List<Long> itemIds = jdbcTemplate.queryForList("SELECT oi.order_item_id FROM order_item oi JOIN shop_order o ON o.order_id = oi.order_id WHERE o.member_id = ? AND oi.variant_id = ?", Long.class, memberId, variantId);
        int orderedQuantity = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi JOIN shop_order o ON o.order_id = oi.order_id WHERE o.member_id = ? AND oi.variant_id = ?", Integer.class, memberId, variantId);
        List<InventoryHistory> histories = inventoryHistoryRepository.findAllByProductVariant_IdOrderByIdDesc(variantId)
                .stream()
                .filter(history -> history.getChangeType() == InventoryChangeType.ORDER)
                .toList();
        int historyDecrease = histories.stream().mapToInt(history -> -history.getChangeQuantity()).sum();
        int actualHistoryDecrease = histories.stream().mapToInt(history -> history.getBeforeQuantity() - history.getAfterQuantity()).sum();

        System.out.printf("success=%d, insufficient=%d, unexpected=%d, finalInventory=%d, orders=%d, items=%d, orderedQuantity=%d, histories=%d, historyDecrease=%d%n", successfulOrderIds.size(), insufficientFailures.size(), unexpectedFailures.size(), finalQuantity, orderIds.size(), itemIds.size(), orderedQuantity, histories.size(), historyDecrease);
        System.out.printf("insufficient availableQuantity values=%s%n", insufficientFailures.stream().map(InsufficientInventoryException::getAvailableQuantity).sorted().toList());

        assertThat(unexpectedFailures).as("재고 부족 이외의 예외").isEmpty();
        assertThat(successfulOrderIds).hasSize(initialQuantity).doesNotHaveDuplicates();
        assertThat(insufficientFailures).hasSize(requestCount - initialQuantity).allSatisfy(exception -> {
            assertThat(exception.getVariantId()).isEqualTo(variantId);
            assertThat(exception.getRequestedQuantity()).isEqualTo(1);
        });
        assertThat(finalQuantity).isGreaterThanOrEqualTo(0).isZero();
        assertThat(orderIds).containsExactlyInAnyOrderElementsOf(successfulOrderIds);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shop_order WHERE member_id = ?", Integer.class, memberId)).isEqualTo(initialQuantity);
        assertThat(itemIds).hasSize(initialQuantity);
        assertThat(orderedQuantity).isEqualTo(initialQuantity).isEqualTo(initialQuantity - finalQuantity);
        assertThat(histories).hasSize(initialQuantity);
        assertThat(historyDecrease).isEqualTo(orderedQuantity);
        assertThat(actualHistoryDecrease).isEqualTo(orderedQuantity);
        assertThat(histories).extracting(InventoryHistory::getOrderItemId).containsExactlyInAnyOrderElementsOf(itemIds);
        assertThat(histories).allSatisfy(history -> {
            assertThat(history.getChangeQuantity()).isEqualTo(-1);
            assertThat(history.getAfterQuantity()).isEqualTo(history.getBeforeQuantity() - 1);
        });
        assertThat(histories).extracting(InventoryHistory::getBeforeQuantity)
                .containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, initialQuantity).boxed().toList());
        assertThat(histories).extracting(InventoryHistory::getAfterQuantity)
                .containsExactlyInAnyOrderElementsOf(IntStream.range(0, initialQuantity).boxed().toList());
    }

    @Test
    void 재고가_부족하면_주문과_이력을_롤백하고_가용_수량을_반환한다() {
        // Given
        Long memberId = memberRepository.save(new Member(UUID.randomUUID() + "@example.com", null, "재고 부족 테스트 회원")).getId();
        Long variantId = createVariant(1);
        OrderCreateRequestDto request = new OrderCreateRequestDto(memberId, List.of(new OrderItemCreateRequestDto(variantId, 2)));

        // When
        InsufficientInventoryException exception = catchThrowableOfType(InsufficientInventoryException.class, () -> orderService.createOrder(request));

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getVariantId()).isEqualTo(variantId);
        assertThat(exception.getRequestedQuantity()).isEqualTo(2);
        assertThat(exception.getAvailableQuantity()).isEqualTo(1);
        assertRolledBack(memberId, variantId, 1);
    }

    @Test
    void 두번째_SKU_차감이_실패하면_앞선_차감과_주문_전체를_롤백한다() {
        // Given: 첫 SKU는 차감에 성공하고 다음 SKU에서 재고 부족이 발생한다.
        Long memberId = memberRepository.save(new Member(UUID.randomUUID() + "@example.com", null, "전체 롤백 테스트 회원")).getId();
        Long firstVariantId = createVariant(3);
        Long secondVariantId = createVariant(1);
        OrderCreateRequestDto request = new OrderCreateRequestDto(memberId, List.of(new OrderItemCreateRequestDto(firstVariantId, 1), new OrderItemCreateRequestDto(secondVariantId, 2)));

        // When
        InsufficientInventoryException exception = catchThrowableOfType(InsufficientInventoryException.class, () -> orderService.createOrder(request));

        // Then
        assertThat(exception).isNotNull();
        assertThat(exception.getVariantId()).isEqualTo(secondVariantId);
        assertThat(exception.getAvailableQuantity()).isEqualTo(1);
        assertRolledBack(memberId, firstVariantId, 3);
        assertRolledBack(memberId, secondVariantId, 1);
    }

    private Long createVariant(int initialQuantity) {
        return new TransactionTemplate(transactionManager).execute(status -> {
            Product product = new Product("통합 테스트 상품", ProductStatus.ON_SALE);
            ProductVariant variant = new ProductVariant("concurrency-" + UUID.randomUUID(), "기본 옵션", new BigDecimal("10000"), ProductVariantStatus.ON_SALE);
            product.addVariant(variant);
            productRepository.save(product);
            inventoryRepository.save(new Inventory(variant, initialQuantity));
            inventoryHistoryRepository.save(InventoryHistory.initial(variant, initialQuantity));
            return variant.getId();
        });
    }

    private void assertRolledBack(Long memberId, Long variantId, int initialQuantity) {
        assertThat(inventoryRepository.findByProductVariant_Id(variantId).orElseThrow().getQuantity()).isEqualTo(initialQuantity);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM shop_order WHERE member_id = ?", Integer.class, memberId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM order_item WHERE variant_id = ?", Integer.class, variantId)).isZero();
        assertThat(inventoryHistoryRepository.findAllByProductVariant_IdOrderByIdDesc(variantId)).singleElement().satisfies(history -> {
            assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.INITIAL);
            assertThat(history.getBeforeQuantity()).isZero();
            assertThat(history.getAfterQuantity()).isEqualTo(initialQuantity);
            assertThat(history.getChangeQuantity()).isEqualTo(initialQuantity);
        });
    }
}
