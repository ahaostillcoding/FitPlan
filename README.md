# FitPlan

FitPlan 是一个安卓端健身计划管理 App，使用 Kotlin、Jetpack Compose、MVVM、Room、Navigation Compose、DataStore 和 Retrofit 构建。目标是让用户快速查看“今天练什么”，并能离线维护训练计划和历史记录。

## 当前功能

- 首页展示当前计划、今日训练日、动作预览、本周训练次数和最近一次训练时间。
- 支持选择今日训练日，默认使用当前计划的第一个训练日。
- 支持新建、编辑、复制、删除训练计划，并可设置当前计划。
- 支持训练日和动作排序。
- 支持开始训练、勾选动作完成状态、填写实际备注、休息倒计时。
- 训练中途退出后会保存草稿，再次进入同一计划训练日时可恢复。
- 支持保存训练记录、按日期倒序查看历史记录和记录详情。
- 支持本地备份 JSON 导出、预览导入和确认导入；导入不会覆盖已有数据。
- 支持在设置页保存 DeepSeek API Key、模型名，并测试 DeepSeek 连接。
- 支持 DeepSeek 生成训练计划；生成结果先预览，可编辑后保存为新计划。
- 手动计划、训练执行和历史记录功能可离线使用。

## DeepSeek 配置

1. 打开 App 的“设置”页面。
2. 输入 DeepSeek API Key。
3. 模型默认使用 `deepseek-v4-flash`，可在设置页修改。
4. 点击“测试 DeepSeek 连接”验证 Key、模型和网络。
5. 返回“AI”页面填写训练需求并生成计划。

API Key 当前使用 DataStore 保存在本机，符合开发阶段要求；后续可升级为加密存储。

## 本地构建

项目推荐使用 JDK 17 和 Android SDK 36。

常用验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

如果使用当前机器的便携工具链，可以在当前命令中临时设置：

```powershell
$env:JAVA_HOME='D:\fp-toolchain\jdk17-full\jdk-17.0.19+10'
$env:ANDROID_HOME='D:\fp-toolchain\android-sdk'
$env:ANDROID_SDK_ROOT='D:\fp-toolchain\android-sdk'
D:\fp-toolchain\gradle-8.14.3\bin\gradle.bat --no-daemon :app:assembleDebug
```

## UI 原型

可直接打开 `ui-preview/index.html` 查看移动端可点击 HTML 原型，用于确认页面风格、核心流程和阶段 5 功能点。

## 阶段状态

- 阶段 1：项目骨架、本地 Room 数据层、Repository。
- 阶段 2：Compose UI、MVVM、本地计划与训练记录闭环。
- 阶段 3：DeepSeek 生成计划、设置页 API Key 保存。
- 阶段 4：中文文案修复、删除确认、操作反馈、AI/设置体验增强、发布前说明。
- 阶段 5：训练日选择与排序、本地备份导入导出、训练恢复与休息计时、AI 连接测试、发布候选 QA。

## 发布候选 QA

阶段 5 的真实设备和内部测试清单见 `docs/phase5-qa-checklist.md`。
