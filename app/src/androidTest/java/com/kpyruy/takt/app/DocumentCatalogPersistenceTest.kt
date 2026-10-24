package com.kpyruy.takt.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kpyruy.takt.core.data.*
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DocumentCatalogPersistenceTest {
    @Test fun catalogAndSavedNamesSurviveDatabaseReopenAndAtomicBackupRestore() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="catalog-${UUID.randomUUID()}.db"
        val prefs="catalog-${UUID.randomUUID()}";var db=Room.databaseBuilder(context,TaktDatabase::class.java,name).build()
        try {
            RoomStudyPlanRepository(db).addCourse("Documents","DOC",5,1)
            val owner="d".repeat(64)
            val listing=CourseDocumentListing(listOf(CourseDocumentFile("10","20","Lecture","application/pdf","lecture.pdf",downloadedFileName="actual.pdf")),true,
                roots=listOf(CourseDocumentFolder("10","DOC Documents","Documents")))
            RoomCourseDocumentsStore(db).write(owner,"DOC",listing)
            val settings=SharedPreferencesAppSettingsRepository(context,prefs)
            val backup=RoomBackupRepository(db,settings)
            val raw=backup.exportJson()
            assertEquals("actual.pdf",BackupPayloadCodec.decode(raw).courseDocuments!!.single().listing.files.single().downloadedFileName)
            db.close();db=Room.databaseBuilder(context,TaktDatabase::class.java,name).build()
            assertEquals(listing,RoomCourseDocumentsStore(db).read(owner,"DOC"))
            assertNull(RoomCourseDocumentsStore(db).read("e".repeat(64),"DOC"))
            db.courseDocumentsDao().clear()
            RoomBackupRepository(db,settings).importJson(raw)
            assertEquals(listing,RoomCourseDocumentsStore(db).read(owner,"DOC"))
            val invalid=BackupPayloadCodec.decode(raw).copy(courseDocuments=BackupPayloadCodec.decode(raw).courseDocuments!!.map {
                it.copy(listing=it.listing.copy(folderUrl="https://elsewhere.example/private"))
            })
            assertTrue(runCatching {RoomBackupRepository(db,settings).importJson(BackupPayloadCodec.encode(invalid))}.isFailure)
            assertEquals(listing,RoomCourseDocumentsStore(db).read(owner,"DOC"))
        } finally {db.close();context.deleteDatabase(name);context.getSharedPreferences(prefs,0).edit().clear().commit()}
    }
}
