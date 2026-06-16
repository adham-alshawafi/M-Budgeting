package com.example.data

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

sealed class SyncResult {
    object Success : SyncResult()
    data class Failure(val message: String) : SyncResult()
}

class FirebaseSyncService(
    private val context: Context,
    private val repository: FinanceRepository
) {
    private val tag = "FirebaseSyncService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Decoupled JSON DTO Transfer models to keep cloud sync cleanly segregated from DB keys
    data class TransactionDto(
        val syncId: String = "",
        val title: String = "",
        val amount: Double = 0.0,
        val type: String = "",
        val category: String = "",
        val timestamp: Long = 0L,
        val notes: String = "",
        val isDeleted: Boolean = false,
        val lastModified: Long = 0L,
        val account: String = "Cash",
        val toAccount: String? = null
    )

    data class BudgetDto(
        val syncId: String = "",
        val category: String = "",
        val limitAmount: Double = 0.0,
        val monthYear: String = "",
        val alertThreshold: Double = 80.0,
        val isDeleted: Boolean = false,
        val lastModified: Long = 0L
    )

    data class NoteDto(
        val syncId: String = "",
        val title: String = "",
        val content: String = "",
        val timestamp: Long = 0L,
        val isDeleted: Boolean = false,
        val lastModified: Long = 0L
    )

    data class RecurringDto(
        val syncId: String = "",
        val title: String = "",
        val amount: Double = 0.0,
        val type: String = "",
        val category: String = "",
        val frequency: String = "",
        val startDateTimestamp: Long = 0L,
        val lastTriggeredTimestamp: Long = 0L,
        val nextTriggerTimestamp: Long = 0L,
        val isActive: Boolean = true,
        val notes: String = "",
        val isDeleted: Boolean = false,
        val lastModified: Long = 0L
    )

    private fun sanitizeEmail(email: String): String {
        return email.trim()
            .replace(".", "_")
            .replace("@", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
    }

    private fun normalizeBaseUrl(databaseUrl: String): String {
        var url = databaseUrl.trim()
        if (url.isEmpty()) return ""
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // Treat the entry as Firebase project ID and auto-format
            url = "https://$url-default-rtdb.firebaseio.com/"
        }
        
        if (!url.endsWith("/")) {
            url += "/"
        }
        return url
    }

    suspend fun performFullSync(email: String, rawDatabaseUrl: String): SyncResult = withContext(Dispatchers.IO) {
        val sanitizedEmail = sanitizeEmail(email)
        val baseUrl = normalizeBaseUrl(rawDatabaseUrl)
        
        if (sanitizedEmail.isEmpty() || baseUrl.isEmpty()) {
            return@withContext SyncResult.Failure("Invalid sync settings. Please provide a database URL and Sync Email.")
        }

        try {
            Log.d(tag, "Beginning complete background sync pull-merge-push flow...")
            
            val tSync = syncTransactions(sanitizedEmail, baseUrl)
            val bSync = syncBudgets(sanitizedEmail, baseUrl)
            val nSync = syncNotes(sanitizedEmail, baseUrl)
            val rSync = syncRecurring(sanitizedEmail, baseUrl)

            if (tSync && bSync && nSync && rSync) {
                Log.d(tag, "Full replication sync succeeded across all nodes!")
                SyncResult.Success
            } else {
                SyncResult.Failure("Unable to synchronize one or more tables with Firebase. Check your credentials and database Rules.")
            }
        } catch (e: Exception) {
            Log.e(tag, "Exception during replication sync: ${e.message}", e)
            SyncResult.Failure(e.localizedMessage ?: "Sync Error: connection failed")
        }
    }

    private suspend fun syncTransactions(sanitizedEmail: String, baseUrl: String): Boolean {
        val endpoint = "${baseUrl}users/$sanitizedEmail/transactions.json"
        
        // 1. Fetch local records (including deleted placeholders)
        val localEntities = repository.getAllTransactionsForSync()
        val localMap = localEntities.associateBy { it.syncId }

        // 2. Fetch remote records from Firebase
        val request = Request.Builder().url(endpoint).get().build()
        val remoteMap: Map<String, TransactionDto> = try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    emptyMap()
                } else if (!response.isSuccessful) {
                    Log.e(tag, "GET Transactions failed: HTTP ${response.code}")
                    return false
                } else {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString == "null" || bodyString.trim().isEmpty()) {
                        emptyMap()
                    } else {
                        val type = Types.newParameterizedType(Map::class.java, String::class.java, TransactionDto::class.java)
                        val adapter = moshi.adapter<Map<String, TransactionDto>>(type)
                        adapter.fromJson(bodyString) ?: emptyMap()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "GET Transactions crashed: ${e.message}")
            return false
        }

        // 3. Resolve conflict and merge lists using Last-Write-Wins (LWW)
        val mergedList = mutableListOf<TransactionDto>()
        val allSyncIds = localMap.keys + remoteMap.keys

        for (syncId in allSyncIds) {
            val local = localMap[syncId]
            val remote = remoteMap[syncId]

            if (local != null && remote != null) {
                if (local.lastModified >= remote.lastModified) {
                    mergedList.add(local.toDto())
                } else {
                    mergedList.add(remote)
                }
            } else if (local != null) {
                mergedList.add(local.toDto())
            } else if (remote != null) {
                mergedList.add(remote)
            }
        }

        // 4. Save updates locally
        for (item in mergedList) {
            val localMatch = repository.getTransactionBySyncId(item.syncId)
            val localId = localMatch?.id ?: 0
            
            // Re-apply to local Room
            val entity = TransactionEntity(
                id = localId,
                title = item.title,
                amount = item.amount,
                type = item.type,
                category = item.category,
                timestamp = item.timestamp,
                notes = item.notes,
                syncId = item.syncId,
                isDeleted = item.isDeleted,
                lastModified = item.lastModified,
                account = item.account,
                toAccount = item.toAccount
            )
            repository.insertTransaction(entity)
        }

        // 5. Upload merged outcome back to Firebase
        val mergedUploadMap = mergedList.associateBy { it.syncId }
        val adapter = moshi.adapter<Map<String, TransactionDto>>(
            Types.newParameterizedType(Map::class.java, String::class.java, TransactionDto::class.java)
        )
        val uploadJson = adapter.toJson(mergedUploadMap)
        
        val putRequest = Request.Builder()
            .url(endpoint)
            .put(uploadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            client.newCall(putRequest).execute().use { response ->
                if (response.isSuccessful) {
                    true
                } else {
                    Log.e(tag, "PUT Transactions failed: HTTP ${response.code}")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "PUT Transactions exceptions: ${e.message}")
            false
        }
    }

    private suspend fun syncBudgets(sanitizedEmail: String, baseUrl: String): Boolean {
        val endpoint = "${baseUrl}users/$sanitizedEmail/budgets.json"
        
        val localEntities = repository.getAllBudgetsForSync()
        val localMap = localEntities.associateBy { it.syncId }

        val request = Request.Builder().url(endpoint).get().build()
        val remoteMap: Map<String, BudgetDto> = try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    emptyMap()
                } else if (!response.isSuccessful) {
                    Log.e(tag, "GET Budgets failed: HTTP ${response.code}")
                    return false
                } else {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString == "null" || bodyString.trim().isEmpty()) {
                        emptyMap()
                    } else {
                        val type = Types.newParameterizedType(Map::class.java, String::class.java, BudgetDto::class.java)
                        val adapter = moshi.adapter<Map<String, BudgetDto>>(type)
                        adapter.fromJson(bodyString) ?: emptyMap()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "GET Budgets crashed: ${e.message}")
            return false
        }

        val mergedList = mutableListOf<BudgetDto>()
        val allSyncIds = localMap.keys + remoteMap.keys

        for (syncId in allSyncIds) {
            val local = localMap[syncId]
            val remote = remoteMap[syncId]

            if (local != null && remote != null) {
                if (local.lastModified >= remote.lastModified) {
                    mergedList.add(local.toDto())
                } else {
                    mergedList.add(remote)
                }
            } else if (local != null) {
                mergedList.add(local.toDto())
            } else if (remote != null) {
                mergedList.add(remote)
            }
        }

        for (item in mergedList) {
            val localMatch = repository.getBudgetBySyncId(item.syncId)
            val localId = localMatch?.id ?: 0
            
            val entity = BudgetEntity(
                id = localId,
                category = item.category,
                limitAmount = item.limitAmount,
                monthYear = item.monthYear,
                alertThreshold = item.alertThreshold,
                syncId = item.syncId,
                isDeleted = item.isDeleted,
                lastModified = item.lastModified
            )
            repository.insertBudget(entity)
        }

        val mergedUploadMap = mergedList.associateBy { it.syncId }
        val adapter = moshi.adapter<Map<String, BudgetDto>>(
            Types.newParameterizedType(Map::class.java, String::class.java, BudgetDto::class.java)
        )
        val uploadJson = adapter.toJson(mergedUploadMap)
        
        val putRequest = Request.Builder()
            .url(endpoint)
            .put(uploadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            client.newCall(putRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun syncNotes(sanitizedEmail: String, baseUrl: String): Boolean {
        val endpoint = "${baseUrl}users/$sanitizedEmail/notes.json"
        
        val localEntities = repository.getAllNotesForSync()
        val localMap = localEntities.associateBy { it.syncId }

        val request = Request.Builder().url(endpoint).get().build()
        val remoteMap: Map<String, NoteDto> = try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    emptyMap()
                } else if (!response.isSuccessful) {
                    Log.e(tag, "GET Notes failed: HTTP ${response.code}")
                    return false
                } else {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString == "null" || bodyString.trim().isEmpty()) {
                        emptyMap()
                    } else {
                        val type = Types.newParameterizedType(Map::class.java, String::class.java, NoteDto::class.java)
                        val adapter = moshi.adapter<Map<String, NoteDto>>(type)
                        adapter.fromJson(bodyString) ?: emptyMap()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "GET Notes crashed: ${e.message}")
            return false
        }

        val mergedList = mutableListOf<NoteDto>()
        val allSyncIds = localMap.keys + remoteMap.keys

        for (syncId in allSyncIds) {
            val local = localMap[syncId]
            val remote = remoteMap[syncId]

            if (local != null && remote != null) {
                if (local.lastModified >= remote.lastModified) {
                    mergedList.add(local.toDto())
                } else {
                    mergedList.add(remote)
                }
            } else if (local != null) {
                mergedList.add(local.toDto())
            } else if (remote != null) {
                mergedList.add(remote)
            }
        }

        for (item in mergedList) {
            val localMatch = repository.getNoteBySyncId(item.syncId)
            val localId = localMatch?.id ?: 0
            
            val entity = FinancialNoteEntity(
                id = localId,
                title = item.title,
                content = item.content,
                timestamp = item.timestamp,
                syncId = item.syncId,
                isDeleted = item.isDeleted,
                lastModified = item.lastModified
            )
            repository.insertNote(entity)
        }

        val mergedUploadMap = mergedList.associateBy { it.syncId }
        val adapter = moshi.adapter<Map<String, NoteDto>>(
            Types.newParameterizedType(Map::class.java, String::class.java, NoteDto::class.java)
        )
        val uploadJson = adapter.toJson(mergedUploadMap)
        
        val putRequest = Request.Builder()
            .url(endpoint)
            .put(uploadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            client.newCall(putRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun syncRecurring(sanitizedEmail: String, baseUrl: String): Boolean {
        val endpoint = "${baseUrl}users/$sanitizedEmail/recurring.json"
        
        val localEntities = repository.getAllRecurringTransactionsForSync()
        val localMap = localEntities.associateBy { it.syncId }

        val request = Request.Builder().url(endpoint).get().build()
        val remoteMap: Map<String, RecurringDto> = try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    emptyMap()
                } else if (!response.isSuccessful) {
                    Log.e(tag, "GET Recurring failed: HTTP ${response.code}")
                    return false
                } else {
                    val bodyString = response.body?.string() ?: ""
                    if (bodyString == "null" || bodyString.trim().isEmpty()) {
                        emptyMap()
                    } else {
                        val type = Types.newParameterizedType(Map::class.java, String::class.java, RecurringDto::class.java)
                        val adapter = moshi.adapter<Map<String, RecurringDto>>(type)
                        adapter.fromJson(bodyString) ?: emptyMap()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "GET Recurring crashed: ${e.message}")
            return false
        }

        val mergedList = mutableListOf<RecurringDto>()
        val allSyncIds = localMap.keys + remoteMap.keys

        for (syncId in allSyncIds) {
            val local = localMap[syncId]
            val remote = remoteMap[syncId]

            if (local != null && remote != null) {
                if (local.lastModified >= remote.lastModified) {
                    mergedList.add(local.toDto())
                } else {
                    mergedList.add(remote)
                }
            } else if (local != null) {
                mergedList.add(local.toDto())
            } else if (remote != null) {
                mergedList.add(remote)
            }
        }

        for (item in mergedList) {
            val localMatch = repository.getRecurringTransactionBySyncId(item.syncId)
            val localId = localMatch?.id ?: 0
            
            val entity = RecurringTransactionEntity(
                id = localId,
                title = item.title,
                amount = item.amount,
                type = item.type,
                category = item.category,
                frequency = item.frequency,
                startDateTimestamp = item.startDateTimestamp,
                lastTriggeredTimestamp = item.lastTriggeredTimestamp,
                nextTriggerTimestamp = item.nextTriggerTimestamp,
                isActive = item.isActive,
                notes = item.notes,
                syncId = item.syncId,
                isDeleted = item.isDeleted,
                lastModified = item.lastModified
            )
            repository.insertRecurringTransaction(entity)
        }

        val mergedUploadMap = mergedList.associateBy { it.syncId }
        val adapter = moshi.adapter<Map<String, RecurringDto>>(
            Types.newParameterizedType(Map::class.java, String::class.java, RecurringDto::class.java)
        )
        val uploadJson = adapter.toJson(mergedUploadMap)
        
        val putRequest = Request.Builder()
            .url(endpoint)
            .put(uploadJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        return try {
            client.newCall(putRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }

    // Mapper helper extensions to cleanly convert between Dto and Entity
    private fun TransactionEntity.toDto() = TransactionDto(
        syncId = syncId,
        title = title,
        amount = amount,
        type = type,
        category = category,
        timestamp = timestamp,
        notes = notes,
        isDeleted = isDeleted,
        lastModified = lastModified,
        account = account,
        toAccount = toAccount
    )

    private fun BudgetEntity.toDto() = BudgetDto(
        syncId = syncId,
        category = category,
        limitAmount = limitAmount,
        monthYear = monthYear,
        alertThreshold = alertThreshold,
        isDeleted = isDeleted,
        lastModified = lastModified
    )

    private fun FinancialNoteEntity.toDto() = NoteDto(
        syncId = syncId,
        title = title,
        content = content,
        timestamp = timestamp,
        isDeleted = isDeleted,
        lastModified = lastModified
    )

    private fun RecurringTransactionEntity.toDto() = RecurringDto(
        syncId = syncId,
        title = title,
        amount = amount,
        type = type,
        category = category,
        frequency = frequency,
        startDateTimestamp = startDateTimestamp,
        lastTriggeredTimestamp = lastTriggeredTimestamp,
        nextTriggerTimestamp = nextTriggerTimestamp,
        isActive = isActive,
        notes = notes,
        isDeleted = isDeleted,
        lastModified = lastModified
    )
}
