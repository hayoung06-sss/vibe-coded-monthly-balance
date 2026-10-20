package com.balance.monthly.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * `./gradlew :app:generateBaselineProfile`로 실행한다. 결과는 app/src/release/generated/baselineProfiles에 저장된다.
 * 기록이 있어야 그래프 경로까지 수집되므로 시작 전에 샘플 기록을 넣는다. 기기의 기존 기록은 지워진다.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    /** 콜드 스타트부터 홈이 그려질 때까지. Startup Profile(dex 배치)에도 넣는다. */
    @Test
    fun startup() = rule.collect(PACKAGE_NAME, includeInStartupProfile = true) {
        seedSampleDataOnce()
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.desc("잔고 그래프")), 5_000)
    }

    /** 탭 전환과 입력 시트. 첫 실행 이후 경로라 Baseline Profile에만 넣는다. */
    @Test
    fun journeys() = rule.collect(PACKAGE_NAME) {
        seedSampleDataOnce()
        pressHome()
        startActivityAndWait()
        device.wait(Until.hasObject(By.desc("잔고 그래프")), 5_000)
        device.findObject(By.desc("잔고 그래프"))?.click()
        device.findObject(By.text("기록"))?.click()
        device.waitForIdle()
        device.findObject(By.desc("기록 추가"))?.click()
        device.wait(Until.hasObject(By.text("저장")), 2_000)
        device.pressBack()
        device.pressBack()
        device.findObject(By.text("설정"))?.click()
        device.waitForIdle()
        device.findObject(By.text("홈"))?.click()
        device.waitForIdle()
    }
}

private const val PACKAGE_NAME = "com.balance.monthly"

private var seeded = false

/** 그래프 경로까지 수집되도록 처음 한 번 샘플 기록을 넣는다(측정용 빌드에만 있는 화면). */
private fun MacrobenchmarkScope.seedSampleDataOnce() {
    if (seeded) return
    device.executeShellCommand("am start -W -n $PACKAGE_NAME/.debug.SeedSampleActivity")
    device.wait(Until.hasObject(By.desc("잔고 그래프")), 15_000)
    seeded = true
}
