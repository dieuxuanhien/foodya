package com.foodya.backend.application.usecases;

import com.foodya.backend.application.dto.ActiveCartView;
import com.foodya.backend.application.dto.CartItemView;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.in.CartUseCase;
import com.foodya.backend.application.ports.out.CartItemPort;
import com.foodya.backend.application.ports.out.CartPort;
import com.foodya.backend.application.ports.out.MenuItemPort;
import com.foodya.backend.domain.value_objects.CartStatus;
import com.foodya.backend.domain.entities.Cart;
import com.foodya.backend.domain.entities.CartItem;
import com.foodya.backend.domain.entities.MenuItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class CartService implements CartUseCase {

    private final CartPort cartPort;
    private final CartItemPort cartItemPort;
    private final MenuItemPort menuItemPort;

    public CartService(CartPort cartPort,
                       CartItemPort cartItemPort,
                       MenuItemPort menuItemPort) {
        this.cartPort = cartPort;
        this.cartItemPort = cartItemPort;
        this.menuItemPort = menuItemPort;
    }

    @Transactional(readOnly = true)
    public ActiveCartView getActiveCart(UUID customerUserId) {
        return cartPort.findByCustomerUserIdAndStatus(customerUserId, CartStatus.ACTIVE)
                .map(this::toView)
                .orElseGet(ActiveCartView::empty);
    }

    @Transactional
    public ActiveCartView addItem(UUID customerUserId, String menuItemIdRaw, int quantity, String note) {
        UUID menuItemId = parseUuid(menuItemIdRaw, "menuItemId");
        if (quantity <= 0) {
            throw new ValidationException("invalid quantity", Map.of("quantity", "must be > 0"));
        }

        MenuItem menuItem = menuItemPort.findById(menuItemId)
                .orElseThrow(() -> new NotFoundException("menu item not found"));
        assertMenuItemOrderable(menuItem);

        Cart cart = cartPort.findByCustomerUserIdAndStatus(customerUserId, CartStatus.ACTIVE)
                .orElseGet(() -> createActiveCart(customerUserId, menuItem.getRestaurantId()));

        // BR29: active cart must be scoped to a single restaurant.
        cart.validateRestaurantScope(menuItem.getRestaurantId());

        CartItem line = cartItemPort.findByCartIdAndMenuItemId(cart.getId(), menuItemId)
                .orElseGet(() -> {
                    CartItem item = new CartItem();
                    item.setCartId(cart.getId());
                    item.setMenuItemId(menuItemId);
                    item.setQuantity(0);
                    item.setUnitPriceSnapshot(menuItem.getPrice());
                    return item;
                });

        line.increaseQuantity(quantity);
        line.setNote(note);
        cartItemPort.save(line);

        return toView(cart);
    }

    @Transactional
    public ActiveCartView updateItem(UUID customerUserId, String menuItemIdRaw, int quantity, String note) {
        UUID menuItemId = parseUuid(menuItemIdRaw, "menuItemId");

        Cart cart = requiredActiveCart(customerUserId);
        CartItem line = cartItemPort.findByCartIdAndMenuItemId(cart.getId(), menuItemId)
                .orElseThrow(() -> new NotFoundException("cart item not found"));

        line.updateQuantity(quantity);
        line.setNote(note);
        cartItemPort.save(line);

        return toView(cart);
    }

    @Transactional
    public ActiveCartView removeItem(UUID customerUserId, String menuItemIdRaw) {
        UUID menuItemId = parseUuid(menuItemIdRaw, "menuItemId");
        Cart cart = requiredActiveCart(customerUserId);
        CartItem line = cartItemPort.findByCartIdAndMenuItemId(cart.getId(), menuItemId)
                .orElseThrow(() -> new NotFoundException("cart item not found"));
        cartItemPort.delete(line);
        return toView(cart);
    }

    @Transactional
    public ActiveCartView clearActiveCart(UUID customerUserId) {
        Cart cart = requiredActiveCart(customerUserId);
        cartItemPort.deleteByCartId(cart.getId());
        return toView(cart);
    }

    private Cart requiredActiveCart(UUID customerUserId) {
        return cartPort.findByCustomerUserIdAndStatus(customerUserId, CartStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("active cart not found"));
    }

    private Cart createActiveCart(UUID customerUserId, UUID restaurantId) {
        Cart cart = new Cart();
        cart.setCustomerUserId(customerUserId);
        cart.setRestaurantId(restaurantId);
        cart.setStatus(CartStatus.ACTIVE);
        return cartPort.save(cart);
    }

    private ActiveCartView toView(Cart cart) {
        List<CartItemView> items = cartItemPort.findByCartId(cart.getId()).stream()
                .map(item -> new CartItemView(
                        item.getMenuItemId(),
                        item.getQuantity(),
                        item.getUnitPriceSnapshot(),
                        item.calculateLineTotal(),
                        item.getNote()))
                .toList();

        // Load items into cart for domain calculations
        cart.setItems(cartItemPort.findByCartId(cart.getId()));

        return new ActiveCartView(
                cart.getId(),
                cart.getRestaurantId(),
                cart.calculateSubtotal(),
                cart.getTotalItemCount(),
                items
        );
    }

    private static UUID parseUuid(String raw, String field) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid uuid", Map.of(field, "must be a valid UUID"));
        }
    }

    private static void assertMenuItemOrderable(MenuItem menuItem) {
        if (!menuItem.isOrderable()) {
            throw new ValidationException("menu item is not orderable", Map.of("menuItemId", "inactive or unavailable"));
        }
    }
}
