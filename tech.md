# AccountKeeper 技术栈

本文记录项目正在使用的主要技术、版本号和作用。后续升级依赖、SDK、构建工具或核心架构时，需要同步更新本文。

## 应用平台

- Android
  - `compileSdk`: 36
  - `targetSdk`: 36
  - `minSdk`: 23
  - 作用：面向 Android 16 / OriginOS 6 开发，同时覆盖 Android 6.0 及以上设备。

## 开发语言

- Kotlin: 2.0.21
  - 配置位置：`build.gradle.kts`
  - 作用：主要业务逻辑和界面代码语言。

- Java Toolchain: 17
  - 配置位置：`gradle.properties`、`app/build.gradle.kts`
  - 本机路径：`D:/Java/jdk-17`
  - 作用：Android Gradle Plugin 和 Kotlin 编译使用的 Java 版本。

## 构建工具

- Gradle Wrapper: 8.13
  - 配置位置：`gradle/wrapper/gradle-wrapper.properties`
  - 作用：统一项目构建版本，避免不同电脑 Gradle 版本不一致。

- Android Gradle Plugin: 8.13.0
  - 配置位置：`build.gradle.kts`
  - 作用：负责 Android 项目的编译、资源处理、打包和安装配置。

## UI 技术

- Jetpack Compose BOM: 2024.12.01
  - 配置位置：`app/build.gradle.kts`
  - 作用：统一 Compose 组件版本。

- Jetpack Compose Foundation
  - 作用：基础布局、列表和交互能力。

- Jetpack Compose Material 3
  - 作用：按钮、输入框、卡片、底部导航等 Material 组件。

- Material Icons Extended
  - 作用：首页、流水、添加、账户、导入等图标。

- Activity Compose: 1.9.3
  - 作用：让 `MainActivity` 使用 Compose 渲染界面。

## 数据与存储

- 当前存储：SharedPreferences + JSON
  - 主要文件：`LedgerStore.kt`
  - 作用：MVP 阶段保存账户、分类和流水。
  - 后续方向：迁移到 Room，以支持大量流水、自动识别原始事件、待确认队列、去重记录和预算计划。

## 自动识别与预算基础

- `RawEvent`
  - 主要文件：`RecognitionModels.kt`
  - 作用：承接通知、短信、导入、分享、OCR 等来源的原始信息。

- `ParsedTransaction`
  - 主要文件：`RecognitionModels.kt`
  - 作用：保存自动识别后的交易候选，包括金额、方向、平台、商户和置信度。

- `RecognitionEngine`
  - 主要文件：`RecognitionEngine.kt`
  - 作用：MVP 级文本解析器，后续会扩展为多来源、多规则的识别管线。

- `BudgetPlan` / `DailyBudgetState`
  - 主要文件：`BudgetModels.kt`
  - 作用：支持月预算、年攒钱目标和每日推荐额度。

- `BudgetEngine`
  - 主要文件：`BudgetEngine.kt`
  - 作用：根据预算计划和正式流水计算剩余额度、今日推荐额度和超额金额。

## 兼容旧系统

- Core Library Desugaring: 2.1.5
  - 配置位置：`app/build.gradle.kts`
  - 作用：让 Android 6.0 及以上设备也能使用 `java.time` 等较新的 Java API。

## 测试

- JUnit: 4.13.2
  - 配置位置：`app/build.gradle.kts`
  - 作用：单元测试。

- 当前测试：
  - `BudgetEngineTest.kt`
  - `RecognitionEngineTest.kt`

## 本机工具路径

- Android Studio: `D:/Android/AndroidStudio`
- Android SDK: `D:/Android/Sdk`
- JDK 17: `D:/Java/jdk-17`

## 版本维护要求

- 修改 `compileSdk`、`targetSdk`、`minSdk` 后，必须同步更新本文。
- 修改 Kotlin、Gradle、Android Gradle Plugin、Compose BOM、Activity Compose、JUnit、desugar 版本后，必须同步更新本文。
- 新增核心技术，例如 Room、WorkManager、通知监听、短信识别、OCR、机器学习分类，也必须写入本文。
