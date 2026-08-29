package com.oms.auth.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LoginRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void weakPasswordShouldFailValidation() {
        LoginRequest request = new LoginRequest("testuser", "password");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void strongPasswordShouldPassValidation() {
        LoginRequest request = new LoginRequest("testuser", "Password123!");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Test
    void passwordWithoutSpecialCharacterShouldFailValidation() {
        LoginRequest request = new LoginRequest("testuser", "Password123");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void passwordWithoutUppercaseShouldFailValidation() {
        LoginRequest request = new LoginRequest("testuser", "password123!");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void passwordWithoutLowercaseShouldFailValidation() {
        LoginRequest request = new LoginRequest("testuser", "PASSWORD123!");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void passwordWithoutDigitShouldFailValidation() {
        LoginRequest request = new LoginRequest("testuser", "Password!!!!");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void shortPasswordShouldFailValidation() {
        LoginRequest request = new LoginRequest("testuser", "Pa1!");

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }

    @Test
    void nullPasswordShouldFailValidation() {
        LoginRequest request = new LoginRequest("testuser", null);

        Set<ConstraintViolation<LoginRequest>> violations = validator.validate(request);

        assertThat(violations)
                .anyMatch(violation -> violation.getPropertyPath().toString().equals("password"));
    }
}