package com.foodya.backend.application.dto;

import com.foodya.backend.domain.value_objects.ParameterValueType;

import java.time.OffsetDateTime;

public class SystemParameterModel {

    private String key;
    private ParameterValueType valueType;
    private String value;
    private boolean runtimeApplicable;
    private int version;
    private String description;
    private String updatedByActor;
    private OffsetDateTime updatedAt;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ParameterValueType getValueType() {
        return valueType;
    }

    public void setValueType(ParameterValueType valueType) {
        this.valueType = valueType;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public boolean isRuntimeApplicable() {
        return runtimeApplicable;
    }

    public void setRuntimeApplicable(boolean runtimeApplicable) {
        this.runtimeApplicable = runtimeApplicable;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUpdatedByActor() {
        return updatedByActor;
    }

    public void setUpdatedByActor(String updatedByActor) {
        this.updatedByActor = updatedByActor;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}