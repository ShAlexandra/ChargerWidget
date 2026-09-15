package com.chargerwidget.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

/**
 * Виджет от неё не зависит — эта activity нужна только для того, чтобы
 * у приложения была нормальная иконка в лаунчере и короткая подсказка,
 * как добавить сам виджет на главный экран.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
