package com.vibo.vibolearning.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private val KNOWN_BLOCK_TYPES =
    setOf("text", "code", "quiz", "practice", "heading", "callout", "image", "quote")

/**
 * Decodes a [Block] leniently. A known `type` is parsed strictly; an unknown
 * `type` falls back to its `fallback` block (one level) or becomes
 * [Block.Unsupported] — an unfamiliar block never fails the whole import.
 * Keeps the "the JSON outlives the renderer" guarantee.
 */
object BlockSerializer : KSerializer<Block> {
    private val delegate = Block.serializer()
    override val descriptor: SerialDescriptor = delegate.descriptor

    override fun serialize(encoder: Encoder, value: Block) =
        encoder.encodeSerializableValue(delegate, value)

    override fun deserialize(decoder: Decoder): Block {
        val input = decoder as? JsonDecoder ?: return decoder.decodeSerializableValue(delegate)
        return fromElement(input, input.decodeJsonElement())
    }

    private fun fromElement(input: JsonDecoder, element: JsonElement): Block {
        val obj = element as? JsonObject ?: return Block.Unsupported("?")
        val type = obj["type"]?.jsonPrimitive?.contentOrNull
        if (type in KNOWN_BLOCK_TYPES) {
            return runCatching { input.json.decodeFromJsonElement(delegate, obj) }
                .getOrElse { Block.Unsupported(type ?: "?") }
        }
        val fallback = obj["fallback"]
        return if (fallback is JsonObject) fromElement(input, fallback)
        else Block.Unsupported(type ?: "?")
    }
}

object BlockListSerializer : KSerializer<List<Block>> {
    private val delegate = ListSerializer(BlockSerializer)
    override val descriptor: SerialDescriptor = delegate.descriptor
    override fun serialize(encoder: Encoder, value: List<Block>) = delegate.serialize(encoder, value)
    override fun deserialize(decoder: Decoder): List<Block> = delegate.deserialize(decoder)
}
