package com.foodya.backend.interfaces.rest.mapper;

import com.foodya.backend.application.dto.ActiveCartView;
import com.foodya.backend.application.dto.CartItemView;
import com.foodya.backend.interfaces.rest.dto.ActiveCartResponse;
import com.foodya.backend.interfaces.rest.dto.CartItemResponse;

public final class CartRestMapper {

    private CartRestMapper() {
    }

    public static ActiveCartResponse toResponse(ActiveCartView view) {
        return new ActiveCartResponse(
                view.cartId() == null ? null : view.cartId().toString(),
                view.restaurantId() == null ? null : view.restaurantId().toString(),
                view.subtotal(),
                view.itemCount(),
                view.items().stream().map(CartRestMapper::toItemResponse).toList()
        );
    }

    private static CartItemResponse toItemResponse(CartItemView item) {
        return new CartItemResponse(
                item.menuItemId().toString(),
                item.quantity(),
                item.unitPriceSnapshot(),
                item.lineTotal(),
                item.note()
        );
    }
}
