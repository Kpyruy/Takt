package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.StudentHubSnapshot
import com.kpyruy.takt.core.model.StudentHubState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface StudentHubRepository {
    val state: StateFlow<StudentHubState>
    suspend fun refresh()
    suspend fun selectStudy(studyId: String)
    suspend fun setPreviousStudiesVisible(visible: Boolean)
    suspend fun setDisplayName(name: String)
}

/** Implementations persist a single validated account snapshot atomically with Room backups. */
interface StudentHubStore {
    val snapshots: Flow<StudentHubSnapshot?>
    suspend fun read(): StudentHubSnapshot?
    suspend fun write(snapshot: StudentHubSnapshot)
    suspend fun clear()
}
