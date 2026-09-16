# Заряд виджет

Android-виджет для домашнего экрана: крупно показывает процент заряда батареи на прозрачном фоне, минимальный размер — 2 ячейки.

## Как обновляется

1. **Раз в 30 минут** — штатный механизм AppWidget (`updatePeriodMillis` в `battery_widget_info.xml`; это минимум, который реально соблюдает система, меньше выставить нельзя).
2. **При подключении/отключении зарядки** — `ACTION_POWER_CONNECTED` / `ACTION_POWER_DISCONNECTED`, статически объявлены в манифесте (в отличие от `ACTION_BATTERY_CHANGED`, эти два действия разрешено ловить манифест-ресивером).
3. **По тапу на сам виджет** — клик шлёт себе же broadcast с кастомным action `ACTION_REFRESH_CLICK`, он приходит в `BatteryWidgetProvider.onReceive()` и обновляет данные немедленно.

Сам текущий заряд каждый раз читается через `registerReceiver(null, IntentFilter(ACTION_BATTERY_CHANGED))` — при `receiver = null` система не регистрирует ничего постоянного, а просто мгновенно возвращает последний sticky-intent с данными о батарее.

## Цветовая индикация

Цвет процента меняется в зависимости от состояния:

- **Заряжается** — зелёный, независимо от уровня, плюс значок молнии рядом с процентом.
- **≤ 15%** — красный (критический уровень).
- **≤ 30%** — оранжевый (низкий уровень).
- **Остальное** — обычный белый.

Пороги заданы константами `CRITICAL_THRESHOLD` / `LOW_THRESHOLD` в `BatteryWidgetProvider.kt`.

Фон виджета полностью прозрачный (виден рабочий стол под ним), поэтому у текста процента есть чёрная тень-подсветка (`shadowColor`/`shadowRadius` в `battery_widget.xml`) — она держит контраст с текстом независимо от того, какие обои у пользователя.

## Требования

- minSdk 21 (Android 5.0) — техническая нижняя планка не нужна выше, всё используемое (BatteryManager, VectorDrawable, App Widgets) поддерживается с 21-й версии
- compileSdk / targetSdk 35
- Kotlin

## Как открыть

1. Откройте папку `ChargerWidget/` в Android Studio (File → Open).
2. В проекте нет бинарного `gradle-wrapper.jar`. Android Studio при первом открытии обычно сама предложит сгенерировать wrapper — согласитесь. Если не предложит, выполните в терминале внутри папки проекта (если у вас установлен Gradle локально):

   ```
   gradle wrapper --gradle-version 8.7
   ```

3. Дождитесь Gradle sync и запустите на устройстве/эмуляторе.
4. На главном экране: долгое нажатие на пустом месте → «Виджеты» → «Заряд виджет» → перетащить на стол.

## Структура

```
app/src/main/java/com/chargerwidget/app/
  BatteryWidgetProvider.kt   — вся логика виджета
  MainActivity.kt            — заглушка-подсказка в лаунчере
app/src/main/res/layout/
  battery_widget.xml         — макет самого виджета
  activity_main.xml
app/src/main/res/xml/
  battery_widget_info.xml    — метаданные AppWidget (размер, период обновления)
app/src/main/res/values/
  strings.xml, colors.xml, themes.xml
app/src/main/res/drawable/
  ic_launcher.xml             — векторная иконка (без PNG)
```

## Что можно доработать дальше

- Адаптивный размер шрифта процента под размер виджета при ресайзе (`onAppWidgetOptionsChanged`).
- Конфиг-activity при добавлении виджета (выбор темы/цвета).
