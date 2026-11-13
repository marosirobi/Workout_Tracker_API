package com.marosirobi.workouttrackerapi.dto;

import com.marosirobi.workouttrackerapi.dto.user.CreateUserRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CreateUserRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private static List<String> invalidFields(CreateUserRequest request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .toList();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Password1!", "Árvíztűrő9_", "a1!a1!a1!"})
    void acceptsStrongPasswords(String password) {
        assertThat(invalidFields(new CreateUserRequest("User", "user@gmail.com", password))).isEmpty();
    }

    @ParameterizedTest(name = "{1}")
    @CsvSource(textBlock = """
            Ab1!,        too short
            Passwordd!,  no digit
            12345678!,   no letter
            Password123, no special char
            """)
    void rejectsWeakPasswords(String password, String reason) {
        assertThat(invalidFields(new CreateUserRequest("User", "user@gmail.com", password)))
                .as(reason)
                .contains("password");
    }

    @Test
    void rejectsTooLongPassword() {
        String password = "Aa1!" + "a".repeat(125);

        assertThat(invalidFields(new CreateUserRequest("User", "user@gmail.com", password))).contains("password");
    }

    @Test
    void validatesUsernameAndEmail() {
        assertThat(invalidFields(new CreateUserRequest("ab", "not-an-email", "Password1!")))
                .containsExactlyInAnyOrder("username", "email");
    }
}
