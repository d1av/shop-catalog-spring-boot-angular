package io.davi.catalog.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.davi.catalog.dto.ProductDTO;
import io.davi.catalog.services.ProductService;
import io.davi.catalog.services.exceptions.DatabaseException;
import io.davi.catalog.services.exceptions.ResourceNotFoundException;
import io.davi.catalog.tests.Factory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(ProductResource.class)
public class ProductResourcesTests {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private ProductService service;

    @Autowired
    private ObjectMapper objectMapper;
    private PageImpl<ProductDTO> page;
    private ProductDTO productDTO;

    private Long existingId;
    private Long nonExistingId;
    private Long dependantId;

    @BeforeEach
    void setUp() throws Exception {

        existingId = 1L;
        nonExistingId = 220L;
        dependantId = 2L;

        productDTO = Factory.createProductDTO();
        page = new PageImpl<>(List.of(productDTO));
        when(service.findAllPaged(ArgumentMatchers.any()))
                .thenReturn(page);

        when(service.findById(existingId))
                .thenReturn(productDTO);
        when(service.findById(nonExistingId))
                .thenThrow(ResourceNotFoundException.class);

        when(service.update(eq(existingId), ArgumentMatchers.any()))
                .thenReturn(productDTO);
        when(service.update(eq(nonExistingId), ArgumentMatchers.any()))
                .thenThrow(ResourceNotFoundException.class);

        doNothing().when(service).delete(existingId);
        doThrow(ResourceNotFoundException.class).when(service).delete(nonExistingId);
        doThrow(DatabaseException.class).when(service).delete(dependantId);

        when(service.insert(ArgumentMatchers.any())).thenReturn(productDTO);


    }


    @Test
    public void deleteShouldThrowDatabaseExceptionWhenIdIsDependant() throws Exception {
        ResultActions result = mockMvc
                .perform(delete("/products/{id}",dependantId));
        result.andExpect(status().isBadRequest());
    }

    @Test
    public void deleteShouldThrowResourceNotFoundExceptionWhenIdNotExist() throws Exception {
        ResultActions result = mockMvc
                .perform(delete("/products/{id}", nonExistingId));

        result.andExpect(status().isNotFound());
    }


    @Test
    public void deleteShouldReturnNoContentWhenIdExist() throws Exception {
        ResultActions result = mockMvc
                .perform(delete("/products/{id}", existingId));

        result.andExpect(status().isNoContent());
    }


    @Test
    public void insertShouldReturnStatusCreatedAndProductDTO() throws Exception {
        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc
                .perform(post("/products")
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.name").exists());
        result.andExpect(jsonPath("$.price").exists());
        result.andExpect(jsonPath("$.date").exists());
        result.andExpect(jsonPath("$.description").exists());
    }


    @Test
    public void updateShouldReturnProductDTOWhenIdExist() throws Exception {

        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(put("/products/{id}", existingId)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").exists());
        result.andExpect(jsonPath("$.name").exists());
        result.andExpect(jsonPath("$.description").exists());
    }

    @Test
    public void updateShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {

        String jsonBody = objectMapper.writeValueAsString(productDTO);

        ResultActions result = mockMvc.perform(put("/products/{id}", nonExistingId)
                .content(jsonBody)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    public void findByIdShouldReturnProductWhenIdExists() throws Exception {
        ResultActions result = mockMvc
                .perform(get("/products/{id}", existingId)
                        .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isOk());
        result.andExpect(jsonPath("$.id").exists());
        result.andExpect(jsonPath("$.name").exists());
        result.andExpect(jsonPath("$.description").exists());
    }

    @Test
    public void findByIdShouldReturnNotFoundWhenIdDoesNotExist() throws Exception {
        ResultActions result = mockMvc.perform(get("/products/{id}", nonExistingId)
                .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isNotFound());
    }

    @Test
    public void findAllShouldReturnPage() throws Exception {
        ResultActions result =
                mockMvc.perform(get("/products")
                        .accept(MediaType.APPLICATION_JSON));
        result.andExpect(status().isOk());
    }

}
