package br.dev.demoraes.abrolhos.infrastructure.web.handlers

import br.dev.demoraes.abrolhos.domain.exceptions.AccountAlreadyActiveException
import br.dev.demoraes.abrolhos.domain.exceptions.AuthenticationException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidInviteException
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidTotpCodeException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ErrorResponse(
    val message: String,
    val status: Int,
)

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

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ErrorResponse(e.message ?: "Invalid input", HttpStatus.BAD_REQUEST.value()))
    }
}
