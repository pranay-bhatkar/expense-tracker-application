package com.expense_tracker.exception;

import com.expense_tracker.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // handle validation errors
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        ex.getBindingResult().getAllErrors().forEach((
                error) -> {
            String field = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(field, errorMessage);
        });

        ApiResponse<Map<String, String>> response = new ApiResponse<>(
                "error",
                "Validation failed. Please check your input",
                errors,
                HttpStatus.BAD_REQUEST.value()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }


    // handle all ApiExceptions
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                "error",
                ex.getMessage(),
                null,
                ex.getStatus().value()
        );
        return ResponseEntity.status(ex.getStatus()).body(response);
    }


    // handle any other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<String>> handleGeneralException(Exception ex) {
        ex.printStackTrace();
        ApiResponse<String> response = new ApiResponse<>(
                "error",
                "An unexpected error occurred.",
                ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR.value()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // Handle Spring Security Access Denied (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                "error",
                "Access denied",
                null,
                HttpStatus.FORBIDDEN.value()
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // Handle Authentication issues (401)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(AuthenticationException ex) {
        ApiResponse<Void> response = new ApiResponse<>(
                "error",
                "Invalid or missing token",
                null,
                HttpStatus.UNAUTHORIZED.value()
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }


    // handle all ApiExceptions

//    // handle user not found errors
//    @ExceptionHandler(UserNotFoundException.class)
//    @ResponseStatus(HttpStatus.NOT_FOUND)
//    public ResponseEntity<ApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex) {
//        ApiResponse<Void> response = new ApiResponse<>(
//                "error",
//                ex.getMessage(),
//                null,
//                HttpStatus.NOT_FOUND.value()
//        );
//        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
//    }
//
//    // handle user already exists
//    @ExceptionHandler(UserAlreadyExistException.class)
//    @ResponseStatus(HttpStatus.CONFLICT)
//    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyExistsException(UserAlreadyExistException ex) {
//        ApiResponse<Void> response = new ApiResponse<>(
//                "error",
//                ex.getMessage(),
//                null,
//                HttpStatus.CONFLICT.value()
//        );
//        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
//    }


}