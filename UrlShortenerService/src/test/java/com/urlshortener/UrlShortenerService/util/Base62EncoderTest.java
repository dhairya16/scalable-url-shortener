package com.urlshortener.UrlShortenerService.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Base62Encoder")
class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    @DisplayName("encode(0) returns all padding characters")
    void encode_zero_returnsAllPadding() {
        assertEquals("aaaaaaa", encoder.encode(0));
    }

    @Test
    @DisplayName("encode always produces exactly 7 characters")
    void encode_alwaysProduces7Chars() {
        assertEquals(7, encoder.encode(0).length());
        assertEquals(7, encoder.encode(1).length());
        assertEquals(7, encoder.encode(999_999).length());
        assertEquals(7, encoder.encode(9_234_529_445L).length());
    }

    @Test
    @DisplayName("same number always produces same code")
    void encode_sameNumber_alwaysSameCode() {
        long number = 9_234_529_445L;
        assertEquals(encoder.encode(number), encoder.encode(number));
    }

    @Test
    @DisplayName("decode(encode(n)) == n for various values")
    void roundTrip_preservesOriginalValue() {
        long[] samples = { 0L, 1L, 62L, 999_999L, 9_234_529_445L, 3_521_614_606_207L };
        for (long v : samples) {
            assertEquals(v, encoder.decode(encoder.encode(v)),
                    "Round-trip failed for value: " + v);
        }
    }

    @Test
    @DisplayName("different numbers produce different codes")
    void encode_differentNumbers_produceDifferentCodes() {
        assertNotEquals(encoder.encode(0),   encoder.encode(1));
        assertNotEquals(encoder.encode(100), encoder.encode(200));
    }

    @Test
    @DisplayName("encode negative number throws IllegalArgumentException")
    void encode_negativeNumber_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> encoder.encode(-1));
    }

    @Test
    @DisplayName("decode invalid character throws IllegalArgumentException")
    void decode_invalidCharacter_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> encoder.decode("aaa!aaa"));
    }

    @Test
    @DisplayName("decode wrong length throws IllegalArgumentException")
    void decode_wrongLength_throwsException() {
        assertThrows(IllegalArgumentException.class,
                () -> encoder.decode("abc"));
    }

    @Test
    @DisplayName("max encodable value in 7 chars is 62^7 - 1")
    void encode_maxValue_doesNotExceed7Chars() {
        long maxValue = (long) Math.pow(62, 7) - 1;
        String code = encoder.encode(maxValue);
        assertEquals(7, code.length());
        assertEquals(maxValue, encoder.decode(code));
    }
}