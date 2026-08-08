package com.whyy.snapnotes.logic

import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 手环端文件树管理器。
 *
 * 协议对齐手环端 interconnTree.js（tag="tree"）：
 * - 手机→手环：{tag:"tree", action:"getTree"/"createFolder"/"deleteNode"/"renameNode", ...}
 * - 手环→手机：{tag:"tree", response:"treeData"/"folderCreated"/"nodeDeleted"/"nodeRenamed", ...}
 *
 * 与推书 / storage 查询 / Amadeus chat 共用同一条 BLE 通道，必须串行下发，
 * 复用 [JsonFilePusher.sendMutex]。
 */
class BandFileTreeManager(
    private val conn: InterHandshake,
    private val pusher: JsonFilePusher
) {
    companion object {
        private const val TAG = "BandFileTree"
        private const val TREE_TAG = "tree"
        private const val OP_TIMEOUT_MS = 10_000L
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private var treeDeferred: CompletableDeferred<List<BandFileTreeNode>> = CompletableDeferred()
    private var folderCreatedDeferred: CompletableDeferred<FolderCreatedResult> = CompletableDeferred()
    private var nodeDeletedDeferred: CompletableDeferred<Boolean> = CompletableDeferred()
    private var nodeRenamedDeferred: CompletableDeferred<Boolean> = CompletableDeferred()

    init {
        conn.addListener(TREE_TAG) { payload ->
            try {
                val obj = json.parseToJsonElement(payload).jsonObject
                val response = obj["response"]?.jsonPrimitive?.contentOrNull
                when (response) {
                    "treeData" -> handleTreeData(obj)
                    "folderCreated" -> handleFolderCreated(obj)
                    "nodeDeleted" -> handleNodeDeleted(obj)
                    "nodeRenamed" -> handleNodeRenamed(obj)
                    else -> Log.d(TAG, "unknown tree response: $response")
                }
            } catch (e: Exception) {
                Log.e(TAG, "parse tree response fail", e)
            }
        }
    }

    /**
     * 请求手环端文件树。
     * 返回根级节点列表（每个节点可能含子节点）。
     */
    suspend fun requestTree(): List<BandFileTreeNode> = pusher.sendMutex.withLock {
        treeDeferred = CompletableDeferred()
        try {
            Log.d(TAG, "requestTree sending...")
            withTimeout(OP_TIMEOUT_MS) {
                conn.sendMessage(json.encodeToString(TreeMessages.GetTree())).await()
            }
            withTimeout(OP_TIMEOUT_MS) { treeDeferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "requestTree timeout")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "requestTree fail: ${e.message}")
            emptyList()
        }
    }

    /**
     * 在手环端创建文件夹。
     * @param name 文件夹名
     * @param parentId 父文件夹 ID，空或 "bt_root" 表示根级
     * @return 创建结果（含文件夹 ID 和错误信息）
     */
    suspend fun createFolder(name: String, parentId: String = "bt_root"): FolderCreateResult = pusher.sendMutex.withLock {
        folderCreatedDeferred = CompletableDeferred()
        try {
            val msg = TreeMessages.CreateFolder(name = name, parentId = parentId)
            withTimeout(OP_TIMEOUT_MS) {
                conn.sendMessage(json.encodeToString(msg)).await()
            }
            val result = withTimeout(OP_TIMEOUT_MS) { folderCreatedDeferred.await() }
            if (result.success && !result.folderId.isNullOrBlank()) {
                FolderCreateResult(success = true, folderId = result.folderId, error = null)
            } else {
                FolderCreateResult(success = false, folderId = null, error = result.error ?: "手环返回失败")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "createFolder timeout")
            FolderCreateResult(success = false, folderId = null, error = "手环未响应（超时）")
        } catch (e: Exception) {
            Log.e(TAG, "createFolder fail: ${e.message}")
            FolderCreateResult(success = false, folderId = null, error = e.message ?: "发送失败")
        }
    }

    /**
     * 删除手环端节点（文件或文件夹，文件夹递归删除）。
     * @return 是否成功
     */
    suspend fun deleteNode(nodeId: String): Boolean = pusher.sendMutex.withLock {
        nodeDeletedDeferred = CompletableDeferred()
        try {
            val msg = TreeMessages.DeleteNode(nodeId = nodeId)
            withTimeout(OP_TIMEOUT_MS) {
                conn.sendMessage(json.encodeToString(msg)).await()
            }
            withTimeout(OP_TIMEOUT_MS) { nodeDeletedDeferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "deleteNode timeout")
            false
        } catch (e: Exception) {
            Log.e(TAG, "deleteNode fail: ${e.message}")
            false
        }
    }

    /**
     * 重命名手环端节点。
     * @return 是否成功
     */
    suspend fun renameNode(nodeId: String, newName: String): Boolean = pusher.sendMutex.withLock {
        nodeRenamedDeferred = CompletableDeferred()
        try {
            val msg = TreeMessages.RenameNode(nodeId = nodeId, newName = newName)
            withTimeout(OP_TIMEOUT_MS) {
                conn.sendMessage(json.encodeToString(msg)).await()
            }
            withTimeout(OP_TIMEOUT_MS) { nodeRenamedDeferred.await() }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "renameNode timeout")
            false
        } catch (e: Exception) {
            Log.e(TAG, "renameNode fail: ${e.message}")
            false
        }
    }

    // ── 回包处理 ──

    private fun handleTreeData(obj: JsonObject) {
        try {
            val treeArr = obj["tree"]?.jsonArray
            val nodes = treeArr?.map { parseTreeNode(it.jsonObject) } ?: emptyList()
            Log.d(TAG, "treeData recv: ${nodes.size} top-level nodes")
            if (!treeDeferred.isCompleted) treeDeferred.complete(nodes)
        } catch (e: Exception) {
            Log.e(TAG, "parse treeData fail", e)
            if (!treeDeferred.isCompleted) treeDeferred.complete(emptyList())
        }
    }

    private fun handleFolderCreated(obj: JsonObject) {
        // 兼容 JSON boolean / string "true" / number 1 三种表示
        val successEl = obj["success"]?.jsonPrimitive
        val success = successEl?.let { p ->
            p.booleanOrNull == true ||
                p.contentOrNull?.equals("true", ignoreCase = true) == true ||
                p.contentOrNull == "1"
        } ?: false
        val folderId = obj["folderId"]?.jsonPrimitive?.contentOrNull
        val error = obj["error"]?.jsonPrimitive?.contentOrNull
        Log.d(TAG, "folderCreated: success=$success id=$folderId error=$error")
        if (!folderCreatedDeferred.isCompleted) {
            folderCreatedDeferred.complete(FolderCreatedResult(success, folderId, error))
        }
    }

    private fun handleNodeDeleted(obj: JsonObject) {
        val success = obj["success"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        val error = obj["error"]?.jsonPrimitive?.contentOrNull
        Log.d(TAG, "nodeDeleted: success=$success error=$error")
        if (!nodeDeletedDeferred.isCompleted) nodeDeletedDeferred.complete(success)
    }

    private fun handleNodeRenamed(obj: JsonObject) {
        val success = obj["success"]?.jsonPrimitive?.contentOrNull?.toBoolean() ?: false
        val error = obj["error"]?.jsonPrimitive?.contentOrNull
        Log.d(TAG, "nodeRenamed: success=$success error=$error")
        if (!nodeRenamedDeferred.isCompleted) nodeRenamedDeferred.complete(success)
    }

    // ── JSON 解析 ──

    private fun parseTreeNode(obj: JsonObject): BandFileTreeNode {
        val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: ""
        val name = obj["name"]?.jsonPrimitive?.contentOrNull ?: ""
        val type = obj["type"]?.jsonPrimitive?.contentOrNull ?: "content"
        val children = obj["children"]?.jsonArray?.map { parseTreeNode(it.jsonObject) } ?: emptyList()
        return BandFileTreeNode(id, name, type, children)
    }

    private data class FolderCreatedResult(
        val success: Boolean,
        val folderId: String?,
        val error: String?
    )

    @Serializable
    private sealed class TreeMessages {
        @Serializable
        data class GetTree(
            val tag: String = "tree",
            val action: String = "getTree"
        ) : TreeMessages()

        @Serializable
        data class CreateFolder(
            val tag: String = "tree",
            val action: String = "createFolder",
            val name: String,
            val parentId: String
        ) : TreeMessages()

        @Serializable
        data class DeleteNode(
            val tag: String = "tree",
            val action: String = "deleteNode",
            val nodeId: String
        ) : TreeMessages()

        @Serializable
        data class RenameNode(
            val tag: String = "tree",
            val action: String = "renameNode",
            val nodeId: String,
            val newName: String
        ) : TreeMessages()
    }
}

/**
 * 创建文件夹的结果。
 */
data class FolderCreateResult(
    val success: Boolean,
    val folderId: String?,
    val error: String?
)

/**
 * 手环端文件树节点（logic 层 DTO）。
 * 与 [com.whyy.snapnotes.ui.viewmodel.BandFileNode] 结构一致，由 ViewModel 转换。
 */
data class BandFileTreeNode(
    val id: String,
    val name: String,
    val type: String,
    val children: List<BandFileTreeNode> = emptyList()
)
