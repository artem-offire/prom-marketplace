package ua.marketplace.catalog;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    long countBySellerIdAndStatusNot(Long sellerId, ProductStatus status);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    List<Product> findByIdIn(Collection<Long> ids);

    /**
     * Символи подстановки LIKE (% і _) у пошуковому рядку не екрануються: відома межа.
     */
    @Query("select p from Product p where p.status = ua.marketplace.catalog.ProductStatus.ACTIVE "
            + "and p.sellerId in :sellerIds and lower(p.title) like :pattern")
    Page<Product> searchVisible(@Param("sellerIds") Collection<Long> sellerIds,
                                @Param("pattern") String pattern, Pageable pageable);

    @Query("select p from Product p where p.status = ua.marketplace.catalog.ProductStatus.ACTIVE "
            + "and p.sellerId in :sellerIds and p.categoryId = :categoryId and lower(p.title) like :pattern")
    Page<Product> searchVisibleInCategory(@Param("sellerIds") Collection<Long> sellerIds,
                                          @Param("categoryId") Long categoryId,
                                          @Param("pattern") String pattern, Pageable pageable);
}
