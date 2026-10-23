package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.*
import org.junit.Assert.*
import org.junit.Test
import java.io.IOException

class CachedStudentHubRepositoryTest {
    private fun source()=StudentHubSnapshot(StudentIdentity(name="Alex"),listOf(StudentStudy("101","Current",active=true),
        StudentStudy("202","Previous",active=false)),"101","101",ownerKey="owner")
    @Test fun cachedProfileIsImmediateAndFailureDoesNotDiscardData()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined);val store=InMemoryStudentHubStore();store.write(source())
        try {
            val repo=CachedStudentHubRepository(store,scope,{throw IOException("offline")},{it},{true},{"owner"})
            assertEquals("Alex",repo.state.value.snapshot!!.identity!!.name)
            repo.refresh();assertEquals(StudentHubProblem.UNAVAILABLE,repo.state.value.problem)
            assertEquals(source(),store.read())
        }finally{scope.cancel()}
    }
    @Test fun choosingArchivePreservesCurrentIdAndOnlyLoadsThatRecord()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined);val store=InMemoryStudentHubStore();store.write(source())
        val loaded=mutableListOf<String>()
        try{
            val repo=CachedStudentHubRepository(store,scope,{source()},{loaded+=it.id;it.copy(feesLoaded=true,scholarshipsLoaded=true)},{true},{"owner"})
            repo.setPreviousStudiesVisible(true);repo.selectStudy("202")
            assertEquals(listOf("202"),loaded);assertEquals("101",store.read()!!.currentStudyId)
            assertEquals("202",store.read()!!.viewedStudyId);assertTrue(store.read()!!.viewedStudy()!!.feesLoaded)
            repo.setPreviousStudiesVisible(false);assertEquals("101",store.read()!!.viewedStudyId)
        }finally{scope.cancel()}
    }
    @Test fun archiveSelectionLoadsPersistedSelectionBeforeRoomObserverEmits()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined)
        var persisted=source().copy(showPreviousStudies=true, studies=listOf(
            source().studies.first().copy(feesLoaded=true,scholarshipsLoaded=true,
                courses=listOf(Course("uis:101:1","LIVE","Current course",5,1,CourseStatus.ENROLLED))),
            source().studies.last()))
        val store=object:StudentHubStore {
            override val snapshots=kotlinx.coroutines.flow.flowOf(persisted)
            override suspend fun read()=persisted
            override suspend fun write(snapshot:StudentHubSnapshot){persisted=snapshot}
            override suspend fun clear(){error("not used")}
        }
        val loaded=mutableListOf<String>()
        try {
            val repo=CachedStudentHubRepository(store,scope,{source()},{loaded+=it.id;it.copy(feesLoaded=true,scholarshipsLoaded=true)},{true},{"owner"})
            repo.selectStudy("202")
            assertEquals(listOf("202"),loaded)
            assertEquals("101",persisted.currentStudyId)
            assertEquals("202",persisted.viewedStudyId)
        } finally {scope.cancel()}
    }
    @Test fun officialIdentityIsUnchangedByDisplayNameAndDifferentAccountCacheIsHidden()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined);val store=InMemoryStudentHubStore();store.write(source())
        try{
            val repo=CachedStudentHubRepository(store,scope,{source()},{it},{true},{"owner"})
            repo.setDisplayName("Local Alex");assertEquals("Alex",store.read()!!.identity!!.name)
            assertEquals("Local Alex",store.read()!!.displayName)
            val other=CachedStudentHubRepository(store,scope,{source()},{it},{true},{"different"})
            assertNull(other.state.value.snapshot)
        }finally{scope.cancel()}
    }
    @Test fun responseFinishingAfterAccountClearCannotRepopulateTheOldIdentity()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined);val store=InMemoryStudentHubStore()
        val entered=CompletableDeferred<Unit>();val release=CompletableDeferred<Unit>()
        try{
            val repo=CachedStudentHubRepository(store,scope,{entered.complete(Unit);release.await();source()},{it},{true},{"owner"})
            val job=launch { runCatching { repo.refresh() } }
            entered.await();repo.clear();release.complete(Unit);job.join()
            assertNull(store.read());assertNull(repo.state.value.snapshot)
        }finally{scope.cancel()}
    }
    @Test fun loadingFailureAfterMetadataRetainsThePreviouslyCachedFees()=runBlocking {
        val scope=CoroutineScope(SupervisorJob()+Dispatchers.Unconfined);val store=InMemoryStudentHubStore()
        val cached=source().copy(studies=listOf(source().studies[0].copy(feesLoaded=true,fees=listOf(StudentFee("fee","101","ISIC",1500,status=StudentFeeStatus.PAID)))))
        store.write(cached)
        try{
            val repo=CachedStudentHubRepository(store,scope,{source()},{throw IOException("offline")},{true},{"owner"})
            repo.refresh();assertEquals(1500L,store.read()!!.studies.first().fees.single().amountMinor)
            assertEquals(StudentHubProblem.UNAVAILABLE,repo.state.value.problem)
        }finally{scope.cancel()}
    }
}
