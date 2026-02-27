package sentence_embedding

import org.json.JSONObject
import java.nio.charset.StandardCharsets

class HFTokenizer(
    tokenizerBytes: ByteArray,
    private val maxLength: Int,
) {
    data class Result(
        val ids: LongArray = longArrayOf(),
        val attentionMask: LongArray = longArrayOf(),
        val tokenTypeIds: LongArray = longArrayOf(),
    )

    private val vocab: Map<String, Int>
    private val unkId: Int
    private val clsId: Int?
    private val sepId: Int?

    init {
        val jsonString = tokenizerBytes.toString(StandardCharsets.UTF_8)
        val root = JSONObject(jsonString)
        val model = root.getJSONObject("model")
        val vocabObject = model.getJSONObject("vocab")
        val keys = vocabObject.keys()
        val map = mutableMapOf<String, Int>()
        while (keys.hasNext()) {
            val k = keys.next()
            map[k] = vocabObject.getInt(k)
        }
        vocab = map

        val unkTokenId =
            when {
                !model.has("unk_token") || model.isNull("unk_token") -> {
                    vocab["[UNK]"] ?: 0
                }

                else -> {
                    val value = model.get("unk_token")
                    when (value) {
                        is JSONObject -> value.optInt("id", 0)
                        is String -> vocab[value] ?: 0
                        else -> vocab["[UNK]"] ?: 0
                    }
                }
            }
        unkId = unkTokenId

        clsId =
            when {
                vocab.containsKey("[CLS]") -> vocab["[CLS]"]
                vocab.containsKey("<s>") -> vocab["<s>"]
                else -> null
            }
        sepId =
            when {
                vocab.containsKey("[SEP]") -> vocab["[SEP]"]
                vocab.containsKey("</s>") -> vocab["</s>"]
                else -> null
            }
    }

    fun tokenize(text: String): Result {
        val rawTokens =
            text
                .lowercase()
                .split("\\s+".toRegex())
                .filter { it.isNotEmpty() }

        val idsList = mutableListOf<Long>()

        clsId?.let { idsList.add(it.toLong()) }

        for (token in rawTokens) {
            val id =
                vocab[token]
                    ?: vocab[token.replace("##", "")]
                    ?: unkId
            idsList.add(id.toLong())
        }

        sepId?.let { idsList.add(it.toLong()) }

        val truncated =
            if (idsList.size > maxLength) {
                idsList.subList(0, maxLength)
            } else {
                idsList
            }

        val padLength = maxLength - truncated.size
        val ids =
            if (padLength > 0) {
                val padded = LongArray(maxLength)
                for (i in truncated.indices) {
                    padded[i] = truncated[i]
                }
                padded
            } else {
                truncated.toLongArray()
            }

        val attentionMask = LongArray(maxLength) { index -> if (index < truncated.size) 1L else 0L }
        val tokenTypeIds = LongArray(maxLength) { 0L }

        return Result(ids, attentionMask, tokenTypeIds)
    }

    fun close() {
    }
}