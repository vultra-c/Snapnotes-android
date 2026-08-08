package com.whyy.snapnotes.logic

import android.content.Context
import android.util.Log
import com.xiaomi.xms.wearable.Wearable.getAuthApi
import com.xiaomi.xms.wearable.Wearable.getMessageApi
import com.xiaomi.xms.wearable.Wearable.getNodeApi
import com.xiaomi.xms.wearable.auth.AuthApi
import com.xiaomi.xms.wearable.auth.Permission
import com.xiaomi.xms.wearable.message.MessageApi
import com.xiaomi.xms.wearable.message.OnMessageReceivedListener
import com.xiaomi.xms.wearable.node.Node
import com.xiaomi.xms.wearable.node.NodeApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * XMS wearable SDK 直调封装层。
 * 三大 API：NodeApi（发现/拉起快应用/检测安装）、AuthApi（申请 DEVICE_MANAGER）、MessageApi（收发 byte[]）。
 * 所有方法返回 [CompletableDeferred] 供协程层 `.await()` 同步化。
 *
 * 对标参考工程 com.bandbbs.ebook-android/logic/Interconn.kt；唯一差异：openApp 拉起页改 `/pages/index`。
 */
open class Interconn(context: Context) {
    val nodeApi: NodeApi = getNodeApi(context)
    val authApi: AuthApi = getAuthApi(context)
    val messageApi: MessageApi = getMessageApi(context)
    var currentNode: Node? = null

    /** 宽容模式 JSON：手环未来扩展字段不会解析失败。 */
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    /** tag → 回调分发表。由 InterHandshake / JsonFilePusher 通过 addListener 注册。 */
    val onMessage = mutableMapOf<String, (String) -> Unit>()

    open val onMessageListener = OnMessageReceivedListener { _, message ->
        val messageStr = message.decodeToString()
        // 入站包分发入口（不打印消息内容：BLE 消息可能含聊天文本/文件数据，防 logcat 泄漏）。
        onRawMessageReceived()
        try {
            val msg = json.decodeFromString<Message>(messageStr)
            // ① 先按 type 预分发无 tag 的 chat 入站（手环 chat 消息不带 tag 字段）
            val typeField = msg.type
            if (!typeField.isNullOrEmpty() &&
                (typeField.startsWith("chat_") || typeField == "reply_ack")
            ) {
                onMessage["__chat__"]?.invoke(messageStr)
                return@OnMessageReceivedListener
            }
            // ② 再按 tag 分发（__hs__ / file / ...）
            msg.tag?.let { onMessage[it]?.invoke(messageStr) }
        } catch (e: Exception) {
            Log.e("Interconn", "msg parse fail", e)
        }
    }

    /** 入站回调前置钩子：子类（InterHandshake）用来刷新握手心跳超时等。默认空实现。 */
    open fun onRawMessageReceived() {}

    /** 发现已通过「小米运动健康」连接的设备，取第一个。 */
    fun connect(): CompletableDeferred<String> = CompletableDeferred<String>().apply {
        Log.e("Interconn", "PROBE connect: querying connectedNodes...")
        nodeApi.connectedNodes.addOnSuccessListener { nodes ->
            Log.e("Interconn", "PROBE connectedNodes success count=${nodes.size}")
            if (nodes.isEmpty()) {
                completeExceptionally(Exception("未找到设备！"))
                return@addOnSuccessListener
            }
            currentNode = nodes[0]
            complete(nodes[0].name)
        }.addOnFailureListener {
            Log.e("Interconn", "PROBE connectedNodes fail", it)
            completeExceptionally(Exception("获取设备列表失败，请检查小米运动健康是否已连接！"))
        }
    }

    /**
     * 申请/校验 DEVICE_MANAGER 权限（控制类操作必需）。
     * 先 checkPermissions 查询已授权状态；对未授权的权限调 requestPermission 并等待结果，
     * 确保后续 isWearAppInstalled / launchWearApp / sendMessage 等操作不会因权限缺失而失败。
     */
    fun auth(): CompletableDeferred<Unit> = CompletableDeferred<Unit>().apply {
        val node = currentNode
        if (node == null) {
            completeExceptionally(Exception("设备未连接！"))
            return@apply
        }
        val permissions = arrayOf<Permission?>(Permission.DEVICE_MANAGER)
        authApi.checkPermissions(node.id, permissions).addOnSuccessListener { results ->
            // 收集未授权的权限
            val ungranted = mutableListOf<Permission>()
            for ((index, granted) in results.withIndex()) {
                if (!granted) {
                    permissions[index]?.let { ungranted.add(it) }
                }
            }

            if (ungranted.isEmpty()) {
                // 全部已授权，直接完成
                Log.d("Auth", "all permissions already granted")
                complete(Unit)
            } else {
                // 申请未授权的权限，等待申请结果后再完成（避免 fire-and-forget 导致后续操作因权限未就绪而失败）
                Log.d("Auth", "requesting ${ungranted.size} ungranted permissions")
                authApi.requestPermission(node.id, *ungranted.toTypedArray())
                    .addOnSuccessListener { grantedPermissions ->
                        Log.d("Auth", "permissions granted: ${grantedPermissions?.size ?: 0}")
                        complete(Unit)
                    }
                    .addOnFailureListener { e ->
                        Log.e("Auth", "requestPermission fail", e)
                        completeExceptionally(Exception("权限授权失败，请在小米运动健康中授权"))
                    }
            }
        }.addOnFailureListener {
            Log.e("Auth", "checkPermissions fail", it)
            completeExceptionally(Exception("获取权限失败！"))
        }
    }

    /** 拉起手环端闪念小抄快应用到主页（manifest router 注册的 /pages/index），连上后自动同步文件树。 */
    fun openApp(): CompletableDeferred<Unit> = CompletableDeferred<Unit>().apply {
        val node = currentNode
        if (node == null) {
            completeExceptionally(Exception("设备未连接！"))
            return@apply
        }
        nodeApi.launchWearApp(node.id, "/pages/index").addOnSuccessListener {
            Log.d("OpenApp", "success")
            complete(Unit)
        }.addOnFailureListener {
            Log.e("OpenApp", "fail", it)
            completeExceptionally(Exception("打开应用失败！"))
        }
    }

    /** 注册消息接收监听；已注册(You have registered)视为成功兼容。 */
    fun registerListener(): CompletableDeferred<Unit> = CompletableDeferred<Unit>().apply {
        val node = currentNode
        if (node == null) {
            completeExceptionally(Exception("设备未连接！"))
            return@apply
        }
        messageApi.addListener(node.id, onMessageListener)
            .addOnSuccessListener { complete(Unit) }
            .addOnFailureListener { error ->
                val msg = error.message.orEmpty()
                if (msg.contains("You have registered", ignoreCase = true)) {
                    Log.w("RegisterListener", "already registered, continue")
                    complete(Unit)
                } else {
                    completeExceptionally(error)
                }
            }
    }

    open fun sendMessage(message: String): CompletableDeferred<Unit> {
        return CompletableDeferred<Unit>().apply {
            val node = currentNode
            if (node == null) {
                completeExceptionally(Exception("设备未连接！"))
                return@apply
            }
            messageApi.sendMessage(node.id, message.toByteArray()).addOnSuccessListener {
                complete(Unit)
            }.addOnFailureListener {
                Log.e("Send", "fail", it)
                completeExceptionally(it)
            }
        }
    }

    fun addListener(type: String, callback: (String) -> Unit) {
        onMessage[type] = callback
    }

    fun removeListener(type: String) {
        onMessage.remove(type)
    }

    @Serializable
    data class Message(
        val tag: String? = null,
        val type: String? = null
    )

    /** 检测手环是否已安装闪念小抄快应用（isWearAppInstalled）。 */
    fun getAppState(): CompletableDeferred<Boolean> = CompletableDeferred<Boolean>().apply {
        val node = currentNode
        if (node == null) {
            completeExceptionally(Exception("设备未连接！"))
            return@apply
        }
        nodeApi.isWearAppInstalled(node.id)
            .addOnSuccessListener { complete(it) }
            .addOnFailureListener { completeExceptionally(it) }
    }

    fun destroy(): CompletableDeferred<Unit> = CompletableDeferred<Unit>().apply {
        val node = currentNode
        if (node == null) {
            complete(Unit)
        } else {
            messageApi.removeListener(node.id)
                .addOnSuccessListener { currentNode = null; complete(Unit) }
                .addOnFailureListener { currentNode = null; complete(Unit) }
        }
    }

    open suspend fun init() {
        if (currentNode != null) return
        connect().await()
        auth().await()
        openApp().await()
    }
}
