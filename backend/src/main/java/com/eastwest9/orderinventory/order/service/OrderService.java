package com.eastwest9.orderinventory.order.service;

import com.eastwest9.orderinventory.inventory.domain.Inventory;
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
import com.eastwest9.orderinventory.order.dto.OrderCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderItemCreateRequestDto;
import com.eastwest9.orderinventory.order.dto.OrderResponseDto;
import com.eastwest9.orderinventory.order.dto.OrderSummaryResponseDto;
import com.eastwest9.orderinventory.order.exception.InvalidOrderException;
import com.eastwest9.orderinventory.order.exception.OrderNotFoundException;
import com.eastwest9.orderinventory.order.repository.OrderRepository;
import com.eastwest9.orderinventory.product.domain.ProductStatus;
import com.eastwest9.orderinventory.product.domain.ProductVariant;
import com.eastwest9.orderinventory.product.domain.ProductVariantStatus;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotFoundException;
import com.eastwest9.orderinventory.product.exception.ProductVariantNotOrderableException;
import com.eastwest9.orderinventory.product.repository.ProductVariantRepository;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    @Transactional
    public OrderResponseDto createOrder(OrderCreateRequestDto request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new MemberNotFoundException(
                        request.memberId()
                ));

        List<Long> variantIds = validateAndGetUniqueVariantIds(request);
        Map<Long, ProductVariant> variantsById = findVariantsById(variantIds);

        List<OrderItem> orderItems = request.items()
                .stream()
                .map(item -> createOrderItem(item, variantsById))
                .toList();

        Order order = new Order(member, orderItems);
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : savedOrder.getItems()) {
            decreaseInventory(item);
        }

        return OrderResponseDto.from(savedOrder);
    }

    public OrderResponseDto getOrder(Long orderId) {
        return OrderResponseDto.from(findOrder(orderId));
    }

    public List<OrderSummaryResponseDto> getOrders() {
        return orderRepository.findAllByOrderByIdDesc()
                .stream()
                .map(OrderSummaryResponseDto::from)
                .toList();
    }

    @Transactional
    public OrderResponseDto cancelOrder(Long orderId) {
        Order order = findOrder(orderId);

        order.cancel(LocalDateTime.now());

        for (OrderItem item : order.getItems()) {
            restoreInventory(item);
        }

        orderRepository.flush();

        return OrderResponseDto.from(order);
    }

    private void decreaseInventory(OrderItem item) {
        Long variantId = item.getProductVariant().getId();
        int requestedQuantity = item.getQuantity();
        int updatedRows = inventoryRepository.decreaseQuantityIfAvailable(
                variantId,
                requestedQuantity
        );
        Inventory inventory = findInventory(variantId);

        if (updatedRows == 0) {
            throw new InsufficientInventoryException(
                    variantId,
                    requestedQuantity,
                    inventory.getQuantity()
            );
        }

        int afterQuantity = inventory.getQuantity();
        int beforeQuantity = Math.addExact(afterQuantity, requestedQuantity);

        InventoryHistory history = InventoryHistory.order(
                item.getProductVariant(),
                item.getId(),
                beforeQuantity,
                afterQuantity
        );
        inventoryHistoryRepository.save(history);
    }

    private void restoreInventory(OrderItem item) {
        Long variantId = item.getProductVariant().getId();
        Inventory inventory = findInventory(variantId);
        int beforeQuantity = inventory.getQuantity();

        inventory.restore(item.getQuantity());

        InventoryHistory history = InventoryHistory.orderCancel(
                item.getProductVariant(),
                item.getId(),
                beforeQuantity,
                inventory.getQuantity()
        );
        inventoryHistoryRepository.save(history);
    }

    private Inventory findInventory(Long variantId) {
        return inventoryRepository.findByProductVariant_Id(variantId)
                .orElseThrow(() -> new InventoryNotFoundException(variantId));
    }

    private List<Long> validateAndGetUniqueVariantIds(OrderCreateRequestDto request) {
        Set<Long> uniqueVariantIds = new LinkedHashSet<>();

        for (OrderItemCreateRequestDto item : request.items()) {
            if (!uniqueVariantIds.add(item.variantId())) {
                throw new InvalidOrderException(
                        "동일한 상품 SKU를 중복 주문할 수 없습니다. variantId="
                                + item.variantId()
                );
            }
        }

        return List.copyOf(uniqueVariantIds);
    }

    private Map<Long, ProductVariant> findVariantsById(List<Long> variantIds) {
        Map<Long, ProductVariant> variantsById = new HashMap<>();

        productVariantRepository.findAllById(variantIds)
                .forEach(variant -> variantsById.put(variant.getId(), variant));

        for (Long variantId : variantIds) {
            if (!variantsById.containsKey(variantId)) {
                throw new ProductVariantNotFoundException(variantId);
            }
        }

        return variantsById;
    }

    private OrderItem createOrderItem(OrderItemCreateRequestDto item, Map<Long, ProductVariant> variantsById) {
        ProductVariant variant = variantsById.get(item.variantId());
        validateOrderable(variant);

        return new OrderItem(variant, item.quantity());
    }

    private void validateOrderable(ProductVariant variant) {
        if (variant.getProduct().getStatus() != ProductStatus.ON_SALE
                || variant.getStatus() != ProductVariantStatus.ON_SALE) {
            throw new ProductVariantNotOrderableException(variant.getId());
        }
    }

    private Order findOrder(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
