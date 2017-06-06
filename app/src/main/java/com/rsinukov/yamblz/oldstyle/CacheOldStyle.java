package com.rsinukov.yamblz.oldstyle;

import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.support.v4.util.Pair;

import rx.functions.Action1;

public class CacheOldStyle {

    private final LruCache<Pair<String, String>, String> cache = new LruCache<>(20);

    public void readFromCache(String text, String lang, Action1<String> callback) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                return cache.get(Pair.create(text, lang));
            }

            @Override
            protected void onPostExecute(String translation) {
                callback.call(translation);
            }
        }.execute();
    }

    public void saveToCache(String text, String lang, String translation) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                cache.put(Pair.create(text, lang), translation);
                return null;
            }
        }.execute();
    }
}
