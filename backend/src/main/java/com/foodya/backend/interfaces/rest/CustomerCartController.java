package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.dto.ActiveCartView;
import com.foodya.backend.application.dto.AddCartItemRequest;
import com.foodya.backend.application.dto.UpdateCartItemRequest;
import com.foodya.backend.application.service.CartService;
import com.foodya.backend.interfaces.rest.dto.ActiveCartResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.mapper.CartRestMapper;
import com.foodya.backend.interfaces.rest.support.CurrentUser;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customer/carts/active")
@Tag(name = "Customer Cart", description = "Customer cart management lifecycle")
public class CustomerCartController {

    private final CartService cartService;

    public CustomerCartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get active cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Active cart returned"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> getActive(Authentication authentication,
                                                                             HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.getActiveCart(CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to active cart")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "422", description = "Validation failed")
    })
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> addItem(Authentication authentication,
                                                                           @Valid @RequestBody AddCartItemRequest request,
                                                                           HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.addItem(CurrentUser.userId(authentication), request.menuItemId(), request.quantity(), request.note());
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }

    @PatchMapping("/items/{menuItemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> updateItem(Authentication authentication,
                                                                              @PathVariable String menuItemId,
                                                                              @Valid @RequestBody UpdateCartItemRequest request,
                                                                              HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.updateItem(CurrentUser.userId(authentication), menuItemId, request.quantity(), request.note());
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/items/{menuItemId}")
    @Operation(summary = "Remove item from active cart")
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> removeItem(Authentication authentication,
                                                                              @PathVariable String menuItemId,
                                                                              HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.removeItem(CurrentUser.userId(authentication), menuItemId);
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }

    @DeleteMapping("/items")
    @Operation(summary = "Clear active cart")
    public ResponseEntity<ApiSuccessResponse<ActiveCartResponse>> clear(Authentication authentication,
                                                                         HttpServletRequest httpServletRequest) {
        ActiveCartView view = cartService.clearActiveCart(CurrentUser.userId(authentication));
        return ResponseEntity.ok(ApiSuccessResponse.of(CartRestMapper.toResponse(view), RequestTrace.from(httpServletRequest)));
    }
}
