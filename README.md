# FitPlan

FitPlan 是一个安卓端健身计划管理 App，使用 Kotlin、Jetpack Compose、MVVM、Room、Navigation Compose、DataStore 和 Retrofit 构建。目标是让用户快速看到“今天练什么”，并能离线维护训练计划、执行训练和查看历史记录。

## 当前功能

- 首页展示当前计划、今日训练日、动作预览、本周训练次数、近 7 天训练次数和总分钟数。
- 支持首次使用时一键创建本地示例计划，帮助内测用户快速走通完整流程。
- 支持新建、编辑、复制、删除训练计划，并可设置当前计划。
- 支持训练日和动作的新增、删除、上移、下移。
- 支持开始训练、动作完成勾选、实际备注、完成进度、休息计时和草稿恢复。
- 存在未完成动作时，完成训练前会先提示确认。
- 支持按全部、本周、本月筛选历史记录，记录详情使用动作快照。
- 支持本地备份 JSON 导出、导入预览和确认导入；导入只新增，不覆盖已有数据。
- 支持在设置页保存 DeepSeek API Key 和模型名，AI 生成结果先预览、可编辑，再保存为新计划。
- 手动计划、训练执行、历史记录和备份功能均可离线使用。

## 阶段 9 状态

阶段 9 聚焦内测反馈修复和 1.0 发布候选收口：

- 新增内测反馈日志，用于记录问题、复现步骤、优先级和处理状态。
- 继续修复真实设备上的小屏、长文本、键盘遮挡、弹窗和深色模式问题。
- 增强备份导入结果反馈，显示新增计划数、记录数和跳过项。
- 训练草稿增加过期提示，避免很久以前的旧草稿被误恢复。
- 生成 1.0 RC notes，记录功能范围、已知限制、验证结果和回滚建议。

## DeepSeek 配置

1. 打开 App 的“设置”页面。
2. 输入 DeepSeek API Key。
3. 模型默认使用 `deepseek-v4-flash`，可在设置页修改。
4. 点击“测试 DeepSeek 连接”验证 Key、模型和网络。
5. 返回“AI”页面填写训练需求并生成计划。

API Key 当前使用 DataStore 保存到本机；卸载 App 会丢失，后续可升级为加密存储。

## 本地构建

项目推荐使用 JDK 17 和 Android SDK 36。

常用验证命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:lintDebug
```

如果公司电脑的安全软件拦截 wrapper 下载，或 PATH 中仍是 Java 8，可使用当前机器的便携工具链：

```powershell
$env:JAVA_HOME='D:\fp-toolchain\jdk17-full\jdk-17.0.19+10'
$env:ANDROID_HOME='D:\fp-toolchain\android-sdk'
$env:ANDROID_SDK_ROOT='D:\fp-toolchain\android-sdk'
D:\fp-toolchain\gradle-8.14.3\bin\gradle.bat :app:testDebugUnitTest
D:\fp-toolchain\gradle-8.14.3\bin\gradle.bat :app:assembleDebug
D:\fp-toolchain\gradle-8.14.3\bin\gradle.bat :app:assembleRelease
D:\fp-toolchain\gradle-8.14.3\bin\gradle.bat :app:lintDebug
```

## Release 签名

- 仓库只提供 `signing.properties.example`。
- 复制为 `signing.properties` 后填写本机 keystore 信息，即可生成签名 release APK。
- `signing.properties`、`.jks` 和 `.keystore` 已加入 `.gitignore`，不要提交真实密钥或密码。
- 未配置 `signing.properties` 时，仍可运行 `:app:assembleRelease` 生成 unsigned release APK 用于构建检查。

## UI 原型

可直接打开 `ui-preview/index.html` 查看移动端可点击 HTML 原型，用于确认页面风格、核心流程和阶段功能点。

## 阶段状态

- 阶段 1：项目骨架、本地 Room 数据层、Repository。
- 阶段 2：Compose UI、MVVM、本地计划与训练记录闭环。
- 阶段 3：DeepSeek 生成计划、设置页 API Key 保存。
- 阶段 4：中文文案修复、删除确认、操作反馈、AI/设置体验增强。
- 阶段 5：训练日选择与排序、本地备份导入导出、训练恢复与休息计时。
- 阶段 6：内测发布准备、release 签名模板、草稿处理、历史筛选和发布清单。
- 阶段 7：全 App UI 现代化、新建/编辑计划体验升级、HTML 原型同步。
- 阶段 8：真实内测体验闭环、示例计划、误操作防护、可访问性和发布质量加固。
- 阶段 9：内测反馈修复、真机体验收口、1.0 RC 发布候选准备。

## 发布与 QA 文档

- 阶段 5 真实设备和内部测试清单见 `docs/phase5-qa-checklist.md`。
- 发布检查清单见 `docs/release-checklist.md`。
- 阶段 8 内测反馈清单见 `docs/phase8-internal-test.md`。
- 阶段 9 内测反馈日志见 `docs/beta-feedback-log.md`。
- 1.0 RC 说明见 `docs/1.0-rc-notes.md`。
