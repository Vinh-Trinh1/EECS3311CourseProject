package DashboardCategories;

import java.math.BigDecimal;

public interface CategoryComponent {
    BigDecimal getBalance();

    void updateBalance(BigDecimal amount);
}
