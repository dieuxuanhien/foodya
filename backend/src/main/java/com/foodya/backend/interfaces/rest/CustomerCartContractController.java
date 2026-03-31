package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.ActiveCartView;
import com.foodya.backend.application.dto.AddCartItemRequest;
import com.foodya.backend.application.dto.UpdateCartItemRequest;
import com.foodya.backend.application.usecases.CartService;
import com.foodya.backend.interfaces.rest.dto.ActiveCartResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.mapper.CartRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/carts")
public class CustomerCartContractController {

    private final CartService cartService;

    public CustomerCartContractController(CartService cartService) {
        this.cartService = cartService;
    }

    @PostMapping("/items")
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> addItem(Authentication authentication,
                                                                           @Valid @RequestBody AddCartItemRequest request,
                                                                           HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.addItem(CurrentUser.userId(authentication), request.menuItemId(), request.quantity(), request.note());
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> updateItem(Authentication authentication,
                                                                              @PathVariable String id,
                                                                              @Valid @RequestBody UpdateCartItemRequest request,
                                                                              HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.updateItem(CurrentUser.userId(authentication), id, request.quantity(), request.note());
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<Void> removeItem(Authentication authentication,
                                           @PathVariable String id) {
        cartService.removeItem(CurrentUser.userId(authentication), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/active/clear")
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> clear(Authentication authentication,
                                                                         HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.clearActiveCart(CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }
}
