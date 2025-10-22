Инструкция по запуску с помощью докера.
1) Запустить в Android Studio девайс. Например, Medium Phone API 36.1.
2) Перейти в корень проекта AndroidStudioProjects\Notifyer
3) Скопировать APK.
APK находится в /app/app/build/outputs/apk/debug/app-debug.apk:

Выполнить команду:
docker cp notifyer-app-1:/app/app/build/outputs/apk/debug/app-debug.apk .

4) Установить APK.
Выполнить команду:
adb install app-debug.apk

5) Запустить приложение.
Выполнить команду:
adb shell am start -n com.example.notifyer/.MainActivity
