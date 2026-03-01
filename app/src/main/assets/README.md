# Assets 结构说明

请将你的素材放在 `app/src/main/assets/emotions` 目录下，结构如下：

```
emotions/
  official_cn14/                    # 第一层：固定数据集分类（示例）
    <meme_name>/                    # 第二层：14种情绪之一
      emotion.gif                   # 第三层：媒体文件（必须有）
      emotion.gif.dialog.csv        # 该 gif 对应的台词文件（推荐）
      emotion.jpg                   # 第三层：媒体文件（必须有）
      emotion.jpg.dialog.csv        # 该 jpg 对应的台词文件（推荐）
      emotion.mp4                   # 第三层：媒体文件（必须有）
      emotion.mp4.dialog.csv        # 该 mp4 对应的台词文件（推荐）
      dialogue.csv                  # 目录级兜底台词 CSV（可选）
```

`dialogue.csv` 每行一条台词，例如：

```csv
今天状态很好！
你到底在说什么？
我真的太无语了……
```

> 应用会先随机选一个媒体文件，再优先读取该媒体对应的 dialog 文件。  
> 若未找到媒体级 dialog 文件，会自动回退到 `dialogue.csv` / `dialog.csv`。
