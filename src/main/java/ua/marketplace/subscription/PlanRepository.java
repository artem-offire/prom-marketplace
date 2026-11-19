package ua.marketplace.subscription;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, PlanCode> {

    List<Plan> findAllByOrderByMonthlyPriceAsc();
}
