# LinguaLink 语链

多设备联机翻译 Android 应用。

**v1.0** | 使用 **Xiaomi MiMo V2.5 Pro** 开发

---

## 功能特性

### 翻译
- **在线翻译** - 支持任意 OpenAI 兼容 API（DeepSeek、OpenAI、通义千问、Ollama 等）
- **离线翻译** - 内置词典，支持中↔英、中↔日、英↔日，无需网络即可使用
- **20 种语言** - 中文、English、日本語、한국어、Français、Deutsch、Español、Русский 等
- **模型自动获取** - 一键从 API 端点拉取可用模型列表，无需手动输入

### 多设备联机
- **自动发现** - UDP 组播自动发现同一 Wi-Fi 下的设备
- **实时互译** - 点击连接后翻译结果自动同步
- **连接管理** - 显示服务状态、已连接设备、可点击连接/断开

### 其他
- **翻译历史** - 自动保存所有翻译记录，支持查看和清空
- **自动更新** - 从 GitHub Release 检测并下载更新
- **Material 3** - 动态主题，Android 12+ 支持壁纸取色
- **API Key 安全** - 密码遮罩显示，可切换可见性

---

## 快速开始

### 安装

从 [Releases](https://github.com/hzq1122/lingualink/releases) 下载 APK 安装。

### 配置 API

1. 打开应用 → **设置**
2. 填写 API 端点（如 `https://api.deepseek.com`）
3. 填写 API Key
4. 点击 **获取** 按钮自动拉取模型列表，从下拉框选择
5. 点击 **保存配置**

### 翻译

1. 首页选择源语言和目标语言
2. 输入文本，点击发送按钮
3. 翻译结果自动显示，历史记录自动保存

### 多设备联机

1. 确保所有设备连接同一 Wi-Fi
2. 点击右上角扫描按钮
3. 发现设备后点击名称连接
4. 翻译结果自动同步给已连接设备

### 离线翻译

无需配置 API，直接选择支持的语言对（中↔英、中↔日、英↔日）即可使用离线词典翻译。

---

## 系统要求

- Android 8.0 (API 26) 及以上
- 联机翻译需所有设备在同一局域网

---

## 本地编译

### 环境要求
- Android Studio Ladybug 或更高版本
- JDK 17
- Android SDK 35

### 编译 Debug

```bash
cd lingualink
# Windows
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew assembleDebug

# macOS / Linux
export JAVA_HOME=/path/to/android-studio/jbr
./gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

### Release 构建

```bash
./gradlew assembleRelease
```

需要在 `app/build.gradle.kts` 中配置签名密钥。

---

## 发布

```bash
git tag v1.0
git push origin v1.0
```

GitHub Actions 自动构建并发布 Release。

---

## API 兼容性

LinguaLink 兼容所有支持 OpenAI 格式的 API 服务：

| 端点 | 用途 |
|------|------|
| `GET /v1/models` | 获取可用模型列表 |
| `POST /v1/chat/completions` | 执行翻译 |

已测试服务：
- DeepSeek API (`https://api.deepseek.com`)
- OpenAI API (`https://api.openai.com`)
- 通义千问 API（端点设为 `https://dashscope.aliyuncs.com/compatible-mode`）
- Ollama（本地 `http://localhost:11434`）

---

## 技术栈

- **语言**: Kotlin
- **UI**: Jetpack Compose + Material 3
- **架构**: MVVM + Hilt DI
- **数据库**: Room
- **网络**: OkHttp
- **序列化**: kotlinx.serialization
- **异步**: Kotlin Coroutines + Flow

---

## 项目结构

```
app/src/main/java/com/lingualink/
├── app/
│   ├── MainActivity.kt           # 入口 Activity
│   ├── LinguaLinkApp.kt          # Application
│   └── di/AppModule.kt           # Hilt 依赖注入
├── data/db/                      # Room 数据库
│   ├── AppDatabase.kt
│   ├── dao/TranslationDao.kt
│   └── entity/TranslationEntity.kt
├── domain/model/                 # 领域模型
├── network/                      # 网络层
│   ├── client/DeviceHttpClient.kt    # HTTP 客户端
│   ├── discovery/MulticastDiscovery.kt # 设备发现
│   ├── dto/NetworkDto.kt            # 数据传输对象
│   └── server/FanyiHttpServer.kt    # HTTP 服务端
├── translation/                  # 翻译引擎
│   ├── TranslationEngine.kt         # 引擎接口
│   ├── OnlineTranslationEngine.kt   # 在线引擎
│   ├── OfflineTranslationEngine.kt  # 离线引擎
│   └── SupportedLanguage.kt         # 语言定义
├── update/                       # 应用更新
└── ui/                           # UI 层
    ├── LinguaLinkNavHost.kt         # 导航
    ├── components/LanguageSelector.kt # 语言选择器
    ├── screens/                     # 页面
    │   ├── HomeScreen.kt            # 首页
    │   ├── HistoryScreen.kt         # 历史记录
    │   └── SettingsScreen.kt        # 设置
    └── viewmodel/                   # ViewModel
```

---

## 许可证

MIT License

---

## 开发说明

本项目使用 **Xiaomi MiMo V2.5 Pro** AI 编程助手开发完成。
