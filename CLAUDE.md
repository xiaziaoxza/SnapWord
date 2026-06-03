# CLAUDE.md

## Karpathy 四原则

1. **先思考再写代码** — 每次改动前明确目标，列出影响范围，不在实现中途改设计
2. **简单至上** — 用最直接的方式解决问题，不过度抽象，不引入不需要的库
3. **外科手术式修改** — 每次只改一个模块，最小化 diff，不让重构混入功能变更
4. **目标驱动执行** — 以最终功能验收为准，能跑通 > 完美设计

## 项目概述

拍词学单词 — Kotlin + Jetpack Compose 安卓应用。拍照 Google ML Kit Text Recognition v2 → 查词翻译 → TTS 朗读 → 生词本 → Ebbinghaus 间隔复习。

## 复习系统

艾宾浩斯遗忘曲线驱动。每单词有 `forgettingDays`（遗忘天数），系统日历每日递增。
复习时按遗忘概率 `1 - e^(-forgettingDays/7)` 加权抽取，遗忘越久越容易出现。
用户打字输入中文翻译，匹配任意关键词算正确。正确 → forgettingDays 归零；跳过/答错 → +1。
复习配置可调：总词数 + 各天数段配额（如 "1:5,3:5,7:5,14:3,30:2"）。

## OCR 引擎

使用 Google ML Kit Text Recognition v2 (Latin script)，模型约 5MB 打进 APK，100% 离线运行，英语单词识别准确率远超 PaddleOCR 中英混合模型。
API: `TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)` → Text.Element 级单词提取。

## 构建

```bash
./gradlew assembleDebug    # 编译 Debug APK
./gradlew lint             # 代码检查
```

## 音频

内置 ~4700 词的美式发音 OGG 文件放在 `app/src/main/assets/audio/`，APK 增量约 33MB。
`AudioManager` 优先播放资产音频，缺失时回退系统 TTS。
词形变化词（如 removed）通过 `audio/index.json` 映射到原形音频。

CI 在 `.github/workflows/build-apk.yml`，每次 push 自动编译 APK 并上传 artifact。
