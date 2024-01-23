package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.media.MediaPlayer;
import android.widget.Toast;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer[] mediaPlayers;
    private int targetNumber;
    private TextView hintTextView;
    private Button muteButton; // Добавлено поле для кнопки mute

    private boolean isMuted = false; // Добавлено поле для отслеживания состояния mute

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hintTextView = findViewById(R.id.hintTextView);
        muteButton = findViewById(R.id.muteButton); // Инициализация кнопки mute

        generateTargetNumber();

        // Идентификаторы ресурсов для ваших аудиофайлов в папке raw
        int[] audioResourceIds = {R.raw.ambient_light, R.raw.bilateral_eternal, R.raw.bilateral_harp};
        // Инициализация массива MediaPlayer
        mediaPlayers = new MediaPlayer[audioResourceIds.length];
        // Создание MediaPlayer для каждого аудиофайла
        for (int i = 0; i < audioResourceIds.length; i++) {
            mediaPlayers[i] = MediaPlayer.create(this, audioResourceIds[i]);
            mediaPlayers[i].setLooping(true); // для зацикливания звука
        }

        // Воспроизведение первого аудиофайла
        if (mediaPlayers.length > 0) {
            mediaPlayers[0].start();
        }
    }

    public void onNumberButtonClick(View view) {
        Button clickedButton = (Button) view;
        String buttonText = clickedButton.getText().toString();

        // Проверка, была ли нажата кнопка "info"
        if (buttonText.equals(getString(R.string.info_button))) {
            // Открывать браузер с определенным адресом (в данном случае, google.com)
            openBrowser("http://www.google.com");
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
        stopMediaPlayers(); // Останавливаем все медиапроигрыватели
        isMuted = true; // Устанавливаем состояние mute
      //  muteButton.setBackgroundResource(R.drawable.ic_volume_off); // Устанавливаем иконку выключенного звука
        Toast.makeText(this, "Sound Off", Toast.LENGTH_SHORT).show();
    }

    private void unmute() {
        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer != null) {
                mediaPlayer.start(); // Запускаем звук, если он был выключен
            }
        }
        isMuted = false; // Устанавливаем состояние unmute
       // muteButton.setBackgroundResource(R.drawable.ic_volume_up); // Устанавливаем иконку включенного звука
        Toast.makeText(this, "Sound On", Toast.LENGTH_SHORT).show();
    }

    private void stopMediaPlayers() {
        for (MediaPlayer mediaPlayer : mediaPlayers) {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
                mediaPlayer.reset();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopMediaPlayers();
    }
}
