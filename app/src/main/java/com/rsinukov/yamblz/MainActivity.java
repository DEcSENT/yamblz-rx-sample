package com.rsinukov.yamblz;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jakewharton.rxbinding.widget.RxAdapterView;
import com.jakewharton.rxbinding.widget.RxTextView;
import com.rxsinukov.yamblz.api.YandexTranslateApi;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

import static com.rsinukov.yamblz.Utils.initRetrofit;
import static com.rxsinukov.yamblz.api.YandexTranslateApi.YANDEX_API_KEY;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private static final String[] LANGUAGES = {"ru", "en", "uk", "tr", "not valid"};

    private YandexTranslateApi translateApi;

    private final CompositeSubscription compositeSubscription = new CompositeSubscription();

    @BindView(R.id.main_original_field)
    EditText originalField;

    @BindView(R.id.main_translate_label)
    TextView translateLabel;

    @BindView(R.id.main_language_spinner)
    Spinner languageSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        final ArrayAdapter<String> languagesAdapter
                = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, LANGUAGES);
        languageSpinner.setAdapter(languagesAdapter);

        translateApi = initRetrofit();
        initSubscription(languagesAdapter);
    }

    private void initSubscription(ArrayAdapter<String> languagesAdapter) {
        final Cache cache = new Cache();

        final Observable<String> textObservable = RxTextView.textChanges(originalField)
                .debounce(400, TimeUnit.MILLISECONDS, AndroidSchedulers.mainThread())
                .filter(text -> text.length() > 0)
                .map(CharSequence::toString)
                .distinctUntilChanged();

        final Observable<String> langObservable = RxAdapterView.itemSelections(languageSpinner)
                .map(languagesAdapter::getItem)
                .distinctUntilChanged();

        final Subscription subscription = Observable
                .combineLatest(textObservable, langObservable, Pair::create)
                .observeOn(Schedulers.io())
                .flatMapSingle(pair ->
                        Observable.concat(
                                cache.readFromCache(pair.first, pair.second),
                                translateApi.translate(YANDEX_API_KEY, pair.first, pair.second)
                                        .doOnSuccess(response -> cache.saveToCache(pair.first, pair.second, response.text[0])
                                                .subscribeOn(Schedulers.io())
                                                .subscribe())
                                        .map(response -> response.text[0])
                                        .toObservable()
                        ).first().toSingle()
                )
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> Toast.makeText(this, "Translation error", Toast.LENGTH_SHORT).show())
                .retryWhen(error -> error
                        .flatMap(e -> Observable.merge(RxTextView.textChanges(originalField).skip(1), RxAdapterView.itemSelections(languageSpinner).skip(1)))
                )
                .subscribe(translation -> translateLabel.setText(translation));
        compositeSubscription.add(subscription);
    }

    @Override
    protected void onDestroy() {
        compositeSubscription.clear();
        super.onDestroy();
    }
}
