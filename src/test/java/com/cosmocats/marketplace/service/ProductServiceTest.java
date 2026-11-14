package com.cosmocats.marketplace.service;

import com.cosmocats.marketplace.domain.CategoryNotFoundException;
import com.cosmocats.marketplace.domain.DuplicateProductException;
import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.entity.ProductEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import com.cosmocats.marketplace.persistence.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ProductService service;

    private CategoryEntity category;

    @BeforeEach
    void setUp() {
        category = new CategoryEntity("Cat Toys", "Toys");
        ReflectionTestUtils.setField(category, "id", 1L);
    }

    private ProductEntity entity(Long id, String name) {
        ProductEntity entity = new ProductEntity(name, "Description", new BigDecimal("9.99"), category, 5);
        ReflectionTestUtils.setField(entity, "id", id);
        return entity;
    }

    private Product input(String name, Long categoryId) {
        return new Product(null, name, "Description", new BigDecimal("9.99"), categoryId, 5);
    }

    @Test
    void createStoresProductInExistingCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdAndName(1L, "Star Mug")).thenReturn(false);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(invocation -> {
            ProductEntity saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 4L);
            return saved;
        });

        Product created = service.create(input("Star Mug", 1L));

        assertThat(created.id()).isEqualTo(4L);
        assertThat(created.categoryId()).isEqualTo(1L);
        assertThat(created.name()).isEqualTo("Star Mug");
    }

    @Test
    void createThrowsWhenCategoryDoesNotExist() {
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(input("Star Mug", 9L)))
                .isInstanceOf(CategoryNotFoundException.class);
        verifyNoInteractions(productRepository);
    }

    @Test
    void createThrowsWhenNameAlreadyExistsInCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdAndName(1L, "Star Mug")).thenReturn(true);

        assertThatThrownBy(() -> service.create(input("Star Mug", 1L)))
                .isInstanceOf(DuplicateProductException.class);
        verify(productRepository, never()).save(any(ProductEntity.class));
    }

    @Test
    void findPageRequestsTheRightPageAndMapsEntities() {
        when(productRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity(1L, "Star Mug"), entity(2L, "Moon Rock"))));

        List<Product> page = service.findPage(1, 2);

        assertThat(page).extracting(Product::name).containsExactly("Star Mug", "Moon Rock");
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(productRepository).findAll(captor.capture());
        assertThat(captor.getValue().getPageNumber()).isEqualTo(1);
        assertThat(captor.getValue().getPageSize()).isEqualTo(2);
    }

    @Test
    void countDelegatesToRepository() {
        when(productRepository.count()).thenReturn(3L);

        assertThat(service.count()).isEqualTo(3L);
    }

    @Test
    void findByIdReturnsMappedProduct() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(entity(2L, "Cosmic Milk")));

        Product product = service.findById(2L);

        assertThat(product.id()).isEqualTo(2L);
        assertThat(product.name()).isEqualTo("Cosmic Milk");
    }

    @Test
    void findByIdThrowsForUnknownId() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessage("Product with id 999 was not found");
    }

    @Test
    void updateReplacesDataAndKeepsId() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(entity(2L, "Cosmic Milk")));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdAndNameAndIdNot(1L, "Cosmic Oat Milk", 2L)).thenReturn(false);

        Product updated = service.update(2L, input("Cosmic Oat Milk", 1L));

        assertThat(updated.id()).isEqualTo(2L);
        assertThat(updated.name()).isEqualTo("Cosmic Oat Milk");
    }

    @Test
    void updateThrowsForUnknownProduct() {
        when(productRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(999L, input("Star Mug", 1L)))
                .isInstanceOf(ProductNotFoundException.class);
        verifyNoInteractions(categoryRepository);
    }

    @Test
    void updateThrowsForUnknownCategory() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(entity(2L, "Cosmic Milk")));
        when(categoryRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(2L, input("Star Mug", 9L)))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void updateThrowsWhenNameTakenByAnotherProduct() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(entity(2L, "Cosmic Milk")));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(productRepository.existsByCategoryIdAndNameAndIdNot(1L, "Star Mug", 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.update(2L, input("Star Mug", 1L)))
                .isInstanceOf(DuplicateProductException.class);
    }

    @Test
    void deleteRemovesExistingProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(productRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsForUnknownProduct() {
        when(productRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(999L)).isInstanceOf(ProductNotFoundException.class);
        verify(productRepository, never()).deleteById(anyLong());
    }
}