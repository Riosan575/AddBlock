package bendev.addblock.core.di

import android.content.Context
import bendev.addblock.feature.allowlist.data.AllowlistRepositoryImpl
import bendev.addblock.feature.allowlist.domain.repository.AllowlistRepository
import bendev.addblock.feature.allowlist.domain.usecase.AddToAllowlistUseCase
import bendev.addblock.feature.allowlist.domain.usecase.ObserveAllowlistUseCase
import bendev.addblock.feature.allowlist.domain.usecase.RemoveFromAllowlistUseCase
import bendev.addblock.feature.blocklist.data.BlockListRepositoryImpl
import bendev.addblock.feature.blocklist.data.local.HostsLocalDataSource
import bendev.addblock.feature.blocklist.data.remote.HostsRemoteDataSource
import bendev.addblock.feature.blocklist.domain.repository.BlockListRepository
import bendev.addblock.feature.blocklist.domain.usecase.EnsureBlockListReadyUseCase
import bendev.addblock.feature.blocklist.domain.usecase.LoadBlockListUseCase
import bendev.addblock.feature.blocklist.domain.usecase.ObserveBlockListUseCase
import bendev.addblock.feature.blocklist.domain.usecase.ToggleHostSourceUseCase
import bendev.addblock.feature.blocklist.domain.usecase.UpdateBlockListsUseCase
import bendev.addblock.feature.protection.data.ProtectionRepositoryImpl
import bendev.addblock.feature.protection.data.ProtectionStateHolder
import bendev.addblock.feature.protection.domain.repository.ProtectionRepository
import bendev.addblock.feature.protection.domain.usecase.CheckPrivateDnsUseCase
import bendev.addblock.feature.protection.domain.usecase.ObserveProtectionUseCase
import bendev.addblock.feature.protection.domain.usecase.ShouldBlockDomainUseCase
import bendev.addblock.feature.protection.domain.usecase.StartProtectionUseCase
import bendev.addblock.feature.protection.domain.usecase.StopProtectionUseCase
import bendev.addblock.feature.querylog.data.InMemoryQueryLogRepository
import bendev.addblock.feature.querylog.domain.repository.QueryLogRepository
import bendev.addblock.feature.querylog.domain.usecase.ClearQueryLogUseCase
import bendev.addblock.feature.querylog.domain.usecase.ObserveQueryStatsUseCase
import bendev.addblock.feature.querylog.domain.usecase.ObserveQueryLogUseCase
import bendev.addblock.feature.querylog.domain.usecase.RecordQueryUseCase

/** Manual dependency injection: one instance per app process. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    // Repositories
    private val blockListRepository: BlockListRepository =
        BlockListRepositoryImpl(HostsLocalDataSource(appContext), HostsRemoteDataSource())
    private val allowlistRepository: AllowlistRepository = AllowlistRepositoryImpl(appContext)
    private val queryLogRepository: QueryLogRepository = InMemoryQueryLogRepository()
    val protectionStateHolder = ProtectionStateHolder()
    private val protectionRepository: ProtectionRepository =
        ProtectionRepositoryImpl(appContext, protectionStateHolder)

    // Blocklist
    val observeBlockList = ObserveBlockListUseCase(blockListRepository)
    val loadBlockList = LoadBlockListUseCase(blockListRepository)
    val updateBlockLists = UpdateBlockListsUseCase(blockListRepository)
    val toggleHostSource = ToggleHostSourceUseCase(blockListRepository)
    val ensureBlockListReady = EnsureBlockListReadyUseCase(blockListRepository)

    // Allowlist
    val observeAllowlist = ObserveAllowlistUseCase(allowlistRepository)
    val addToAllowlist = AddToAllowlistUseCase(allowlistRepository)
    val removeFromAllowlist = RemoveFromAllowlistUseCase(allowlistRepository)

    // Query log
    val observeQueryLog = ObserveQueryLogUseCase(queryLogRepository)
    val observeQueryStats = ObserveQueryStatsUseCase(queryLogRepository)
    val recordQuery = RecordQueryUseCase(queryLogRepository)
    val clearQueryLog = ClearQueryLogUseCase(queryLogRepository)

    // Protection
    val observeProtection = ObserveProtectionUseCase(protectionRepository)
    val startProtection = StartProtectionUseCase(protectionRepository)
    val stopProtection = StopProtectionUseCase(protectionRepository)
    val checkPrivateDns = CheckPrivateDnsUseCase(protectionRepository)
    val shouldBlockDomain = ShouldBlockDomainUseCase(blockListRepository, allowlistRepository)
}
