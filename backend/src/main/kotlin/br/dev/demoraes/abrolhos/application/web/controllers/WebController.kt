package br.dev.demoraes.abrolhos.application.web.controllers

import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.Context

@Controller
class WebController(private val templateEngine: TemplateEngine) {

    @GetMapping("/login", produces = [MediaType.TEXT_HTML_VALUE])
    fun loginPage(): ResponseEntity<String> {
        val context = Context()
        val html = templateEngine.process("login", context)
        return ResponseEntity.ok(html)
    }

    @GetMapping("/")
    fun home(): String {
        return "redirect:/api/v1/posts"
    }
}
