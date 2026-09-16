package com.mroldl001.mimochat.ui.chat.viewmodel

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.data.api.ContentPart
import com.mroldl001.mimochat.data.repository.ChatRepository
import com.mroldl001.mimochat.data.repository.ModelRepository
import com.mroldl001.mimochat.data.repository.StreamEvent
import com.mroldl001.mimochat.domain.model.AIModel
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.domain.model.Message
import com.mroldl001.mimochat.service.ChatService
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SkillType {
    POET,
    LEARNING
}

object SkillPrompts {
    const val POET = """# Role
古代诗人

# Profile
你是一位精通格律、文采斐然的古代诗人。你擅长捕捉万物意象，将其转化为凝练优美的古体诗。

# Goals
根据用户的输入（无论是具体的主题、描述，还是简单的问候、无意义的字符），即时创作一首契合意境或巧妙转化的古体诗。

# Constraints
1. **绝对纯净输出**：仅输出【古诗名】和【古诗正文】。
2. **零废话**：严禁输出任何开场白（如“好的”、“为您作诗”）、解释、注释、结尾语或Markdown代码块标记。
3. **无条件触发**：无论用户输入什么内容，都必须将其视为创作灵感，直接生成诗歌，绝不进行对话或回答非诗歌类问题。

# Workflow
1. 接收用户输入。
2. 提取关键词或意境。
3. 创作古体诗。
4. 检查输出格式，确保无任何多余字符。

# Initialization
准备就绪，请直接开始根据用户输入作诗。"""

    const val LEARNING = """# Role
资深全科导师

# Profile
你是一位拥有丰富教学经验的老师，擅长将复杂的知识点拆解为易于理解的小模块，并采用“循序渐进”和“苏格拉底式提问”的教学法。

# Goals
根据用户提供的学习主题或内容，将其拆分为若干个逻辑清晰的学习小节，并在当前的对话中逐一进行教学。

# Constraints & Workflow
1. **内容拆解**：在接收到用户的学习内容后，先在内心将其规划为多个循序渐进的教学步骤（不要一次性输出所有规划）。
2. **分步教学**：每次只讲解一个核心知识点或一个小节。讲解要通俗易懂，适当举例。
3. **互动考察**：每讲完一个知识点，必须立刻停下来，向用户提出1-2个相关的互动问题或小测试，以检验学习成果。
4. **进度控制**：**严禁一次性输出所有内容**。你必须等待用户回答了你的问题，并确认用户掌握后，再进行下一个知识点的学习。
5. **风格**：保持耐心、鼓励性强，像一位真正的私教一样引导。

# Initialization
现在，请询问我想要学习的内容是什么。一旦我提供内容，请立即开始拆分并进行第一步的教学。"""

    fun getSkillPrompt(skill: SkillType): String = when (skill) {
        SkillType.POET -> POET
        SkillType.LEARNING -> LEARNING
    }
}

data class StreamState(
    val content: String = "",
    val reasoning: String = "",
    val searchResults: List<com.mroldl001.mimochat.domain.model.WebSearchResult>? = null,
    val isActive: Boolean = false
)

data class ChatUiState(
    val currentChat: Chat? = null,
    val availableModels: List<AIModel> = emptyList(),
    val selectedModel: AIModel? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val chats: List<Chat> = emptyList(),
    val apiKey: String = "",
    val apiBaseUrl: String = PreferencesManager.DEFAULT_API_BASE_URL,
    val themeColor: ThemeColor = ThemeColor.WHITE,
    val themeMode: ThemeMode = ThemeMode.FOLLOW_SYSTEM,
    val customSystemPrompt: String = "",
    val activeSkill: SkillType? = null,
    val temperature: Float = PreferencesManager.DEFAULT_TEMPERATURE,
    val topP: Float = PreferencesManager.DEFAULT_TOP_P,
    val frequencyPenalty: Float = PreferencesManager.DEFAULT_FREQUENCY_PENALTY,
    val presencePenalty: Float = PreferencesManager.DEFAULT_PRESENCE_PENALTY
)

private const val DEFAULT_CHAT_TITLE = "新对话"

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val preferencesManager: PreferencesManager,
    private val application: Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ChatUiState(
            themeColor = preferencesManager.getThemeColor(),
            themeMode = preferencesManager.getThemeMode(),
            apiKey = preferencesManager.getApiKey(),
            apiBaseUrl = preferencesManager.getApiBaseUrl(),
            customSystemPrompt = preferencesManager.getCustomSystemPrompt(),
            temperature = preferencesManager.getTemperature(),
            topP = preferencesManager.getTopP(),
            frequencyPenalty = preferencesManager.getFrequencyPenalty(),
            presencePenalty = preferencesManager.getPresencePenalty()
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    val messages = mutableStateListOf<Message>()

    var streamingContent = mutableStateOf("")
        private set
    var streamingReasoning = mutableStateOf("")
        private set
    var isStreaming = mutableStateOf(false)
        private set

    private var streamJob: kotlinx.coroutines.Job? = null
    private var messagesJob: kotlinx.coroutines.Job? = null
    private var chatStreamStates: MutableMap<Long, StreamState> = mutableMapOf()
    private var activeChatId: Long? = null
    private val activeStreams = mutableSetOf<Long>()

    init {
        loadModels()
        loadChats()
    }

    private fun loadModels() {
        val models = modelRepository.getModels()
        val savedModelId = preferencesManager.getSelectedModelId()
        val selectedModel = if (savedModelId.isNotBlank()) {
            models.find { it.id == savedModelId }
        } else {
            null
        }
        _uiState.update { state ->
            state.copy(
                availableModels = models,
                selectedModel = selectedModel ?: models.firstOrNull()
            )
        }
    }

    private fun loadChats() {
        viewModelScope.launch {
            chatRepository.getAllChats().collect { chats ->
                _uiState.update { it.copy(chats = chats) }
            }
        }
    }

    fun setThemeColor(color: ThemeColor) {
        _uiState.update { it.copy(themeColor = color) }
        preferencesManager.saveThemeColor(color)
    }

    fun setThemeMode(mode: ThemeMode) {
        _uiState.update { it.copy(themeMode = mode) }
        preferencesManager.saveThemeMode(mode)
    }

    fun setApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
        preferencesManager.saveApiKey(apiKey)
    }

    fun setApiBaseUrl(url: String) {
        _uiState.update { it.copy(apiBaseUrl = url) }
        preferencesManager.saveApiBaseUrl(url)
    }

    fun setCustomSystemPrompt(prompt: String) {
        _uiState.update { it.copy(customSystemPrompt = prompt) }
        preferencesManager.saveCustomSystemPrompt(prompt)
    }

    fun setTemperature(value: Float) {
        _uiState.update { it.copy(temperature = value) }
        preferencesManager.saveTemperature(value)
    }

    fun setTopP(value: Float) {
        _uiState.update { it.copy(topP = value) }
        preferencesManager.saveTopP(value)
    }

    fun setFrequencyPenalty(value: Float) {
        _uiState.update { it.copy(frequencyPenalty = value) }
        preferencesManager.saveFrequencyPenalty(value)
    }

    fun setPresencePenalty(value: Float) {
        _uiState.update { it.copy(presencePenalty = value) }
        preferencesManager.savePresencePenalty(value)
    }

    fun resetParameters() {
        preferencesManager.resetParameters()
        _uiState.update { 
            it.copy(
                temperature = PreferencesManager.DEFAULT_TEMPERATURE,
                topP = PreferencesManager.DEFAULT_TOP_P,
                frequencyPenalty = PreferencesManager.DEFAULT_FREQUENCY_PENALTY,
                presencePenalty = PreferencesManager.DEFAULT_PRESENCE_PENALTY
            )
        }
    }

    fun setActiveSkill(skill: SkillType?) {
        _uiState.update { it.copy(activeSkill = skill) }
    }



    fun selectModel(model: AIModel) {
        _uiState.update { it.copy(selectedModel = model) }
        preferencesManager.saveSelectedModelId(model.id)
        viewModelScope.launch {
            _uiState.value.currentChat?.let { chat ->
                val updatedChat = chat.copy(modelId = model.id)
                chatRepository.updateChat(updatedChat)
                _uiState.update { it.copy(currentChat = updatedChat) }
            }
        }
    }

    fun createNewChat() {
        viewModelScope.launch {
            activeChatId?.let { currentId ->
                chatStreamStates[currentId] = StreamState(
                    content = streamingContent.value,
                    reasoning = streamingReasoning.value,
                    isActive = activeStreams.contains(currentId)
                )
            }
            
            val modelId = _uiState.value.selectedModel?.id ?: "mimo-v2.5-pro"
            val chatId = chatRepository.createChat(
                title = DEFAULT_CHAT_TITLE,
                modelId = modelId
            )
            val chat = chatRepository.getChatById(chatId)
            if (chat != null) {
                messagesJob?.cancel()
                messages.clear()
                
                streamingContent.value = ""
                streamingReasoning.value = ""
                isStreaming.value = false
                
                activeChatId = chat.id
                _uiState.update {
                    it.copy(
                        currentChat = chat,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(error = "创建对话失败")
                }
            }
        }
    }

    fun selectChat(chat: Chat) {
        activeChatId?.let { currentId ->
            chatStreamStates[currentId] = StreamState(
                content = streamingContent.value,
                reasoning = streamingReasoning.value,
                searchResults = null,
                isActive = activeStreams.contains(currentId)
            )
        }
        
        messagesJob?.cancel()
        viewModelScope.launch {
            val fullChat = chatRepository.getChatById(chat.id)
            val model = modelRepository.getModelById(chat.modelId)
            _uiState.update {
                it.copy(
                    currentChat = fullChat,
                    selectedModel = model ?: it.selectedModel
                )
            }

            val savedState = chatStreamStates[chat.id]
            val chatIsActive = activeStreams.contains(chat.id)
            if (savedState != null) {
                streamingContent.value = savedState.content
                streamingReasoning.value = savedState.reasoning
                isStreaming.value = chatIsActive
            } else {
                streamingContent.value = ""
                streamingReasoning.value = ""
                isStreaming.value = chatIsActive
            }
            
            activeChatId = chat.id

            messagesJob = viewModelScope.launch {
                chatRepository.getMessages(chat.id).collect { msgs ->
                    messages.clear()
                    messages.addAll(msgs)
                }
            }
        }
    }

    fun sendMessage(content: String, thinkingEnabled: Boolean = true, attachment: ContentPart? = null, webSearchEnabled: Boolean = true, attachmentUri: String? = null, attachmentMimeType: String? = null) {
        viewModelScope.launch {
            if (activeStreams.contains(activeChatId)) {
                return@launch
            }
            
            if (_uiState.value.currentChat == null) {
                val modelId = _uiState.value.selectedModel?.id ?: "mimo-v2.5-pro"
                val chatId = chatRepository.createChat(
                    title = DEFAULT_CHAT_TITLE,
                    modelId = modelId
                )
                val chat = chatRepository.getChatById(chatId)
                if (chat != null) {
                    _uiState.update {
                        it.copy(
                            currentChat = chat,
                            error = null
                        )
                    }
                    activeChatId = chat.id
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "创建对话失败"
                        )
                    }
                    return@launch
                }
            }

            val chat = _uiState.value.currentChat!!
            activeChatId = chat.id
            val isNewChat = chat.title == DEFAULT_CHAT_TITLE
            android.util.Log.d("ChatViewModel", "Current chat: ${chat.title}, isNewChat: $isNewChat")

            val userMessage = Message(
                chatId = chat.id,
                role = "user",
                content = content,
                attachmentUri = attachmentUri,
                attachmentMimeType = attachmentMimeType
            )
            chatRepository.saveMessage(userMessage)
            messages.add(userMessage)

            _uiState.update { it.copy(isLoading = true, error = null) }

            val apiKey = _uiState.value.apiKey
            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "请先设置 API Key"
                    )
                }
                return@launch
            }

            val modelId = if (attachment != null) "mimo-v2.5" else (_uiState.value.selectedModel?.id ?: "mimo-v2.5-pro")
            val apiBaseUrl = _uiState.value.apiBaseUrl

            if (isNewChat) {
                val chatId = chat.id
                viewModelScope.launch {
                    try {
                        val result = chatRepository.generateChatTitle(
                            apiKey = apiKey,
                            baseUrl = apiBaseUrl,
                            modelId = modelId,
                            firstMessage = content
                        )
                        val title = result.getOrNull()
                        if (title != null) {
                            val currentChatFromDb = chatRepository.getChatById(chatId)
                            if (currentChatFromDb != null && currentChatFromDb.title == DEFAULT_CHAT_TITLE) {
                                val updatedChat = currentChatFromDb.copy(title = title)
                                chatRepository.updateChat(updatedChat)
                                _uiState.update { state ->
                                    val updatedChats = state.chats.map {
                                        if (it.id == chatId) it.copy(title = title) else it
                                    }
                                    state.copy(
                                        currentChat = updatedChat,
                                        chats = updatedChats
                                    )
                                }
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
            }

            streamingContent.value = ""
            streamingReasoning.value = ""
            isStreaming.value = true
            activeStreams.add(chat.id)

            val contextMessages = messages.filter { !it.isAborted && !it.isFailed }.toList()
            val targetChatId = chat.id

            val contentBuffer = mutableListOf<String>()
            val reasoningBuffer = mutableListOf<String>()
            var streamError: String? = null
            var isStreamDone = false
            var streamSearchResults: List<com.mroldl001.mimochat.domain.model.WebSearchResult>? = null

            val activeSkill = _uiState.value.activeSkill
            val skillPrompt = activeSkill?.let { SkillPrompts.getSkillPrompt(it) } ?: ""
            val effectiveThinkingEnabled = if (activeSkill != null) false else thinkingEnabled

            val serviceIntent = Intent(application, ChatService::class.java).apply {
                action = ChatService.ACTION_START
                putExtra(ChatService.EXTRA_CHAT_ID, chat.id)
            }
            application.startForegroundService(serviceIntent)

            streamJob = viewModelScope.launch {
                chatRepository.sendMessageStream(
                    apiKey = apiKey,
                    baseUrl = apiBaseUrl,
                    chatId = chat.id,
                    messages = contextMessages,
                    modelId = modelId,
                    thinkingEnabled = effectiveThinkingEnabled,
                    skillPrompt = skillPrompt,
                    customSystemPrompt = _uiState.value.customSystemPrompt
                    , attachment = attachment, webSearchEnabled = webSearchEnabled
                ).collect { event ->
                    when (event) {
                        is StreamEvent.ContentDelta -> {
                            contentBuffer.add(event.delta)
                        }
                        is StreamEvent.ReasoningDelta -> {
                            reasoningBuffer.add(event.delta)
                        }
                        is StreamEvent.Done -> {
                            isStreamDone = true
                            streamSearchResults = event.message.searchResults
                            streamJob = null
                        }
                        is StreamEvent.Error -> {
                            isStreamDone = true
                            streamError = event.message
                            streamJob = null
                        }
                    }
                }
            }

            viewModelScope.launch {
                var reasoningPhase = true
                var currentContent = ""
                var currentReasoning = ""
                var lastNotificationUpdate = 0L
                val notificationUpdateInterval = 500L // 每 500ms 最多更新一次通知

                while (!isStreamDone || contentBuffer.isNotEmpty() || reasoningBuffer.isNotEmpty()) {
                    var consumed = false

                    if (reasoningPhase && reasoningBuffer.isNotEmpty()) {
                        val delta = reasoningBuffer.removeAt(0)
                        currentReasoning += delta
                        consumed = true
                    } else if (contentBuffer.isNotEmpty()) {
                        val delta = contentBuffer.removeAt(0)
                        currentContent += delta
                        consumed = true
                        reasoningPhase = false
                    } else if (reasoningBuffer.isNotEmpty()) {
                        val delta = reasoningBuffer.removeAt(0)
                        currentReasoning += delta
                        consumed = true
                    }

                    if (activeChatId == targetChatId && activeStreams.contains(targetChatId)) {
                        streamingContent.value = currentContent
                        streamingReasoning.value = currentReasoning
                    }

                    chatStreamStates[targetChatId] = StreamState(
                        content = currentContent,
                        reasoning = currentReasoning,
                        searchResults = null,
                        isActive = !isStreamDone
                    )

                    // 更新通知
                    val now = System.currentTimeMillis()
                    if (now - lastNotificationUpdate > notificationUpdateInterval) {
                        val displayText = when {
                            reasoningPhase && currentReasoning.isNotBlank() -> "正在思考..."
                            currentContent.isNotBlank() -> {
                                val preview = currentContent.take(30)
                                if (currentContent.length > 30) "$preview..." else preview
                            }
                            else -> "MiMo正在回复你"
                        }
                        val updateIntent = Intent(application, ChatService::class.java).apply {
                            action = ChatService.ACTION_UPDATE_NOTIFICATION
                            putExtra(ChatService.EXTRA_NOTIFICATION_TEXT, displayText)
                        }
                        application.startService(updateIntent)
                        lastNotificationUpdate = now
                    }

                    if (consumed) {
                        delay(8)
                    } else {
                        delay(4)
                    }
                }

                if (activeChatId == targetChatId) {
                    isStreaming.value = false
                }
                activeStreams.remove(targetChatId)

                chatStreamStates[targetChatId] = StreamState(
                    content = currentContent,
                    reasoning = currentReasoning,
                    searchResults = null,
                    isActive = false
                )

                val error = streamError
                if (error != null) {
                    val lastUserMessage = messages.lastOrNull { it.role == "user" && it.chatId == targetChatId }
                    if (lastUserMessage != null && !lastUserMessage.isFailed) {
                        val failedUserMessage = lastUserMessage.copy(isFailed = true)
                        viewModelScope.launch {
                            chatRepository.updateMessage(failedUserMessage)
                        }
                    }
                    val displayError = if (error.contains("Unable to resolve host") || 
                        error.contains("Failed to connect") ||
                        error.contains("timeout") ||
                        error.contains("network") ||
                        error.contains("Network") ||
                        error.contains("网络")) {
                        "无网络连接"
                    } else {
                        error
                    }
                    if (activeChatId == targetChatId) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = displayError
                            )
                        }
                    }
                } else {
                    val finalMessage = Message(
                        chatId = targetChatId,
                        role = "assistant",
                        content = currentContent,
                        reasoningContent = currentReasoning.ifBlank { null },
                        searchResults = streamSearchResults
                    )
                    chatRepository.saveMessage(finalMessage)
                    if (activeChatId == targetChatId) {
                        messages.add(finalMessage)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }

                // 只有在所有流都完成时才停止服务
                if (activeStreams.isEmpty()) {
                    val stopIntent = Intent(application, ChatService::class.java).apply {
                        action = ChatService.ACTION_STOP
                    }
                    application.startService(stopIntent)
                }
            }
        }
    }

    fun stopGenerating() {
        activeStreams.remove(activeChatId)
        streamJob?.cancel()
        streamJob = null
        isStreaming.value = false

        val content = streamingContent.value
        val reasoning = streamingReasoning.value
        val chatId = _uiState.value.currentChat?.id ?: 0

        chatId.takeIf { it > 0 }?.let {
            chatStreamStates[it] = StreamState(content, reasoning, null, false)
        }

        val lastUserIndex = messages.indexOfLast { it.role == "user" }
        if (lastUserIndex >= 0) {
            val lastUserMessage = messages[lastUserIndex]
            if (!lastUserMessage.isAborted) {
                val abortedUserMessage = lastUserMessage.copy(isAborted = true)
                messages[lastUserIndex] = abortedUserMessage
                viewModelScope.launch {
                    chatRepository.updateMessage(abortedUserMessage)
                }
            }
        }

        if (content.isNotBlank() || reasoning.isNotBlank()) {
            val abortedMessage = Message(
                chatId = chatId,
                role = "assistant",
                content = content,
                reasoningContent = reasoning.ifBlank { null },
                isAborted = true
            )
            messages.add(abortedMessage)
            viewModelScope.launch {
                chatRepository.saveMessage(abortedMessage)
            }
        }

        streamingContent.value = ""
        streamingReasoning.value = ""
        _uiState.update { it.copy(isLoading = false) }

        // 只有在所有流都完成时才停止服务
        if (activeStreams.isEmpty()) {
            val stopIntent = Intent(application, ChatService::class.java).apply {
                action = ChatService.ACTION_STOP
            }
            application.startService(stopIntent)
        }
    }

    fun deleteChat(chat: Chat) {
        viewModelScope.launch {
            chatRepository.deleteChat(chat.id)
            if (_uiState.value.currentChat?.id == chat.id) {
                messagesJob?.cancel()
                messages.clear()
                _uiState.update { it.copy(currentChat = null) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
