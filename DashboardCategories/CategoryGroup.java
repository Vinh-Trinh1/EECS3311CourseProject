package DashboardCategories;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CategoryGroup implements CategoryComponent {
    private final String name;
    private final List<CategoryComponent> children = new ArrayList<>();

    public CategoryGroup(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Category group name is required.");
        }
        this.name = name.trim();
    }

    public String getName() {
        return name;
    }

    public void add(CategoryComponent category) {
        children.add(Objects.requireNonNull(category, "Category is required."));
    }

    public boolean remove(CategoryComponent category) {
        return children.remove(category);
    }

    public List<CategoryComponent> getChildren() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public BigDecimal getBalance() {
        BigDecimal total = BigDecimal.ZERO;
        for (CategoryComponent child : children) {
            total = total.add(child.getBalance());
        }
        return total;
    }

    @Override
    public void updateBalance(BigDecimal amount) {
        throw new UnsupportedOperationException(
                "A category group's balance is calculated from its children.");
    }

    @Override
    public String toString() {
        return name + ": $" + getBalance().toPlainString();
    }
}
