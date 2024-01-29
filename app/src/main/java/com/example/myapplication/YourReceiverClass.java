package com.example.myapplication;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class YourReceiverClass extends BroadcastReceiver {

    private static final String TAG = "YourReceiverClass";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Ваш код обработки приема широковещательного намерения
        if ("com.your.package.ACTION_RECEIVE_BILLING".equals(intent.getAction())) {
            // Добавьте вашу логику обработки здесь

            // Пример использования Log для вывода в консоль
            Log.d(TAG, "Broadcast received");

            // Если нужен вывод в Toast, раскомментируйте следующую строку:
            // Toast.makeText(context, "Broadcast received", Toast.LENGTH_SHORT).show();
        }
    }
}
