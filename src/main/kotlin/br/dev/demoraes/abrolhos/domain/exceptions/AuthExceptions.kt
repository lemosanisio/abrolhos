package br.dev.demoraes.abrolhos.domain.exceptions

/**
 * Exceptions related to authentication and user account management.
 *
 * These exceptions are thrown by the Domain layer and are typically handled by the
 * GlobalExceptionHandler in the Web layer.
 */
// TODO(Could this one be moved to infrastructure?)
class InvalidInviteException(message: String) : RuntimeException(message)

class UserNotFoundException(message: String) : RuntimeException(message)

class AccountAlreadyActiveException(message: String) : RuntimeException(message)

class InvalidTotpCodeException(message: String) : RuntimeException(message)

class AuthenticationException(message: String) : RuntimeException(message)
