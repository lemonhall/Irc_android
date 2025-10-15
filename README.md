# IRC Android Client

一个功能完整、界面现代的 Android IRC 客户端应用

![License](https://img.shields.io/badge/license-MIT-blue.svg)
![Platform](https://img.shields.io/badge/platform-Android-green.svg)
![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)
![Language](https://img.shields.io/badge/language-Kotlin-orange.svg)

## ✨ 功能特性

### 核心功能

- ✅ **IRC 协议支持** - 完整实现 IRC 客户端协议
- ✅ **实时通讯** - 基于 Socket 的异步消息收发
- ✅ **频道管理** - 浏览、加入、切换 IRC 频道
- ✅ **MOTD 优化** - 智能折叠服务器欢迎消息
- ✅ **自动滚动** - 新消息自动滚动到可见区域
- ✅ **配置持久化** - SharedPreferences 保存用户设置

### 用户界面

- 📱 **Material Design 3** - 现代化的 UI 设计
- 🎨 **三标签导航** - 聊天、频道列表、设置
- 💬 **聊天界面** - 清晰的消息显示，支持系统消息高亮
- 📋 **频道列表** - 可视化频道浏览，显示在线人数和话题
- ⚙️ **设置页面** - 友好的配置界面，支持一键重置

### 技术亮点

- 🔒 **线程安全** - 使用 Mutex 确保并发安全
- ⚡ **协程异步** - Kotlin Coroutines 实现非阻塞 I/O
- 🏗️ **MVVM 架构** - 清晰的代码分层，易于维护
- 🎯 **LiveData 响应式** - 数据驱动的 UI 更新
- 🛡️ **错误处理** - 完善的异常捕获和用户提示

## 📸 应用截图

### 聊天界面
- 实时消息显示
- 自动滚动到最新消息
- 折叠式服务器消息（点击展开）
- 系统消息淡化显示

### 频道列表
- 显示所有可用频道
- 实时显示在线人数
- 频道话题预览
- 点击即可加入

### 设置页面
- IRC 服务器配置
- 用户昵称设置
- 默认频道配置
- 一键连接和加入

## 🏗️ 技术架构

### 架构模式

```
MVVM (Model-View-ViewModel) 架构
├── View (Fragment/Activity) - UI 层
├── ViewModel - 业务逻辑和状态管理
└── Model - 数据层 (IrcClient, IrcConfig)
```

### 项目结构

```
app/src/main/java/com/lsl/irc_android/
├── MainActivity.kt                 # 主 Activity
├── ui/
│   ├── home/                       # 聊天模块
│   │   ├── HomeFragment.kt         # 聊天界面
│   │   ├── HomeViewModel.kt        # 聊天逻辑
│   │   ├── ChatMessage.kt          # 消息数据类
│   │   ├── ChatMessageAdapter.kt   # 消息列表适配器
│   │   └── ChannelInfo.kt          # 频道信息
│   ├── dashboard/                  # 频道列表模块
│   │   ├── DashboardFragment.kt    # 频道列表界面
│   │   ├── DashboardViewModel.kt   # 频道列表逻辑
│   │   ├── IrcChannel.kt           # 频道数据类
│   │   └── ChannelAdapter.kt       # 频道列表适配器
│   └── notifications/              # 设置模块
│       ├── NotificationsFragment.kt # 设置界面
│       └── NotificationsViewModel.kt # 设置逻辑
├── irc/                            # IRC 协议层
│   ├── IrcClient.kt                # IRC 客户端核心
│   ├── IrcMessage.kt               # IRC 消息解析
│   ├── IrcConfig.kt                # 配置数据类
│   └── IrcConfigManager.kt         # 配置管理器
└── ConnectionState.kt              # 连接状态枚举
```

## 🛠️ 技术栈

### 开发环境

- **开发语言**: Kotlin 1.9.0
- **最低 SDK**: Android 7.0 (API 24)
- **目标 SDK**: Android 14 (API 34)
- **构建工具**: Gradle 8.7 + AGP 8.6.0

### 核心库

| 库名称 | 版本 | 用途 |
|--------|------|------|
| AndroidX Core KTX | 1.13.1 | Kotlin 扩展 |
| AppCompat | 1.7.0 | 兼容性支持 |
| Material Components | 1.12.0 | Material Design UI |
| ConstraintLayout | 2.1.4 | 约束布局 |
| Lifecycle | 2.8.4 | 生命周期组件 |
| Navigation | 2.7.7 | 导航组件 |
| Kotlin Coroutines | 1.7.3 | 异步编程 |

### 网络通信

- **协议**: IRC (Internet Relay Chat)
- **传输**: Socket + BufferedReader/Writer
- **编码**: UTF-8
- **超时设置**: 连接超时 10s，读取超时 30s

## 🚀 快速开始

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK 34
- 一个 Android 设备或模拟器 (API 24+)

### 构建步骤

1. **克隆项目**
```bash
git clone https://github.com/lemonhall/Irc_android.git
cd Irc_android
```

2. **编译项目**
```bash
# Windows
.\gradlew assembleDebug

# Linux/Mac
./gradlew assembleDebug
```

3. **安装到设备**
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 默认配置

应用预设了以下默认配置，可在设置页面修改：

- **服务器**: irc.lemonhall.me
- **端口**: 6667
- **昵称**: lemon_an
- **默认频道**: #ai-collab-test

## 📖 使用指南

### 首次使用

1. 打开应用，点击底部导航栏的 **"设置"** 标签
2. 检查并修改 IRC 服务器配置（或使用默认配置）
3. 点击 **"连接到IRC服务器"** 按钮
4. 连接成功后，点击 **"加入默认频道"** 按钮
5. 切换到 **"聊天"** 标签开始聊天

### 浏览和加入频道

1. 点击底部导航栏的 **"频道"** 标签
2. 点击右上角的 **"刷新"** 按钮获取服务器频道列表
3. 浏览可用频道，查看在线人数和话题
4. 点击任意频道卡片即可加入该频道
5. 自动切换到聊天标签开始交流

### 聊天功能

- **发送消息**: 在底部输入框输入内容，点击发送按钮或按回车键
- **查看历史**: 向上滚动查看历史消息
- **查看 MOTD**: 点击 "📋 服务器欢迎消息" 可展开/折叠详情
- **系统通知**: 用户加入/离开/改名会有系统提示

## 🎨 界面设计

### 配色方案

- **主色**: Material 紫色 (#6200EE)
- **次色**: Material 青色 (#03DAC5)
- **背景**: 白色/浅灰
- **系统消息**: 60% 透明度

### 布局特点

- ✅ 全屏聊天区域（移除冗余状态栏）
- ✅ 输入框固定在底部（不被导航栏遮挡）
- ✅ RecyclerView 高效列表显示
- ✅ Material 卡片式设计
- ✅ 圆角按钮统一样式

## 🔧 IRC 协议支持

### 已实现命令

| 命令 | 说明 | 状态 |
|------|------|------|
| NICK | 设置昵称 | ✅ |
| USER | 用户注册 | ✅ |
| JOIN | 加入频道 | ✅ |
| PRIVMSG | 发送消息 | ✅ |
| PING/PONG | 保持连接 | ✅ |
| LIST | 获取频道列表 | ✅ |
| QUIT | 断开连接 | ✅ |

### 已处理响应码

| 代码 | 名称 | 说明 | 处理方式 |
|------|------|------|----------|
| 001 | RPL_WELCOME | 欢迎消息 | 折叠显示 |
| 002-005 | 服务器信息 | 服务器详情 | 折叠显示 |
| 251-255 | LUSERCLIENT | 用户统计 | 折叠显示 |
| 322 | RPL_LIST | 频道列表项 | 更新频道列表 |
| 323 | RPL_LISTEND | 列表结束 | 显示频道数量 |
| 332 | RPL_TOPIC | 频道话题 | 显示话题 |
| 353 | RPL_NAMREPLY | 用户列表 | 隐藏（太长）|
| 366 | RPL_ENDOFNAMES | 列表结束 | 显示加入成功 |
| 372 | RPL_MOTD | MOTD 内容 | 折叠显示 |
| 375 | RPL_MOTDSTART | MOTD 开始 | 开始收集 |
| 376 | RPL_ENDOFMOTD | MOTD 结束 | 折叠显示 |

## 🐛 已知问题

目前没有已知的严重问题。如有发现，请提交 Issue。

## 📝 开发计划

### 近期计划

- [ ] SSL/TLS 加密连接支持
- [ ] 私聊功能
- [ ] 多频道切换
- [ ] 消息通知
- [ ] 聊天记录持久化

### 远期计划

- [ ] 文件传输 (DCC)
- [ ] 表情符号支持
- [ ] 聊天记录搜索
- [ ] 用户列表显示
- [ ] 频道管理功能（踢人、封禁等）

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 👨‍💻 作者

**lemonhall**

- GitHub: [@lemonhall](https://github.com/lemonhall)

## 🙏 致谢

- Material Design Components
- Kotlin Coroutines
- Android Jetpack
- IRC 社区

## 📞 联系方式

如有问题或建议，请：

- 提交 [Issue](https://github.com/lemonhall/Irc_android/issues)
- 发送邮件
- 加入我们的 IRC 频道: #ai-collab-test @ irc.lemonhall.me

---

⭐ 如果这个项目对你有帮助，请给个 Star！
