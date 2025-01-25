package com.github.sinotca529.fflaunch;

import android.icu.text.Transliterator;

final class StringUtil {
    static final Transliterator k2h = Transliterator.getInstance("Katakana-Hiragana");

    static String regularize(String s) {
        s = s.toLowerCase();
        s = k2h.transliterate(s);
        return s;
    }
}
