package com.chargerwidget.app

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.content.ContextCompat

/**
 * Виджет с крупной цифрой процента заряда батареи.
 *
 * Источники обновления:
 *  1. Штатный AppWidget-таймер: onUpdate(), раз в 30 минут (минимум, который
 *     позволяет сама система для android:updatePeriodMillis).
 *  2. ACTION_POWER_CONNECTED / ACTION_POWER_DISCONNECTED — эти два действия,
 *     в отличие от ACTION_BATTERY_CHANGED, разрешено статически объявлять в
 *     манифесте (не подпадают под ограничения implicit broadcast), поэтому
 *     мы получаем почти мгновенное обновление при подключении/отключении
 *     зарядки без постоянно работающего сервиса.
 *  3. Клик по виджету — RemoteViews шлёт PendingIntent с нашим собственным
 *     action'ом ACTION_REFRESH_CLICK, он приходит в этот же onReceive(),
 *     и мы читаем свежее состояние батареи по требованию.
 */
class BatteryWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_CLICK = "com.chargerwidget.app.ACTION_REFRESH_CLICK"

        // Пороги в процентах: <= CRITICAL — красный, <= LOW — оранжевый, иначе обычный цвет.
        private const val CRITICAL_THRESHOLD = 15
        private const val LOW_THRESHOLD = 30

        fun updateAllWidgets(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, BatteryWidgetProvider::class.java)
            )
            ids.forEach { id -> updateWidget(context, manager, id) }
        }

        fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val (percent, isCharging) = readBatteryState(context)

            val views = RemoteViews(context.packageName, R.layout.battery_widget)

            val percentText = if (percent in 0..100) "$percent%" else "--%"
            views.setTextViewText(R.id.widget_percent, percentText)
            views.setViewVisibility(
                R.id.widget_charging_icon,
                if (isCharging) View.VISIBLE else View.GONE
            )

            val colorRes = when {
                isCharging -> R.color.widget_text_charging
                percent in 0..CRITICAL_THRESHOLD -> R.color.widget_text_critical
                percent in 0..LOW_THRESHOLD -> R.color.widget_text_low
                else -> R.color.widget_text
            }
            views.setTextColor(R.id.widget_percent, ContextCompat.getColor(context, colorRes))

            // requestCode = widgetId, чтобы у каждого экземпляра виджета на столе
            // был свой уникальный PendingIntent и клики не путались между собой.
            val refreshIntent = Intent(context, BatteryWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_CLICK
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                widgetId,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            // Обязательно навешиваем клик заново при каждом апдейте — RemoteViews
            // пересоздаются с нуля, старый pending intent не переживёт обновление.
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            manager.updateAppWidget(widgetId, views)
        }

        /**
         * Спрашивает текущий заряд батареи без постоянной регистрации ресивера:
         * при receiver = null система не регистрирует ничего долгоживущего, а
         * просто синхронно возвращает последний sticky-intent ACTION_BATTERY_CHANGED.
         */
        private fun readBatteryState(context: Context): Pair<Int, Boolean> {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = context.registerReceiver(null, filter)

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

            return Pair(percent, isCharging)
        }
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        when (intent.action) {
            Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED,
            Intent.ACTION_BOOT_COMPLETED,
            ACTION_REFRESH_CLICK -> updateAllWidgets(context)
        }
    }
}
