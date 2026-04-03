package com.foodya.backend.interfaces.rest;

import com.foodya.backend.application.ports.in.SystemParameterUseCase;
import com.foodya.backend.application.dto.SystemParameterModel;
import com.foodya.backend.application.dto.SystemParameterPatchRequest;
import com.foodya.backend.application.dto.SystemParameterPutRequest;
import com.foodya.backend.interfaces.rest.dto.ApiErrorResponse;
import com.foodya.backend.interfaces.rest.dto.ApiSuccessResponse;
import com.foodya.backend.interfaces.rest.dto.SystemParameterPatchRestRequest;
import com.foodya.backend.interfaces.rest.dto.SystemParameterPutRestRequest;
import com.foodya.backend.interfaces.rest.dto.SystemParameterResponse;
import com.foodya.backend.interfaces.rest.mapper.RestDtoMapper;
import com.foodya.backend.interfaces.rest.support.RequestTrace;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/system-parameters")
@Tag(name = "Admin System Parameters", description = "Global runtime parameter management")
public class SystemParameterAdminController {

    private final SystemParameterUseCase systemParameterService;

    public SystemParameterAdminController(SystemParameterUseCase systemParameterService) {
        this.systemParameterService = systemParameterService;
    }

    @GetMapping
    @Operation(summary = "List system parameters", description = "Returns all parameter keys and current active values")
    @ApiResponse(responseCode = "200", description = "Successful")
    public ResponseEntity<ApiSuccessResponse<List<SystemParameterResponse>>> list(HttpServletRequest httpServletRequest) {
        List<SystemParameterResponse> data = systemParameterService.listAll()
                .stream()
                .map(RestDtoMapper::toSystemParameterResponse)
                .toList();

        return ResponseEntity.ok(ApiSuccessResponse.of(data, RequestTrace.from(httpServletRequest)));
    }

    @PutMapping("/{key}")
    @Operation(
            summary = "Replace a system parameter",
            description = "Admin-only full replace for parameter value and runtime metadata",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Updated"),
                    @ApiResponse(
                            responseCode = "422",
                            description = "Validation failed",
                            content = @Content(
                                    schema = @Schema(implementation = ApiErrorResponse.class),
                                    examples = @ExampleObject(value = "{\"code\":\"VALIDATION_FAILED\",\"message\":\"invalid parameter value\",\"details\":{\"value\":\"must be >= 0\"},\"traceId\":\"e7d9\"}")
                            )
                    )
            }
    )
    public ResponseEntity<ApiSuccessResponse<SystemParameterResponse>> put(@PathVariable String key,
                                                                            @Valid @RequestBody SystemParameterPutRestRequest request,
                                                                            @RequestHeader(value = "X-User-Role", required = false) String actorRole,
                                                                            @RequestHeader(value = "X-Actor-Id", required = false, defaultValue = "unknown") String actorId,
                                                                            HttpServletRequest httpServletRequest) {
        SystemParameterPutRequest command = new SystemParameterPutRequest(
                request.valueType(),
                request.value(),
                request.runtimeApplicable(),
                request.description()
        );
        SystemParameterModel updated = systemParameterService.replace(key, command, actorRole, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.of(RestDtoMapper.toSystemParameterResponse(updated), RequestTrace.from(httpServletRequest)));
    }

    @PatchMapping("/{key}")
    @Operation(summary = "Patch a system parameter", description = "Admin-only partial update")
    public ResponseEntity<ApiSuccessResponse<SystemParameterResponse>> patch(@PathVariable String key,
                                                                              @RequestBody SystemParameterPatchRestRequest request,
                                                                              @RequestHeader(value = "X-User-Role", required = false) String actorRole,
                                                                              @RequestHeader(value = "X-Actor-Id", required = false, defaultValue = "unknown") String actorId,
                                                                              HttpServletRequest httpServletRequest) {
        SystemParameterPatchRequest command = new SystemParameterPatchRequest(
                request.valueType(),
                request.value(),
                request.runtimeApplicable(),
                request.description()
        );
                SystemParameterModel updated = systemParameterService.patch(key, command, actorRole, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.of(RestDtoMapper.toSystemParameterResponse(updated), RequestTrace.from(httpServletRequest)));
    }
}
