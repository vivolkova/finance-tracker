package com.example.financetracker.common

import jakarta.persistence.OptimisticLockException
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.orm.ObjectOptimisticLockingFailureException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.time.Instant

/**
 * All handlers return RFC 7807 [ProblemDetail] (Content-Type:
 * application/problem+json) so the error format is consistent across the API.
 * The handlers still differ in *technique* — that variety is intentional, to
 * show what Spring supports.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    // ── Technique 1: build and return a ProblemDetail directly ────────────────
    // No @ResponseStatus needed — the status is carried inside the ProblemDetail.
    // `setProperty` adds custom fields beyond the RFC 7807 standard ones.

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ProblemDetail =
        problem(HttpStatus.NOT_FOUND, "Not Found", ex.message ?: "Resource not found")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(ex: IllegalArgumentException): ProblemDetail =
        problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.message ?: "Bad request")

    // jakarta.persistence.OptimisticLockException — thrown manually in TransactionService
    @ExceptionHandler(OptimisticLockException::class)
    fun handleOptimisticLock(ex: OptimisticLockException): ProblemDetail =
        problem(
            status = HttpStatus.CONFLICT,
            title = "Conflict",
            detail = ex.message ?: "Data was modified by another user. Please refresh and try again."
        )

    // ObjectOptimisticLockingFailureException — thrown by Spring Data JPA when two
    // concurrent requests collide at the database level (Hibernate wraps JPA exception).
    @ExceptionHandler(ObjectOptimisticLockingFailureException::class)
    fun handleSpringOptimisticLock(ex: ObjectOptimisticLockingFailureException): ProblemDetail =
        problem(
            status = HttpStatus.CONFLICT,
            title = "Conflict",
            detail = "Data was modified by another user. Please refresh and try again."
        )

    // ── Technique 2: a custom extension property carrying structured data ──────
    // Field validation errors are attached as an "errors" array, so the client
    // gets machine-readable details, not just a flat message.

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException): ProblemDetail {
        val errors = ex.bindingResult.fieldErrors
            .map { mapOf("field" to it.field, "message" to it.defaultMessage) }
        val detail = problem(HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed")
        detail.setProperty("errors", errors)
        return detail
    }

    // ── Technique 3: one handler for several exceptions + an extra parameter ──
    // @ExceptionHandler accepts multiple types; the parameter uses their common
    // supertype (Exception). Spring also injects HttpServletRequest, so we can
    // record which path failed.

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MethodArgumentTypeMismatchException::class
    )
    fun handleBadParameter(ex: Exception, request: HttpServletRequest): ProblemDetail {
        val detail = problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.message ?: "Invalid parameter")
        detail.setProperty("path", request.requestURI)
        return detail
    }

    // ── Technique 4: wrap the ProblemDetail in ResponseEntity ─────────────────
    // Same RFC 7807 body, but ResponseEntity lets us also set custom headers.

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResource(ex: NoResourceFoundException): ResponseEntity<ProblemDetail> {
        val detail = problem(HttpStatus.NOT_FOUND, "Not Found", "Resource not found: ${ex.resourcePath}")
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .header("X-Error-Source", "global-handler")
            .body(detail)
    }

    // ── Catch-all: log the real cause, hide details from the client ───────────

    @ExceptionHandler(Exception::class)
    fun handleGeneral(ex: Exception): ProblemDetail {
        logger.error("Unhandled exception", ex)
        return problem(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Internal Server Error",
            "An unexpected error occurred"
        )
    }

    /** Builds a ProblemDetail with our standard extra fields (title + timestamp). */
    private fun problem(status: HttpStatus, title: String, detail: String): ProblemDetail =
        ProblemDetail.forStatusAndDetail(status, detail).apply {
            this.title = title
            setProperty("timestamp", Instant.now())
        }
}
