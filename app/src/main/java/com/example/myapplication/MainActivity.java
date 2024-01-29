package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.billingclient.api.*;

import java.util.Collections;
import java.util.List;
import java.util.Random;


public class MainActivity extends AppCompatActivity {

    private MediaPlayer[] mediaPlayers;
    private int currentTrack = 0;
    private int targetNumber;
    private TextView hintTextView;
    private BillingClient billingClient;
    private SharedPreferences preferences;

    private static final String PREF_SUBSCRIPTION_STATUS = "subscription_status";
    private static final String PREF_FIRST_LAUNCH = "first_launch";

    private boolean isMuted = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hintTextView = findViewById(R.id.hintTextView);

        generateTargetNumber();

        int[] audioResourceIds = {R.raw.ambient_light, R.raw.bilateral_eternal, R.raw.bilateral_harp};
        mediaPlayers = new MediaPlayer[audioResourceIds.length];
        for (int i = 0; i < audioResourceIds.length; i++) {
            mediaPlayers[i] = MediaPlayer.create(this, audioResourceIds[i]);
            mediaPlayers[i].setOnCompletionListener(mp -> playNextTrack());
        }
        playCurrentTrack();

        setupBillingClient();
    }
    private void sendBillingBroadcast() {
        Intent broadcastIntent = new Intent("com.your.package.ACTION_RECEIVE_BILLING");
        broadcastIntent.setPackage(getPackageName());
        sendBroadcast(broadcastIntent);
    }
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        preferences = getPreferences(MODE_PRIVATE);

        if (preferences.getBoolean(PREF_FIRST_LAUNCH, true)) {
            showFreeTrialDialog();
        } else {
            checkSubscriptionStatus();
        }
    }

    private void showFreeTrialDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Бесплатные 7 дней");
        builder.setMessage("Вы можете бесплатно пользоваться приложением первые 7 дней. После этого необходимо оформить подписку.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            preferences.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();
            checkSubscriptionStatus();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void setupBillingClient() {

        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener((billingResult, purchases) -> handlePurchasesUpdate(billingResult, purchases))
                .build();
        IntentFilter filter = new IntentFilter("com.your.package.ACTION_RECEIVE_BILLING");
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        MyBillingReceiver billingReceiver = new MyBillingReceiver();
        registerReceiver(billingReceiver, filter, "your.permission", null);

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                handleBillingSetupFinished(billingResult);
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Можете попробовать переподключиться
            }
        });
    }
    public class MyBillingReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("com.your.package.ACTION_RECEIVE_BILLING".equals(action)) {
                // Обработка полученного намерения
                // Например, вы можете извлечь дополнительные данные из намерения и выполнить необходимые действия
                // Например:
                String extraData = intent.getStringExtra("extra_data_key");
                if (extraData != null) {
                    // Выполнить необходимые действия на основе полученных данных
                }
            }
        }
    }

    private void handlePurchasesUpdate(BillingResult billingResult, List<Purchase> purchases) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) {
                handlePurchase(purchase);
            }
        }
    }

    private void handleBillingSetupFinished(BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            checkSubscriptionStatus();
        }
    }

    private void checkSubscriptionStatus() {
        boolean isSubscribed = preferences.getBoolean(PREF_SUBSCRIPTION_STATUS, false);

        if (!isSubscribed) {
            showSubscriptionDialog();
        }
    }

    private void showSubscriptionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Subscribe Now?");
        builder.setMessage("To continue using the application, it is necessary to subscribe.");
        builder.setPositiveButton("Да", (dialog, which) -> initiateSubscriptionProcess());
        builder.setNegativeButton("Нет", (dialog, which) -> finish());
        builder.setCancelable(false);
        builder.show();
    }

    private void initiateSubscriptionProcess() {
        String skuId = "ваш_идентификатор_подписки"; // замените на ваш SKU

        SkuDetailsParams skuDetailsParams = SkuDetailsParams.newBuilder()
                .setSkusList(Collections.singletonList(skuId))
                .setType(BillingClient.SkuType.SUBS)
                .build();

        billingClient.querySkuDetailsAsync(skuDetailsParams, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && skuDetailsList != null) {
                SkuDetails skuDetails = skuDetailsList.get(0);
                if (skuDetails != null) {
                    BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                            .setSkuDetails(skuDetails)
                            .build();

                    BillingResult billingResultFlow = billingClient.launchBillingFlow(this, billingFlowParams);
                    if (billingResultFlow.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                        handleBillingError(billingResultFlow.getResponseCode());
                    } else {
                        // Вызываем метод для отправки широковещательного намерения после успешного запуска процесса подписки
                        sendBillingBroadcast();
                    }
                }
            }
        });
    }

    private void handleBillingError(int responseCode) {
        switch (responseCode) {
            case BillingClient.BillingResponseCode.USER_CANCELED:
                showToast("Payment canceled by user");
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                showToast("Payment service temporarily unavailable");
                break;
            default:
                showToast("Payment error: " + responseCode);
                break;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            preferences.edit().putBoolean(PREF_SUBSCRIPTION_STATUS, true).apply();
        }
    }

    public void onNumberButtonClick(View view) {
        Button clickedButton = (Button) view;
        String buttonText = clickedButton.getText().toString();

        if (buttonText.equalsIgnoreCase(getString(R.string.info_button))) {
            openBrowser("https://mathmeditation.wordpress.com/");
        } else {
            int enteredNumber = Integer.parseInt(buttonText);
            checkGuess(enteredNumber);
        }
    }

    public void onMuteButtonClick(View view) {
        setVolume(isMuted ? 1f : 0f);
        showToast(isMuted ? "Sound On" : "Sound Off");
        isMuted = !isMuted; // переключение значения isMuted
    }

    private void setVolume(float volume) {
        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.setVolume(volume, volume);
            }
        }
    }

    private void openBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        if (browserIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(browserIntent);
        } else {
            showToast("No app to handle this request");
        }
    }

    private void generateTargetNumber() {
        Random random = new Random();
        targetNumber = random.nextInt(9) + 1;
    }

    private void checkGuess(int enteredNumber) {
        String newText;
        if (enteredNumber < targetNumber) {
            newText = enteredNumber + ">>>>>";
        } else if (enteredNumber > targetNumber) {
            newText = "<<<<<" + enteredNumber;
        } else {
            newText = "<<<" + targetNumber + ">>>";
            generateTargetNumber();
        }

        hintTextView.animate()
                .alpha(0f)
                .setDuration(1000)
                .withEndAction(() -> {
                    hintTextView.setText(newText);
                    hintTextView.animate().alpha(1f).setDuration(1000).start();
                })
                .start();
    }

    private void playCurrentTrack() {
        checkMediaPlayerState();
    }

    private void playNextTrack() {
        currentTrack = (currentTrack + 1) % mediaPlayers.length;
        checkMediaPlayerState();
    }

    private void checkMediaPlayerState() {
        if (mediaPlayers.length > 0 && currentTrack < mediaPlayers.length
                && mediaPlayers[currentTrack] != null
                && !mediaPlayers[currentTrack].isPlaying()
                && mediaPlayers[currentTrack].getAudioSessionId() != AudioManager.ERROR) {
            mediaPlayers[currentTrack].start();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayer();
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }

    private void stopMediaPlayer() {
        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.release();
            }
        }
    }
}
