package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.database.StudentHubEntity
import com.kpyruy.takt.core.database.TaktDatabase
import com.kpyruy.takt.core.model.StudentHubSnapshot
import kotlinx.coroutines.flow.*

class RoomStudentHubStore(private val database: TaktDatabase) : StudentHubStore {
    override val snapshots: Flow<StudentHubSnapshot?> = database.studentHubDao().observe().map { it?.let { row -> runCatching { StudentHubCodec.decode(row.payloadJson) }.getOrNull() } }
    override suspend fun read() = database.studentHubDao().read()?.let { runCatching { StudentHubCodec.decode(it.payloadJson) }.getOrNull() }
    override suspend fun write(snapshot: StudentHubSnapshot) { database.studentHubDao().put(StudentHubEntity(payloadJson = StudentHubCodec.encode(snapshot))) }
    override suspend fun clear() { database.studentHubDao().clear() }
}
class InMemoryStudentHubStore : StudentHubStore {
    private val current = MutableStateFlow<StudentHubSnapshot?>(null)
    override val snapshots = current.asStateFlow()
    override suspend fun read() = current.value
    override suspend fun write(snapshot: StudentHubSnapshot) { current.value = snapshot.toBackupStudentHub().validated().toModel() }
    override suspend fun clear() { current.value = null }
}
