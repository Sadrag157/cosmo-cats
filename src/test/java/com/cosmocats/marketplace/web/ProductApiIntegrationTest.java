package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.TestcontainersConfiguration;
import com.cosmocats.marketplace.persistence.entity.CategoryEntity;
import com.cosmocats.marketplace.persistence.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@Transactional
class ProductApiIntegrationTest {

    private static final String ERROR_BASE = "https://cosmo-cats.market/errors/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoryRepository categoryRepository;

    private Long categoryId;

    @BeforeEach
    void setUp() {
        categoryId = categoryRepository.save(new CategoryEntity("Cat Toys", "Toys")).getId();
    }

    private String body(String name, String price, Long category) {
        return "{\"name\":\"" + name + "\",\"description\":\"A product\",\"price\":" + price
                + ",\"categoryId\":" + category + ",\"stock\":10}";
    }

    private String createProduct(String name) throws Exception {
        String response = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(name, "5.00", categoryId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return response.replaceAll(".*\"id\":(\\d+).*", "$1");
    }

    @Test
    void fullCrudCycleAgainstPostgres() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Star Mug", "5.00", categoryId)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.name").value("Star Mug"))
                .andExpect(jsonPath("$.categoryId").value(categoryId));

        String id = createProduct("Moon Rock");

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Moon Rock"));

        mockMvc.perform(put("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Moon Rock XL", "9.99", categoryId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Moon Rock XL"))
                .andExpect(jsonPath("$.price").value(9.99));

        mockMvc.perform(delete("/api/products/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "not-found"));
    }

    @Test
    void listReturnsPagedEnvelope() throws Exception {
        createProduct("Star Mug");
        createProduct("Moon Rock");
        createProduct("Comet Brush");

        mockMvc.perform(get("/api/products").param("page", "1").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2));
    }

    @Test
    void duplicateNameInSameCategoryReturns409() throws Exception {
        createProduct("Star Mug");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Star Mug", "7.00", categoryId)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "conflict"));
    }

    @Test
    void unknownCategoryReturns400() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Star Mug", "5.00", 987654L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "validation"))
                .andExpect(jsonPath("$.errors[0].field").value("categoryId"));
    }

    @Test
    void invalidBodyIsStillRejectedBeforeReachingTheDatabase() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"ab\",\"price\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(ERROR_BASE + "validation"));
    }
}