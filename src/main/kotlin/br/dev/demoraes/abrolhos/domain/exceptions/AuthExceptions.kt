package br.dev.demoraes.abrolhos.domain.exceptions

class InvalidInviteException(message: String) : RuntimeException(message)
class UserNotFoundException(message: String) : RuntimeException(message)
class AccountAlreadyActiveException(message: String) : RuntimeException(message)
class InvalidTotpCodeException(message: String) : RuntimeException(message)
class AuthenticationException(message: String) : RuntimeException(message)
