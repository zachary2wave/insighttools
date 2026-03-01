# Assets 结构说明

请将你的素材放在 `app/src/main/assets/emotions` 目录下，结构如下：

```
emotions/
  official_cn14/                    # 第一层：固定数据集分类（示例）
    <meme_name>/                    # 第二层：14种情绪之一
      emotion.gif                   # 第三层：媒体文件（必须有）
      emotion.jpg                   # 第三层：媒体文件（必须有）
      emotion.mp4                   # 第三层：媒体文件（必须有）
      dialogue.csv                  # 第三层：台词 CSV（可为空）
```

`dialogue.csv` 每行一条台词，例如：

```csv
今天状态很好！
你到底在说什么？
我真的太无语了……
```

> 如果某个表情没有台词，可以让 `dialogue.csv` 为空文件，或不提供该文件。
> 应用会在 `gif/jpg/mp4` 等媒体里随机选一个并按类型自动播放/显示。
