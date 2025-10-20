# Используем Debian как базовый образ
FROM debian:bullseye

# Устанавливаем OpenJDK 17 и другие зависимости
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    wget \
    unzip \
    netcat \
    && rm -rf /var/lib/apt/lists/*

# Устанавливаем Android SDK
ENV ANDROID_SDK_ROOT /opt/android-sdk
RUN mkdir -p ${ANDROID_SDK_ROOT}/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-9477386_latest.zip -O android-tools.zip \
    && unzip android-tools.zip -d ${ANDROID_SDK_ROOT}/cmdline-tools \
    && mv ${ANDROID_SDK_ROOT}/cmdline-tools/cmdline-tools ${ANDROID_SDK_ROOT}/cmdline-tools/latest \
    && rm android-tools.zip

# Устанавливаем необходимые компоненты SDK
RUN yes | ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager --licenses \
    && ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "platform-tools" \
    && ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" \
    && ${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin/sdkmanager "build-tools;34.0.0"

# Добавляем переменные окружения
ENV PATH ${PATH}:${ANDROID_SDK_ROOT}/cmdline-tools/latest/bin:${ANDROID_SDK_ROOT}/platform-tools

# Копируем исходный код приложения
WORKDIR /app
COPY . .

# Делаем скрипт ожидания исполняемым
RUN chmod +x wait-for-db.sh

# Собираем APK
RUN ./gradlew assembleDebug

# Запускаем контейнер и держим его активным
CMD ["tail", "-f", "/dev/null"]