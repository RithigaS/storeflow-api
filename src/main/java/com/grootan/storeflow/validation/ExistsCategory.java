package com.grootan.storeflow.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = ExistsCategoryValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ExistsCategory {

    String message() default "Category does not exist";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}