package com.rsinukov.yamblz;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.EditText;

import com.jakewharton.rxbinding.widget.RxTextView;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.username)
    EditText usernameField;

    @BindView(R.id.password)
    EditText passwordField;

    @BindView(R.id.login)
    Button loginButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        initSubscription();
    }

    private void initSubscription() {
        Observable.combineLatest(
                RxTextView.textChanges(usernameField).map(this::isLoginValid),
                RxTextView.textChanges(passwordField).map(this::isPasswordValid),
                (loginValid, pwdValid) -> loginValid && pwdValid
        )
                .subscribe(valid -> loginButton.setEnabled(valid));
    }

    private boolean isLoginValid(CharSequence login) {
        return login.length() >= 3;
    }

    private boolean isPasswordValid(CharSequence password) {
        return password.length() >= 5;
    }
}
