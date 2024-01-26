package com.example.myapplication;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.widget.Toast;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;

import android.content.DialogInterface;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import com.android.billingclient.api.*;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer[] mediaPlayers;
    private int currentTrack = 0; // Текущий трек
    private int targetNumber;
    private TextView hintTextView;
    private Button muteButton; // Добавлено поле для кнопки mute

    private boolean isMuted = false; // Добавлено поле для отслеживания состояния mute

    private BillingClient billingClient;
    private SharedPreferences preferences;
    private static final String PREF_SUBSCRIPTION_STATUS = "subscription_status";
    private static final String PREF_FIRST_LAUNCH = "first_launch";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hintTextView = findViewById(R.id.hintTextView);
        muteButton = findViewById(R.id.muteButton); // Инициализация кнопки mute

        /*preferences = getPreferences(MODE_PRIVATE);
        boolean firstLaunch = preferences.getBoolean(PREF_FIRST_LAUNCH, true);

        if (firstLaunch) {
            showFreeTrialDialog();
        } else {
            checkSubscriptionStatus();
        }*/



        generateTargetNumber();

        // Идентификаторы ресурсов для ваших аудиофайлов в папке raw
        int[] audioResourceIds = {R.raw.ambient_light, R.raw.bilateral_eternal, R.raw.bilateral_harp};
        // Инициализация массива MediaPlayer
        mediaPlayers = new MediaPlayer[audioResourceIds.length];
        // Создание MediaPlayer для каждого аудиофайла
        for (int i = 0; i < audioResourceIds.length; i++) {
            mediaPlayers[i] = MediaPlayer.create(this, audioResourceIds[i]);
            mediaPlayers[i].setOnCompletionListener(mp -> playNextTrack()); // Слушатель для переключения треков
        }

        // Воспроизведение первого аудиофайла
        playCurrentTrack();

        setupBillingClient();
    }
    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        preferences = getPreferences(MODE_PRIVATE);
        boolean firstLaunch = preferences.getBoolean(PREF_FIRST_LAUNCH, true);

        if (firstLaunch) {
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
            // Пользователь подтвердил, сохраняем информацию о первом запуске
            preferences.edit().putBoolean(PREF_FIRST_LAUNCH, false).apply();
            checkSubscriptionStatus();
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void setupBillingClient() {
        billingClient = BillingClient.newBuilder(this)
                .enablePendingPurchases()
                .setListener(new PurchasesUpdatedListener() {
                    @Override
                    public void onPurchasesUpdated(BillingResult billingResult, List<Purchase> purchases) {
                        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && purchases != null) {
                            for (Purchase purchase : purchases) {
                                handlePurchase(purchase);
                            }
                        }
                    }
                })
                .build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    checkSubscriptionStatus();
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                // Можете попробовать переподключиться
            }
        });
    }

    private void checkSubscriptionStatus() {
        // Здесь вы можете проверить статус подписки
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
                .setType(BillingClient.SkuType.SUBS)  // Укажите тип подписки
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
                        // Обработайте ошибку
                    }
                }
            }
        });
    }

    private void handleBillingError(int responseCode) {
        switch (responseCode) {
            case BillingClient.BillingResponseCode.USER_CANCELED:
                // Пользователь отменил процесс оплаты
                showToast("Payment canceled by user");
                break;
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                // Сервис оплаты временно недоступен
                showToast("Payment service temporarily unavailable");
                break;
            // ... обработка других возможных кодов ошибок ...
            default:
                // Обработка других случаев ошибок
                showToast("Payment error: " + responseCode);
                break;
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private void handlePurchase(Purchase purchase) {
        // Здесь обрабатывайте информацию о покупке, например, сохраняйте статус подписки
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
            preferences.edit().putBoolean(PREF_SUBSCRIPTION_STATUS, true).apply();
        }
    }

   /* @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }*/

    public void onNumberButtonClick(View view) {
        Button clickedButton = (Button) view;
        String buttonText = clickedButton.getText().toString();

        // Проверка, была ли нажата кнопка "info"
        if (buttonText.equals(getString(R.string.info_button))) {
            // Открывать браузер с определенным адресом (в данном случае, google.com)
            openBrowser("https://mathmeditation.wordpress.com/");
        } else {
            // Если это не кнопка "info", обрабатываем как угадывание числа
            int enteredNumber = Integer.parseInt(buttonText);
            checkGuess(enteredNumber);
        }
    }

    public void onMuteButtonClick(View view) {
        if (isMuted) {
            unmute(); // Включаем звук
        } else {
            mute(); // Выключаем звук
        }
    }

    private void openBrowser(String url) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        startActivity(browserIntent);
    }

    private void generateTargetNumber() {
        Random random = new Random();
        // Генерируем случайное число от 1 до 9 включительно
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
            generateTargetNumber(); // Начать новую игру
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

    private void mute() {
        if (!isMuted) {
            for (MediaPlayer mediaPlayer : mediaPlayers) {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause(); // Пауза воспроизведения
                }
            }
            isMuted = true; // Устанавливаем состояние mute
            muteButton.setText("Unmute"); // Изменяем текст кнопки
            Toast.makeText(this, "Sound Off", Toast.LENGTH_SHORT).show();
        }
    }

    private void unmute() {
        if (isMuted) {
            playCurrentTrack(); // Возобновление воспроизведения
            isMuted = false; // Устанавливаем состояние unmute
            muteButton.setText("Mute"); // Изменяем текст кнопки
            Toast.makeText(this, "Sound On", Toast.LENGTH_SHORT).show();
        }
    }

    private void playCurrentTrack() {
        if (mediaPlayers.length > 0 && currentTrack < mediaPlayers.length) {
            mediaPlayers[currentTrack].start(); // Воспроизведение текущего трека
        }
    }

    private void playNextTrack() {
        if (mediaPlayers.length > 0) {
            currentTrack = (currentTrack + 1) % mediaPlayers.length; // Переключение на следующий трек

            // Проверяем, не выходит ли за пределы массива
            if (currentTrack < mediaPlayers.length) {
                playCurrentTrack();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayer(); // Останавливаем воспроизведение и освобождаем ресурсы
        if (billingClient != null && billingClient.isReady()) {
            billingClient.endConnection();
        }
    }

    private void stopMediaPlayer() {
        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer != null) {
                mediaPlayer.stop(); // Остановка воспроизведения
                mediaPlayer.release(); // Освобождение ресурсов
            }
        }
    }
}
