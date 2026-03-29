package com.grootan.storeflow.unit.validation;

import com.grootan.storeflow.repository.CategoryRepository;
import com.grootan.storeflow.validation.ExistsCategoryValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistsCategoryValidatorTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ConstraintValidatorContext context;

    @InjectMocks
    private ExistsCategoryValidator validator;

    @Test
    void isValidReturnsTrueWhenValueIsNull() {
        assertTrue(validator.isValid(null, context));
    }

    @Test
    void isValidReturnsTrueWhenCategoryExists() {
        when(categoryRepository.existsById(1L)).thenReturn(true);

        assertTrue(validator.isValid(1L, context));
    }

    @Test
    void isValidReturnsFalseWhenCategoryDoesNotExist() {
        when(categoryRepository.existsById(999L)).thenReturn(false);

        assertFalse(validator.isValid(999L, context));
    }
}