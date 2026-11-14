package com.cosmocats.marketplace.persistence;

import com.cosmocats.marketplace.TestcontainersConfiguration;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.projection.ProductSummary;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import com.cosmocats.marketplace.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private CategoryEntity toys;
    private CategoryEntity food;

    @BeforeEach
    void setUp() {
        toys = categoryRepository.save(new CategoryEntity("Cat Toys", "Toys"));
        food = categoryRepository.save(new CategoryEntity("Cosmic Food", "Food"));
    }

    private ProductEntity product(String name, String price, CategoryEntity category) {
        return new ProductEntity(name, "Description", new BigDecimal(price), category, 10);
    }

    @Test
    void savedProductGetsIdFromSequence() {
        ProductEntity saved = productRepository.save(product("Star Mug", "5.00", toys));

        assertThat(saved.getId()).isNotNull().isPositive();
    }

    @Test
    void productSurvivesRoundTripThroughDatabase() {
        Long id = productRepository.save(product("Star Mug", "5.50", toys)).getId();
        entityManager.flush();
        entityManager.clear();

        ProductEntity loaded = productRepository.findById(id).orElseThrow();

        assertThat(loaded.getName()).isEqualTo("Star Mug");
        assertThat(loaded.getPrice()).isEqualByComparingTo("5.50");
        assertThat(loaded.toDomain().categoryId()).isEqualTo(toys.getId());
    }

    @Test
    void existsByCategoryAndNameChecksTheCategoryScope() {
        productRepository.save(product("Star Mug", "5.00", toys));

        assertThat(productRepository.existsByCategoryIdAndName(toys.getId(), "Star Mug")).isTrue();
        assertThat(productRepository.existsByCategoryIdAndName(food.getId(), "Star Mug")).isFalse();
    }

    @Test
    void existsIgnoringGivenIdAllowsKeepingOwnName() {
        Long id = productRepository.save(product("Star Mug", "5.00", toys)).getId();

        assertThat(productRepository.existsByCategoryIdAndNameAndIdNot(toys.getId(), "Star Mug", id)).isFalse();
        assertThat(productRepository.existsByCategoryIdAndNameAndIdNot(toys.getId(), "Star Mug", id + 1)).isTrue();
    }

    @Test
    void sameNameInDifferentCategoriesIsAllowed() {
        productRepository.save(product("Star Mug", "5.00", toys));
        productRepository.save(product("Star Mug", "5.00", food));

        productRepository.flush();

        assertThat(productRepository.count()).isEqualTo(2);
    }

    @Test
    void projectionReturnsOnlyRequestedCategorySortedByPrice() {
        productRepository.save(product("Expensive Comet", "30.00", toys));
        productRepository.save(product("Cheap Star", "3.00", toys));
        productRepository.save(product("Moon Milk", "1.00", food));

        List<ProductSummary> summaries = productRepository.findByCategoryIdOrderByPriceAsc(toys.getId());

        assertThat(summaries).extracting(ProductSummary::getName).containsExactly("Cheap Star", "Expensive Comet");
        assertThat(summaries.get(0).getPrice()).isEqualByComparingTo("3.00");
    }

    @Test
    void databaseRejectsNonPositivePrice() {
        assertThatThrownBy(() -> productRepository.saveAndFlush(product("Free Star", "0.00", toys)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateNameInSameCategory() {
        productRepository.save(product("Star Mug", "5.00", toys));
        productRepository.flush();

        assertThatThrownBy(() -> productRepository.saveAndFlush(product("Star Mug", "7.00", toys)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}