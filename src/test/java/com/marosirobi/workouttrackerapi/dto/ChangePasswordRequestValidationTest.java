package com.marosirobi.workouttrackerapi.dto;

import com.marosirobi.workouttrackerapi.dto.user.ChangePasswordRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ChangePasswordRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static List<String> invalidFields(ChangePasswordRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }

    @Test
    void oldWeakCurrentPassword_isAllowed() {
        assertThat(invalidFields(new ChangePasswordRequest("jelszo", "Password1!"))).isEmpty();
    }

    @Test
    void newPassword_mustFollowPolicy() {
        assertThat(invalidFields(new ChangePasswordRequest("jelszo", "weak"))).containsOnly("newPassword");
    }

    @Test
    void currentPassword_isRequired() {
        assertThat(invalidFields(new ChangePasswordRequest("", "Password1!"))).containsOnly("currentPassword");
    }
}
