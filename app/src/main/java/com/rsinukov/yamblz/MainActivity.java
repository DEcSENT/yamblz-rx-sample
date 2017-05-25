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
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;

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

        initRetrofit();
        initSubsription(languagesAdapter);
    }

    private void initSubsription(ArrayAdapter<String> languagesAdapter) {
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
                .flatMapSingle(pair -> translateApi.translate(YANDEX_API_KEY, pair.first, pair.second))
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError(error -> Toast.makeText(this, "Translation error", Toast.LENGTH_SHORT).show())
                .retryWhen(error -> error
                        .switchMap(e -> Observable.merge(RxTextView.textChanges(originalField).skip(1), RxAdapterView.itemSelections(languageSpinner).skip(1)))
                )
                .map(response -> response.text[0])
                .subscribe(translation -> translateLabel.setText(translation));
        compositeSubscription.add(subscription);
    }

    private void initRetrofit() {
        final OkHttpClient client = new OkHttpClient();
        final OkHttpClient.Builder retrofitClientBuilder = client.newBuilder();

        if (BuildConfig.DEBUG) {
            final HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
            interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            retrofitClientBuilder.addInterceptor(interceptor);
            retrofitClientBuilder.addNetworkInterceptor(interceptor);
        }

        final Retrofit retrofit = new Retrofit.Builder()
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl("https://translate.yandex.net/api/v1.5/tr.json/")
                .client(retrofitClientBuilder.build())
                .build();

        translateApi = retrofit.create(YandexTranslateApi.class);
    }

    @Override
    protected void onDestroy() {
        compositeSubscription.clear();
        super.onDestroy();
    }
}
