# FitPlan

FitPlan 是一个安卓端健身计划管理 App，使用 Kotlin、Jetpack Compose、MVVM、Room、Navigation Compose、DataStore 和 Retrofit 构建。目标是让用户快速看到“今天练什么”，并能离线维护训练计划、执行训练和查看历史记录。

## 当前功能

- 首页展示当前计划、今日训练日、动作预览、本周训练次数、近 7 天训练次数和总分钟数。
- 支持选择今日训练日，选择结果会保存到本机；默认使用当前计划的第一个训练日。
- 支持首次使用时一键创建本地示例计划，帮助内测用户快速走通完整流程。
- 支持新建、编辑、复制、删除训练计划，并可设置当前计划。
- 支持训练日和动作的新增、删除、上移、下移。
- 支持开始训练、勾选动作完成状态、填写实际备注、完成进度、休息倒计时和休息结束提示。
- 训练中途退出后会保存草稿，再次进入同一计划训练日时可继续或放弃草稿。
- 训练存在未完成动作时，完成训练前会提醒用户确认。
- 支持保存训练记录、按日期倒序查看历史记录、按全部/本周/本月筛选，并查看记录详情。
- 历史记录使用动作快照，计划后续编辑不会影响已有记录。
- 支持本地备份 JSON 导出、版本和导出时间展示、预览导入和确认导入；导入只新增，不覆盖已有数据。
- 支持在设置页保存 DeepSeek API Key 和模型名，并测试 DeepSeek 连接。
- 支持 DeepSeek 生成训练计划；生成结果先预览，可编辑后保存为新计划。
- 手动计划、训练执行、历史记录和备份功能可离线使用。

## 阶段 8 状态

阶段 8 聚焦真实内测体验闭环和可维护性加固：

- 首次使用空状态增加“创建示例计划/快速开始”入口。
- 编辑计划校验提示更具体，能指出具体训练日和动作位置。
- 完成训练前会检查未完成动作，降低误保存风险。
- 删除、放弃草稿、导入备份等风险操作保持确认提示。
- AI 生成结果保存前进行更明确校验，保存成功后进入计划详情。
- 补充内测反馈清单和构建验证记录。

## DeepSeek 配置

1. 打开 App 的“设置”页面。
2. 输入 DeepSeek API Key。
3. 模型默认使用 `deepseek-v4-flash`，可在设置页修改。
4. 点击“测试 DeepSeek 连接”验证 Key、模型和网络。
5. 返回“AI”页面填写训练需求并生成计划。

API Key 当前使用 DataStore 保存在本机；卸载 App 会丢失，后续可升级为加密存储。

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
D:\fp-toolchain\gradle-8.14.3\bin\gradle.bat :app:lintDebug
```

## Release 签名

- 仓库只提供 `signing.properties.example`。
- 复制为 `signing.properties` 后填写本机 keystore 信息，即可生成签名 release APK。
- `signing.properties`、`.jks` 和 `.keystore` 已加入 `.gitignore`，不要提交真实密钥或密码。
- 未配置 `signing.properties` 时，仍可运行 `:app:assembleRelease` 生成未签名 release APK 用于构建检查。

## UI 原型

可直接打开 `ui-preview/index.html` 查看移动端可点击 HTML 原型，用于确认页面风格、核心流程和阶段 8 功能点。

## 阶段状态

- 阶段 1：项目骨架、本地 Room 数据层、Repository。
- 阶段 2：Compose UI、MVVM、本地计划与训练记录闭环。
- 阶段 3：DeepSeek 生成计划、设置页 API Key 保存。
- 阶段 4：中文文案修复、删除确认、操作反馈、AI/设置体验增强、发布前说明。
- 阶段 5：训练日选择与排序、本地备份导入导出、训练恢复与休息计时、AI 连接测试、发布候选 QA。
- 阶段 6：内测发布准备、release 签名模板、草稿处理、训练进度、历史筛选、备份安全提示和发布清单。
- 阶段 7：全 App UI 现代化、新建/编辑计划体验升级、HTML 原型同步、主要中文乱码修复和构建验证。
- 阶段 8：真实内测体验闭环、首次使用示例计划、误操作防护、可访问性和发布质量加固。

## 发布候选 QA

- 阶段 5 真实设备和内部测试清单见 `docs/phase5-qa-checklist.md`。
- 阶段 6 发布检查清单见 `docs/release-checklist.md`。
- 阶段 8 内测反馈清单见 `docs/phase8-internal-test.md`。
