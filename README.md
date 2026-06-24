# FitPlan

FitPlan 是一个安卓端健身计划管理 App，使用 Kotlin、Jetpack Compose、MVVM、Room、Navigation Compose、DataStore 和 Retrofit 构建。

## 当前功能

- 首页展示当前计划、“今天练什么”、本周训练次数和最近一次训练时间。
- 支持新建、编辑、复制、删除训练计划，并可设置当前计划。
- 支持查看计划详情、按训练日开始训练、勾选动作完成状态、填写训练备注。
- 支持保存训练记录、按日期倒序查看历史记录和记录详情。
- 支持在设置页保存 DeepSeek API Key 和模型名。
- 支持 DeepSeek 生成训练计划，生成结果先预览，可编辑后再保存为新计划。
- 手动计划和历史记录功能离线可用。

## DeepSeek 配置

1. 打开 App 的“设置”页面。
2. 输入 DeepSeek API Key。
3. 模型默认使用 `deepseek-v4-flash`，可在设置页修改。
4. 返回“AI”页面填写训练需求并生成计划。

API Key 当前使用 DataStore 保存在本机，符合开发阶段要求；后续可升级为加密存储。

## 本地构建

项目使用 Android Gradle Plugin、Kotlin、Room、Compose 和 KSP。推荐使用 JDK 17 与 Android SDK 36。

常用验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

如果使用本项目便携工具链，可在当前命令中临时设置：

```powershell
$env:JAVA_HOME='D:\fp-toolchain\jdk17-msi\PFiles64\Microsoft\jdk-17.0.19.10-hotspot'
$env:ANDROID_HOME='D:\fp-toolchain\android-sdk'
$env:ANDROID_SDK_ROOT='D:\fp-toolchain\android-sdk'
$env:PATH="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:PATH"
D:\fp-toolchain\gradle-8.14.3\bin\gradle.bat :app:assembleDebug
```

## UI 原型

可直接打开 `ui-preview/index.html` 查看移动端可点击 HTML 原型，用于确认页面风格和功能点。

## 阶段状态

- 阶段 1：项目骨架、本地 Room 数据层、Repository。
- 阶段 2：Compose UI、MVVM、本地计划与训练记录闭环。
- 阶段 3：DeepSeek 生成计划、设置页 API Key 保存。
- 阶段 4：中文文案修复、删除确认、操作反馈、AI/设置体验增强、测试与发布前说明。
