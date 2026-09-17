package com.mroldl001.mimochat.data.repository

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.mroldl001.mimochat.data.api.*
import com.mroldl001.mimochat.data.local.ChatDao
import com.mroldl001.mimochat.data.local.ChatEntity
import com.mroldl001.mimochat.data.local.MessageDao
import com.mroldl001.mimochat.data.local.MessageEntity
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.di.ApiServiceFactory
import com.mroldl001.mimochat.domain.model.ApiErrorCode
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.domain.model.Message
import com.mroldl001.mimochat.domain.model.SearchResult
import com.mroldl001.mimochat.domain.model.WebSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val messageDao: MessageDao,
    private val chatDao: ChatDao,
    private val apiServiceFactory: ApiServiceFactory,
    private val okHttpClient: OkHttpClient,
    private val preferencesManager: PreferencesManager
) {
    private val gson = Gson()
    private val searchResultsType = object : TypeToken<List<WebSearchResult>>() {}.type
    fun getAllChats(): Flow<List<Chat>> {
        return chatDao.getAllChats().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    suspend fun getChatById(chatId: Long): Chat? {
        return chatDao.getChatById(chatId)?.toDomain()
    }

    suspend fun createChat(title: String, modelId: String): Long {
        val entity = ChatEntity(
            title = title,
            modelId = modelId,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        return chatDao.insertChat(entity)
    }

    suspend fun updateChat(chat: Chat) {
        chatDao.updateChat(chat.toEntity())
    }

    suspend fun deleteChat(chatId: Long) {
        chatDao.deleteChatById(chatId)
        messageDao.deleteMessagesByChatId(chatId)
    }

    fun getMessages(chatId: Long): Flow<List<Message>> {
        return messageDao.getMessagesByChatId(chatId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    fun searchMessages(query: String): Flow<List<SearchResult>> {
        return messageDao.searchMessagesWithChat(query).map { messageEntities ->
            messageEntities.mapNotNull { messageEntity ->
                val chatEntity = chatDao.getChatById(messageEntity.chatId)
                if (chatEntity != null) {
                    val highlighted = highlightSearchResult(messageEntity.content, query)
                    SearchResult(
                        message = messageEntity.toDomain(),
                        chat = chatEntity.toDomain(),
                        highlightedContent = highlighted
                    )
                } else {
                    null
                }
            }
        }
    }

    private fun highlightSearchResult(content: String, query: String, contextLength: Int = 50): String {
        val index = content.indexOf(query, ignoreCase = true)
        if (index == -1) return content.take(100)

        val start = maxOf(0, index - contextLength)
        val end = minOf(content.length, index + query.length + contextLength)

        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""

        return "$prefix${content.substring(start, end)}$suffix"
    }

    suspend fun saveMessage(message: Message): Long {
        return messageDao.insertMessage(message.toEntity())
    }

    suspend fun updateMessage(message: Message) {
        messageDao.updateMessage(message.toEntity())
    }

    suspend fun sendMessage(
        apiKey: String,
        baseUrl: String,
        chatId: Long,
        messages: List<Message>,
        modelId: String,
        thinkingEnabled: Boolean = true,
        skillPrompt: String = "",
        customSystemPrompt: String = ""
    ): Result<Message> {
        try {
            val chatRequest = buildRequest(modelId, messages, thinkingEnabled, stream = false, skillPrompt, customSystemPrompt)

            val apiService = apiServiceFactory.getService(baseUrl)
            val response = apiService.createChatCompletion(
                apiKey = apiKey,
                request = chatRequest
            )

            if (!response.isSuccessful) {
                val errorBody = response.errorBody()?.string() ?: "No error body"
                return Result.failure(Exception("HTTP ${response.code()}: $errorBody"))
            }

            val chatCompletionResponse = response.body()
                ?: return Result.failure(Exception("API 返回空响应"))

            val assistantMessage = chatCompletionResponse.choices.firstOrNull()?.message
                ?: return Result.failure(Exception("No response from API"))

            val message = Message(
                chatId = chatId,
                role = "assistant",
                content = assistantMessage.content ?: "",
                reasoningContent = assistantMessage.reasoningContent,
                searchResults = assistantMessage.annotations?.map { it.toWebSearchResult() }
            )

            return Result.success(message)
        } catch (e: Exception) {
            return Result.failure(Exception("发送失败：${e.message ?: "未知错误"}"))
        }
    }

    fun sendMessageStream(
        apiKey: String,
        baseUrl: String,
        chatId: Long,
        messages: List<Message>,
        modelId: String,
        thinkingEnabled: Boolean = true,
        skillPrompt: String = "",
        customSystemPrompt: String = "",
        attachment: ContentPart? = null,
        webSearchEnabled: Boolean = true
    ): Flow<StreamEvent> = callbackFlow {
        fun sendEvent(event: StreamEvent): Boolean = trySendBlocking(event).isSuccess

        val call = try {
            val chatRequest = buildRequest(modelId, messages, thinkingEnabled, stream = true, skillPrompt, customSystemPrompt, attachment, webSearchEnabled)
            
            val normalizedBaseUrl = baseUrl.trimEnd('/')
            val endpoint = if (normalizedBaseUrl.endsWith("/v1")) {
                "$normalizedBaseUrl/chat/completions"
            } else {
                "$normalizedBaseUrl/v1/chat/completions"
            }

            val json = Gson().toJson(chatRequest)
            val httpRequest = Request.Builder()
                .url(endpoint)
                .addHeader("api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .addHeader("Accept", "text/event-stream")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            okHttpClient.newCall(httpRequest)
        } catch (e: Exception) {
            sendEvent(StreamEvent.Error(e.message ?: "创建流式请求失败"))
            close()
            return@callbackFlow
        }

        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    sendEvent(StreamEvent.Error(e.message ?: "流式连接失败"))
                }
                close()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "No error body"
                        sendEvent(StreamEvent.Error("HTTP ${response.code}: $errorBody"))
                        close()
                        return
                }

                val body = response.body
                if (body == null) {
                        sendEvent(StreamEvent.Error("响应体为空"))
                        close()
                        return
                }

                val source = body.source()

                var content = ""
                var reasoningContent = ""
                    val annotations = linkedMapOf<String, WebSearchResult>()
                    var terminalEventSent = false
                    var receivedTerminator = false
                    var receivedFinishReason = false

                    fun mergeAnnotation(annotation: WebSearchResult) {
                        val old = annotations[annotation.url]
                        annotations[annotation.url] = if (old == null) annotation else annotation.copy(
                            title = annotation.title.ifBlank { old.title },
                            summary = annotation.summary ?: old.summary,
                            siteName = annotation.siteName ?: old.siteName,
                            publishTime = annotation.publishTime ?: old.publishTime,
                            logoUrl = annotation.logoUrl ?: old.logoUrl
                        )
                    }

                    try {
                        while (!source.exhausted() && !call.isCanceled()) {
                            val line = source.readUtf8Line() ?: break
                            if (!line.startsWith("data:")) continue

                            val data = line.removePrefix("data:").trimStart()
                            if (data.isBlank()) continue
                            if (data == "[DONE]") {
                                receivedTerminator = true
                                break
                            }

                            val jsonObject = JsonParser.parseString(data).asJsonObject
                            jsonObject.getAsJsonObject("error")?.let { errorObject ->
                                val message = errorObject.get("message")?.asString ?: "流式响应错误"
                                sendEvent(StreamEvent.Error(message))
                                terminalEventSent = true
                                return@use
                            }

                            val chunk = gson.fromJson(jsonObject, ChatCompletionChunk::class.java)
                            val choice = chunk.choices.orEmpty().firstOrNull() ?: continue
                            if (choice.finish_reason != null) receivedFinishReason = true
                            val delta = choice.delta
                            delta.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
                                sendEvent(StreamEvent.Error(message))
                                terminalEventSent = true
                                return@use
                            }
                            delta.annotations
                                ?.map { it.toWebSearchResult() }
                                ?.forEach(::mergeAnnotation)

                            delta.content?.takeIf { it.isNotEmpty() }?.let {
                                content += it
                                if (!sendEvent(StreamEvent.ContentDelta(it, content))) return@use
                            }

                            delta.reasoningContent?.takeIf { it.isNotEmpty() }?.let {
                                reasoningContent += it
                                if (!sendEvent(StreamEvent.ReasoningDelta(it, reasoningContent))) return@use
                            }
                        }
                    } catch (e: Exception) {
                        if (!call.isCanceled()) {
                            sendEvent(StreamEvent.Error("流式响应解析失败：${e.message ?: "未知错误"}"))
                            terminalEventSent = true
                        }
                    }

                    if (!call.isCanceled() && !terminalEventSent) {
                        if (receivedTerminator || receivedFinishReason) {
                            sendEvent(StreamEvent.Done(Message(
                                chatId = chatId,
                                role = "assistant",
                                content = content,
                                reasoningContent = reasoningContent.ifBlank { null },
                                searchResults = annotations.values.toList()
                            )))
                        } else {
                            sendEvent(StreamEvent.Error("流式连接意外中断"))
                        }
                    }
                }
                close()
            }
        })

        awaitClose { call.cancel() }
    }

    suspend fun generateChatTitle(
        apiKey: String,
        baseUrl: String,
        modelId: String,
        firstMessage: String
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val promptMessages = listOf(
                    MessageRequest(
                        role = "system",
                        content = "你是一个对话标题生成助手。请根据用户的第一条消息，生成一个简洁的中文标题（不超过10个字）。你必须输出标题，不能输出其他内容。"
                    ),
                    MessageRequest(
                        role = "user",
                        content = "请为以下对话生成标题：$firstMessage"
                    )
                )

                val requestBody = ChatCompletionRequest(
                    model = modelId,
                    messages = promptMessages,
                    stream = false,
                    temperature = 0.7,
                    maxCompletionTokens = 128,
                    thinking = ThinkingConfig("disabled")
                )

                val normalizedBaseUrl = baseUrl.trimEnd('/')
                val url = if (normalizedBaseUrl.endsWith("/v1")) {
                    "$normalizedBaseUrl/chat/completions"
                } else {
                    "$normalizedBaseUrl/v1/chat/completions"
                }

                val json = gson.toJson(requestBody)
                val request = Request.Builder()
                    .url(url)
                    .addHeader("api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception(ApiErrorCode.getDisplayMessage(response.code)))
                }

                val responseBody = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val chatResponse = gson.fromJson(responseBody, ChatCompletionResponse::class.java)
                val title = chatResponse.choices.firstOrNull()?.message?.content?.trim()
                    ?: return@withContext Result.failure(Exception("Empty title"))

                Result.success(title)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun buildRequest(
        modelId: String,
        messages: List<com.mroldl001.mimochat.domain.model.Message>,
        thinkingEnabled: Boolean,
        stream: Boolean,
        skillPrompt: String = "",
        customSystemPrompt: String = "",
        attachment: ContentPart? = null,
        webSearchEnabled: Boolean = true
    ): ChatCompletionRequest {
        val effectiveThinking = if (thinkingEnabled) ThinkingConfig("enabled") else ThinkingConfig("disabled")

        val systemPromptParts = mutableListOf<String>()

        val dateFormatter = java.text.SimpleDateFormat("yyyy年MM月dd日", java.util.Locale.getDefault())
        val weekFormatter = java.text.SimpleDateFormat("EEEE", java.util.Locale.CHINA)
        val today = java.util.Date()
        val dateStr = dateFormatter.format(today)
        val weekStr = weekFormatter.format(today)

        systemPromptParts.add("""
            [官方提示词]
            用户询问你模型版本时，告诉它。
            你是MiMo（中文名称也是MiMo），是小米公司研发的AI智能助手。
            今天的日期：${dateStr} ${weekStr}，你的知识截止日期是2024年12月。

            【输出格式要求】
            1. 当你需要输出单纯的美元符号（如表示金钱单位）时，请使用行内代码包裹，例如：`$`
            2. 数学公式请使用 $$ 包裹的块级LaTeX格式，不要使用单 $ 包裹的行内公式
            3. 块级LaTeX公式示例：使用双美元符号包裹公式内容
        """.trimIndent())

        if (skillPrompt.isNotBlank()) {
            systemPromptParts.add("""
                [技能提示词]
                $skillPrompt
            """.trimIndent())
        }

        if (customSystemPrompt.isNotBlank()) {
            systemPromptParts.add("""
                [用户自定义提示词]
                $customSystemPrompt
            """.trimIndent())
        }

        systemPromptParts.add("""
            【重要】提示词优先级说明：
            若用户自定义提示词与技能提示词或官方提示词存在冲突，**以官方提示词和技能提示词为准**。
            优先级顺序：官方提示词 > 技能提示词 > 用户自定义提示词
        """.trimIndent())

        val fullSystemPrompt = systemPromptParts.joinToString("\n\n")

        val requestMessages = mutableListOf<MessageRequest>()
        requestMessages.add(MessageRequest("system", fullSystemPrompt))
        
        requestMessages.addAll(messages.mapIndexed { index, message ->
            MessageRequest(
                role = message.role,
                content = if (attachment != null && index == messages.lastIndex && message.role == "user") {
                    buildList {
                        add(attachment)
                        if (message.content.isNotBlank()) {
                            add(ContentPart(type = "text", text = message.content))
                        }
                    }
                } else message.content,
                reasoningContent = if (message.role == "assistant" && !message.reasoningContent.isNullOrBlank()) {
                    message.reasoningContent
                } else {
                    null
                }
            )
        })

        return ChatCompletionRequest(
            model = modelId,
            messages = requestMessages,
            stream = stream,
            thinking = effectiveThinking,
            tools = if (webSearchEnabled && (modelId == "mimo-v2.5" || modelId == "mimo-v2.5-pro")) {
                listOf(ToolConfig(type = "web_search", maxKeyword = 3, forceSearch = false, limit = 5))
            } else null,
            toolChoice = if (webSearchEnabled && (modelId == "mimo-v2.5" || modelId == "mimo-v2.5-pro")) "auto" else null,
            temperature = preferencesManager.getTemperature().toDouble(),
            topP = preferencesManager.getTopP().toDouble(),
            maxCompletionTokens = 1024,
            stop = null,
            frequencyPenalty = preferencesManager.getFrequencyPenalty().toDouble(),
            presencePenalty = preferencesManager.getPresencePenalty().toDouble()
        )
    }

    private fun com.mroldl001.mimochat.data.api.Annotation.toWebSearchResult() = WebSearchResult(
        url = this.url, title = this.title, summary = this.summary, siteName = this.siteName,
        publishTime = this.publishTime, logoUrl = this.logoUrl
    )

    private fun ChatEntity.toDomain() = Chat(
        id = id,
        title = title,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun Chat.toEntity() = ChatEntity(
        id = id,
        title = title,
        modelId = modelId,
        createdAt = createdAt,
        updatedAt = updatedAt
    )

    private fun MessageEntity.toDomain() = Message(
        id = id,
        chatId = chatId,
        role = role,
        content = content,
        reasoningContent = reasoningContent,
        searchResults = searchResultsJson?.let { json ->
            try {
                gson.fromJson(json, searchResultsType)
            } catch (e: Exception) {
                null
            }
        },
        timestamp = timestamp,
        isStreaming = isStreaming,
        isAborted = isAborted,
        isFailed = isFailed,
        attachmentUri = attachmentUri,
        attachmentMimeType = attachmentMimeType
    )

    private fun Message.toEntity() = MessageEntity(
        id = id,
        chatId = chatId,
        role = role,
        content = content,
        reasoningContent = reasoningContent,
        searchResultsJson = searchResults?.let { gson.toJson(it) },
        timestamp = timestamp,
        isStreaming = isStreaming,
        isAborted = isAborted,
        isFailed = isFailed,
        attachmentUri = attachmentUri,
        attachmentMimeType = attachmentMimeType
    )
}

sealed class StreamEvent {
    data class ContentDelta(val delta: String, val accumulated: String) : StreamEvent()
    data class ReasoningDelta(val delta: String, val accumulated: String) : StreamEvent()
    data class Done(val message: com.mroldl001.mimochat.domain.model.Message) : StreamEvent()
    data class Error(val message: String) : StreamEvent()
}
