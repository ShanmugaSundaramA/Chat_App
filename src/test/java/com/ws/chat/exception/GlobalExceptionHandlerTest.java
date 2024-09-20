package com.ws.chat.exception;

import com.ws.chat.responsebody.ResponseDTO;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

     private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

     @Test
     void handleNotFoundExceptionTest() {

          NotFound notFoundException = new NotFound("Resource not found");

          ResponseEntity<ResponseDTO> response = exceptionHandler.handleNotFoundException(notFoundException);

          assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
          ResponseDTO body = response.getBody();
          assertEquals(404, body.getStatusCode());
          assertEquals("Resource not found", body.getStatus());
          assertEquals("Not Found", body.getData());

     }

     @Test
     void handleValidationExceptionsTest() {

          BindingResult bindingResult = mock(BindingResult.class);
          FieldError fieldError = new FieldError("object", "field", "default message");
          when(bindingResult.getFieldErrors()).thenReturn(Collections.singletonList(fieldError));
          MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

          ResponseEntity<ResponseDTO> response = exceptionHandler.handleValidationExceptions(ex);

          assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
          ResponseDTO body = response.getBody();
          assertEquals(400, body.getStatusCode());
          assertEquals("{field=default message}", body.getStatus());
          assertEquals("Invalid Input", body.getData());
     }

     @Test
     void handleDatabaseExceptionTest() {

          DataAccessException dataAccessException = new DataAccessException("Database access error") {
          };

          ResponseEntity<ResponseDTO> response = exceptionHandler.handleDatabaseException(dataAccessException);

          assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
          ResponseDTO body = response.getBody();
          assertEquals(500, body.getStatusCode());
          assertEquals("Database access error", body.getStatus());
          assertEquals("Database Error", body.getData());
     }

     @Test
     void HandleGenericExceptionTest() {

          Exception exception = new Exception("Exception...");
          ResponseEntity<ResponseDTO> response = exceptionHandler.handleGenericException(exception);

          assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
          ResponseDTO body = response.getBody();
          assertEquals(500, body.getStatusCode());
          assertEquals("Exception...", body.getStatus());
          assertEquals("Internal Server Error", body.getData());
     }
}