# DwgViewer (LibreDWG + Jetpack Compose)

1. Установите Android NDK (через SDK Manager) и задайте ANDROID_NDK_HOME.
2. Соберите LibreDWG:  ./scripts/build_libredwg.sh
3. Откройте проект в Android Studio, нажмите Run.

Лицензия: LibreDWG распространяется под GPLv3, поэтому приложение
в целом тоже должно распространяться под GPL с открытым исходным кодом.
Поддерживаются LINE, CIRCLE, ARC, LWPOLYLINE (расширяется в dwg_jni.cpp).

## Сборка APK через GitHub Actions
Залейте проект в репозиторий на GitHub. Сборка стартует при каждом push
(или вручную: вкладка Actions → Build APK → Run workflow).
Готовый APK: Actions → последний запуск → Artifacts → DwgViewer-debug-apk.
