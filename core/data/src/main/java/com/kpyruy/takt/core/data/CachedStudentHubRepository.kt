package com.kpyruy.takt.core.data

import com.kpyruy.takt.core.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A cached view of one account. Viewed study is independent from the live planning/import context. */
class CachedStudentHubRepository(
    private val store: StudentHubStore,
    private val scope: CoroutineScope,
    private val loadOverview: suspend () -> StudentHubSnapshot,
    private val loadStudy: suspend (StudentStudy) -> StudentStudy,
    private val hasAccount: () -> Boolean,
    private val ownerKey: () -> String?,
    private val coordinate: suspend (suspend () -> Unit) -> Unit = { it() },
    private val stableWrite: suspend (suspend () -> Unit) -> Unit = { it() },
    private val retryDelaysMillis: List<Long> = listOf(2_000L, 8_000L, 30_000L),
) : StudentHubRepository {
    private val mutable=MutableStateFlow(StudentHubState())
    override val state=mutable.asStateFlow()
    private val generation=java.util.concurrent.atomic.AtomicLong()
    private var requestGeneration=0L
    private val writes=Mutex()
    private val requests=Mutex()
    private var retryJob: Job? = null
    private fun accepted(value:StudentHubSnapshot?)=value?.takeIf { !hasAccount() || ownerKey()!=null && it.ownerKey==ownerKey() }
    init { scope.launch {
        try { store.snapshots.collect { current -> mutable.value=mutable.value.copy(snapshot=accepted(current)) } }
        catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){ mutable.value=mutable.value.copy(problem=StudentHubProblem.UNAVAILABLE) }
    } }
    private suspend fun persist(expected:Long?=null,transform:(StudentHubSnapshot?)->StudentHubSnapshot) = writes.withLock {
        stableWrite {
            if(expected!=null&&generation.get()!=expected) throw CancellationException("Student account changed")
            store.write(transform(accepted(store.read())))
        }
    }
    override suspend fun refresh() = request {
        val fresh=loadOverview()
        persist(requestGeneration) { old ->
            val studies=fresh.studies.map { item -> old?.studies?.firstOrNull { it.id==item.id }?.let { cached ->
                item.copy(titleEn=item.titleEn?:cached.titleEn,titleSk=item.titleSk?:cached.titleSk,courses=cached.courses,fees=cached.fees,scholarships=cached.scholarships,
                    feesLoaded=cached.feesLoaded,scholarshipsLoaded=cached.scholarshipsLoaded,updatedAtEpochMillis=cached.updatedAtEpochMillis)
            } ?: item }
            fresh.copy(studies=studies,viewedStudyId=old?.viewedStudyId?.takeIf { id->studies.any{it.id==id} }?:fresh.currentStudyId,
                displayName=old?.displayName.orEmpty(),showPreviousStudies=old?.showPreviousStudies?:false,ownerKey=ownerKey())
        }
        loadViewed()
    }
    private suspend fun request(attempt:Int=0, expectedGeneration:Long=generation.get(), action:suspend ()->Unit) {
        requests.withLock {
            if (generation.get() != expectedGeneration) return@withLock
            if (attempt == 0) { retryJob?.cancel(); retryJob = null }
            if(!hasAccount()){mutable.value=mutable.value.copy(problem=StudentHubProblem.SIGN_IN_REQUIRED);return@withLock}
            requestGeneration=expectedGeneration
            mutable.value=mutable.value.copy(loading=true,problem=null)
            try { coordinate { action() } }
            catch(cancelled:CancellationException){throw cancelled}
            catch(error:Exception){ mutable.value=mutable.value.copy(problem=when(error){
                is TeachingAuthenticationRequired -> StudentHubProblem.SIGN_IN_REQUIRED
                is TeachingSessionUnavailable -> StudentHubProblem.SECOND_FACTOR_REQUIRED
                is IllegalArgumentException, is IllegalStateException -> StudentHubProblem.UNRECOGNIZED_RESPONSE
                else->StudentHubProblem.UNAVAILABLE
            })
                if (mutable.value.problem in setOf(StudentHubProblem.UNAVAILABLE, StudentHubProblem.UNRECOGNIZED_RESPONSE)
                    && attempt < retryDelaysMillis.size && generation.get() == expectedGeneration) {
                    // No account/session or backup lock is held while waiting. A new request supersedes this timer.
                    retryJob = scope.launch {
                        delay(retryDelaysMillis[attempt])
                        request(attempt + 1, expectedGeneration, action)
                    }
                }
            }
            finally { mutable.value=mutable.value.copy(loading=false) }
        }
    }
    private suspend fun loadViewed() {
        val selected=accepted(store.read())?.viewedStudy()?:return
        val fresh=loadStudy(selected)
        require(fresh.id==selected.id)
        persist(requestGeneration) { old -> requireNotNull(old).copy(studies=old.studies.map { if(it.id==fresh.id)fresh else it }) }
    }
    override suspend fun selectStudy(studyId:String) {
        edit { it.withViewedStudy(studyId) }
        // Room observers can lag behind this write; consult the committed selection.
        val selected=accepted(store.read())?.viewedStudy()?:return
        if(!selected.feesLoaded||!selected.scholarshipsLoaded||selected.courses.isEmpty()) request { loadViewed() }
    }
    override suspend fun setPreviousStudiesVisible(visible:Boolean) = edit { it.withPreviousStudies(visible) }
    override suspend fun setDisplayName(name:String) {
        val clean=name.trim();if(clean.length>100){mutable.value=mutable.value.copy(problem=StudentHubProblem.SAVE_FAILED);return}
        edit { it.copy(displayName=clean) }
    }
    private suspend fun edit(change:(StudentHubSnapshot)->StudentHubSnapshot) {
        try { persist { old->change(old?:StudentHubSnapshot(ownerKey=ownerKey())) };mutable.value=mutable.value.copy(problem=null) }
        catch(cancelled:CancellationException){throw cancelled}
        catch(_:Exception){mutable.value=mutable.value.copy(problem=StudentHubProblem.SAVE_FAILED)}
    }
    suspend fun clear() = writes.withLock { generation.incrementAndGet();retryJob?.cancel();retryJob=null;stableWrite { store.clear() };mutable.value=StudentHubState() }
}
