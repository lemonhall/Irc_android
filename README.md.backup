# Irc_android

一个基于 Android 的 IRC 客户端项目

## 📋 项目信息

- **项目名称**: Irc_android
- **包名**: `com.lsl.irc_android`
- **目标 SDK**: 34 (Android 14)
- **最低 SDK**: 24 (Android 7.0)
- **开发语言**: Kotlin
- **构建工具**: Gradle (Kotlin DSL)

## 🏗️ 项目架构

### 架构模式

- **MVVM 架构** (Model-View-ViewModel)
- **单 Activity + 多 Fragment** 的导航模式

### 核心组件

#### 主 Activity
- `MainActivity` - 应用入口,包含底部导航栏

#### 功能模块 (Fragment + ViewModel)

```
app/src/main/java/com/lsl/irc_android/
├── MainActivity.kt
└── ui/
    ├── home/
    │   ├── HomeFragment.kt
    │   └── HomeViewModel.kt
    ├── dashboard/
    │   ├── DashboardFragment.kt
    │   └── DashboardViewModel.kt
    └── notifications/
        ├── NotificationsFragment.kt
        └── NotificationsViewModel.kt
```

## 🛠️ 技术栈

### 核心库

- **AndroidX Core KTX** - Kotlin 扩展库
- **AppCompat** - 向下兼容支持
- **Material Design Components** - Material 设计组件
- **ConstraintLayout** - 约束布局

### 架构组件

- **Lifecycle** - 生命周期感知组件
  - LiveData - 可观察的数据持有类
  - ViewModel - UI 相关数据管理
- **Navigation Component** - 导航组件
  - Navigation Fragment
  - Navigation UI

### 构建特性

- **ViewBinding** - 类型安全的视图绑定 (已启用)
- **ProGuard** - 代码混淆 (Release 模式)

### 测试框架

- **JUnit** - 单元测试
- **Espresso** - UI 测试
- **AndroidX Test** - Android 测试支持

## 📱 UI 结构

### 导航方式

- 使用 `BottomNavigationView` 实现底部导航栏
- 通过 `Navigation Graph` 管理 Fragment 导航
- 三个主要导航目的地: Home, Dashboard, Notifications

### 布局文件

```
app/src/main/res/
├── layout/
│   ├── activity_main.xml          # 主 Activity 布局
│   ├── fragment_home.xml          # 主页 Fragment
│   ├── fragment_dashboard.xml     # 仪表盘 Fragment
│   └── fragment_notifications.xml # 通知 Fragment
├── navigation/
│   └── mobile_navigation.xml      # 导航图
└── menu/
    └── bottom_nav_menu.xml        # 底部导航菜单
```

## 🔧 开发配置

### Java 版本

- **源码兼容性**: Java 8
- **目标兼容性**: Java 8
- **Kotlin JVM 目标**: 1.8

### Gradle 配置

- **Gradle JVM 参数**: `-Xmx2048m -Dfile.encoding=UTF-8`
- **AndroidX**: 已启用
- **Kotlin 代码风格**: Official
- **R 类命名空间**: 非传递性 (已启用)

### 依赖仓库

- Google Maven Repository
- Maven Central

## 📝 代码规范

### Kotlin 最佳实践

- 使用 Kotlin 官方代码风格
- ViewBinding 的正确使用模式 (防止内存泄漏)
- Fragment 生命周期的标准管理

### ViewBinding 使用示例

```kotlin
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // 避免内存泄漏
    }
}
```

## 🚀 构建与运行

### 构建项目

```bash
# Windows
gradlew.bat build

# 构建 Debug 版本
gradlew.bat assembleDebug

# 构建 Release 版本
gradlew.bat assembleRelease
```

### 运行测试

```bash
# 单元测试
gradlew.bat test

# Android 仪器测试
gradlew.bat connectedAndroidTest
```

## 📦 项目结构

```
Irc_android/
├── app/                           # 应用模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/              # Kotlin/Java 源代码
│   │   │   ├── res/               # 资源文件
│   │   │   └── AndroidManifest.xml
│   │   ├── test/                  # 单元测试
│   │   └── androidTest/           # Android 仪器测试
│   └── build.gradle.kts           # 应用级构建配置
├── build.gradle.kts               # 项目级构建配置
├── settings.gradle.kts            # Gradle 设置
├── gradle.properties              # Gradle 属性配置
└── gradlew.bat                    # Gradle Wrapper (Windows)
```

## 📄 许可证

待定

## 👨‍💻 开发者

LSL
