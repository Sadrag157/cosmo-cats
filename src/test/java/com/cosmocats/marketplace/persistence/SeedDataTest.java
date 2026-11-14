package com.cosmocats.marketplace.persistence;

import com.cosmocats.marketplace.TestcontainersConfiguration;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import com.cosmocats.marketplace.persistence.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.liquibase.contexts=seed")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class SeedDataTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void seedChangelogCreatesCategoriesAndProducts() {
        assertThat(categoryRepository.count()).isEqualTo(2);
        assertThat(productRepository.findAll(Sort.by("id")))
                .extracting(ProductEntity::getName)
                .containsExactly("Anti-Gravity Yarn Ball", "Cosmic Milk", "Comet Tail Scratcher");
    }
}