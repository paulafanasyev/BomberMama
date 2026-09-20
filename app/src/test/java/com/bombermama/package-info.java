/**
 * Тесты Android-слоя. Отключаем нативный Conscrypt: его aarch64 .so требует
 * GLIBC 2.32, а в окружении доступен 2.31. Криптопровайдер этим тестам не нужен.
 */
@org.robolectric.annotation.ConscryptMode(
    org.robolectric.annotation.ConscryptMode.Mode.OFF
)
package com.bombermama;
