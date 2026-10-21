package com.balance.monthly

import android.app.Application
import com.balance.monthly.data.AppDatabase
import com.balance.monthly.data.EntryRepository
import com.balance.monthly.data.ForecastManager
import com.balance.monthly.data.Settings
import com.balance.monthly.data.SnapshotStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

/** onCreate에서는 아무것도 초기화하지 않는다. 의존성은 처음 쓸 때 만든다. */
class BalanceApp : Application() {
    val settings: Settings by lazy { Settings.create(this) }

    val snapshotStore: SnapshotStore by lazy { SnapshotStore(filesDir) }

    /** Room을 열므로 메인 스레드에서 처음 접근하지 않는다. */
    val repository: EntryRepository by lazy {
        EntryRepository(AppDatabase.create(this).entryDao(), settings)
    }

    /** 앱이 살아 있는 동안의 백그라운드 작업용. */
    private val appScope by lazy { CoroutineScope(SupervisorJob()) }

    /** 만들 때 forecast.bin(1KB 미만)을 읽어, 캐시가 유효하면 첫 화면부터 예측선을 보여준다. */
    val forecastManager: ForecastManager by lazy {
        ForecastManager(appScope, { repository }, settings, { snapshotStore }, snapshotStore.readForecast())
    }
}
