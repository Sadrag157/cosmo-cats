package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.domain.Product;
import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.service.ProductService;
import com.cosmocats.marketplace.web.mapper.ProductMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
@Import(ProductMapper.class)
class ProductControllerTest {

    private static final String VALID_BODY = """
            {"name":"Star Mug","description":"A mug","price":5.00,"categoryId":1,"stock":10}
            """;

    private static final String ERROR_BASE = "https://cosmo-cats.market/errors/";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    private static Product product(Long id) {
        return new Product(id, "Star Mug", "A mug", new BigDecimal("5.00"), 1L, 10);
    }

    // ---------- positive scenarios ----------

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        when(productService.findPage(0, 20)).thenReturn(List.of(product(1L)));
        when(productService.count()).thenReturn(1L);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listComputesTotalPages() throws Exception {
        when(productService.findPage(0, 2)).thenReturn(List.of(product(1L), product(2L)));
        when(productService.count()).thenReturn(3L);

        mockMvc.perform(get("/api/products").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    @Test
    void getReturnsProduct() throws Exception {
        when(productService.findById(1L)).thenReturn(product(1L));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Star Mug"))
                .andExpect(jsonPath("$.price").value(5.0));
    }

    @Test
    void createReturns201WithLocationHeader() throws Exception {
        when(productService.create(any(Product.class))).thenReturn(product(4L));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/api/products/4")))
                .andExpect(jsonPath("$.id").value(4));
    }

    @Test
    void createAcceptsMissingOptionalFields() throws Exception {
        when(productService.create(any(Product.class))).thenReturn(product(4L));

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Star Mug\",\"price\":5.00,\"categoryId\":1}"))
                .andExpect(status().isCreated());
    }

    @Test
    void updateReturns200() throws Exception {
        when(productService.update(eq(1L), any(Product.class))).thenReturn(product(1L));

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void deleteReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(productService).delete(1L);
    }

    // ---------- negative scenarios: body validation ----------

    @Test
    void createWithSeveralInvalidFieldsReportsAllOfThem() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ab\",\"price\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "validation"))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.instance").value("/api/products"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("categoryId")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("price")))
                .andExpect(jsonPath("$.detail", containsString("Field 'categoryId'")))
                .andExpect(jsonPath("$.detail", containsString("Field 'name'")))
                .andExpect(jsonPath("$.detail", containsString("Field 'price'")));

        verify(productService, never()).create(any(Product.class));
    }

    @Test
    void createWithoutCosmicTermInNameReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Plain Mug\",\"price\":5.00,\"categoryId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.detail",
                        containsString("Field 'name' must contain a cosmic term (e.g. star, galaxy, comet).")));
    }

    @Test
    void createWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"price\":5.00,\"categoryId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));
    }

    @Test
    void createWithPriceHavingThreeDecimalsReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Star Mug\",\"price\":5.123,\"categoryId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("price")));
    }

    @Test
    void createWithZeroPriceReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Star Mug\",\"price\":0,\"categoryId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("price")));
    }

    @Test
    void createWithNegativeStockReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Star Mug\",\"price\":5.00,\"categoryId\":1,\"stock\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("stock")));
    }

    @Test
    void createWithTooLongDescriptionReturns400() throws Exception {
        String body = "{\"name\":\"Star Mug\",\"description\":\"" + "a".repeat(501)
                + "\",\"price\":5.00,\"categoryId\":1}";

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[*].field", hasItem("description")));
    }

    @Test
    void updateWithInvalidBodyReturns400() throws Exception {
        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ab\",\"price\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "validation"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("price")));
    }

    // ---------- negative scenarios: malformed input ----------

    @Test
    void createWithBrokenJsonReturns400MalformedRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "malformed-request"))
                .andExpect(jsonPath("$.instance").value("/api/products"));
    }

    @Test
    void createWithWrongValueTypeReturns400MalformedRequest() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Star Mug\",\"price\":\"cheap\",\"categoryId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "malformed-request"));
    }

    @Test
    void createWithoutBodyReturns400() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "malformed-request"));
    }

    @Test
    void getWithNonNumericIdReturns400MalformedRequest() throws Exception {
        mockMvc.perform(get("/api/products/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "malformed-request"))
                .andExpect(jsonPath("$.instance").value("/api/products/abc"))
                .andExpect(jsonPath("$.detail", containsString("'id'")));
    }

    // ---------- negative scenarios: query and path parameters ----------

    @Test
    void listWithTooLargeSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "1000"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "validation"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void listWithZeroSizeReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void listWithNegativePageReturns400() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("page"));
    }

    @Test
    void getWithZeroIdReturns400() throws Exception {
        mockMvc.perform(get("/api/products/0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "validation"))
                .andExpect(jsonPath("$.errors[0].field").value("id"));
    }

    @Test
    void deleteWithNegativeIdReturns400() throws Exception {
        mockMvc.perform(delete("/api/products/-5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"));

        verify(productService, never()).delete(any(Long.class));
    }

    // ---------- negative scenarios: not found, unsupported requests, server errors ----------

    @Test
    void getUnknownIdReturns404ProblemJson() throws Exception {
        when(productService.findById(999L)).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(get("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "not-found"))
                .andExpect(jsonPath("$.title").value("Product not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Product with id 999 was not found."))
                .andExpect(jsonPath("$.instance").value("/api/products/999"));
    }

    @Test
    void updateUnknownIdReturns404() throws Exception {
        when(productService.update(eq(999L), any(Product.class))).thenThrow(new ProductNotFoundException(999L));

        mockMvc.perform(put("/api/products/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "not-found"));
    }

    @Test
    void deleteUnknownIdReturns404() throws Exception {
        doThrow(new ProductNotFoundException(999L)).when(productService).delete(999L);

        mockMvc.perform(delete("/api/products/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "not-found"));
    }

    @Test
    void unsupportedMethodReturns405NotBare500() throws Exception {
        mockMvc.perform(patch("/api/products/1"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.type").exists())
                .andExpect(jsonPath("$.instance").value("/api/products/1"));
    }

    @Test
    void unsupportedMediaTypeReturns415() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    void unexpectedExceptionReturns500WithoutLeakingDetails() throws Exception {
        when(productService.findById(1L)).thenThrow(new IllegalStateException("secret internal failure"));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "internal"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."))
                .andExpect(jsonPath("$.instance").value("/api/products/1"))
                .andExpect(content().string(not(containsString("secret internal failure"))));
    }
}