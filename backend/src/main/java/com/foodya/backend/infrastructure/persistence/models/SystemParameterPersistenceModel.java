package com.foodya.backend.infrastructure.persistence.models;

import com.foodya.backend.domain.value_objects.ParameterValueType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;

@Entity
@Table(name = "system_parameters")
public class SystemParameterPersistenceModel {

    @Id
    @Column(name = "param_key", nullable = false, length = 128)
    private String key;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 16)
    private ParameterValueType valueType;

    @Column(name = "param_value", nullable = false, columnDefinition = "text")
    private String value;

    @Column(name = "runtime_applicable", nullable = false)
    private boolean runtimeApplicable;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "updated_by_actor", length = 128)
    private String updatedByActor;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void touchUpdatedAt() {
        this.updatedAt = OffsetDateTime.now();
    }

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