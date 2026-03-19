package com.urlshortener.UrlShortenerService.repository;

import com.urlshortener.UrlShortenerService.model.Url;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UrlRepository extends JpaRepository<Url, Long> {

    Optional<Url> findByShortCode(String shortCode);

    boolean existsByShortCode(String shortCode);

    // Used during redirect — only return if active AND not expired
    @Query("SELECT u FROM Url u WHERE u.shortCode = :shortCode " +
            "AND u.active = true " +
            "AND (u.expiresAt IS NULL OR u.expiresAt > CURRENT_TIMESTAMP)")
    Optional<Url> findActiveByShortCode(String shortCode);
}