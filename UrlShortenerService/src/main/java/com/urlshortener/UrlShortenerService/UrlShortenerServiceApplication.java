package com.urlshortener.UrlShortenerService;

import com.urlshortener.UrlShortenerService.util.Base62Encoder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UrlShortenerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UrlShortenerServiceApplication.class, args);
    }

}
