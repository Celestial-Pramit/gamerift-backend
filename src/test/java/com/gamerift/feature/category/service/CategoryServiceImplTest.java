package com.gamerift.feature.category.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.gamerift.feature.category.dto.CategoryRequest;
import com.gamerift.feature.category.dto.CategoryResponse;
import com.gamerift.feature.category.model.Category;
import com.gamerift.feature.category.repository.CategoryRepository;
import com.gamerift.feature.stream.repository.LiveStreamRepository;
import com.gamerift.shared.exception.BadRequestException;
import com.gamerift.shared.exception.ConflictException;
import com.gamerift.shared.exception.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests of the category SERVICE only: no Spring, no database.
 * Mockito creates fake repositories so we control exactly what they return.
 *
 * @Mock        = a fake repository.
 * @InjectMocks = creates the real CategoryServiceImpl and gives it the fakes.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private LiveStreamRepository liveStreamRepository;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category(String id, String name, String slug) {
        Category c = new Category();
        c.setId(id);
        c.setName(name);
        c.setSlug(slug);
        c.setIcon("x");
        return c;
    }

    private CategoryRequest request(String name, String slug) {
        CategoryRequest r = new CategoryRequest();
        r.setName(name);
        r.setSlug(slug);
        r.setIcon("y");
        return r;
    }

    // -------------------------------------------------------------- reads

    @Test
    void getAll_returnsOneDtoPerCategory() {
        when(categoryRepository.findAll()).thenReturn(List.of(category("1", "Esports", "esports"), category("2", "FPS", "fps")));

        List<CategoryResponse> result = categoryService.getAll();

        assertEquals(2, result.size());
        assertEquals("esports", result.get(0).getSlug());
        assertEquals("FPS", result.get(1).getName());
    }

    @Test
    void getBySlug_whenCategoryExists_returnsIt() {
        when(categoryRepository.findBySlug("fps")).thenReturn(Optional.of(category("2", "FPS", "fps")));

        assertEquals("FPS", categoryService.getBySlug("fps").getName());
    }

    @Test
    void getBySlug_whenCategoryMissing_throwsNotFound() {
        when(categoryRepository.findBySlug("nope")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.getBySlug("nope"));
    }

    // ------------------------------------------------------------- create

    @Test
    void create_storesTheSlugInLowerCase() {
        when(categoryRepository.existsBySlug("new-games")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        CategoryResponse response = categoryService.create(request("  New Games ", "New-Games"));

        ArgumentCaptor<Category> saved = ArgumentCaptor.forClass(Category.class);
        verify(categoryRepository).save(saved.capture());
        assertEquals("new-games", saved.getValue().getSlug());
        assertEquals("New Games", saved.getValue().getName());
        assertEquals("new-games", response.getSlug());
    }

    @Test
    void create_duplicateSlug_throwsConflictAndSavesNothing() {
        when(categoryRepository.existsBySlug("fps")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.create(request("FPS 2", "fps")));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    // ------------------------------------------------------------- update

    @Test
    void update_sameSlug_onlyChangesNameAndIcon() {
        Category existing = category("2", "FPS", "fps");
        when(categoryRepository.findById("2")).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        CategoryResponse response = categoryService.update("2", request("First Person Shooters", "FPS"));

        assertEquals("First Person Shooters", response.getName());
        assertEquals("fps", response.getSlug());
        verify(liveStreamRepository, never()).existsByCategorySlug(any());
    }

    @Test
    void update_newSlug_isAllowedWhenNoStreamUsesTheOldOne() {
        Category existing = category("2", "FPS", "fps");
        when(categoryRepository.findById("2")).thenReturn(Optional.of(existing));
        when(categoryRepository.existsBySlug("shooters")).thenReturn(false);
        when(liveStreamRepository.existsByCategorySlug("fps")).thenReturn(false);
        when(categoryRepository.save(any(Category.class))).thenAnswer(call -> call.getArgument(0));

        assertEquals("shooters", categoryService.update("2", request("FPS", "shooters")).getSlug());
    }

    @Test
    void update_changingASlugThatStreamsUse_isRejected() {
        when(categoryRepository.findById("2")).thenReturn(Optional.of(category("2", "FPS", "fps")));
        when(categoryRepository.existsBySlug("shooters")).thenReturn(false);
        when(liveStreamRepository.existsByCategorySlug("fps")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> categoryService.update("2", request("FPS", "shooters")));
        verify(categoryRepository, never()).save(any(Category.class));
    }

    @Test
    void update_slugTakenByAnotherCategory_throwsConflict() {
        when(categoryRepository.findById("2")).thenReturn(Optional.of(category("2", "FPS", "fps")));
        when(categoryRepository.existsBySlug("esports")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.update("2", request("FPS", "esports")));
    }

    @Test
    void update_unknownId_throwsNotFound() {
        when(categoryRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.update("zzz", request("A", "a")));
    }

    // ------------------------------------------------------------- delete

    @Test
    void delete_unusedCategory_isRemoved() {
        Category existing = category("2", "FPS", "fps");
        when(categoryRepository.findById("2")).thenReturn(Optional.of(existing));
        when(liveStreamRepository.existsByCategorySlug("fps")).thenReturn(false);

        categoryService.delete("2");

        verify(categoryRepository).delete(existing);
    }

    @Test
    void delete_categoryUsedByStreams_throwsConflict() {
        Category existing = category("2", "FPS", "fps");
        when(categoryRepository.findById("2")).thenReturn(Optional.of(existing));
        when(liveStreamRepository.existsByCategorySlug("fps")).thenReturn(true);

        assertThrows(ConflictException.class, () -> categoryService.delete("2"));
        verify(categoryRepository, never()).delete(any(Category.class));
    }

    @Test
    void delete_unknownId_throwsNotFound() {
        when(categoryRepository.findById("zzz")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.delete("zzz"));
    }
}
