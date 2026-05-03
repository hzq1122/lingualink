# LinguaLink 语链

多设备联机翻译 Android 应用。

## 功能

- **在线翻译**：支持任意 OpenAI 兼容 API（DeepSeek、通义千问、Ollama 等）
- **26 种语言**：中文、English、日本語、한국어、Français、Deutsch 等
- **多设备联机**：通过 UDP 组播发现设备，HTTP REST 通信
- **自动更新**：从 GitHub Release 检测并下载更新

## 快速开始

### 安装

从 [Releases](https://github.com/hzq1122/lingualink/releases) 下载 APK 安装。

### 配置 API

1. 打开应用 → 设置
2. 填写 API 端点（如 `https://api.deepseek.com`）
3. 填写 API Key
4. 选择模型（如 `deepseek-chat`）

### 翻译

1. 首页选择源语言和目标语言
2. 输入文本，点击发送
3. 翻译结果自动显示

### 多设备联机

1. 确保设备在同一 Wi-Fi 网络
2. 点击搜索按钮发现附近设备
3. 翻译结果自动同步到所有设备

## 本地编译

```bash
# 需要 Android SDK 和 JDK 17+
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set ANDROID_HOME=C:\Users\Admin\AppData\Local\Android\Sdk
gradlew assembleDebug
```

APK 输出：`app/build/outputs/apk/debug/app-debug.apk`

## 发布

```bash
git tag v1.0.0
git push origin v1.0.0
```

GitHub Actions 自动构建并发布 Release。

## 技术栈

- Kotlin + Jetpack Compose
- Hilt (DI) + Room (DB)
- OkHttp (网络通信)
- kotlinx.serialization (JSON)

## 项目结构

```
app/src/main/java/com/lingualink/
├── app/           # Application, DI
├── domain/model/  # 领域模型
├── data/db/       # Room 数据库
├── network/       # HTTP Server, 设备发现, 通信
├── translation/   # 翻译引擎
├── update/        # 自动更新
└── ui/            # Compose UI
```
