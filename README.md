# Sentence Embeddings Android

基于 ONNX Runtime 的 Android 句向量生成库，提供简单的 SDK 封装，支持在本地加载模型与分词器并生成句向量。

## 功能特性

- 基于 ONNX Runtime 运行模型
- 支持从 assets 读取模型和分词器
- 提供简单的 SDK 初始化与编码接口

## 要求

- minSdk 24
- Java/Kotlin 目标版本 17

## 使用方式

### 1. 准备模型与分词器

将 ONNX 模型文件与分词器文件放入应用的 `assets` 目录，例如：

- `assets/models/all-MiniLM-L6-V2.onnx`
- `assets/tokenizers/all-MiniLM-L6-V2/tokenizer.json`

### 2. 初始化 SDK

```kotlin
import sentence_embedding.sdk.EmbeddingCallback
import sentence_embedding.sdk.EmbeddingSdk

EmbeddingSdk.init(
    context = applicationContext,
    modelAssetPath = "models/all-MiniLM-L6-V2.onnx",
    tokenizerAssetPath = "tokenizers/all-MiniLM-L6-V2/tokenizer.json",
    useTokenTypeIds = false,
    listener =
        object : EmbeddingCallback<Unit> {
            override fun onResult(result: Unit) {
            }

            override fun onError(e: Exception) {
            }
        },
)
```

### 3. 生成句向量

```kotlin
EmbeddingSdk.encode(
    text = "Hello world",
    listener =
        object : EmbeddingCallback<FloatArray> {
            override fun onResult(result: FloatArray) {
            }

            override fun onError(e: Exception) {
            }
        },
)
```

### 4. 释放资源

```kotlin
EmbeddingSdk.shutdown()
```

## 主要 API

- 初始化与编码：EmbeddingSdk（见 [SentenceEmbeddingSDK.kt](file:///e:/Sentence-Embedding/Sentence-Embeddings-Android/sentence_embeddings/src/main/java/sentence_embedding/sdk/SentenceEmbeddingSDK.kt)）
- 推理实现：SentenceEmbedding（见 [SentenceEmbedding.kt](file:///e:/Sentence-Embedding/Sentence-Embeddings-Android/sentence_embeddings/src/main/java/sentence_embedding/SentenceEmbedding.kt)）
