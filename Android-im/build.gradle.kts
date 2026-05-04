plugins {
    id("com.android.application") version "8.7.2" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.kapt") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.0.21" apply false
}

// 可选：构建遥测上报（逻辑在独立脚本中，去掉下一行即可关闭）
apply(from = "gradle/polarbear-build-telemetry.gradle")
