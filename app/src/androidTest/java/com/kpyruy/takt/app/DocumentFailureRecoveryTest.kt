package com.kpyruy.takt.app

import android.content.Context
import android.content.ContextWrapper
import androidx.documentfile.provider.DocumentFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.data.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.util.UUID

/** Local file-backed DocumentFile faults; no account, external tree grant or user folder is touched. */
class DocumentFailureRecoveryTest {
    private val app get() = ApplicationProvider.getApplicationContext<Context>()
    private val old = BackupPayloadCodec.encode(BackupPayload(courses = listOf(BackupCourse("old", "OLD", "Old", 5, 1, "enrolled", "COMPULSORY"))))
    private val next = BackupPayloadCodec.encode(BackupPayload(courses = listOf(BackupCourse("new", "NEW", "New", 5, 1, "enrolled", "COMPULSORY"))))
    private fun write(store: TaktDocumentStore, root: DocumentFile, raw: String) {
        try { TaktDocumentStore::class.java.getDeclaredMethod("writeBackup", DocumentFile::class.java, String::class.java)
            .apply { isAccessible = true }.invoke(store, root, raw) }
        finally {
            // RawDocumentFile appends the MIME extension even when it was supplied; normalize the
            // test fixture to the exact names a DocumentsProvider normally returns for these files.
            root.listFiles().filter { it.name.orEmpty().endsWith(".json.json") }.forEach {
                check(it.renameTo(it.name!!.removeSuffix(".json")))
            }
        }
    }
    private fun read(store: TaktDocumentStore, root: DocumentFile) = TaktDocumentStore::class.java
        .getDeclaredMethod("readBestBackup", DocumentFile::class.java).apply { isAccessible = true }.invoke(store, root) as String?
    private suspend fun fixture(test: suspend (TaktDocumentStore, RoomBackupRepository, File, Context) -> Unit) {
        val prefix = "document-fault-${UUID.randomUUID()}"
        val root = File(app.cacheDir, prefix).apply { mkdirs() }
        val context = object : ContextWrapper(app) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int) = app.getSharedPreferences("$prefix-$name", mode)
        }
        val db = Room.inMemoryDatabaseBuilder(context, TaktDatabase::class.java).build()
        try {
            val settings = SharedPreferencesAppSettingsRepository(context)
            val backup = RoomBackupRepository(db, settings)
            backup.importJson(old)
            test(TaktDocumentStore(context, db, backup, settings), backup, root, context)
        } finally {
            db.close(); root.listFiles()?.forEach { it.setWritable(true) }; root.deleteRecursively()
            for (name in listOf("takt_documents", "takt_settings")) context.getSharedPreferences(name, 0).edit().clear().commit()
        }
    }
    @Test fun readOnlyMainKeepsVerifiedPreviousCopyAndAllowsRetry() = runBlocking {
        fixture { store, backup, directory, _ ->
            val root = DocumentFile.fromFile(directory)
            val before = backup.exportJson()
            write(store, root, old)
            val main = File(directory, "data.json")
            assertTrue(main.setWritable(false, false))
            try {
                val failure = runCatching { write(store, root, next) }.exceptionOrNull()
                assertTrue(failure is InvocationTargetException)
                assertEquals(old, read(store, root))
                assertEquals(old, File(directory, "data.previous.json").readText())
                assertEquals(before, backup.exportJson())
            } finally { assertTrue(main.setWritable(true, true)) }
            write(store, root, next)
            assertEquals(next, read(store, root))
        }
    }
    @Test fun corruptMainFallsBackWithoutImportingOrOverwritingLocalDatabase() = runBlocking {
        fixture { store, backup, directory, _ ->
            val root = DocumentFile.fromFile(directory)
            val before = backup.exportJson()
            write(store, root, old); write(store, root, next)
            File(directory, "data.json").writeText("{truncated")
            assertEquals(old, read(store, root))
            assertEquals(before, backup.exportJson())
        }
    }
    @Test fun missingTreeGrantDisconnectsWithoutDownloadFallbackOrLocalDataLoss() = runBlocking {
        fixture { store, backup, _, context ->
            val before = backup.exportJson()
            context.getSharedPreferences("takt_documents", 0).edit()
                .putString("tree_uri", "content://missing-provider/tree/Documents").commit()
            store.resume()
            assertFalse(store.isConnected)
            assertEquals(DocumentSyncStatus.DISCONNECTED, store.status.value)
            assertTrue(runCatching { store.saveCurrentToDocuments() }.isFailure)
            assertEquals(before, backup.exportJson())
        }
    }
}
