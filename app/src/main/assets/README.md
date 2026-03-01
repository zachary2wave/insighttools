# Assets 结构说明

请将你的素材放在 `app/src/main/assets/emotions` 目录下，结构如下：

```
emotions/
  <emotion_category>/               # 第一层：表情包种类（建议总共 14 类）
    <meme_name>/                    # 第二层：该种类下的某个具体表情
      emotion.gif                   # 第三层：素材媒体（也支持 png/jpg/webp/mp4/mov/webm）
      dialogue.csv                  # 第三层：台词 CSV（可为空）
```

`dialogue.csv` 每行一条台词，例如：

```csv
今天状态很好！
你到底在说什么？
我真的太无语了……
```

> 如果某个表情没有台词，可以让 `dialogue.csv` 为空文件，或不提供该文件。
> 同一个 `meme_name` 目录里可以放多个媒体文件，应用会随机抽取其中一个并按类型自动播放/显示。
