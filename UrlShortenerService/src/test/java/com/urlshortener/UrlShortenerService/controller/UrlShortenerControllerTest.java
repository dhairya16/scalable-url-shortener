package com.urlshortener.UrlShortenerService.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.ShortenRequest;
import com.urlshortener.UrlShortenerService.dto.UrlDtos.ShortenResponse;
import com.urlshortener.UrlShortenerService.service.RateLimiter;
import com.urlshortener.UrlShortenerService.service.UrlShortenerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UrlShortenerController.class)
@DisplayName("UrlShortenerController")
class UrlShortenerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UrlShortenerService shortenerService;

    @MockitoBean
    private RateLimiter rateLimiter;

    // ── POST /shorten ────────────────────────────────────────────────

    @Test
    @DisplayName("POST /shorten with valid URL returns 201 and shortCode")
    void shorten_validUrl_returns201() throws Exception {
        when(rateLimiter.allowShorten(anyString())).thenReturn(true);
        when(shortenerService.shorten(any())).thenReturn(
                ShortenResponse.builder()
                        .shortUrl("http://localhost/aaaaaaa")
                        .shortCode("aaaaaaa")
                        .longUrl("https://github.com")
                        .createdAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(86400))
                        .build()
        );

        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://github.com");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.shortCode").value("aaaaaaa"))
                .andExpect(jsonPath("$.shortUrl").value("http://localhost/aaaaaaa"))
                .andExpect(jsonPath("$.longUrl").value("https://github.com"));
    }

    @Test
    @DisplayName("POST /shorten with blank URL returns 400")
    void shorten_blankUrl_returns400() throws Exception {
        when(rateLimiter.allowShorten(anyString())).thenReturn(true);

        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /shorten with invalid URL format returns 400")
    void shorten_invalidUrl_returns400() throws Exception {
        when(rateLimiter.allowShorten(anyString())).thenReturn(true);

        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("not-a-valid-url");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /shorten when rate limited returns 429")
    void shorten_rateLimited_returns429() throws Exception {
        when(rateLimiter.allowShorten(anyString())).thenReturn(false);

        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://github.com");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("POST /shorten with conflicting custom code returns 409")
    void shorten_conflictingCustomCode_returns409() throws Exception {
        when(rateLimiter.allowShorten(anyString())).thenReturn(true);
        when(shortenerService.shorten(any()))
                .thenThrow(new UrlShortenerService.ShortCodeConflictException(
                        "Custom code 'my-brand' is already taken"));

        ShortenRequest request = new ShortenRequest();
        request.setLongUrl("https://github.com");
        request.setCustomCode("my-brand");

        mockMvc.perform(post("/shorten")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    // ── DELETE /{shortCode} ──────────────────────────────────────────

    @Test
    @DisplayName("DELETE /{shortCode} existing URL returns 204")
    void deactivate_existingUrl_returns204() throws Exception {
        when(shortenerService.deactivate("aaaaaaa")).thenReturn(true);

        mockMvc.perform(delete("/aaaaaaa"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("DELETE /{shortCode} non-existing URL returns 404")
    void deactivate_nonExistingUrl_returns404() throws Exception {
        when(shortenerService.deactivate("aaaaaaa")).thenReturn(false);

        mockMvc.perform(delete("/aaaaaaa"))
                .andExpect(status().isNotFound());
    }
}