package com.buckpal.controller;

import com.buckpal.controller.CategoryController.CategoryCreateRequest;
import com.buckpal.entity.Category;
import com.buckpal.entity.User;
import com.buckpal.service.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController Unit Tests")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CategoryController categoryController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User testUser;
    private Category testCategory;
    private Category defaultCategory;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(categoryController).build();
        objectMapper = new ObjectMapper();
        
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        
        testCategory = new Category();
        testCategory.setId(1L);
        testCategory.setName("Test Custom Category");
        testCategory.setDescription("A test category");
        testCategory.setIconName("🛒");
        testCategory.setColorCode("#ef4444");
        testCategory.setUser(testUser);
        testCategory.setIsDefault(false);
        
        defaultCategory = new Category();
        defaultCategory.setId(2L);
        defaultCategory.setName("Default Category");
        defaultCategory.setDescription("A default system category");
        defaultCategory.setIconName("📊");
        defaultCategory.setColorCode("#3b82f6");
        defaultCategory.setUser(testUser);
        defaultCategory.setIsDefault(true);
    }

    @Nested
    @DisplayName("GET /api/categories/mapping")
    class GetCategoryMapping {
        
        @Test
        @DisplayName("Should return category mapping successfully")
        void shouldReturnCategoryMappingSuccessfully() throws Exception {
            // Given
            Map<String, String> categoryMapping = Map.of(
                "groceries", "needs",
                "entertainment", "wants",
                "salary", "income",
                "savings", "savings"
            );
            
            when(categoryService.getDetailedToBudgetCategoryMappingWithCustom())
                .thenReturn(categoryMapping);
            
            // When & Then
            mockMvc.perform(get("/api/categories/mapping")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.groceries").value("needs"))
                    .andExpect(jsonPath("$.entertainment").value("wants"))
                    .andExpect(jsonPath("$.salary").value("income"))
                    .andExpect(jsonPath("$.savings").value("savings"));
            
            verify(categoryService).getDetailedToBudgetCategoryMappingWithCustom();
        }
        
        @Test
        @DisplayName("Should handle service errors gracefully")
        void shouldHandleServiceErrorsGracefully() throws Exception {
            // Given
            when(categoryService.getDetailedToBudgetCategoryMappingWithCustom())
                .thenThrow(new RuntimeException("Service error"));
            
            // When & Then
            mockMvc.perform(get("/api/categories/mapping")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/categories/icons")
    class GetAvailableIcons {
        
        @Test
        @DisplayName("Should return available icons list")
        void shouldReturnAvailableIconsList() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/categories/icons"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").value("💰"))
                    .andExpect(jsonPath("$[1]").value("💼"))
                    .andExpect(jsonPath("$[2]").value("📈"))
                    .andExpect(jsonPath("$[3]").value("💸"));
            
            // No service calls needed - hardcoded list
            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("GET /api/categories/colors")
    class GetAvailableColors {
        
        @Test
        @DisplayName("Should return available colors list")
        void shouldReturnAvailableColorsList() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/categories/colors"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0]").value("#ef4444"))
                    .andExpect(jsonPath("$[1]").value("#f97316"))
                    .andExpect(jsonPath("$[2]").value("#f59e0b"));
            
            // No service calls needed - hardcoded list
            verifyNoInteractions(categoryService);
        }
    }

    @Nested
    @DisplayName("GET /api/categories")
    class GetUserCategories {
        
        @Test
        @DisplayName("Should return user categories when authenticated")
        void shouldReturnUserCategoriesWhenAuthenticated() throws Exception {
            // Given
            List<Category> userCategories = List.of(testCategory, defaultCategory);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoriesForUser(testUser)).thenReturn(userCategories);
            
            // When & Then
            mockMvc.perform(get("/api/categories")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(2))
                    .andExpect(jsonPath("$[0].name").value("Test Custom Category"))
                    .andExpect(jsonPath("$[1].name").value("Default Category"));
            
            verify(categoryService).getCategoriesForUser(testUser);
        }
        
        @Test
        @DisplayName("Should return all categories when not authenticated")
        void shouldReturnAllCategoriesWhenNotAuthenticated() throws Exception {
            // Given
            List<Category> allCategories = List.of(defaultCategory);
            
            when(categoryService.getAllCategories()).thenReturn(allCategories);
            
            // When & Then
            mockMvc.perform(get("/api/categories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Default Category"));
            
            verify(categoryService).getAllCategories();
            verify(categoryService, never()).getCategoriesForUser(any());
        }
        
        @Test
        @DisplayName("Should handle service errors gracefully")
        void shouldHandleServiceErrorsGracefully() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoriesForUser(testUser))
                .thenThrow(new RuntimeException("Service error"));
            
            // When & Then
            mockMvc.perform(get("/api/categories")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("GET /api/categories/{id}")
    class GetCategoryById {
        
        @Test
        @DisplayName("Should return category when user owns it")
        void shouldReturnCategoryWhenUserOwnsIt() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(testCategory));
            
            // When & Then
            mockMvc.perform(get("/api/categories/1")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Test Custom Category"))
                    .andExpect(jsonPath("$.isDefault").value(false));
            
            verify(categoryService).getCategoryById(1L);
        }
        
        @Test
        @DisplayName("Should return default category for any authenticated user")
        void shouldReturnDefaultCategoryForAnyAuthenticatedUser() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(2L)).thenReturn(Optional.of(defaultCategory));
            
            // When & Then
            mockMvc.perform(get("/api/categories/2")
                    .principal(authentication))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.name").value("Default Category"))
                    .andExpect(jsonPath("$.isDefault").value(true));
        }
        
        @Test
        @DisplayName("Should return 404 when category not found")
        void shouldReturn404WhenCategoryNotFound() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(get("/api/categories/999")
                    .principal(authentication))
                    .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("Should return 404 when user doesn't own non-default category")
        void shouldReturn404WhenUserDoesntOwnNonDefaultCategory() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Category otherUserCategory = new Category();
            otherUserCategory.setId(3L);
            otherUserCategory.setUser(otherUser);
            otherUserCategory.setIsDefault(false);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(3L)).thenReturn(Optional.of(otherUserCategory));
            
            // When & Then
            mockMvc.perform(get("/api/categories/3")
                    .principal(authentication))
                    .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("Should handle service errors gracefully")
        void shouldHandleServiceErrorsGracefully() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(1L))
                .thenThrow(new RuntimeException("Service error"));
            
            // When & Then
            mockMvc.perform(get("/api/categories/1")
                    .principal(authentication))
                    .andExpect(status().isInternalServerError());
        }
    }

    @Nested
    @DisplayName("POST /api/categories")
    class CreateCategory {
        
        @Test
        @DisplayName("Should create custom category successfully")
        void shouldCreateCustomCategorySuccessfully() throws Exception {
            // Given
            CategoryCreateRequest request = new CategoryCreateRequest(
                "New Custom Category",
                "A new custom category",
                "🎯",
                "#22c55e"
            );
            
            Category savedCategory = new Category();
            savedCategory.setId(3L);
            savedCategory.setName(request.getName());
            savedCategory.setDescription(request.getDescription());
            savedCategory.setIconName(request.getIconName());
            savedCategory.setColorCode(request.getColorCode());
            savedCategory.setUser(testUser);
            savedCategory.setIsDefault(false);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.saveCategory(any(Category.class))).thenReturn(savedCategory);
            
            // When & Then
            mockMvc.perform(post("/api/categories")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(3))
                    .andExpect(jsonPath("$.name").value("New Custom Category"))
                    .andExpect(jsonPath("$.description").value("A new custom category"))
                    .andExpect(jsonPath("$.iconName").value("🎯"))
                    .andExpect(jsonPath("$.colorCode").value("#22c55e"))
                    .andExpect(jsonPath("$.isDefault").value(false));
            
            verify(categoryService).saveCategory(any(Category.class));
        }
        
        @Test
        @DisplayName("Should handle creation errors gracefully")
        void shouldHandleCreationErrorsGracefully() throws Exception {
            // Given
            CategoryCreateRequest request = new CategoryCreateRequest(
                "Invalid Category",
                "Description",
                "🎯",
                "#invalid"
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.saveCategory(any(Category.class)))
                .thenThrow(new RuntimeException("Validation error"));
            
            // When & Then
            mockMvc.perform(post("/api/categories")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/categories/{id}")
    class UpdateCategory {
        
        @Test
        @DisplayName("Should update custom category successfully")
        void shouldUpdateCustomCategorySuccessfully() throws Exception {
            // Given
            CategoryCreateRequest updateRequest = new CategoryCreateRequest(
                "Updated Category Name",
                "Updated description",
                "🎨",
                "#8b5cf6"
            );
            
            Category updatedCategory = new Category();
            updatedCategory.setId(1L);
            updatedCategory.setName(updateRequest.getName());
            updatedCategory.setDescription(updateRequest.getDescription());
            updatedCategory.setIconName(updateRequest.getIconName());
            updatedCategory.setColorCode(updateRequest.getColorCode());
            updatedCategory.setUser(testUser);
            updatedCategory.setIsDefault(false);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(testCategory));
            when(categoryService.saveCategory(any(Category.class))).thenReturn(updatedCategory);
            
            // When & Then
            mockMvc.perform(put("/api/categories/1")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Category Name"))
                    .andExpect(jsonPath("$.description").value("Updated description"))
                    .andExpect(jsonPath("$.iconName").value("🎨"))
                    .andExpect(jsonPath("$.colorCode").value("#8b5cf6"));
            
            verify(categoryService).getCategoryById(1L);
            verify(categoryService).saveCategory(any(Category.class));
        }
        
        @Test
        @DisplayName("Should return 404 when category not found for update")
        void shouldReturn404WhenCategoryNotFoundForUpdate() throws Exception {
            // Given
            CategoryCreateRequest updateRequest = new CategoryCreateRequest(
                "Updated Name", "Updated desc", "🎯", "#22c55e"
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(put("/api/categories/999")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isNotFound());
        }
        
        @Test
        @DisplayName("Should return 403 when updating other user's category")
        void shouldReturn403WhenUpdatingOtherUsersCategory() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Category otherUserCategory = new Category();
            otherUserCategory.setId(3L);
            otherUserCategory.setUser(otherUser);
            otherUserCategory.setIsDefault(false);
            
            CategoryCreateRequest updateRequest = new CategoryCreateRequest(
                "Updated Name", "Updated desc", "🎯", "#22c55e"
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(3L)).thenReturn(Optional.of(otherUserCategory));
            
            // When & Then
            mockMvc.perform(put("/api/categories/3")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
            
            verify(categoryService, never()).saveCategory(any());
        }
        
        @Test
        @DisplayName("Should return 403 when updating default category")
        void shouldReturn403WhenUpdatingDefaultCategory() throws Exception {
            // Given
            CategoryCreateRequest updateRequest = new CategoryCreateRequest(
                "Updated Default", "Updated desc", "🎯", "#22c55e"
            );
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(2L)).thenReturn(Optional.of(defaultCategory));
            
            // When & Then
            mockMvc.perform(put("/api/categories/2")
                    .principal(authentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isForbidden());
            
            verify(categoryService, never()).saveCategory(any());
        }
    }

    @Nested
    @DisplayName("DELETE /api/categories/{id}")
    class DeleteCategory {
        
        @Test
        @DisplayName("Should delete custom category successfully")
        void shouldDeleteCustomCategorySuccessfully() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(1L)).thenReturn(Optional.of(testCategory));
            
            // When & Then
            mockMvc.perform(delete("/api/categories/1")
                    .principal(authentication))
                    .andExpect(status().isNoContent());
            
            verify(categoryService).getCategoryById(1L);
            verify(categoryService).deleteCategory(1L);
        }
        
        @Test
        @DisplayName("Should return 404 when category not found for delete")
        void shouldReturn404WhenCategoryNotFoundForDelete() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(999L)).thenReturn(Optional.empty());
            
            // When & Then
            mockMvc.perform(delete("/api/categories/999")
                    .principal(authentication))
                    .andExpect(status().isNotFound());
            
            verify(categoryService, never()).deleteCategory(any());
        }
        
        @Test
        @DisplayName("Should return 403 when deleting other user's category")
        void shouldReturn403WhenDeletingOtherUsersCategory() throws Exception {
            // Given
            User otherUser = new User();
            otherUser.setId(2L);
            
            Category otherUserCategory = new Category();
            otherUserCategory.setId(3L);
            otherUserCategory.setUser(otherUser);
            otherUserCategory.setIsDefault(false);
            
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(3L)).thenReturn(Optional.of(otherUserCategory));
            
            // When & Then
            mockMvc.perform(delete("/api/categories/3")
                    .principal(authentication))
                    .andExpect(status().isForbidden());
            
            verify(categoryService, never()).deleteCategory(any());
        }
        
        @Test
        @DisplayName("Should return 403 when deleting default category")
        void shouldReturn403WhenDeletingDefaultCategory() throws Exception {
            // Given
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(categoryService.getCategoryById(2L)).thenReturn(Optional.of(defaultCategory));
            
            // When & Then
            mockMvc.perform(delete("/api/categories/2")
                    .principal(authentication))
                    .andExpect(status().isForbidden());
            
            verify(categoryService, never()).deleteCategory(any());
        }
    }
}