package com.rsinukov.yamblz;

import android.support.v4.util.LruCache;
import android.support.v4.util.Pair;

import rx.Completable;
import rx.Observable;

public class Cache {

    private final LruCache<Pair<String, String>, String> cache = new LruCache<>(20);

    public Observable<String> readFromCache(String text, String lang) {
        return Observable.defer(() -> {
            final String translation = cache.get(Pair.create(text, lang));
            if (translation != null) {
                return Observable.just(translation);
            } else {
                return Observable.empty();
            }
        });
    }

    public Completable saveToCache(String text, String lang, String translation) {
        return Completable.fromCallable(() -> {
            cache.put(Pair.create(text, lang), translation);
            return translation;
        });
    }
}
