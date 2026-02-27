package br.dev.demoraes.abrolhos.infrastructure.web.handlers

import br.dev.demoraes.abrolhos.domain.exceptions.AccountAlreadyActiveException
import br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidInviteException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidPasswordException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidTotpCodeException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordPolicyViolationException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenExpiredException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenNotFoundException
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.ErrorResponse
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.PasswordValidationErrorResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Centralized exception handling for the web layer.
 *
 * Translates domain exceptions into appropriate HTTP status codes and standard JSON error
 * responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(AuthenticationException::class)
    @Suppress("UnusedParameter")
    fun handleAuthenticationException(e: AuthenticationException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse("Invalid credentials", HttpStatus.UNAUTHORIZED.value()))
    }

    @ExceptionHandler(InvalidInviteException::class)
    fun handleInvalidInviteException(e: InvalidInviteException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Invalid invite", HttpStatus.BAD_REQUEST.value()))
    }

    @ExceptionHandler(AccountAlreadyActiveException::class)
    fun handleAccountAlreadyActiveException(
            e: AccountAlreadyActiveException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(
                        ErrorResponse(
                                e.message ?: "Account already active",
                                HttpStatus.CONFLICT.value()
                        )
                )
    }

    @ExceptionHandler(InvalidTotpCodeException::class)
    fun handleInvalidTotpCodeException(e: InvalidTotpCodeException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(
                        ErrorResponse(
                                e.message ?: "Invalid TOTP code",
                                HttpStatus.BAD_REQUEST.value()
                        )
                )
    }

    @ExceptionHandler(PasswordPolicyViolationException::class)
    fun handlePasswordPolicyViolationException(
            e: PasswordPolicyViolationException
    ): ResponseEntity<PasswordValidationErrorResponse> {
        return ResponseEntity.badRequest().body(PasswordValidationErrorResponse(e.violations))
    }

    @ExceptionHandler(InvalidPasswordException::class)
    fun handleInvalidPasswordException(e: InvalidPasswordException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                        ErrorResponse(
                                e.message ?: "Invalid password",
                                HttpStatus.UNAUTHORIZED.value()
                        )
                )
    }

    @ExceptionHandler(PasswordResetTokenExpiredException::class)
    fun handlePasswordResetTokenExpiredException(
            e: PasswordResetTokenExpiredException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest()
                .body(
                        ErrorResponse(
                                e.message ?: "Invalid or expired token",
                                HttpStatus.BAD_REQUEST.value()
                        )
                )
    }

    @ExceptionHandler(PasswordResetTokenNotFoundException::class)
    fun handlePasswordResetTokenNotFoundException(
            e: PasswordResetTokenNotFoundException
    ): ResponseEntity<ErrorResponse> {
        return ResponseEntity.badRequest()
                .body(
                        ErrorResponse(
                                e.message ?: "Invalid or expired token",
                                HttpStatus.BAD_REQUEST.value()
                        )
                )
    }

    @ExceptionHandler(PasswordResetException::class)
    fun handlePasswordResetException(e: PasswordResetException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(
                        ErrorResponse(
                                e.message ?: "Too many attempts",
                                HttpStatus.TOO_MANY_REQUESTS.value()
                        )
                )
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse(e.message ?: "Invalid input", HttpStatus.BAD_REQUEST.value()))
    }
}
