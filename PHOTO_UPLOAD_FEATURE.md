# IRC 客户端拍照上传功能实现说明

## 功能概览

添加了一个完整的拍照和上传图片到图床的功能，用户可以在聊天输入栏中点击拍照按钮，拍照后自动上传到配置的图床，生成链接后附加到聊天消息中。

## 实现的功能模块

### 1. **图片上传管理器** (`ImageUploadManager.kt`)
   - 位置: `app/src/main/java/com/lsl/irc_android/data/ImageUploadManager.kt`
   - 功能:
     - 从相机 URI 读取和保存图片到缓存
     - 压缩图片以减少上传大小
     - 上传图片到图床（支持 ImgBB、Imgur 等标准图床 API）
     - 解析图床返回的 JSON 响应获取图片 URL
     - 清理缓存中的图片文件

### 2. **HomeViewModel 扩展**
   - 添加了图片上传相关的 LiveData:
     - `uploadingImage`: 监听上传状态（加载中/完成）
     - `uploadedImageUrl`: 获取上传后的图片链接
   - 新增方法:
     - `handleCameraImage()`: 处理相机拍照后的图片，自动上传到图床
     - `sendMessageWithImage()`: 发送包含图片链接的消息
     - 在 `onCleared()` 中添加清理缓存图片的逻辑

### 3. **HomeFragment 拍照功能**
   - 位置: `app/src/main/java/com/lsl/irc_android/ui/home/HomeFragment.kt`
   - 功能:
     - 在输入栏添加了"📷"拍照按钮
     - 实现了相机权限请求流程
     - 集成了 Android 的相机 Intent（支持 Android Q 及以上版本）
     - 拍照完成后显示确认对话框，让用户选择是否上传
     - 上传中状态下禁用发送和拍照按钮
     - 上传成功后自动在输入框中插入图片链接

### 4. **配置管理扩展** (`IrcConfigManager.kt`)
   - 添加了图床相关的配置:
     - `imageHost`: 图床的上传 API URL
     - `apiKey`: 图床的 API Key
     - 配置支持 SharedPreferences 持久化

### 5. **UI 布局更新** (`fragment_home.xml`)
   - 在消息输入栏添加了拍照按钮
   - 拍照按钮显示为 "📷" emoji

### 6. **权限配置** (`AndroidManifest.xml`)
   - 添加了所需权限:
     - `CAMERA`: 使用相机
     - `READ_EXTERNAL_STORAGE`: 读取外部存储
     - `WRITE_EXTERNAL_STORAGE`: 写入外部存储
   - 配置了 FileProvider 用于保存相机拍照

### 7. **FileProvider 配置** (`file_paths.xml`)
   - 配置了文件访问权限路径
   - 支持外部文件目录、缓存目录等

## 使用流程

### 用户侧操作
1. 点击输入栏左侧的"📷"拍照按钮
2. 系统请求相机权限（首次使用）
3. 启动手机相机进行拍照
4. 拍照完成后显示确认对话框
5. 选择"上传"按钮上传图片
6. 上传中显示加载状态
7. 上传成功后 Markdown 格式的图片链接自动插入到输入框最前面：`![图片](url) `
8. 用户可以在图片链接后添加文字描述后点击发送按钮发送消息
9. 最终发送的消息格式为：`![图片](url) 用户输入的文字描述`

### 消息格式

应用使用 **Markdown 图片语法** 来标记图片资源：

```
![图片](https://i.ibb.co/xxx/img.jpg)
```

或带有文字描述：

```
![图片](https://i.ibb.co/xxx/img.jpg) 这是一张示例图片
```

#### 格式优势

- **标准格式** - 使用广泛认可的 Markdown 语法
- **易于解析** - 正则表达式可轻松提取：`!\[.*?\]\((.*?)\)`
- **易于扩展** - 支持链接、加粗等其他 Markdown 特性
- **Bot 友好** - 服务端可用 Markdown 解析库直接处理

#### 服务端解析建议

**Python 示例：**
```python
import re
import markdown

def extract_image_urls(text):
    """从消息中提取图片 URL"""
    pattern = r'!\[.*?\]\((.*?)\)'
    urls = re.findall(pattern, text)
    return urls

def extract_text_without_images(text):
    """移除图片 Markdown，保留文字"""
    pattern = r'!\[.*?\]\(.*?\)\s*'
    return re.sub(pattern, '', text).strip()

# 使用
message = "![图片](https://i.ibb.co/xxx/img.jpg) 这是描述"
urls = extract_image_urls(message)  # ['https://i.ibb.co/xxx/img.jpg']
text = extract_text_without_images(message)  # '这是描述'
```

### 图床配置

目前需要手动在代码中配置图床地址和 API Key。建议的配置方式：

#### 方式一：在设置界面中添加配置（推荐）
编辑 `NotificationsFragment.kt` 或创建新的设置界面，添加以下配置选项：
- 图床地址输入框 (imageHost)
- API Key 输入框 (apiKey)

示例配置代码：
```kotlin
val configManager = IrcConfigManager(context)
configManager.saveImageHost("https://api.imgbb.com/1/upload")
configManager.saveApiKey("your-api-key-here")
```

#### 方式二：使用公共图床服务
推荐使用免费的公共图床服务：

**ImgBB** (推荐)
- URL: `https://api.imgbb.com/1/upload`
- 获取 API Key: https://imgbb.com/
- 特点: 免费、支持较大文件、保留时间长

**Imgur**
- URL: `https://api.imgur.com/3/image`
- 获取 API Key: https://api.imgur.com/oauth2/addclient
- 特点: 免费、国际知名、支持 HTTPS

## 技术细节

### 图片上传流程
```
拍照 → 保存到缓存 → 压缩 → 上传到图床 → 解析响应 → 获取 URL → 插入输入框
```

### 权限处理
- 使用 Android 13+ 的现代权限 API (`ActivityResultContracts`)
- 支持向下兼容到 Android 6.0+
- 优雅的权限请求和拒绝处理

### 图片压缩
- 自动将大于 1920x1920 的图片按比例缩放
- JPEG 压缩质量设置为 85%
- 减少网络上传时间和数据消耗

### OkHttp 配置
- 连接超时: 30 秒
- 读取超时: 30 秒  
- 写入超时: 30 秒
- 支持 Multipart 文件上传

## 后续工作

### 需要配置的内容
1. **设置界面配置** - 创建用户友好的图床配置界面
2. **图床适配** - 根据不同图床 API 调整响应解析逻辑
3. **错误处理** - 优化网络错误提示和重试逻辑
4. **图片预览** - 在发送前显示图片预览（可选）

### 建议的改进
1. 支持多个图床切换
2. 上传进度条显示
3. 图片缓存管理
4. 离线图片队列
5. 本地相册选择（补充拍照功能）

## 文件清单

新增文件：
- `app/src/main/java/com/lsl/irc_android/data/ImageUploadManager.kt`
- `app/src/main/res/xml/file_paths.xml`

修改文件：
- `app/src/main/java/com/lsl/irc_android/ui/home/HomeViewModel.kt`
- `app/src/main/java/com/lsl/irc_android/ui/home/HomeFragment.kt`
- `app/src/main/java/com/lsl/irc_android/data/IrcConfigManager.kt`
- `app/src/main/res/layout/fragment_home.xml`
- `app/build.gradle.kts`
- `gradle/libs.versions.toml`
- `app/src/main/AndroidManifest.xml`

## 构建和测试

```bash
# 清理并构建
.\gradlew clean build

# 构建 Debug APK
.\gradlew assembleDebug

# 安装到设备
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 运行应用
adb shell am start -n com.lsl.irc_android/.MainActivity
```

## 依赖库

新增依赖：
- `androidx.activity:activity-ktx:1.7.0` - 用于 ActivityResultContracts
- `com.squareup.okhttp3:okhttp:4.11.0` - 用于 HTTP 请求和文件上传

## 注意事项

1. **图床配置** - 在首次使用前必须在设置中配置图床 URL 和 API Key，否则拍照功能无法使用
2. **权限请求** - 应用会在首次拍照时请求必要权限，用户需要授予权限
3. **网络环境** - 图片上传需要网络连接，建议在 WiFi 环境下使用
4. **缓存清理** - 应用关闭时会自动清理缓存图片，不会占用大量存储空间
5. **文件格式** - 目前仅支持 JPEG 格式，拍照后自动转换

## 常见问题

**Q: 拍照后无法上传图片？**
A: 请检查：
1. 是否在设置中配置了图床地址
2. API Key 是否正确
3. 手机网络连接是否正常
4. 检查应用日志查看具体错误信息

**Q: 为什么上传这么慢？**
A: 可能原因：
1. 网络连接速度慢
2. 图床服务器响应慢
3. 可考虑切换到其他图床

**Q: 如何更换图床？**
A: 在设置中重新输入图床地址和 API Key 即可

## 相关文档

- [ImgBB API 文档](https://api.imgbb.com/)
- [Imgur API 文档](https://apidocs.imgur.com/)
- [Android 相机开发指南](https://developer.android.google.cn/guide/topics/media/camera)
- [Android 文件访问指南](https://developer.android.google.cn/training/data-storage)
