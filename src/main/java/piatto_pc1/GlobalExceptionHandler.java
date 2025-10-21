package piatto_pc1;

import com.example.piatto_pc1.exception.BadRequest;
import com.example.piatto_pc1.exception.Conflict;
import com.example.piatto_pc1.exception.NotFound;
import com.example.piatto_pc1.exception.Error;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<Error> handleBadRequest(BadRequest ex) {
        com.example.piatto_pc1.exception.Error error = new com.example.piatto_pc1.exception.Error(
                "bad request",
                ex.getMessage(),
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);}

    @ExceptionHandler(NotFound.class)
    public ResponseEntity<Error> handleNotFound(NotFound ex) {
        Error error = new Error(
                "not found",
                ex.getMessage(),
                HttpStatus.NOT_FOUND.value());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);}

    @ExceptionHandler(Conflict.class)
    public ResponseEntity<Error> handleConflict(Conflict ex) {
        Error error = new Error(
                "conflict",
                ex.getMessage(),
                HttpStatus.CONFLICT.value());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);}

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Error> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Error error = new Error(
                "bad request",
                " ¡! el tipo de dato no es el esperado",
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);}

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Error> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> String.format("'%s': %s", fieldError.getField(), fieldError.getDefaultMessage()))
                .collect(Collectors.joining(" | "));
        Error error = new Error(
                "bad request",
                errores,
                HttpStatus.BAD_REQUEST.value());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);}}