package br.dev.demoraes.abrolhos.properties

import br.dev.demoraes.abrolhos.application.utils.SensitiveDataRedactor
import io.kotest.core.spec.style.StringSpec
import io.kotest.property.Arb
import io.kotest.property.arbitrary.email
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.forAll

class SensitiveDataRedactorPropertiesTest :
        StringSpec({
            "property - redacts email addresses" {
                forAll(Arb.email()) { email ->
                    val text = "User contact is $email"
                    val redacted = SensitiveDataRedactor.redact(text)
                    !redacted.contains(email) && redacted.contains("[REDACTED_EMAIL]")
                }
            }

            "property - redacts JWTs" {
                // basic jwt pattern mock
                val jwtGen =
                        Arb.stringPattern(
                                "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9\\." +
                                        "eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ\\." +
                                        "[SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c]+"
                        )
                forAll(jwtGen) { jwt ->
                    val text = "Authorization: Bearer $jwt"
                    val redacted = SensitiveDataRedactor.redact(text)
                    !redacted.contains(jwt) && redacted.contains("[REDACTED_JWT]")
                }
            }
        })
