# Emotion Data Collector (Android)

用于采集“表情 + 台词 + 用户演绎视频 + 自评分数”的 Android APK 示例工程。

## 已实现功能

1. **录制模式**
   - 从 `assets/emotions` 随机抽取表情素材（类别/子表情目录）。
   - 每次会在该目录中随机选择一个媒体文件（支持 `gif/jpg/png/webp/mp4/mov/webm`）。
   - 从对应 `dialogue.csv` 随机抽取台词；若无台词则隐藏台词区域。
   - 使用 CameraX 内嵌前置摄像头实时预览与录制（前摄优先，失败回退后摄）。
   - 录制后用户可预览、0-10 打分，并保存进入下一条。

2. **本地保存**
   - 视频保存到：`files/recordings/videos/*.mp4`
   - 元数据 JSON 保存到：`files/recordings/metadata/*.json`
   - JSON 字段包含：
     - `selectedemo`
     - `Dialogue`
     - `recordedvedio`
     - `score`

3. **分享模式**
   - 浏览已录制样本，可预览视频。
   - 支持单选 / 全选。
   - 点击“打包并分享”后，将所选记录的 **json + video** 一并打包为 zip。
   - 打包完成后拉起系统分享面板，可转发到微信或其他应用。

## Assets 目录规范

见 `app/src/main/assets/README.md`，核心结构如下：

```text
emotions/
  official_cn14/
    <meme_name(14种情绪之一)>/
      emotion.gif
      emotion.jpg
      emotion.mp4
      dialogue.csv
```

仓库里已预置你指定的 14 种情绪 `meme_name` 模板（用于快速填充你自己的素材）。

14 种情绪如下：
1. 感冒（打喷嚏，咳嗽，流鼻涕）
2. 困倦
3. 疲劳
4. 专注
5. 走神/发呆
6. 挫折/沮丧
7. 厌倦
8. 急切/着急
9. 忧虑/焦虑/压力大
10. 困惑/疑惑
11. 紧张
12. 激动
13. 感兴趣/好奇
14. 不耐烦
