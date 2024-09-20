package com.ws.chat.exception;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.ws.chat.responsebody.ResponseDTO;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

     @ExceptionHandler(NotFound.class)
     public ResponseEntity<ResponseDTO> handleNotFoundException(NotFound ex) {

          ResponseDTO error = ResponseDTO.builder()
                    .data("Not Found")
                    .statusCode(404)
                    .status(ex.getMessage())
                    .timeStamp(new Date())
                    .build();
          return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
     }

     @ExceptionHandler(MethodArgumentNotValidException.class)
     public ResponseEntity<ResponseDTO> handleValidationExceptions(MethodArgumentNotValidException ex) {
          Map<String, String> errors = new HashMap<>();
          ex.getBindingResult().getFieldErrors()
                    .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

          ResponseDTO error = ResponseDTO.builder()
                    .data("Invalid Input")
                    .statusCode(400)
                    .status(errors.toString())
                    .timeStamp(new Date())
                    .build();
          return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
     }

     @ExceptionHandler(DataAccessException.class)
     public ResponseEntity<ResponseDTO> handleDatabaseException(DataAccessException ex) {

          ResponseDTO error = ResponseDTO.builder()
                    .data("Database Error")
                    .statusCode(500)
                    .status(ex.getMessage())
                    .timeStamp(new Date())
                    .build();
          return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
     }

     @ExceptionHandler(Exception.class)
     public ResponseEntity<ResponseDTO> handleGenericException(Exception ex) {

          ResponseDTO error = ResponseDTO.builder()
                    .data("Internal Server Error")
                    .statusCode(500)
                    .status(ex.getMessage())
                    .timeStamp(new Date())
                    .build();
          return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
     }
}
