package com.balance.monthly.benchmark

import androidx.benchmark.macro.BaselineProfileMode
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 릴리스 빌드(benchmarkRelease)의 콜드 스타트 시간.
 * `./gradlew :benchmark:connectedBenchmarkReleaseAndroidTest`
 * timeToInitialDisplayMs가 TTID, timeToFullDisplayMs가 reportFullyDrawn까지(TTFD)다.
 */
@RunWith(AndroidJUnit4::class)
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /** 프로파일 적용 전: 사전 컴파일 없음 */
    @Test
    fun startupWithoutProfile() = startup(CompilationMode.None())

    /** 프로파일 적용 후: Baseline Profile로 시작 경로를 미리 컴파일 */
    @Test
    fun startupWithBaselineProfile() = startup(CompilationMode.Partial(BaselineProfileMode.Require))

    private fun startup(compilationMode: CompilationMode) = rule.measureRepeated(
        packageName = "com.balance.monthly",
        metrics = listOf(StartupTimingMetric()),
        compilationMode = compilationMode,
        startupMode = StartupMode.COLD,
        iterations = 10,
        setupBlock = {
            seedSampleDataOnce()
            pressHome()
        },
    ) {
        startActivityAndWait()
        device.wait(Until.hasObject(By.desc("잔고 그래프")), 5_000)
    }
}

private var seeded = false

/** 그래프가 있는 홈을 측정하도록 처음 한 번 샘플 기록을 넣는다(측정용 빌드에만 있는 화면). 기존 기록은 지워진다. */
private fun MacrobenchmarkScope.seedSampleDataOnce() {
    if (seeded) return
    device.executeShellCommand("am start -W -n com.balance.monthly/.debug.SeedSampleActivity")
    device.wait(Until.hasObject(By.desc("잔고 그래프")), 15_000)
    seeded = true
}
