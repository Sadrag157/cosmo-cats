package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.TestcontainersConfiguration;
import com.cosmocats.marketplace.domain.Category;
import com.cosmocats.marketplace.domain.CategoryNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({TestcontainersConfiguration.class, CategoryService.class})
class CategoryServiceIntegrationTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void createAndFindById() {
        Category created = categoryService.create(new Category(null, "Cat Toys", "Toys"));

        Category found = categoryService.findById(created.id());

        assertThat(found.id()).isNotNull();
        assertThat(found.name()).isEqualTo("Cat Toys");
        assertThat(found.description()).isEqualTo("Toys");
    }

    @Test
    void findAllReturnsCategoriesInIdOrder() {
        categoryService.create(new Category(null, "Cat Toys", "Toys"));
        categoryService.create(new Category(null, "Cosmic Food", "Food"));

        assertThat(categoryService.findAll()).extracting(Category::name).containsExactly("Cat Toys", "Cosmic Food");
    }

    @Test
    void updateChangesNameAndDescription() {
        Category created = categoryService.create(new Category(null, "Cat Toys", "Toys"));

        Category updated = categoryService.update(created.id(), new Category(null, "Space Toys", "Better toys"));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(categoryService.findById(created.id()).name()).isEqualTo("Space Toys");
    }

    @Test
    void deleteRemovesCategory() {
        Category created = categoryService.create(new Category(null, "Cat Toys", "Toys"));

        categoryService.delete(created.id());

        assertThatThrownBy(() -> categoryService.findById(created.id()))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void unknownIdIsReportedForFindUpdateAndDelete() {
        assertThatThrownBy(() -> categoryService.findById(987654L)).isInstanceOf(CategoryNotFoundException.class);
        assertThatThrownBy(() -> categoryService.update(987654L, new Category(null, "X", "Y")))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThatThrownBy(() -> categoryService.delete(987654L)).isInstanceOf(CategoryNotFoundException.class);
    }
}