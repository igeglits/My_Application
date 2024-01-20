package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.TextView;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int targetNumber;
    private TextView hintTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hintTextView = findViewById(R.id.hintTextView);
        generateTargetNumber();
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
        if (enteredNumber < targetNumber) {
            hintTextView.setText(enteredNumber + ">>>>>");
        } else if (enteredNumber > targetNumber) {
            hintTextView.setText("<<<<<" + enteredNumber);
        } else {
            hintTextView.setText("<<<"+targetNumber+">>>");
            generateTargetNumber(); // Начать новую игру
        }
    }
}
