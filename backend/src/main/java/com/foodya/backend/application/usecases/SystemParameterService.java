package com.foodya.backend.application.usecases;

import com.foodya.backend.domain.entities.AuditLog;
import com.foodya.backend.domain.value_objects.ParameterValueType;
import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.application.dto.SystemParameterModel;
import com.foodya.backend.application.dto.SystemParameterPatchRequest;
import com.foodya.backend.application.dto.SystemParameterPutRequest;
import com.foodya.backend.application.exception.ForbiddenException;
import com.foodya.backend.application.exception.NotFoundException;
import com.foodya.backend.application.exception.ValidationException;
import com.foodya.backend.application.ports.out.AuditLogPort;
import com.foodya.backend.application.ports.out.SystemParameterPort;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SystemParameterService {

    private static final String ADMIN_ROLE = "ADMIN";

    private final SystemParameterPort systemParameterPort;
    private final AuditLogPort auditLogPort;
    private final Map<String, SystemParameterCatalog.ParameterRule> rules = SystemParameterCatalog.defaults();

    public SystemParameterService(SystemParameterPort systemParameterPort,
                                  AuditLogPort auditLogPort) {
        this.systemParameterPort = systemParameterPort;
        this.auditLogPort = auditLogPort;
    }

    @PostConstruct
    @Transactional
    void bootstrapDefaults() {
        for (Map.Entry<String, SystemParameterCatalog.ParameterRule> entry : rules.entrySet()) {
            String key = entry.getKey();
            if (systemParameterPort.existsById(key)) {
                continue;
            }

            SystemParameterCatalog.ParameterRule rule = entry.getValue();
            SystemParameter parameter = new SystemParameter();
            parameter.setKey(key);
            parameter.setValueType(rule.type());
            parameter.setValue(rule.defaultValue());
            parameter.setRuntimeApplicable(rule.runtimeApplicable());
            parameter.setVersion(1);
            parameter.setDescription("Seeded by system defaults");
            parameter.setUpdatedByActor("system-bootstrap");
            systemParameterPort.save(parameter);
        }
    }

    @Transactional(readOnly = true)
    public List<SystemParameterModel> listAll() {
        return systemParameterPort.findAllOrderedByKey().stream().map(this::toModel).toList();
    }

    @Transactional
    public SystemParameterModel replace(String key, SystemParameterPutRequest request, String actorRole, String actorId) {
        assertAdmin(actorRole);

        SystemParameterCatalog.ParameterRule rule = requiredRule(key);
        assertRuntimeApplicable(key, rule);
        if (request.valueType() != rule.type()) {
            throw new ValidationException("valueType does not match parameter schema", Map.of("valueType", "expected " + rule.type()));
        }
        if (request.runtimeApplicable() != null && request.runtimeApplicable() != rule.runtimeApplicable()) {
            throw new ValidationException("runtimeApplicable does not match parameter schema",
                    Map.of("runtimeApplicable", "expected " + rule.runtimeApplicable()));
        }

        validateByType(key, request.valueType(), request.value());
        SystemParameter existing = requiredParameter(key);
        String oldSnapshot = snapshot(existing);

        existing.setValueType(request.valueType());
        existing.setValue(normalizeValue(request.valueType(), request.value()));
        existing.setRuntimeApplicable(rule.runtimeApplicable());
        existing.setDescription(request.description());
        existing.setVersion(existing.getVersion() + 1);
        existing.setUpdatedByActor(actorId);

        SystemParameter saved = systemParameterPort.save(existing);
        auditLogPort.save(AuditLog.parameterUpdate(actorId, key, oldSnapshot, snapshot(saved)));
        return toModel(saved);
    }

    @Transactional
    public SystemParameterModel patch(String key, SystemParameterPatchRequest request, String actorRole, String actorId) {
        assertAdmin(actorRole);
        SystemParameter existing = requiredParameter(key);
        SystemParameterCatalog.ParameterRule rule = requiredRule(key);
        assertRuntimeApplicable(key, rule);
        String oldSnapshot = snapshot(existing);

        if (request.valueType() != null && request.valueType() != rule.type()) {
            throw new ValidationException("valueType does not match parameter schema", Map.of("valueType", "expected " + rule.type()));
        }
        if (request.runtimeApplicable() != null && request.runtimeApplicable() != rule.runtimeApplicable()) {
            throw new ValidationException("runtimeApplicable does not match parameter schema",
                    Map.of("runtimeApplicable", "expected " + rule.runtimeApplicable()));
        }

        if (request.value() != null) {
            ParameterValueType incomingType = request.valueType() != null ? request.valueType() : existing.getValueType();
            validateByType(key, incomingType, request.value());
            existing.setValue(normalizeValue(incomingType, request.value()));
            existing.setValueType(incomingType);
        }
        existing.setRuntimeApplicable(rule.runtimeApplicable());
        if (request.description() != null) {
            existing.setDescription(request.description());
        }

        existing.setVersion(existing.getVersion() + 1);
        existing.setUpdatedByActor(actorId);

        SystemParameter saved = systemParameterPort.save(existing);
        auditLogPort.save(AuditLog.parameterUpdate(actorId, key, oldSnapshot, snapshot(saved)));
        return toModel(saved);
    }

    private void assertAdmin(String actorRole) {
        if (actorRole == null || !ADMIN_ROLE.equalsIgnoreCase(actorRole)) {
            throw new ForbiddenException("admin role is required");
        }
    }

    private SystemParameter requiredParameter(String key) {
        return systemParameterPort.findById(key)
                .orElseThrow(() -> new NotFoundException("system parameter not found"));
    }

    private SystemParameterCatalog.ParameterRule requiredRule(String key) {
        SystemParameterCatalog.ParameterRule rule = rules.get(key);
        if (rule == null) {
            throw new ValidationException("unknown parameter key", Map.of("key", key));
        }
        return rule;
    }

    private void assertRuntimeApplicable(String key, SystemParameterCatalog.ParameterRule rule) {
        if (!rule.runtimeApplicable()) {
            throw new ValidationException(
                    "parameter requires controlled restart",
                    Map.of("key", key + " is non-runtime; update via controlled restart/redeploy process")
            );
        }
    }

    private void validateByType(String key, ParameterValueType type, String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("parameter value must not be blank", Map.of("value", "must not be blank"));
        }

        if (type == ParameterValueType.BOOLEAN) {
            if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                throw new ValidationException("invalid boolean value", Map.of("value", "must be true or false"));
            }
            return;
        }

        if (type == ParameterValueType.JSON) {
            String trimmed = value.trim();
            if (!(trimmed.startsWith("{") && trimmed.endsWith("}")) && !(trimmed.startsWith("[") && trimmed.endsWith("]"))) {
                throw new ValidationException("invalid json value", Map.of("value", "must be JSON object or array"));
            }
            return;
        }

        SystemParameterCatalog.ParameterRule rule = requiredRule(key);
        if (rule.type() != type) {
            throw new ValidationException("valueType mismatch for key", Map.of("valueType", "expected " + rule.type()));
        }

        try {
            rule.validator().validate(value.trim());
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("invalid parameter value", Map.of("value", ex.getMessage()));
        }
    }

    private static String normalizeValue(ParameterValueType type, String value) {
        if (type == ParameterValueType.BOOLEAN) {
            return String.valueOf(Boolean.parseBoolean(value));
        }
        return value.trim();
    }

    private static String snapshot(SystemParameter parameter) {
        return "{" +
                "\"key\":\"" + parameter.getKey() + "\"," +
                "\"valueType\":\"" + parameter.getValueType() + "\"," +
                "\"value\":\"" + parameter.getValue() + "\"," +
                "\"runtimeApplicable\":" + parameter.isRuntimeApplicable() + "," +
                "\"version\":" + parameter.getVersion() +
                "}";
    }

    private SystemParameterModel toModel(SystemParameter parameter) {
        SystemParameterModel model = new SystemParameterModel();
        model.setKey(parameter.getKey());
        model.setValueType(parameter.getValueType());
        model.setValue(parameter.getValue());
        model.setRuntimeApplicable(parameter.isRuntimeApplicable());
        model.setVersion(parameter.getVersion());
        model.setDescription(parameter.getDescription());
        model.setUpdatedByActor(parameter.getUpdatedByActor());
        model.setUpdatedAt(parameter.getUpdatedAt());
        return model;
    }
}
