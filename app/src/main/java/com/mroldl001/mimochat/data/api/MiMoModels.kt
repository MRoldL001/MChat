package com.mroldl001.mimochat.data.api

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String,
    val messages: List<MessageRequest>,
    @SerializedName("max_completion_tokens")
    val maxCompletionTokens: Int = 1024,
    val temperature: Double = 1.0,
    @SerializedName("top_p")
    val topP: Double = 0.95,
    val stream: Boolean = false,
    val stop: Any? = null,
    @SerializedName("frequency_penalty")
    val frequencyPenalty: Double = 0.0,
    @SerializedName("presence_penalty")
    val presencePenalty: Double = 0.0,
    val thinking: ThinkingConfig? = null,
    val tools: List<ToolConfig>? = null,
    @SerializedName("tool_choice")
    val toolChoice: String? = null
)

data class ToolConfig(
    val type: String,
    val function: FunctionConfig? = null,
    @SerializedName("max_keyword")
    val maxKeyword: Int? = null,
    @SerializedName("force_search")
    val forceSearch: Boolean? = null,
    val limit: Int? = null,
    @SerializedName("user_location")
    val userLocation: UserLocation? = null
)

data class FunctionConfig(
    val name: String
)

data class UserLocation(
    val type: String,
    val country: String? = null,
    val region: String? = null,
    val city: String? = null
)

data class MessageRequest(
    val role: String,
    val content: Any,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null
)

data class ContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url") val imageUrl: ImageUrl? = null,
    @SerializedName("input_audio") val inputAudio: InputAudio? = null,
    @SerializedName("video_url") val videoUrl: VideoUrl? = null,
    val fps: Double? = null,
    @SerializedName("media_resolution") val mediaResolution: String? = null
)

data class ImageUrl(val url: String)
data class InputAudio(val data: String)
data class VideoUrl(val url: String)

data class ThinkingConfig(
    val type: String = "enabled"
)

// Non-streaming response
data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String,
    val usage: Usage?
)

data class Choice(
    val finish_reason: String?,
    val message: AssistantMessage
)

data class AssistantMessage(
    val content: String?,
    @SerializedName("reasoning_content")
    val reasoningContent: String?,
    val annotations: List<Annotation>? = null
)

data class Annotation(
    val type: String,
    val url: String,
    val title: String,
    val summary: String? = null,
    @SerializedName("site_name")
    val siteName: String? = null,
    @SerializedName("publish_time")
    val publishTime: String? = null,
    @SerializedName("logo_url")
    val logoUrl: String? = null
)

// Streaming response chunk
data class ChatCompletionChunk(
    val id: String,
    val choices: List<ChunkChoice>,
    val created: Long,
    val model: String,
    val `object`: String
)

data class ChunkChoice(
    val index: Int,
    val delta: DeltaMessage,
    val finish_reason: String?
)

data class DeltaMessage(
    val role: String? = null,
    val content: String? = null,
    @SerializedName("reasoning_content")
    val reasoningContent: String? = null,
    val annotations: List<Annotation>? = null
)

data class Usage(
    @SerializedName("completion_tokens")
    val completionTokens: Int,
    @SerializedName("prompt_tokens")
    val promptTokens: Int,
    @SerializedName("total_tokens")
    val totalTokens: Int
)
