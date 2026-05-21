# CLAUDE.md

## Karpathy 四原则

1. **先思考再写代码** — 每次改动前明确目标，列出影响范围，不在实现中途改设计
2. **简单至上** — 用最直接的方式解决问题，不过度抽象，不引入不需要的库
3. **外科手术式修改** — 每次只改一个模块，最小化 diff，不让重构混入功能变更
4. **目标驱动执行** — 以最终功能验收为准，能跑通 > 完美设计

## 项目概述

拍词学单词 — Kotlin + Jetpack Compose 安卓应用。拍照 PaddleOCR(PP-OCRv5/ncnn) → DeepSeek 翻译 → TTS 朗读 → 生词本 → 间隔复习。

## OCR 引擎

使用 PaddleOCR PP-OCRv5 移动端模型，通过 ncnn 推理框架运行，封装库：[equationl/paddleocr4android](https://github.com/equationl/paddleocr4android) (JitPack)。
模型文件约 10MB，放在 `app/src/main/assets/` 下，首次运行时从 assets 加载。

## 构建

```bash
./gradlew assembleDebug    # 编译 Debug APK
./gradlew lint             # 代码检查
```

CI 在 `.github/workflows/build-apk.yml`，每次 push 自动编译 APK 并上传 artifact。
