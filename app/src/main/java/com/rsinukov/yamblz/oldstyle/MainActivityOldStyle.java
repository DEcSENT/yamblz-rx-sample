package com.rsinukov.yamblz.oldstyle;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.rsinukov.yamblz.R;
import com.rsinukov.yamblz.Utils;
import com.rxsinukov.yamblz.api.YandexTranslateApi;
import com.rxsinukov.yamblz.api.YandexTranslateReponse;

import java.io.IOException;
import java.lang.ref.WeakReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import retrofit2.Call;
import retrofit2.Response;

import static com.rxsinukov.yamblz.api.YandexTranslateApi.YANDEX_API_KEY;

public class MainActivityOldStyle extends AppCompatActivity {

    private static final String TAG = MainActivityOldStyle.class.getSimpleName();

    private static final String[] LANGUAGES = {"ru", "en", "uk", "tr", "not valid"};

    private YandexTranslateApi translateApi;

    @BindView(R.id.main_original_field)
    EditText originalField;

    @BindView(R.id.main_translate_label)
    TextView translateLabel;

    @BindView(R.id.main_language_spinner)
    Spinner languageSpinner;

    private Unbinder unbinder;

    final CacheOldStyle cache = new CacheOldStyle();

    private Handler debounceHandler = new Handler();

    private Runnable contentChangedRunnable = () -> {
        String text = originalField.getText().toString();
        String lang = (String) languageSpinner.getSelectedItem();

        cache.readFromCache(text, lang, translation -> {
            if (translation == null) {
                new TranslationAsyncTask(translateApi, MainActivityOldStyle.this, cache)
                        .execute(text, lang);
            } else {
                if (translateLabel != null) {
                    translateLabel.setText(translation);
                }
            }
        });
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);

        final ArrayAdapter<String> languagesAdapter
                = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, LANGUAGES);
        languageSpinner.setAdapter(languagesAdapter);

        translateApi = Utils.initRetrofit();

        initSubscription();
    }

    private void initSubscription() {
        originalField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                debounceHandler.removeCallbacks(contentChangedRunnable);
                debounceHandler.postDelayed(contentChangedRunnable, 400);
            }
        });

        languageSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                debounceHandler.removeCallbacks(contentChangedRunnable);
                debounceHandler.postDelayed(contentChangedRunnable, 400);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) { }
        });
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    private void setTranslation(String translation) {
        translateLabel.setText(translation);
    }

    private static class TranslationAsyncTask extends AsyncTask<String, Void, Object> {

        private final YandexTranslateApi translateApi;

        private final WeakReference<MainActivityOldStyle> activityRef;

        private final CacheOldStyle cache;

        private String text;

        private String lang;

        private TranslationAsyncTask(YandexTranslateApi translateApi, MainActivityOldStyle activity, CacheOldStyle cache) {
            this.translateApi = translateApi;
            this.activityRef = new WeakReference<>(activity);
            this.cache = cache;
        }

        @Override
        protected Object doInBackground(String... params) {
            text = params[0];
            lang = params[1];

            Call<YandexTranslateReponse> yandexTranslateReponse = translateApi.translateOldStyle(YANDEX_API_KEY, text, lang);
            try {
                return yandexTranslateReponse.execute();
            } catch (IOException e) {
                return e;
            }
        }

        @Override
        protected void onPostExecute(Object result) {
            MainActivityOldStyle activity = activityRef.get();
            if (activity == null || activity.translateLabel == null) {
                return;
            }

            if (result instanceof Exception) {
                activity.translateLabel.setText(((Exception) result).getLocalizedMessage());
            } else {
                Response<YandexTranslateReponse> response = (Response<YandexTranslateReponse>) result;
                if (response.isSuccessful()) {
                    final String translation = response.body().text[0];
                    activity.setTranslation(translation);
                    cache.saveToCache(text, lang, translation);
                } else {
                    activity.setTranslation("Something went wrong!");
                }
            }
        }
    }
}
