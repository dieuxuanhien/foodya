package com.foodya.backend.application.ports.in;

import com.foodya.backend.application.dto.ActiveCartView;

import java.util.UUID;

public interface CartUseCase {

    ActiveCartView getActiveCart(UUID customerUserId);

    ActiveCartView addItem(UUID customerUserId, String menuItemIdRaw, int quantity, String note);

    ActiveCartView updateItem(UUID customerUserId, String menuItemIdRaw, int quantity, String note);

    ActiveCartView removeItem(UUID customerUserId, String menuItemIdRaw);

    ActiveCartView clearActiveCart(UUID customerUserId);
}
