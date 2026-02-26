# Emotion Data Collector (Android)

用于采集“表情 + 台词 + 用户演绎视频 + 自评分数”的 Android APK 示例工程。

## 已实现功能

1. **录制模式**
   - 从 `assets/emotions` 随机抽取表情素材（类别/子表情目录）。
   - 从对应 `dialogue.csv` 随机抽取台词；若无台词则隐藏台词区域。
   - 调起前置摄像头录制（系统相机，前摄优先）。
   - 录制后用户可预览、0-10 打分，并保存进入下一条。

2. **本地保存**
   - 视频保存到：`files/recordings/videos/*.mp4`
   - 元数据 JSON 保存到：`files/recordings/metadata/*.json`
   - JSON 字段包含：
     - `selectedemo`
     - `Dialogue`
     - `recordedvedio`
     - `score`

3. **上传模式**
   - 浏览已录制样本，可预览视频。
   - 支持单选 / 全选。
   - 上传时将 **json + video** 一起以 multipart/form-data POST 到固定接口。

## 关键配置

- 上传地址在 `app/src/main/java/com/insighttools/emotioncollector/AppConfig.kt`：
  - `UPLOAD_URL = "https://example.com/api/upload"`
  - 请改为你的真实后端地址。

## Assets 目录规范

见 `app/src/main/assets/README.md`，核心结构如下：

```text
emotions/
  <category>/
    <meme_name>/
      emotion.gif
      dialogue.csv
```

仓库里已预置 14 类目录模板（用于快速填充你自己的素材）。
