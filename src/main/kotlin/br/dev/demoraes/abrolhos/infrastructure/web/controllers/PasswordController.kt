package br.dev.demoraes.abrolhos.infrastructure.web.controllers

import br.dev.demoraes.abrolhos.application.services.PasswordService
import br.dev.demoraes.abrolhos.domain.entities.PasswordResetToken
import br.dev.demoraes.abrolhos.domain.entities.PlaintextPassword
import br.dev.demoraes.abrolhos.domain.entities.Username
import br.dev.demoraes.abrolhos.domain.exceptions.InvalidPasswordException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordPolicyViolationException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenExpiredException
import br.dev.demoraes.abrolhos.domain.exceptions.PasswordResetTokenNotFoundException
import br.dev.demoraes.abrolhos.domain.repository.UserRepository
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.ChangePasswordRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.ConfirmPasswordResetRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.request.PasswordResetRequest
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.ErrorResponse
import br.dev.demoraes.abrolhos.infrastructure.web.dto.response.PasswordValidationErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import ulid.ULID

/**
 * REST Controller for password management operations.
 *
 * Exposes:
 * - `POST /api/password/change` — Change password for authenticated users
 * - `POST /api/password/reset/request` — Request a password reset token
 * - `POST /api/password/reset/confirm` — Confirm a password reset with the token
 */
@RestController
@RequestMapping("/api/password")
@Validated
class PasswordController(
    private val passwordService: PasswordService,
    private val userRepository: UserRepository,
) {

    /**
     * Changes the password of the currently authenticated user.
     *
     * Requires the current password to prevent unauthorized changes. Returns 204 No Content on
     * success.
     *
     * _Requirements: 5.1 – 5.6_
     */
    @PostMapping("/change")
    @PreAuthorize("isAuthenticated()")
    fun changePassword(
        @org.springframework.security.core.annotation.AuthenticationPrincipal userId: String,
        @RequestBody @Valid request: ChangePasswordRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<*> {
        return try {
            val clientIp = httpRequest.remoteAddr ?: "unknown"
            val userUlid = ULID.parseULID(userId)
            val user =
                userRepository.findById(userUlid)
                    ?: return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(
                            ErrorResponse(
                                "User not found",
                                HttpStatus.UNAUTHORIZED.value()
                            )
                        )

            val currentHash =
                user.passwordHash
                    ?: return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(
                            ErrorResponse(
                                "No password set. Use the reset flow to set a password.",
                                HttpStatus.BAD_REQUEST.value()
                            )
                        )

            val newHash =
                passwordService.changePassword(
                    userId = userUlid,
                    currentPassword = PlaintextPassword(request.currentPassword),
                    newPassword = PlaintextPassword(request.newPassword),
                    currentHash = currentHash,
                    clientIp = clientIp,
                )

            // Persist the new hash
            userRepository.save(user.copy(passwordHash = newHash))

            ResponseEntity.noContent().build<Unit>()
        } catch (e: PasswordPolicyViolationException) {
            ResponseEntity.badRequest().body(PasswordValidationErrorResponse(e.violations))
        } catch (e: InvalidPasswordException) {
            ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(
                    ErrorResponse(
                        e.message ?: "Invalid password",
                        HttpStatus.UNAUTHORIZED.value()
                    )
                )
        } catch (e: PasswordResetException) {
            ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(
                    ErrorResponse(
                        e.message ?: "Too many attempts",
                        HttpStatus.TOO_MANY_REQUESTS.value()
                    )
                )
        }
    }

    /**
     * Initiates a password reset by generating a token.
     *
     * Always returns 202 Accepted to prevent user enumeration — even if the user does not exist.
     *
     * _Requirements: 6.1 – 6.3, 7.3, 7.4_
     */
    @PostMapping("/reset/request")
    fun requestPasswordReset(
        @RequestBody @Valid request: PasswordResetRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<Unit> {
        return try {
            val clientIp = httpRequest.remoteAddr ?: "unknown"
            val username = Username(request.username)
            val user = userRepository.findByUsername(username)
            if (user != null) {
                passwordService.generateResetToken(user.id, username, clientIp)
            }
            // Always return 202 to prevent user enumeration (Requirement 3.7)
            ResponseEntity.accepted().build()
        } catch (e: PasswordResetException) {
            // Rate limited — still return 202 to prevent enumeration
            ResponseEntity.accepted().build()
        } catch (e: IllegalArgumentException) {
            // Invalid username format — also return 202
            ResponseEntity.accepted().build()
        }
    }

    /**
     * Completes a password reset using a previously issued token.
     *
     * Returns 204 No Content on success.
     *
     * _Requirements: 6.4 – 6.7_
     */
    @PostMapping("/reset/confirm")
    fun confirmPasswordReset(
        @RequestBody @Valid request: ConfirmPasswordResetRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<*> {
        return try {
            val clientIp = httpRequest.remoteAddr ?: "unknown"
            val token = PasswordResetToken(request.token)
            passwordService.resetPassword(token, PlaintextPassword(request.newPassword), clientIp)
            ResponseEntity.noContent().build<Unit>()
        } catch (e: PasswordPolicyViolationException) {
            ResponseEntity.badRequest().body(PasswordValidationErrorResponse(e.violations))
        } catch (e: PasswordResetTokenExpiredException) {
            ResponseEntity.badRequest()
                .body(
                    ErrorResponse(
                        e.message ?: "Invalid or expired token",
                        HttpStatus.BAD_REQUEST.value()
                    )
                )
        } catch (e: PasswordResetTokenNotFoundException) {
            ResponseEntity.badRequest()
                .body(
                    ErrorResponse(
                        e.message ?: "Invalid or expired token",
                        HttpStatus.BAD_REQUEST.value()
                    )
                )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PasswordController::class.java)
    }
}
