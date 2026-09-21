package com.kpyruy.takt.core.data

import android.content.Context
import androidx.room.Room
import androidx.room.withTransaction
import com.kpyruy.takt.core.database.TaktDatabase

class TaktDataContainer(context: Context) {
    val database: TaktDatabase = Room.databaseBuilder(
        context.applicationContext,
        TaktDatabase::class.java,
        "takt.db",
    ).build()

    val studyPlanRepository: StudyPlanRepository = RoomStudyPlanRepository(database.courseDao())

    suspend fun seedIfNeeded() {
        val dao = database.courseDao()
        if (dao.count() > 0) return
        database.withTransaction { dao.insertAll(StudyPlanSeed.courses) }
    }
}
