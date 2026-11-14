package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.TestcontainersConfiguration;
import com.cosmocats.marketplace.domain.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.DuplicateProductException;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, ProductService.class})
class ProductServiceIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        categoryId = categoryRepository.save(new CategoryEntity("Cat Toys", "Toys")).getId();
    }

    private Product input(String name) {
        return new Product(null, name, "Description", new BigDecimal("9.99"), categoryId, 5);
    }

    @Test
    void createPersistsProductAndAssignsId() {
        Product created = productService.create(input("Star Mug"));

        assertThat(created.id()).isNotNull();
        assertThat(productService.findById(created.id()).name()).isEqualTo("Star Mug");
        assertThat(productService.findById(created.id()).price()).isEqualByComparingTo("9.99");
    }

    @Test
    void createRejectsDuplicateNameInSameCategory() {
        productService.create(input("Star Mug"));

        assertThatThrownBy(() -> productService.create(input("Star Mug")))
                .isInstanceOf(DuplicateProductException.class);
    }

    @Test
    void createRejectsUnknownCategory() {
        Product product = new Product(null, "Star Mug", "Description", new BigDecimal("9.99"), 987654L, 5);

        assertThatThrownBy(() -> productService.create(product))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void findByIdThrowsForUnknownId() {
        assertThatThrownBy(() -> productService.findById(987654L))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void findPageAndCountWorkTogether() {
        productService.create(input("Star Mug"));
        productService.create(input("Moon Rock"));
        productService.create(input("Comet Brush"));

        List<Product> firstPage = productService.findPage(0, 2);
        List<Product> secondPage = productService.findPage(1, 2);

        assertThat(firstPage).extracting(Product::name).containsExactly("Star Mug", "Moon Rock");
        assertThat(secondPage).extracting(Product::name).containsExactly("Comet Brush");
        assertThat(productService.findPage(5, 2)).isEmpty();
        assertThat(productService.count()).isEqualTo(3);
    }

    @Test
    void updateReplacesFieldsAndKeepsId() {
        Product created = productService.create(input("Star Mug"));

        Product updated = productService.update(created.id(),
                new Product(null, "Star Mug XL", "Bigger", new BigDecimal("12.00"), categoryId, 7));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo("Star Mug XL");
        assertThat(productService.findById(created.id()).stock()).isEqualTo(7);
    }

    @Test
    void updateAllowsKeepingTheSameName() {
        Product created = productService.create(input("Star Mug"));

        Product updated = productService.update(created.id(), input("Star Mug"));

        assertThat(updated.name()).isEqualTo("Star Mug");
    }

    @Test
    void updateRejectsNameOfAnotherProduct() {
        productService.create(input("Star Mug"));
        Product other = productService.create(input("Moon Rock"));

        assertThatThrownBy(() -> productService.update(other.id(), input("Star Mug")))
                .isInstanceOf(DuplicateProductException.class);
    }

    @Test
    void updateThrowsForUnknownId() {
        assertThatThrownBy(() -> productService.update(987654L, input("Star Mug")))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteRemovesProduct() {
        Product created = productService.create(input("Star Mug"));

        productService.delete(created.id());

        assertThatThrownBy(() -> productService.findById(created.id()))
                .isInstanceOf(ProductNotFoundException.class);
    }

    @Test
    void deleteThrowsForUnknownId() {
        assertThatThrownBy(() -> productService.delete(987654L))
                .isInstanceOf(ProductNotFoundException.class);
    }
}