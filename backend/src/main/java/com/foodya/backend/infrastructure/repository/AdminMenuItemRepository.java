package com.foodya.backend.infrastructure.repository;

import com.foodya.backend.infrastructure.persistence.models.MenuItemPersistenceModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface AdminMenuItemRepository extends JpaRepository<MenuItemPersistenceModel, UUID> {

		@Query(value = """
						select m from MenuItemPersistenceModel m
						where m.deletedAt is null
							and (:restaurantId is null or m.restaurantId = :restaurantId)
							and (:keyword is null or lower(m.name) like lower(concat('%', :keyword, '%')))
							and (:taxonomyCode is null or exists (
										select 1
										from MenuItemPersistenceModel m2
										join m2.taxonomyCodes tc
										where m2.id = m.id and tc = :taxonomyCode
							))
						""",
						countQuery = """
						select count(m) from MenuItemPersistenceModel m
						where m.deletedAt is null
							and (:restaurantId is null or m.restaurantId = :restaurantId)
							and (:keyword is null or lower(m.name) like lower(concat('%', :keyword, '%')))
							and (:taxonomyCode is null or exists (
										select 1
										from MenuItemPersistenceModel m2
										join m2.taxonomyCodes tc
										where m2.id = m.id and tc = :taxonomyCode
							))
						""")
		Page<MenuItemPersistenceModel> searchAdmin(@Param("restaurantId") UUID restaurantId,
																							 @Param("taxonomyCode") String taxonomyCode,
																							 @Param("keyword") String keyword,
																							 Pageable pageable);
}
