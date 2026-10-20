package com.balance.monthly

import android.app.Application
import com.balance.monthly.data.AppDatabase
import com.balance.monthly.data.EntryRepository
import com.balance.monthly.data.Settings
import com.balance.monthly.data.SnapshotStore

/** onCreate에서는 아무것도 초기화하지 않는다. 의존성은 처음 쓸 때 만든다. */
class BalanceApp : Application() {
    val settings: Settings by lazy { Settings.create(this) }

    val snapshotStore: SnapshotStore by lazy { SnapshotStore(filesDir) }

    /** Room을 열므로 메인 스레드에서 처음 접근하지 않는다. */
    val repository: EntryRepository by lazy {
        EntryRepository(AppDatabase.create(this).entryDao(), settings)
    }
}
