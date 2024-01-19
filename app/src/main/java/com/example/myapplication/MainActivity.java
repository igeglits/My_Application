package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
        import android.view.View;
        import android.widget.Button;
        import android.widget.GridLayout;
        import android.widget.TextView;

        import androidx.appcompat.app.AppCompatActivity;

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
        Button button = (Button) view;
        int enteredNumber = Integer.parseInt(button.getText().toString());
        checkGuess(enteredNumber);
    }

    public void onEnterButtonClick(View view) {
        // Добавь здесь логику для обработки нажатия на кнопку "Enter"
    }

    public void onExitButtonClick(View view) {
        // Добавь здесь логику для обработки нажатия на кнопку "Exit"
    }

    private void generateTargetNumber() {
        Random random = new Random();
        targetNumber = random.nextInt(10);
    }

    private void checkGuess(int enteredNumber) {
        if (enteredNumber < targetNumber) {
            hintTextView.setText(enteredNumber + " < ?");
        } else if (enteredNumber > targetNumber) {
            hintTextView.setText("? > " + enteredNumber);
        } else {
            hintTextView.setText("Поздравляем! Вы угадали число!");
            generateTargetNumber(); // Начать новую игру
        }
    }
}
