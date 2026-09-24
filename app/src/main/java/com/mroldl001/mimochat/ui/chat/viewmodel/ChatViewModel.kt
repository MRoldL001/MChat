package com.mroldl001.mimochat.ui.chat.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.pm.PackageInfoCompat
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mroldl001.mimochat.R
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.data.api.ContentPart
import com.mroldl001.mimochat.data.repository.ChatRepository
import com.mroldl001.mimochat.data.repository.ModelRepository
import com.mroldl001.mimochat.data.repository.StreamEvent
import com.mroldl001.mimochat.data.repository.UsageRepository
import com.mroldl001.mimochat.data.repository.UsageAccountType
import com.mroldl001.mimochat.data.repository.UsageSnapshot
import com.mroldl001.mimochat.data.repository.UsageUnauthorizedException
import com.mroldl001.mimochat.data.update.GitHubRelease
import com.mroldl001.mimochat.data.update.GitHubUpdateRepository
import com.mroldl001.mimochat.data.update.UpdateCheckResult
import com.mroldl001.mimochat.domain.model.AIModel
import com.mroldl001.mimochat.domain.model.Chat
import com.mroldl001.mimochat.domain.model.Message
import com.mroldl001.mimochat.service.ChatService
import com.mroldl001.mimochat.service.UpdateDownloadService
import com.mroldl001.mimochat.ui.chat.components.LatexBitmapRenderer
import com.mroldl001.mimochat.ui.theme.CodeBlockColorMode
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.settings.AppLocale
import com.mroldl001.mimochat.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

/** 启动且未保存过模型选择时的默认模型。 */
private const val DEFAULT_MODEL_ID = "mimo-v2.6-flash"

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

sealed interface UpdateUiState {
    data object Idle : UpdateUiState
    data object Checking : UpdateUiState
    data class Available(val release: GitHubRelease) : UpdateUiState
    data class Failed(val message: String) : UpdateUiState
}

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
    val codeBlockColorMode: CodeBlockColorMode = CodeBlockColorMode.DARK,
    val customSystemPrompt: String = "",
    val chatBackgroundUri: String? = null,
    val chatBackgroundOpacity: Float = PreferencesManager.DEFAULT_CHAT_BACKGROUND_OPACITY,
    val activeSkill: SkillType? = null,
    val temperature: Float = PreferencesManager.DEFAULT_TEMPERATURE,
    val topP: Float = PreferencesManager.DEFAULT_TOP_P,
    val frequencyPenalty: Float = PreferencesManager.DEFAULT_FREQUENCY_PENALTY,
    val presencePenalty: Float = PreferencesManager.DEFAULT_PRESENCE_PENALTY,
    val acceptPrereleaseUpdates: Boolean = false,
    val updateState: UpdateUiState = UpdateUiState.Idle,
    // 剩余用量（侧边栏展示，开关控制）
    // 小米的用量接口只认控制台登录 Cookie，API Key 只能算兜底，故以登录态为主
    val showUsage: Boolean = false,
    val usageLoading: Boolean = false,
    val usageText: String? = null,
    val usageLoggedIn: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val modelRepository: ModelRepository,
    private val usageRepository: UsageRepository,
    private val preferencesManager: PreferencesManager,
    private val application: Application
) : ViewModel() {

    /** 按用户选择的语言包装出的 Context，确保本 ViewModel 读取的字符串资源也随语言切换。 */
    private val localizedContext: Context
        get() = AppLocale.wrap(application, preferencesManager.getAppLanguage())

    /** 新对话的默认标题，随系统语言本地化 */
    private val defaultChatTitle: String
        get() = localizedContext.getString(R.string.new_chat)

    private val _uiState = MutableStateFlow(
        ChatUiState(
            themeColor = preferencesManager.getThemeColor(),
            themeMode = preferencesManager.getThemeMode(),
            codeBlockColorMode = preferencesManager.getCodeBlockColorMode(),
            apiKey = preferencesManager.getApiKey(),
            apiBaseUrl = preferencesManager.getApiBaseUrl(),
            customSystemPrompt = preferencesManager.getCustomSystemPrompt(),
            chatBackgroundUri = preferencesManager.getChatBackgroundUri(),
            chatBackgroundOpacity = preferencesManager.getChatBackgroundOpacity(),
            temperature = preferencesManager.getTemperature(),
            topP = preferencesManager.getTopP(),
            frequencyPenalty = preferencesManager.getFrequencyPenalty(),
            presencePenalty = preferencesManager.getPresencePenalty(),
            acceptPrereleaseUpdates = preferencesManager.getAcceptPrereleaseUpdates(),
            showUsage = preferencesManager.getShowUsage(),
            usageLoggedIn = preferencesManager.getUsageCookie().isNotBlank()
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

    private val streamJobs = mutableMapOf<Long, Job>()
    private var messagesJob: Job? = null
    private var chatSelectionJob: Job? = null
    private var chatStreamStates: MutableMap<Long, StreamState> = mutableMapOf()
    private var activeChatId: Long? = null
    private val activeStreams = mutableSetOf<Long>()

    init {
        loadModels()
        loadChats()
        // 平板端是常驻抽屉，没有"打开抽屉"事件，故启动时先拉一次
        refreshUsage()
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
                selectedModel = selectedModel ?: models.find { it.id == DEFAULT_MODEL_ID } ?: models.firstOrNull()
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

    private fun observeMessages(chatId: Long) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            chatRepository.getMessages(chatId).collect { msgs ->
                if (activeChatId == chatId) {
                    messages.clear()
                    messages.addAll(msgs)
                }
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

    fun setCodeBlockColorMode(mode: CodeBlockColorMode) {
        _uiState.update { it.copy(codeBlockColorMode = mode) }
        preferencesManager.saveCodeBlockColorMode(mode)
    }

    fun getCustomThemeColorHex(): String = preferencesManager.getCustomThemeColorHex()

    fun setCustomThemeColorHex(hex: String) {
        _uiState.update { it.copy(themeColor = ThemeColor.CUSTOM) }
        preferencesManager.saveCustomThemeColorHex(hex)
    }

    fun setApiKey(apiKey: String) {
        _uiState.update { it.copy(apiKey = apiKey) }
        preferencesManager.saveApiKey(apiKey)
        // 用量依赖 API Key，换了 Key 就重新查一次
        refreshUsage()
    }

    fun setApiBaseUrl(url: String) {
        _uiState.update { it.copy(apiBaseUrl = url) }
        preferencesManager.saveApiBaseUrl(url)
        // 不同接口（标准 / 订阅）查的端点不同，换地址后重新查
        refreshUsage()
    }

    fun setShowUsage(enabled: Boolean) {
        _uiState.update { it.copy(showUsage = enabled) }
        preferencesManager.saveShowUsage(enabled)
        if (enabled) refreshUsage()
    }

    /** 内置登录页拿到控制台 Cookie 后调用：存起来并立即查一次用量。 */
    fun saveUsageCookie(cookie: String) {
        preferencesManager.saveUsageCookie(cookie)
        _uiState.update { it.copy(usageLoggedIn = cookie.isNotBlank()) }
        refreshUsage()
    }

    /** 登出控制台登录态，卡片回到"点击登录"。 */
    fun clearUsageCookie() {
        preferencesManager.clearUsageCookie()
        _uiState.update { it.copy(usageLoggedIn = false, usageText = null, usageLoading = false) }
    }

    /**
     * 查询剩余用量。
     * 优先用控制台 Cookie（订阅账号给剩余积分），没有 Cookie 才退而用 API Key 试余额接口。
     */
    fun refreshUsage() {
        if (!_uiState.value.showUsage) return
        val cookie = preferencesManager.getUsageCookie()
        val apiKey = _uiState.value.apiKey
        if (cookie.isBlank() && apiKey.isBlank()) {
            _uiState.update {
                it.copy(usageLoading = false, usageLoggedIn = false, usageText = null)
            }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(usageLoading = true) }
            val result = usageRepository.fetchUsage(cookie, apiKey, _uiState.value.apiBaseUrl)
            _uiState.update { state ->
                result.fold(
                    onSuccess = { snapshot ->
                        state.copy(
                            usageLoading = false,
                            usageLoggedIn = true,
                            usageText = formatUsage(snapshot)
                        )
                    },
                    onFailure = { error ->
                        if (error is CancellationException) throw error
                        if (error is UsageUnauthorizedException) {
                            // 真正的登录过期（401/403）：清掉 Cookie，卡片回到可重新登录的状态
                            preferencesManager.clearUsageCookie()
                            state.copy(
                                usageLoading = false,
                                usageLoggedIn = false,
                                usageText = localizedContext.getString(R.string.usage_expired)
                            )
                        } else {
                            // 非鉴权失败（接口路径/字段变化、网络、解析失败）：保留登录态，
                            // 提示重试，不要误清 Cookie（否则会循环提示"过期"）。
                            state.copy(
                                usageLoading = false,
                                usageLoggedIn = true,
                                usageText = localizedContext.getString(R.string.usage_error)
                            )
                        }
                    }
                )
            }
        }
    }

    private fun formatUsage(snapshot: UsageSnapshot): String {
        val ctx = localizedContext
        return when (snapshot.accountType) {
            UsageAccountType.TOKEN_PLAN -> {
                val credits = snapshot.remainingCredits ?: snapshot.balance ?: 0.0
                val millions = credits / 1_000_000.0
                val amount = if (millions >= 1000.0) {
                    String.format(Locale.US, "%.2f B", millions / 1000.0)
                } else {
                    String.format(Locale.US, "%.0f M", millions)
                }
                ctx.getString(R.string.usage_plan_credits, amount)
            }
            UsageAccountType.PAYG -> {
                val balance = snapshot.balance ?: 0.0
                val symbol = if (snapshot.currency.equals("CNY", ignoreCase = true)) "¥" else "$"
                val amount = String.format(Locale.US, "%.2f", balance)
                ctx.getString(R.string.usage_recharge_balance, "$symbol$amount")
            }
        }
    }

    fun setCustomSystemPrompt(prompt: String) {
        _uiState.update { it.copy(customSystemPrompt = prompt) }
        preferencesManager.saveCustomSystemPrompt(prompt)
    }

    fun setChatBackgroundUri(uri: String?) {
        _uiState.update { it.copy(chatBackgroundUri = uri) }
        preferencesManager.saveChatBackgroundUri(uri)
    }

    fun setChatBackgroundOpacity(value: Float) {
        val opacity = value.coerceIn(0f, 1f)
        _uiState.update { it.copy(chatBackgroundOpacity = opacity) }
        preferencesManager.saveChatBackgroundOpacity(opacity)
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

    fun setAcceptPrereleaseUpdates(accept: Boolean) {
        preferencesManager.saveAcceptPrereleaseUpdates(accept)
        _uiState.update { it.copy(acceptPrereleaseUpdates = accept) }
    }

    fun checkForUpdate() {
        if (_uiState.value.updateState is UpdateUiState.Checking) return
        val includePrerelease = _uiState.value.acceptPrereleaseUpdates
        _uiState.update { it.copy(updateState = UpdateUiState.Checking) }
        viewModelScope.launch {
            val packageInfo = application.packageManager.getPackageInfo(application.packageName, 0)
            val currentVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo)
            val currentVersionName = packageInfo.versionName ?: currentVersionCode.toString()
            val state = when (
                val result = GitHubUpdateRepository.checkForUpdate(
                    includePrerelease = includePrerelease,
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                )
            ) {
                is UpdateCheckResult.Available -> UpdateUiState.Available(result.release)
                is UpdateCheckResult.Latest -> {
                    Toast.makeText(
                        application,
                        localizedContext.getString(R.string.toast_version_latest, result.currentVersion),
                        Toast.LENGTH_SHORT
                    ).show()
                    UpdateUiState.Idle
                }
                is UpdateCheckResult.Failed -> {
                    Toast.makeText(
                        application,
                        localizedContext.getString(R.string.toast_check_update_failed, result.message ?: ""),
                        Toast.LENGTH_SHORT
                    ).show()
                    UpdateUiState.Idle
                }
            }
            _uiState.update { it.copy(updateState = state) }
        }
    }

    fun clearUpdateState() {
        _uiState.update { it.copy(updateState = UpdateUiState.Idle) }
    }

    fun downloadUpdate(release: GitHubRelease) {
        application.startForegroundService(
            Intent(application, UpdateDownloadService::class.java).apply {
                action = UpdateDownloadService.ACTION_DOWNLOAD
                putExtra(UpdateDownloadService.EXTRA_DOWNLOAD_URL, release.apkUrl)
                putExtra(UpdateDownloadService.EXTRA_VERSION_NAME, release.tagName)
            }
        )
        clearUpdateState()
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
        chatSelectionJob?.cancel()
        viewModelScope.launch {
            activeChatId?.let { currentId ->
                chatStreamStates[currentId] = StreamState(
                    content = streamingContent.value,
                    reasoning = streamingReasoning.value,
                    isActive = activeStreams.contains(currentId)
                )
            }
            
            val modelId = _uiState.value.selectedModel?.id ?: DEFAULT_MODEL_ID
            val chatId = chatRepository.createChat(
                title = defaultChatTitle,
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
                observeMessages(chat.id)
                _uiState.update {
                    it.copy(
                        currentChat = chat,
                        error = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(error = localizedContext.getString(R.string.error_create_chat_failed))
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
        
        chatSelectionJob?.cancel()
        messagesJob?.cancel()
        chatSelectionJob = viewModelScope.launch {
            val fullChat = chatRepository.getChatById(chat.id)
            val model = modelRepository.getModelById(chat.modelId)
            if (fullChat == null) return@launch
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
            observeMessages(chat.id)
        }
    }

    fun sendMessage(content: String, thinkingEnabled: Boolean = true, attachment: ContentPart? = null, webSearchEnabled: Boolean = true, attachmentUri: String? = null, attachmentMimeType: String? = null) {
        viewModelScope.launch {
            if (activeStreams.contains(activeChatId)) {
                return@launch
            }
            
            if (_uiState.value.currentChat == null) {
                val modelId = _uiState.value.selectedModel?.id ?: DEFAULT_MODEL_ID
                val chatId = chatRepository.createChat(
                    title = defaultChatTitle,
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
                    messages.clear()
                    observeMessages(chat.id)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = localizedContext.getString(R.string.error_create_chat_failed)
                        )
                    }
                    return@launch
                }
            }

            val chat = _uiState.value.currentChat!!
            activeChatId = chat.id
            val isNewChat = chat.title == defaultChatTitle
            android.util.Log.d("ChatViewModel", "Current chat: ${chat.title}, isNewChat: $isNewChat")

            val pendingUserMessage = Message(
                chatId = chat.id,
                role = "user",
                content = content,
                attachmentUri = attachmentUri,
                attachmentMimeType = attachmentMimeType
            )
            val userMessageId = chatRepository.saveMessage(pendingUserMessage)
            val userMessage = pendingUserMessage.copy(id = userMessageId)

            _uiState.update { it.copy(isLoading = true, error = null) }

            val apiKey = _uiState.value.apiKey
            if (apiKey.isBlank()) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = localizedContext.getString(R.string.error_set_api_key)
                    )
                }
                return@launch
            }

            val modelId = if (attachment != null && _uiState.value.selectedModel?.capabilities?.contains("multimodal") != true) DEFAULT_MODEL_ID else (_uiState.value.selectedModel?.id ?: DEFAULT_MODEL_ID)
            val apiBaseUrl = _uiState.value.apiBaseUrl
            val titleSource = content.ifBlank {
                when {
                    attachmentMimeType?.startsWith("image/") == true -> "图片对话"
                    attachmentMimeType?.startsWith("video/") == true -> "视频对话"
                    attachmentMimeType?.startsWith("audio/") == true -> "音频对话"
                    else -> "附件对话"
                }
            }

            if (isNewChat) {
                val chatId = chat.id
                viewModelScope.launch {
                    try {
                        val result = chatRepository.generateChatTitle(
                            apiKey = apiKey,
                        baseUrl = apiBaseUrl,
                        modelId = modelId,
                        firstMessage = titleSource
                        )
                        val title = result.getOrNull()
                        if (title != null) {
                            val currentChatFromDb = chatRepository.getChatById(chatId)
                            if (currentChatFromDb != null && currentChatFromDb.title == defaultChatTitle) {
                                val updatedChat = currentChatFromDb.copy(title = title)
                                chatRepository.updateChat(updatedChat)
                                _uiState.update { state ->
                                    val updatedChats = state.chats.map {
                                        if (it.id == chatId) it.copy(title = title) else it
                                    }
                                    state.copy(
                                        currentChat = if (state.currentChat?.id == chatId) updatedChat else state.currentChat,
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

            val contextMessages = (
                messages.filter { !it.isAborted && !it.isFailed && it.id != userMessage.id } + userMessage
            ).distinctBy { message ->
                if (message.id > 0) "id:${message.id}" else "${message.role}:${message.timestamp}:${message.content}"
            }
            val targetChatId = chat.id

            val activeSkill = _uiState.value.activeSkill
            val skillPrompt = activeSkill?.let { SkillPrompts.getSkillPrompt(it) } ?: ""
            val effectiveThinkingEnabled = if (activeSkill != null) false else thinkingEnabled

            val serviceIntent = Intent(application, ChatService::class.java).apply {
                action = ChatService.ACTION_START
                putExtra(ChatService.EXTRA_CHAT_ID, chat.id)
            }
            application.startForegroundService(serviceIntent)

            var sessionJob: Job? = null
            sessionJob = viewModelScope.launch(start = CoroutineStart.LAZY) {
                var currentContent = ""
                var currentReasoning = ""
                var streamSearchResults: List<com.mroldl001.mimochat.domain.model.WebSearchResult>? = null
                var streamError: String? = null
                var doneReceived = false
                var lastNotificationUpdate = 0L
                val notificationUpdateInterval = 500L // 每 500ms 最多更新一次通知

                fun publishStreamState() {
                    if (activeChatId == targetChatId && activeStreams.contains(targetChatId)) {
                        streamingContent.value = currentContent
                        streamingReasoning.value = currentReasoning
                    }
                    chatStreamStates[targetChatId] = StreamState(
                        content = currentContent,
                        reasoning = currentReasoning,
                        searchResults = streamSearchResults,
                        isActive = true
                    )

                    val now = System.currentTimeMillis()
                    if (now - lastNotificationUpdate >= notificationUpdateInterval) {
                        val displayText = when {
                            currentContent.isNotBlank() -> currentContent.take(30).let {
                                if (currentContent.length > 30) "$it..." else it
                            }
                            currentReasoning.isNotBlank() -> localizedContext.getString(R.string.thinking_status)
                            else -> localizedContext.getString(R.string.notif_chat_reply)
                        }
                        application.startService(Intent(application, ChatService::class.java).apply {
                            action = ChatService.ACTION_UPDATE_NOTIFICATION
                            putExtra(ChatService.EXTRA_NOTIFICATION_TEXT, displayText)
                        })
                        lastNotificationUpdate = now
                    }
                }

                suspend fun persistFailure(error: String) {
                    chatRepository.updateMessage(userMessage.copy(isFailed = true))
                    if (currentContent.isNotBlank() || currentReasoning.isNotBlank()) {
                        chatRepository.saveMessage(
                            Message(
                                chatId = targetChatId,
                                role = "assistant",
                                content = currentContent,
                                reasoningContent = currentReasoning.ifBlank { null },
                                searchResults = streamSearchResults,
                                isFailed = true
                            )
                        )
                    }
                    if (activeChatId == targetChatId) {
                        val displayError = if (
                            error.contains("Unable to resolve host") ||
                            error.contains("Failed to connect") ||
                            error.contains("timeout", ignoreCase = true) ||
                            error.contains("network", ignoreCase = true) ||
                            error.contains("网络")
                        ) localizedContext.getString(R.string.error_no_network) else error
                        _uiState.update { it.copy(isLoading = false, error = displayError) }
                    }
                }

                try {
                    chatRepository.sendMessageStream(
                        apiKey = apiKey,
                        baseUrl = apiBaseUrl,
                        chatId = targetChatId,
                        messages = contextMessages,
                        modelId = modelId,
                        thinkingEnabled = effectiveThinkingEnabled,
                        skillPrompt = skillPrompt,
                        customSystemPrompt = _uiState.value.customSystemPrompt,
                        attachment = attachment,
                        webSearchEnabled = webSearchEnabled
                    ).conflate().collect { event ->
                        when (event) {
                            is StreamEvent.ContentDelta -> {
                                currentContent = event.accumulated
                                publishStreamState()
                            }
                            is StreamEvent.ReasoningDelta -> {
                                currentReasoning = event.accumulated
                                publishStreamState()
                            }
                            is StreamEvent.Done -> {
                                currentContent = event.message.content
                                currentReasoning = event.message.reasoningContent.orEmpty()
                                streamSearchResults = event.message.searchResults
                                doneReceived = true
                                publishStreamState()
                            }
                            is StreamEvent.Error -> streamError = event.message
                        }
                    }

                    if (streamError == null && !doneReceived) {
                        streamError = localizedContext.getString(R.string.error_stream_interrupted)
                    }

                    val error = streamError
                    if (error != null) {
                        persistFailure(error)
                    } else {
                        chatRepository.saveMessage(
                            Message(
                                chatId = targetChatId,
                                role = "assistant",
                                content = currentContent,
                                reasoningContent = currentReasoning.ifBlank { null },
                                searchResults = streamSearchResults
                            )
                        )
                        if (activeChatId == targetChatId) {
                            _uiState.update { it.copy(isLoading = false) }
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    persistFailure(e.message ?: localizedContext.getString(R.string.error_stream_failed))
                } finally {
                    if (streamJobs[targetChatId] === sessionJob) {
                        streamJobs.remove(targetChatId)
                        activeStreams.remove(targetChatId)
                        chatStreamStates[targetChatId] = StreamState(
                            content = currentContent,
                            reasoning = currentReasoning,
                            searchResults = streamSearchResults,
                            isActive = false
                        )
                        if (activeChatId == targetChatId) {
                            isStreaming.value = false
                        }
                        if (activeStreams.isEmpty()) {
                            application.startService(Intent(application, ChatService::class.java).apply {
                                action = ChatService.ACTION_STOP
                            })
                        }
                    }
                }
            }
            val startedJob = checkNotNull(sessionJob)
            streamJobs[targetChatId] = startedJob
            startedJob.start()
        }
    }

    fun stopGenerating() {
        val chatId = activeChatId ?: return
        val state = chatStreamStates[chatId]
        val content = state?.content ?: streamingContent.value
        val reasoning = state?.reasoning ?: streamingReasoning.value
        val searchResults = state?.searchResults

        activeStreams.remove(chatId)
        streamJobs.remove(chatId)?.cancel()
        chatStreamStates[chatId] = StreamState(
            content = content,
            reasoning = reasoning,
            searchResults = searchResults,
            isActive = false
        )

        if (content.isNotBlank() || reasoning.isNotBlank()) {
            val abortedMessage = Message(
                chatId = chatId,
                role = "assistant",
                content = content,
                reasoningContent = reasoning.ifBlank { null },
                searchResults = searchResults,
                isAborted = true
            )
            viewModelScope.launch {
                chatRepository.saveMessage(abortedMessage)
            }
        }

        streamingContent.value = ""
        streamingReasoning.value = ""
        isStreaming.value = false
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
        LatexBitmapRenderer.clear()
        streamJobs.remove(chat.id)?.cancel()
        activeStreams.remove(chat.id)
        chatStreamStates.remove(chat.id)
        viewModelScope.launch {
            chatRepository.deleteChat(chat.id)
            if (_uiState.value.currentChat?.id == chat.id) {
                messagesJob?.cancel()
                messages.clear()
                streamingContent.value = ""
                streamingReasoning.value = ""
                isStreaming.value = false
                _uiState.update { it.copy(currentChat = null, isLoading = false) }
            }
            if (activeStreams.isEmpty()) {
                application.startService(Intent(application, ChatService::class.java).apply {
                    action = ChatService.ACTION_STOP
                })
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
