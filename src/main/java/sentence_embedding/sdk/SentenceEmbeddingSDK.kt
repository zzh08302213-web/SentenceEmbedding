package sentence_embedding.sdk

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sentence_embedding.SentenceEmbedding
import java.io.File
import java.io.FileOutputStream

interface EmbeddingCallback<T> {
    fun onResult(result: T)
    fun onError(e: EmbeddingException)
}

data class EmbeddingConfig(
    val modelName: String,
    val modelAssetPath: String,
    val tokenizerAssetPath: String,
    val useTokenTypeIds: Boolean = false,
)

open class EmbeddingException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause)

class NotInitializedException :
    EmbeddingException("sentence_embedding.sdk.EmbeddingSdk is not initialized")

class ModelLoadException(
    cause: Throwable,
) : EmbeddingException("Failed to initialize embedding model", cause)

class InferenceException(
    cause: Throwable,
) : EmbeddingException("Failed to encode sentence", cause)

object EmbeddingSdk {
    private val lock = Any()
    private var sentenceEmbedding: SentenceEmbedding? = null
    private var currentConfig: EmbeddingConfig? = null

    // 创建一个独立的协程作用域来管理SDK的后台任务
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @JvmStatic
    fun isInitialized(): Boolean = sentenceEmbedding != null

    @JvmStatic
    @JvmOverloads
    fun init(
        context: Context,
        modelAssetPath: String,
        tokenizerAssetPath: String,
        useTokenTypeIds: Boolean = false,
        listener: EmbeddingCallback<Unit>,
    ) {
        val modelName =
            modelAssetPath
                .substringAfterLast('/')
                .substringBeforeLast('.')
        val config =
            EmbeddingConfig(
                modelName = modelName,
                modelAssetPath = modelAssetPath,
                tokenizerAssetPath = tokenizerAssetPath,
                useTokenTypeIds = useTokenTypeIds,
            )

        scope.launch {
            if (sentenceEmbedding != null) {
                withContext(Dispatchers.Main) { listener.onResult(Unit) }
                return@launch
            }
            try {
                val modelPath = copyAssetToFile(context, config.modelAssetPath)
                val tokenizerBytes = readAssetBytes(context, config.tokenizerAssetPath)
                val impl = SentenceEmbedding()
                impl.init(
                    modelFilepath = modelPath,
                    tokenizerBytes = tokenizerBytes,
                    useTokenTypeIds = config.useTokenTypeIds,
                    outputTensorName = "last_hidden_state",
                    normalizeEmbeddings = false,
                    maxSeqLengthOverride = null,
                )
                synchronized(lock) {
                    sentenceEmbedding = impl
                    currentConfig = config
                }
                withContext(Dispatchers.Main) { listener.onResult(Unit) }
            } catch (e: Exception) {
                synchronized(lock) {
                    sentenceEmbedding = null
                    currentConfig = null
                }
                withContext(Dispatchers.Main) { listener.onError(ModelLoadException(e)) }
            }
        }
    }

    @JvmStatic
    fun shutdown() {
        synchronized(lock) {
            sentenceEmbedding?.close()
            sentenceEmbedding = null
            currentConfig = null
        }
        // 取消所有正在运行的后台任务
        scope.cancel()
    }

    @JvmStatic
    fun clearCache(context: Context) {
        val config = synchronized(lock) { currentConfig }
        if (config != null) {
            val modelFile = File(context.filesDir, config.modelAssetPath)
            if (modelFile.exists()) {
                modelFile.delete()
            }
        }
    }

    @JvmStatic
    fun encode(text: String, listener: EmbeddingCallback<FloatArray>) {
        scope.launch {
            val impl = synchronized(lock) { sentenceEmbedding }
            if (impl == null) {
                withContext(Dispatchers.Main) { listener.onError(NotInitializedException()) }
                return@launch
            }

            try {
                val result = impl.encode(text)
                withContext(Dispatchers.Main) { listener.onResult(result) }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { listener.onError(InferenceException(e)) }
            }
        }
    }


    private fun copyAssetToFile(
        context: Context,
        assetPath: String,
    ): String {
        val outFile = File(context.filesDir, assetPath)
        if (!outFile.exists()) {
            outFile.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return outFile.absolutePath
    }

    private fun readAssetBytes(
        context: Context,
        assetPath: String,
    ): ByteArray {
        return context.assets.open(assetPath).use { it.readBytes() }
    }
}
