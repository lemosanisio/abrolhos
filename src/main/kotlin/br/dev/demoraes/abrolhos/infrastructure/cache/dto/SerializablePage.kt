package br.dev.demoraes.abrolhos.infrastructure.cache.dto

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.JsonNode
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest

@JsonIgnoreProperties(ignoreUnknown = true, value = ["pageable"])
class SerializablePage<T> : PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    constructor(
        @JsonProperty("content") content: List<T>,
        @JsonProperty("number") number: Int,
        @JsonProperty("size") size: Int,
        @JsonProperty("totalElements") totalElements: Long,
        @JsonProperty("pageable") pageable: JsonNode?,
        @JsonProperty("last") last: Boolean,
        @JsonProperty("totalPages") totalPages: Int,
        @JsonProperty("sort") sort: JsonNode?,
        @JsonProperty("first") first: Boolean,
        @JsonProperty("numberOfElements") numberOfElements: Int
    ) : super(content, PageRequest.of(number, if (size <= 0) 1 else size), totalElements)

    constructor(page: org.springframework.data.domain.Page<T>) : super(page.content, page.pageable, page.totalElements)
}
