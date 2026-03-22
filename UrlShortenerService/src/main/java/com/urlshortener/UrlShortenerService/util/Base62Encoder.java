package com.urlshortener.UrlShortenerService.util;

import org.springframework.stereotype.Component;

@Component
public class Base62Encoder {
    public static final String CHARSET    = "abcdefghijklmnopqrstuvwxyz" +
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ" +
            "0123456789";
    public static final int    BASE        = 62;
    public static final int    CODE_LENGTH = 7;
    public static final char   PAD_CHAR    = 'a';

    public String encode(long number) {
        if (number < 0) throw new IllegalArgumentException("Number must be non-negative: " + number);

        char[] buf = new char[CODE_LENGTH];
        int    pos = CODE_LENGTH - 1;

        while (number > 0) {
            buf[pos--] = CHARSET.charAt((int)(number % BASE));
            number     = number / BASE;
        }
        while (pos >= 0) {
            buf[pos--] = PAD_CHAR;
        }
        return new String(buf);
    }

    public long decode(String code) {
        if (code == null || code.length() != CODE_LENGTH)
            throw new IllegalArgumentException("Code must be exactly " + CODE_LENGTH + " characters: " + code);

        long result = 0;
        for (char c : code.toCharArray()) {
            int index = CHARSET.indexOf(c);
            if (index == -1)
                throw new IllegalArgumentException("Invalid character in code: " + c);
            result = result * BASE + index;
        }
        return result;
    }
}