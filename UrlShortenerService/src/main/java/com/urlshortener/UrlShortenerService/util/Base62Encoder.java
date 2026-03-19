package com.urlshortener.UrlShortenerService.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {
    private static final String CHARSET =
            "abcdefghijklmnopqrstuvwxyz" +
                    "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
                    "0123456789";

    private static final int BASE = 62;
    private static final int CODE_LENGTH = 7;

    public String encode(long number) {
        char[] buf = new char[CODE_LENGTH];
        int pos = CODE_LENGTH - 1;

        while (number > 0) {
            buf[pos--] = CHARSET.charAt((int) (number % BASE));
            number = number / BASE;
        }

        // Left-pad remaining positions with 'a' (index 0)
        while (pos >= 0) {
            buf[pos--] = 'a';
        }

        return new String(buf);
    }

    public long decode(String code) {
        long result = 0;
        for (char c : code.toCharArray()) {
            result = result * BASE + CHARSET.indexOf(c);
        }
        return result;
    }
}