package com.foodya.backend.infrastructure.mapper;

import com.foodya.backend.domain.entities.OrderReview;
import com.foodya.backend.infrastructure.persistence.models.OrderReviewPersistenceModel;
import org.springframework.stereotype.Component;

@Component
public class OrderReviewMapper {

    public OrderReview toDomain(OrderReviewPersistenceModel model) {
        if (model == null) {
            return null;
        }

        OrderReview domain = new OrderReview();
        domain.setId(model.getId());
        domain.setOrderId(model.getOrderId());
        domain.setRestaurantId(model.getRestaurantId());
        domain.setCustomerUserId(model.getCustomerUserId());
        domain.setStars(model.getStars());
        domain.setComment(model.getComment());
        domain.setMerchantResponse(model.getMerchantResponse());
        domain.setRespondedAt(model.getRespondedAt());
        domain.setCreatedAt(model.getCreatedAt());
        domain.setUpdatedAt(model.getUpdatedAt());
        return domain;
    }

    public OrderReviewPersistenceModel toPersistence(OrderReview domain) {
        if (domain == null) {
            return null;
        }

        OrderReviewPersistenceModel model = new OrderReviewPersistenceModel();
        model.setId(domain.getId());
        model.setOrderId(domain.getOrderId());
        model.setRestaurantId(domain.getRestaurantId());
        model.setCustomerUserId(domain.getCustomerUserId());
        model.setStars(domain.getStars());
        model.setComment(domain.getComment());
        model.setMerchantResponse(domain.getMerchantResponse());
        model.setRespondedAt(domain.getRespondedAt());
        model.setCreatedAt(domain.getCreatedAt());
        model.setUpdatedAt(domain.getUpdatedAt());
        return model;
    }
}
