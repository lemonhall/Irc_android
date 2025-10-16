# Copilot Instructions for IRC Android Client

## 项目概览
- 本项目为 Android 平台的 IRC 客户端，采用 MVVM 架构，核心功能为 IRC 协议通讯、频道管理与现代化 Material Design UI。
- 主要代码位于 `app/src/main/java/com/lsl/irc_android/`，分为聊天、频道列表、设置、IRC 协议层等模块。

## 架构与模块
- MVVM 分层：
  - View (Fragment/Activity)：UI 展现与事件响应
  - ViewModel：业务逻辑、状态管理，使用 LiveData 驱动 UI
  - Model：数据与协议处理（如 `IrcClient`, `IrcConfig`）
- IRC 协议实现于 `irc/` 目录，支持 NICK、USER、JOIN、PRIVMSG、PING/PONG 等命令，响应码有智能折叠与高亮处理。
- UI 采用 Material Design 3，三标签导航（聊天、频道、设置），RecyclerView 用于高效消息/频道列表。

## 开发与构建流程
- 构建命令（Windows）：
  ```pwsh
  .\gradlew assembleDebug
  ```
- 安装 APK 到设备：
  ```pwsh
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```
- 推荐使用 Android Studio Hedgehog (2023.1.1+) 与 JDK 17+

## 关键约定与模式
- 配置持久化采用 SharedPreferences，相关逻辑在 `IrcConfigManager.kt`
- 并发安全通过 Kotlin Mutex 实现，异步通信用 Kotlin Coroutines
- 所有 UI 事件通过 ViewModel 处理，避免直接在 Fragment/Activity 操作 Model
- 系统消息、MOTD、频道列表等均有专门的数据类与适配器（如 `ChatMessageAdapter`, `ChannelAdapter`）
- 频道与消息数据类分别为 `IrcChannel`, `ChatMessage`，请保持类型一致性

## 依赖与集成
- 主要依赖见 `build.gradle.kts` 与 `libs.versions.toml`，如 AndroidX、Material、Coroutines、Lifecycle、Navigation
- 网络通信基于 Socket，编码为 UTF-8，连接/读取有超时设置（10s/30s）
- 未来计划支持 SSL/TLS、私聊、多频道、消息通知等功能

## 贡献与协作
- 分支命名建议：`feature/xxx`，提交信息需简明描述变更内容
- Pull Request 前请确保本地编译通过，遵循 MVVM 与模块分层约定

## 典型文件/目录参考
- `app/src/main/java/com/lsl/irc_android/ui/home/HomeViewModel.kt`：聊天业务逻辑
- `app/src/main/java/com/lsl/irc_android/irc/IrcClient.kt`：IRC 协议核心实现
- `app/src/main/java/com/lsl/irc_android/ui/dashboard/DashboardViewModel.kt`：频道列表逻辑
- `app/src/main/java/com/lsl/irc_android/irc/IrcConfigManager.kt`：配置管理与持久化

---
如遇架构或约定不明之处，请优先查阅 README 或相关 ViewModel/Model 文件。