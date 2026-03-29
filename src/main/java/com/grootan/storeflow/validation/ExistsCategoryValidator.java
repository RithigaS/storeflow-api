package com.grootan.storeflow.validation;

import com.grootan.storeflow.repository.CategoryRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExistsCategoryValidator implements ConstraintValidator<ExistsCategory, Long> {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public boolean isValid(Long value, ConstraintValidatorContext context) {
        if (value == null) return true; // handled by @NotNull
        return categoryRepository.existsById(value);
    }
}