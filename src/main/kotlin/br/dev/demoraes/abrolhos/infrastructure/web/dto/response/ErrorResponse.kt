package br.dev.demoraes.abrolhos.infrastructure.web.dto.response

// TODO-USER(Can i make it inherit ErrorResponse? Will do some research later, dont know what it does)
data class ErrorResponse(val message: String, val status: Int)
