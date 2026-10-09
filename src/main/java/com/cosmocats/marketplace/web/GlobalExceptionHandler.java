package com.cosmocats.marketplace.web;

import com.cosmocats.marketplace.domain.ProductNotFoundException;
import com.cosmocats.marketplace.web.dto.FieldViolation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String ERROR_TYPE_BASE = "https://cosmo-cats.market/errors/";
    private static final URI BLANK_TYPE = URI.create("about:blank");

    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<Object> handleProductNotFound(ProductNotFoundException ex, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.NOT_FOUND, "not-found", "Product not found",
                "Product with id " + ex.getProductId() + " was not found.", request);
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception while processing request to {}", requestPath(request), ex);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal", "Internal server error",
                "An unexpected error occurred.", request);
        return handleExceptionInternal(ex, problem, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldViolation(error.getField(),
                        Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid")))
                .toList();
        return validationResponse(ex, violations, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = new ArrayList<>();
        for (ParameterValidationResult result : ex.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                parameterErrors.getFieldErrors().forEach(error -> violations.add(new FieldViolation(
                        error.getField(), Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid"))));
            } else {
                String name = Objects.requireNonNullElse(result.getMethodParameter().getParameterName(), "parameter");
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    violations.add(new FieldViolation(name,
                            Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid")));
                }
            }
        }
        return validationResponse(ex, violations, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
                "Request body is missing, unreadable, or contains a value of the wrong type.", request);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String parameter = ex instanceof MethodArgumentTypeMismatchException mismatch
                ? mismatch.getName()
                : ex.getPropertyName();
        String detail = parameter == null
                ? "A request parameter has an invalid value."
                : "Parameter '" + parameter + "' has an invalid value.";
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "malformed-request", "Malformed request",
                detail, request);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        Object responseBody = body;
        if (responseBody == null && ex instanceof ErrorResponse errorResponse) {
            responseBody = errorResponse.updateAndGetBody(getMessageSource(), LocaleContextHolder.getLocale());
        }
        if (responseBody instanceof ProblemDetail problemDetail) {
            completeProblem(problemDetail, request);
        }
        return super.handleExceptionInternal(ex, responseBody, headers, statusCode, request);
    }

    private ResponseEntity<Object> validationResponse(Exception ex, List<FieldViolation> rawViolations,
                                                      HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> violations = rawViolations.stream()
                .sorted(Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message))
                .toList();
        String detail = violations.isEmpty()
                ? "Request validation failed."
                : violations.stream()
                .map(violation -> "Field '" + violation.field() + "' " + violation.message() + ".")
                .collect(Collectors.joining(" "));
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation", "Validation failed", detail, request);
        problem.setProperty("errors", violations);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private ProblemDetail problem(HttpStatus status, String kind, String title, String detail, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(ERROR_TYPE_BASE + kind));
        problem.setTitle(title);
        String path = requestPath(request);
        if (path != null) {
            problem.setInstance(URI.create(path));
        }
        return problem;
    }

    private void completeProblem(ProblemDetail problem, WebRequest request) {
        if (BLANK_TYPE.equals(problem.getType())) {
            problem.setType(URI.create(ERROR_TYPE_BASE + kindFor(problem.getStatus())));
        }
        if (problem.getInstance() == null) {
            String path = requestPath(request);
            if (path != null) {
                problem.setInstance(URI.create(path));
            }
        }
    }

    private String kindFor(int status) {
        if (status == HttpStatus.NOT_FOUND.value()) {
            return "not-found";
        }
        if (status >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            return "internal";
        }
        return "malformed-request";
    }

    private String requestPath(WebRequest request) {
        if (request instanceof ServletWebRequest servletWebRequest) {
            return servletWebRequest.getRequest().getRequestURI();
        }
        return null;
    }
}