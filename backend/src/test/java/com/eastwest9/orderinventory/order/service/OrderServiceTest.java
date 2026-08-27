package com.eastwest9.orderinventory.order.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.eastwest9.orderinventory.inventory.domain.Inventory;
import com.eastwest9.orderinventory.inventory.domain.InventoryChangeType;
import com.eastwest9.orderinventory.inventory.domain.InventoryHistory;
import com.eastwest9.orderinventory.inventory.exception.InsufficientInventoryException;
import com.eastwest9.orderinventory.inventory.exception.InventoryNotFoundException;
import com.eastwest9.orderinventory.inventory.repository.InventoryHistoryRepository;
import com.eastwest9.orderinventory.inventory.repository.InventoryRepository;

import com.eastwest9.orderinventory.member.domain.Member;
import com.eastwest9.orderinventory.member.exception.MemberNotFoundException;
import com.eastwest9.orderinventory.member.repository.MemberRepository;
import com.eastwest9.orderinventory.order.domain.Order;
import com.eastwest9.orderinventory.order.domain.OrderItem;
import com.eastwest9.orderinventory.order.domain.OrderStatus;
import com.eastwest9.orderinventory.order.dto.OrderCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderItemCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderResponseDto;
import com.eastwest9.orderinventory.order.dto.OrderSummaryResponseDto;
import com.eastwest9.orderinventory.order.exception.InvalidOrderException;
import com.eastwest9.orderinventory.order.exception.OrderAlreadyCanceledException;
import com.eastwest9.orderinventory.order.exception.OrderNotFoundException;
import com.eastwest9.orderinventory.order.repository.OrderRepository;
import com.eastwest9.orderinventory.product.domain.Product;
import com.eastwest9.orderinventory.product.domain.ProductStatus;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotFoundException;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotOrderableException;
import com.eastwest9.orderinventory.product.repository.ProductVariantRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private InventoryHistoryRepository inventoryHistoryRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void 주문을_생성한다() {
        Member member = createMember(1L);
        ProductVariant variant = createVariant(
                10L,
                ProductStatus.ON_SALE,
                ProductVariantStatus.ON_SALE,
                "테스트 상품",
                "기본 옵션",
                "10000"
        );
        OrderCreateRequestDto request = createRequest(
                new OrderItemCreateRequestDto(10L, 2)
        );

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(productVariantRepository.findAllById(anyList()))
                .willReturn(List.of(variant));
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> assignOrderItemIds(
                        invocation.getArgument(0)
                ));
        Inventory inventory = new Inventory(variant, 100);
        given(inventoryRepository.findByProductVariant_Id(10L))
                .willReturn(Optional.of(inventory));

        OrderResponseDto response = orderService.createOrder(request);

        assertThat(response.memberId()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(OrderStatus.CREATED);
        assertThat(response.totalAmount()).isEqualByComparingTo("20000");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).variantId()).isEqualTo(10L);
        assertThat(response.items().get(0).productName())
                .isEqualTo("테스트 상품");
        verify(orderRepository).save(any(Order.class));
        assertThat(inventory.getQuantity()).isEqualTo(98);

        ArgumentCaptor<InventoryHistory> historyCaptor =
                ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());
        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getOrderItemId()).isEqualTo(100L);
        assertThat(history.getChangeType()).isEqualTo(InventoryChangeType.ORDER);
        assertThat(history.getBeforeQuantity()).isEqualTo(100);
        assertThat(history.getAfterQuantity()).isEqualTo(98);
        assertThat(history.getChangeQuantity()).isEqualTo(-2);
    }

    @Test
    void 여러_SKU로_주문을_생성하고_총액을_합산한다() {
        ProductVariant first = createVariant(
                10L, ProductStatus.ON_SALE, ProductVariantStatus.ON_SALE,
                "첫 상품", "첫 옵션", "10000"
        );
        ProductVariant second = createVariant(
                20L, ProductStatus.ON_SALE, ProductVariantStatus.ON_SALE,
                "둘째 상품", "둘째 옵션", "2500.50"
        );
        OrderCreateRequestDto request = createRequest(
                new OrderItemCreateRequestDto(10L, 2),
                new OrderItemCreateRequestDto(20L, 3)
        );

        Member member = createMember(1L);
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));
        given(productVariantRepository.findAllById(anyList()))
                .willReturn(List.of(second, first));
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> assignOrderItemIds(
                        invocation.getArgument(0)
                ));
        Inventory firstInventory = new Inventory(first, 10);
        Inventory secondInventory = new Inventory(second, 20);
        given(inventoryRepository.findByProductVariant_Id(10L))
                .willReturn(Optional.of(firstInventory));
        given(inventoryRepository.findByProductVariant_Id(20L))
                .willReturn(Optional.of(secondInventory));

        OrderResponseDto response = orderService.createOrder(request);

        assertThat(response.items()).hasSize(2);
        assertThat(response.items())
                .extracting(item -> item.variantId())
                .containsExactly(10L, 20L);
        assertThat(response.totalAmount()).isEqualByComparingTo("27501.50");
        assertThat(firstInventory.getQuantity()).isEqualTo(8);
        assertThat(secondInventory.getQuantity()).isEqualTo(17);

        ArgumentCaptor<InventoryHistory> historyCaptor =
                ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository, org.mockito.Mockito.times(2))
                .save(historyCaptor.capture());
        assertThat(historyCaptor.getAllValues())
                .extracting(InventoryHistory::getOrderItemId)
                .containsExactly(100L, 101L);
        assertThat(historyCaptor.getAllValues())
                .extracting(InventoryHistory::getChangeQuantity)
                .containsExactly(-2, -3);
    }

    @Test
    void 회원이_없으면_주문을_생성할_수_없다() {
        given(memberRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(createRequest(
                new OrderItemCreateRequestDto(10L, 1)
        )))
                .isInstanceOf(MemberNotFoundException.class)
                .hasMessage("회원을 찾을 수 없습니다. memberId=1");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 상품_SKU가_없으면_주문을_생성할_수_없다() {
        Member member = createMember(1L);
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));
        given(productVariantRepository.findAllById(anyList()))
                .willReturn(List.of());

        assertThatThrownBy(() -> orderService.createOrder(createRequest(
                new OrderItemCreateRequestDto(10L, 1)
        )))
                .isInstanceOf(ProductVariantNotFoundException.class)
                .hasMessage("상품 SKU를 찾을 수 없습니다. variantId=10");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 판매_중지된_상품의_SKU는_주문할_수_없다() {
        ProductVariant variant = createVariant(
                10L, ProductStatus.STOPPED, ProductVariantStatus.ON_SALE,
                "중지 상품", "기본 옵션", "10000"
        );
        stubOrderCreationDependencies(variant);

        assertThatThrownBy(() -> orderService.createOrder(createRequest(
                new OrderItemCreateRequestDto(10L, 1)
        )))
                .isInstanceOf(ProductVariantNotOrderableException.class)
                .hasMessageContaining("variantId=10");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 판매_중지된_SKU는_주문할_수_없다() {
        ProductVariant variant = createVariant(
                10L, ProductStatus.ON_SALE, ProductVariantStatus.STOPPED,
                "테스트 상품", "중지 옵션", "10000"
        );
        stubOrderCreationDependencies(variant);

        assertThatThrownBy(() -> orderService.createOrder(createRequest(
                new OrderItemCreateRequestDto(10L, 1)
        )))
                .isInstanceOf(ProductVariantNotOrderableException.class)
                .hasMessageContaining("variantId=10");

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 동일한_SKU가_중복되면_DB_조회_전에_주문을_거부한다() {
        Member member = createMember(1L);
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));
        OrderCreateRequestDto request = createRequest(
                new OrderItemCreateRequestDto(10L, 1),
                new OrderItemCreateRequestDto(20L, 1),
                new OrderItemCreateRequestDto(10L, 2)
        );

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("variantId=10");

        verify(productVariantRepository, never()).findAllById(anyList());
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 주문을_단건_조회하면_주문_항목을_포함한다() {
        Order order = createOrder();
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        OrderResponseDto response = orderService.getOrder(1L);

        assertThat(response.orderNumber()).isEqualTo(order.getOrderNumber());
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).variantId()).isEqualTo(10L);
    }

    @Test
    void 주문이_없으면_단건_조회할_수_없다() {
        given(orderRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(1L))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessage("주문을 찾을 수 없습니다. orderId=1");
    }

    @Test
    void 주문_목록은_Repository_순서를_유지하고_Summary로_반환한다() {
        Order first = createOrder();
        Order second = createOrder();
        given(orderRepository.findAllByOrderByIdDesc())
                .willReturn(List.of(first, second));

        List<OrderSummaryResponseDto> response = orderService.getOrders();

        assertThat(response)
                .extracting(OrderSummaryResponseDto::orderNumber)
                .containsExactly(first.getOrderNumber(), second.getOrderNumber());
        assertThat(response).allSatisfy(summary ->
                assertThat(summary.itemCount()).isEqualTo(1)
        );
    }

    @Test
    void 주문을_취소한다() {
        Order order = createOrder();
        Inventory inventory = new Inventory(
                order.getItems().get(0).getProductVariant(),
                7
        );
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(inventoryRepository.findByProductVariant_Id(10L))
                .willReturn(Optional.of(inventory));

        OrderResponseDto response = orderService.cancelOrder(1L);

        assertThat(response.status()).isEqualTo(OrderStatus.CANCELED);
        assertThat(response.canceledAt()).isNotNull();
        assertThat(inventory.getQuantity()).isEqualTo(8);
        ArgumentCaptor<InventoryHistory> historyCaptor =
                ArgumentCaptor.forClass(InventoryHistory.class);
        verify(inventoryHistoryRepository).save(historyCaptor.capture());
        InventoryHistory history = historyCaptor.getValue();
        assertThat(history.getOrderItemId()).isEqualTo(100L);
        assertThat(history.getChangeType())
                .isEqualTo(InventoryChangeType.ORDER_CANCEL);
        assertThat(history.getBeforeQuantity()).isEqualTo(7);
        assertThat(history.getAfterQuantity()).isEqualTo(8);
        assertThat(history.getChangeQuantity()).isEqualTo(1);
        verify(orderRepository).flush();
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void 이미_취소된_주문은_다시_취소할_수_없다() {
        Order order = createOrder();
        order.cancel(LocalDateTime.of(2026, 8, 26, 12, 0));
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(OrderAlreadyCanceledException.class);

        verify(orderRepository, never()).flush();
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(inventoryRepository, inventoryHistoryRepository);
    }

    @Test
    void 재고가_없는_SKU는_주문할_수_없다() {
        ProductVariant variant = createVariant(
                10L, ProductStatus.ON_SALE, ProductVariantStatus.ON_SALE,
                "테스트 상품", "기본 옵션", "10000"
        );
        stubOrderCreationDependencies(variant);
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> assignOrderItemIds(
                        invocation.getArgument(0)
                ));
        given(inventoryRepository.findByProductVariant_Id(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.createOrder(createRequest(
                new OrderItemCreateRequestDto(10L, 1)
        )))
                .isInstanceOf(InventoryNotFoundException.class);

        verifyNoInteractions(inventoryHistoryRepository);
    }

    @Test
    void 재고가_부족하면_예외가_발생하고_이후_항목을_처리하지_않는다() {
        ProductVariant first = createVariant(
                10L, ProductStatus.ON_SALE, ProductVariantStatus.ON_SALE,
                "첫 상품", "첫 옵션", "10000"
        );
        ProductVariant second = createVariant(
                20L, ProductStatus.ON_SALE, ProductVariantStatus.ON_SALE,
                "둘째 상품", "둘째 옵션", "20000"
        );
        Member member = createMember(1L);
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));
        given(productVariantRepository.findAllById(anyList()))
                .willReturn(List.of(first, second));
        given(orderRepository.save(any(Order.class)))
                .willAnswer(invocation -> assignOrderItemIds(
                        invocation.getArgument(0)
                ));
        given(inventoryRepository.findByProductVariant_Id(10L))
                .willReturn(Optional.of(new Inventory(first, 1)));

        assertThatThrownBy(() -> orderService.createOrder(createRequest(
                new OrderItemCreateRequestDto(10L, 2),
                new OrderItemCreateRequestDto(20L, 1)
        )))
                .isInstanceOf(InsufficientInventoryException.class);

        verify(inventoryRepository, never()).findByProductVariant_Id(20L);
        verifyNoInteractions(inventoryHistoryRepository);
    }

    @Test
    void 주문_취소_중_재고가_없으면_예외를_전파한다() {
        Order order = createOrder();
        given(orderRepository.findById(1L)).willReturn(Optional.of(order));
        given(inventoryRepository.findByProductVariant_Id(10L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(1L))
                .isInstanceOf(InventoryNotFoundException.class);

        verify(orderRepository, never()).flush();
        verifyNoInteractions(inventoryHistoryRepository);
    }

    private void stubOrderCreationDependencies(ProductVariant variant) {
        Member member = createMember(1L);
        given(memberRepository.findById(1L))
                .willReturn(Optional.of(member));
        given(productVariantRepository.findAllById(anyList()))
                .willReturn(List.of(variant));
    }

    private OrderCreateRequestDto createRequest(
            OrderItemCreateRequestDto... items
    ) {
        return new OrderCreateRequestDto(1L, List.of(items));
    }

    private Order createOrder() {
        ProductVariant variant = createVariant(
                10L, ProductStatus.ON_SALE, ProductVariantStatus.ON_SALE,
                "테스트 상품", "기본 옵션", "10000"
        );
        return assignOrderItemIds(new Order(
                createMember(1L),
                List.of(new OrderItem(variant, 1))
        ));
    }

    private Order assignOrderItemIds(Order order) {
        long orderItemId = 100L;
        for (OrderItem item : order.getItems()) {
            ReflectionTestUtils.setField(item, "id", orderItemId++);
        }
        return order;
    }

    private Member createMember(Long id) {
        Member member = org.mockito.Mockito.mock(Member.class);
        lenient().when(member.getId()).thenReturn(id);
        return member;
    }

    private ProductVariant createVariant(
            Long id,
            ProductStatus productStatus,
            ProductVariantStatus variantStatus,
            String productName,
            String variantName,
            String salePrice
    ) {
        Product product = org.mockito.Mockito.mock(Product.class);
        lenient().when(product.getName()).thenReturn(productName);
        lenient().when(product.getStatus()).thenReturn(productStatus);

        ProductVariant variant = org.mockito.Mockito.mock(ProductVariant.class);
        lenient().when(variant.getId()).thenReturn(id);
        lenient().when(variant.getProduct()).thenReturn(product);
        lenient().when(variant.getName()).thenReturn(variantName);
        lenient().when(variant.getSalePrice())
                .thenReturn(new BigDecimal(salePrice));
        lenient().when(variant.getStatus()).thenReturn(variantStatus);
        return variant;
    }
}
