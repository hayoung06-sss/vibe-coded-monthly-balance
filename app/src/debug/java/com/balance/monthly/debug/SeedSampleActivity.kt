package com.balance.monthly.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.balance.monthly.BalanceApp
import com.balance.monthly.MainActivity
import com.balance.monthly.data.EntryRepository
import com.balance.monthly.domain.EntryType
import com.balance.monthly.domain.balanceAt
import com.balance.monthly.domain.computeTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.random.Random

/**
 * 디버그 빌드 전용. 기존 기록을 모두 지우고 최근 약 75일치 샘플 기록을 넣은 뒤 홈을 연다.
 * `adb shell am start -n com.balance.monthly/.debug.SeedSampleActivity`
 * 기록만 지우려면 `--ez clear true`를 붙인다.
 */
class SeedSampleActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val clearOnly = intent.getBooleanExtra("clear", false)
        val repository = (application as BalanceApp).repository
        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                repository.getAll().forEach { repository.delete(it.id) }
                if (!clearOnly) seed(repository, LocalDate.now())
            }
            startActivity(
                Intent(this@SeedSampleActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
            )
            finish()
        }
    }

    private suspend fun seed(repository: EntryRepository, today: LocalDate) {
        val random = Random(7)
        val start = today.minusDays(75)
        repository.add(EntryType.BALANCE, 1_200_000, start, "시작")

        var date = start.plusDays(1)
        while (date <= today) {
            if (date.dayOfMonth == 25) repository.add(EntryType.INCOME, 2_800_000, date, "월급")
            if (date.dayOfMonth == 1) repository.add(EntryType.EXPENSE, 650_000, date, "월세")
            repeat(random.nextInt(0, 3)) {
                repository.add(EntryType.EXPENSE, random.nextLong(3, 40) * 1_000, date, null)
            }
            // 기록이 빠진 지출을 실제 잔고로 바로잡는 날
            if (date == today.minusDays(40) || date == today.minusDays(9)) {
                val calculated = balanceAt(computeTimeline(repository.getAll()), date)
                repository.add(EntryType.BALANCE, calculated - 37_000, date, "잔고 확인")
            }
            date = date.plusDays(1)
        }
        // 미래 날짜 기록은 오늘까지의 그래프에 반영되지 않아야 한다
        repository.add(EntryType.EXPENSE, 120_000, today.plusDays(3), "보험료")
    }
}
