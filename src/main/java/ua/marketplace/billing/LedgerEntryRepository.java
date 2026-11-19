package ua.marketplace.billing;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("select e from LedgerEntry e join fetch e.account where e.transaction.id = :txId")
    List<LedgerEntry> findFetchedByTransactionId(@Param("txId") UUID txId);

    @Query(value = "select e from LedgerEntry e join fetch e.transaction t join fetch e.account a "
            + "where a.sellerId = :sellerId order by t.createdAt desc, e.id desc",
           countQuery = "select count(e) from LedgerEntry e where e.account.sellerId = :sellerId")
    Page<LedgerEntry> findBySeller(@Param("sellerId") Long sellerId, Pageable pageable);

    @Query(value = "select e from LedgerEntry e join fetch e.transaction t join fetch e.account a "
            + "where a.sellerId = :sellerId and t.txType = :type order by t.createdAt desc, e.id desc",
           countQuery = "select count(e) from LedgerEntry e where e.account.sellerId = :sellerId and e.transaction.txType = :type")
    Page<LedgerEntry> findBySellerAndType(@Param("sellerId") Long sellerId, @Param("type") TransactionType type,
                                          Pageable pageable);
}
