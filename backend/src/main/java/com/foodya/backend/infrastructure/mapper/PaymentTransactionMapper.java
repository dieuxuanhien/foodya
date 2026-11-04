package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.PaymentTransaction;
import com.foodya.backend.infrastructure.persistence.models.PaymentTransactionPersistenceModel;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Component
public class PaymentTransactionMapper {

    public PaymentTransaction toDomain(PaymentTransactionPersistenceModel m) {
        if (m == null) return null;
        PaymentTransaction d = new PaymentTransaction();
        d.setId(m.getId());
        d.setOrderId(m.getOrderId());
        d.setPaymentMethod(m.getPaymentMethod());
        d.setGateway(m.getGateway());
        d.setGatewayTxnRef(m.getGatewayTxnRef());
        d.setGatewayTxnNo(m.getGatewayTxnNo());
        d.setAmount(m.getAmount());
        d.setCurrency(m.getCurrency());
        d.setStatus(m.getStatus());
        d.setGatewayResponse(m.getGatewayResponse());
        d.setReturnUrl(m.getReturnUrl());
        d.setPaymentUrl(m.getPaymentUrl());
        d.setIpAddress(m.getIpAddress());
        d.setTransactionDate(m.getTransactionDate());
        d.setRefundTxnNo(m.getRefundTxnNo());
        d.setCreatedAt(m.getCreatedAt().toInstant());
        d.setUpdatedAt(m.getUpdatedAt().toInstant());
        d.setCompletedAt(m.getCompletedAt() == null ? null : m.getCompletedAt().toInstant());
        return d;
    }

    public PaymentTransactionPersistenceModel toPersistence(PaymentTransaction d) {
        if (d == null) return null;
        PaymentTransactionPersistenceModel m = new PaymentTransactionPersistenceModel();
        m.setId(d.getId());
        m.setOrderId(d.getOrderId());
        m.setPaymentMethod(d.getPaymentMethod());
        m.setGateway(d.getGateway());
        m.setGatewayTxnRef(d.getGatewayTxnRef());
        m.setGatewayTxnNo(d.getGatewayTxnNo());
        m.setAmount(d.getAmount());
        m.setCurrency(d.getCurrency());
        m.setStatus(d.getStatus());
        m.setGatewayResponse(d.getGatewayResponse());
        m.setReturnUrl(d.getReturnUrl());
        m.setPaymentUrl(d.getPaymentUrl());
        m.setIpAddress(d.getIpAddress());
        m.setTransactionDate(d.getTransactionDate());
        m.setRefundTxnNo(d.getRefundTxnNo());
        m.setCreatedAt(OffsetDateTime.ofInstant(d.getCreatedAt(), ZoneOffset.UTC));
        m.setUpdatedAt(OffsetDateTime.ofInstant(d.getUpdatedAt(), ZoneOffset.UTC));
        m.setCompletedAt(d.getCompletedAt() == null ? null : OffsetDateTime.ofInstant(d.getCompletedAt(), ZoneOffset.UTC));
        return m;
    }
}
