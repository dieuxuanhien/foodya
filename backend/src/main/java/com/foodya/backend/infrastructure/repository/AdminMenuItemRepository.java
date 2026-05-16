package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.MenuItemPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AdminMenuItemRepository extends JpaRepository<MenuItemPersistenceModel, UUID> {

	@Query("""
			select m from MenuItemPersistenceModel m
			where m.deletedAt is null
				and (:restaurantId is null or m.restaurantId = :restaurantId)
		""")
	Page<MenuItemPersistenceModel> searchAdmin(@Param("restaurantId") UUID restaurantId,
													   @Param("taxonomyCode") String taxonomyCode,
													   @Param("keyword") String keyword,
													   Pageable pageable);
}
