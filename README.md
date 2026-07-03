# BetterReefLightManager

## 项目说明

这是一个重建中的 Android 灯具控制项目，当前主线工程为：

- Android 原生
- Kotlin
- Jetpack Compose
- 单 `Activity` + Navigation

这个目录同时保留了旧 APK 的反编译材料，但日常开发应只在新工程中进行。

## 目录结构

- `app/`
  新 Android 主工程
- `docs/`
  当前状态、协议、接手文档
- `legacy/`
  旧项目反编译材料，仅用于对照逻辑
- `gradle/`、`gradlew`、`gradlew.bat`
  Gradle Wrapper

## 当前重点能力

### 网络

- `AP 模式`
  - 扫描并连接 `K7_Pro` 设备热点
  - 固定密码 `12345678`
  - 自动带入网关 IP
- `局域网模式`
  - 读取当前 Wi-Fi、本机 IP、网关 IP
  - 扫描同网段设备
  - 一键带入设备 IP 并跳转控制页

### 控制

- TCP `8266` 端口通信
- 自动读取设备配置
- 设备名称解析
- 自动 / 手动模式切换
- 演示模式开关
- 手动亮度发送
- 全量配置发送

### 曲线

- 24 点时间曲线编辑
- 预置方案 / 自定义方案管理
- 二维码方案导入
- 本地持久化保存
- 日出日落辅助时段
- 曲线平滑与跳变检查

## 当前页面

- 首页
- 灯具网络配置
- 灯具控制调试
- Airkiss 配网
- Esptouch 配网

## 已确认的业务约定

- `SPS / LPS / SL` 是预设时间-亮度方案，不是运行模式
- 自动 / 手动 才是运行模式
- 演示模式是独立特殊状态
- `1008` 为读取设备配置
- `1007` 为发送全量配置
- `1004` 为自动 / 手动模式切换

## 开发入口

主入口文件：

- [MainActivity.kt](/D:/Dev/CodeX/NP/app/src/main/java/com/mualanimarine/betterreeflightmanager/MainActivity.kt)

关键底层文件：

- [ConnectThread.kt](/D:/Dev/CodeX/NP/app/src/main/java/com/mualanimarine/betterreeflightmanager/ConnectThread.kt)
- [CommandUtil.kt](/D:/Dev/CodeX/NP/app/src/main/java/com/mualanimarine/betterreeflightmanager/util/CommandUtil.kt)
- [SharedPreferencesUtil.kt](/D:/Dev/CodeX/NP/app/src/main/java/com/mualanimarine/betterreeflightmanager/util/SharedPreferencesUtil.kt)
- [LightingProfiles.kt](/D:/Dev/CodeX/NP/app/src/main/java/com/mualanimarine/betterreeflightmanager/device/LightingProfiles.kt)

## 运行方式

1. 用 Android Studio 打开
2. 等 Gradle 同步完成
3. 运行 `app` 模块
4. 真机联调时优先使用 Android 设备

## 当前状态

当前可稳定使用的是：

- `AP` 直连控制
- `局域网` 发现与控制
- 设备配置读取与基本写回

当前仍在继续调试的是：

- `Esptouch` 配网兼容性
- `Airkiss` 兼容验证

## 相关文档

- [当前状态](docs/REBUILD_STATUS.md)
- [协议说明](docs/PROTOCOL_SPEC.md)
- [接手梳理](docs/TAKEOVER_AUDIT.md)

## 开发建议

- 日常只改 `app/`
- 对照旧逻辑时再查 `legacy/`
- 改协议前先看 `docs/PROTOCOL_SPEC.md`
- 设备联调时保留日志，优先记录 `1007 / 1008 / 1004 / 100A`
