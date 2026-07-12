package com.github.sinotca529.fflaunch;

import android.icu.text.Transliterator;

final class StringUtil {
    // NFKC: 全角英数↔半角、半角カナ→全角カナ、濁点の合成などを吸収
    // Lower: 大文字小文字を吸収
    // Katakana-Hiragana: カタカナとひらがなを吸収
    private static final Transliterator NORMALIZER =
        Transliterator.getInstance("NFKC; Lower; Katakana-Hiragana");

    // Transliterator はスレッドセーフでないため synchronized で保護する
    // (メインスレッドの検索とバックグラウンドのアプリ一覧構築の両方から呼ばれる)
    static synchronized String regularize(String s) {
        return NORMALIZER.transliterate(s);
    }
}
