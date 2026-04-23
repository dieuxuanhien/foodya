package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.SystemParameter;
import com.foodya.backend.infrastructure.persistence.models.SystemParameterPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class SystemParameterMapper {

    public SystemParameter toDomain(SystemParameterPersistenceModel model) {
        if (model == null) {
            return null;
        }

        SystemParameter domain = new SystemParameter();
        domain.setKey(model.getKey());
        domain.setValueType(model.getValueType());
        domain.setValue(model.getValue());
        domain.setRuntimeApplicable(model.isRuntimeApplicable());
        domain.setVersion(model.getVersion());
        domain.setDescription(model.getDescription());
        domain.setUpdatedByActor(model.getUpdatedByActor());
        domain.setUpdatedAt(model.getUpdatedAt());
        return domain;
    }

    public SystemParameterPersistenceModel toPersistence(SystemParameter domain) {
        if (domain == null) {
            return null;
        }

        SystemParameterPersistenceModel model = new SystemParameterPersistenceModel();
        model.setKey(domain.getKey());
        model.setValueType(domain.getValueType());
        model.setValue(domain.getValue());
        model.setRuntimeApplicable(domain.isRuntimeApplicable());
        model.setVersion(domain.getVersion());
        model.setDescription(domain.getDescription());
        model.setUpdatedByActor(domain.getUpdatedByActor());
        model.setUpdatedAt(domain.getUpdatedAt());
        return model;
    }
}