package com.balance.monthly.data

import com.balance.monthly.domain.Period
import com.balance.monthly.domain.PeriodSummary
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.time.LocalDate

/**
 * 홈 첫 프레임을 Room 없이 그리기 위한 값. 이번 기간 기준으로 저장한다.
 * 키(dataVersion, writtenOn, monthStartDay)로 최신인지 판단한다.
 */
class HomeSnapshot(
    val dataVersion: Long,
    val writtenOn: LocalDate,
    val monthStartDay: Int,
    val balance: Long,
    val period: Period,
    val dailyBalances: LongArray,
    /** 그래프 선용 잔고(기록한 날 사이를 직선으로 이은 값) */
    val lineBalances: LongArray,
    /** 첫 기록 이전이라 그리지 않는 날의 수 */
    val firstIndex: Int,
    /** 실제 잔고를 입력한 날의 기간 내 인덱스 */
    val markers: IntArray,
    val summary: PeriodSummary,
) {
    override fun equals(other: Any?): Boolean = other is HomeSnapshot &&
        dataVersion == other.dataVersion &&
        writtenOn == other.writtenOn &&
        monthStartDay == other.monthStartDay &&
        balance == other.balance &&
        period == other.period &&
        dailyBalances.contentEquals(other.dailyBalances) &&
        lineBalances.contentEquals(other.lineBalances) &&
        firstIndex == other.firstIndex &&
        markers.contentEquals(other.markers) &&
        summary == other.summary

    override fun hashCode(): Int = arrayOf<Any>(
        dataVersion, writtenOn, monthStartDay, balance, period,
        dailyBalances.contentHashCode(), lineBalances.contentHashCode(), firstIndex, markers.contentHashCode(), summary,
    ).contentHashCode()
}

/**
 * 계산해 둔 예측 잔고. [balances]의 0번이 [computedOn](오늘)이고 기간 마지막 날까지 이어진다.
 * 키(dataVersion, computedOn, monthStartDay, algorithmVersion)가 현재 상태와 같을 때만 쓴다.
 */
class CachedForecast(
    val dataVersion: Long,
    val computedOn: LocalDate,
    val monthStartDay: Int,
    val algorithmVersion: Int,
    val balances: LongArray,
) {
    override fun equals(other: Any?): Boolean = other is CachedForecast &&
        dataVersion == other.dataVersion &&
        computedOn == other.computedOn &&
        monthStartDay == other.monthStartDay &&
        algorithmVersion == other.algorithmVersion &&
        balances.contentEquals(other.balances)

    override fun hashCode(): Int =
        arrayOf<Any>(dataVersion, computedOn, monthStartDay, algorithmVersion, balances.contentHashCode()).contentHashCode()
}

/**
 * filesDir의 캐시 파일 읽기·쓰기. DataOutputStream으로 직접 쓴 바이너리이고 맨 앞에 포맷 버전 바이트를 둔다.
 * 읽기가 실패하거나 버전이 다르면 파일을 지우고 없는 것으로 취급한다. 지워도 다시 만들어진다.
 */
class SnapshotStore(private val dir: File) {

    private val homeFile get() = File(dir, HOME_FILE)

    fun readHome(): HomeSnapshot? = read(homeFile) { input ->
        if (input.readByte() != HOME_FORMAT) return@read null
        val dataVersion = input.readLong()
        val writtenOn = LocalDate.ofEpochDay(input.readLong())
        val monthStartDay = input.readByte().toInt()
        val balance = input.readLong()
        val period = Period(LocalDate.ofEpochDay(input.readLong()), LocalDate.ofEpochDay(input.readLong()))
        val daily = LongArray(input.readUnsignedShort()) { input.readLong() }
        val line = LongArray(daily.size) { input.readLong() }
        val firstIndex = input.readUnsignedShort()
        val markers = IntArray(input.readUnsignedShort()) { input.readUnsignedShort() }
        val summary = PeriodSummary(
            startBalance = input.readLong(),
            endBalance = input.readLong(),
            income = input.readLong(),
            expense = input.readLong(),
            diff = input.readLong(),
            opening = input.readLong(),
        )
        HomeSnapshot(dataVersion, writtenOn, monthStartDay, balance, period, daily, line, firstIndex, markers, summary)
    }

    fun writeHome(snapshot: HomeSnapshot) = write(homeFile) { out ->
        out.writeByte(HOME_FORMAT.toInt())
        out.writeLong(snapshot.dataVersion)
        out.writeLong(snapshot.writtenOn.toEpochDay())
        out.writeByte(snapshot.monthStartDay)
        out.writeLong(snapshot.balance)
        out.writeLong(snapshot.period.start.toEpochDay())
        out.writeLong(snapshot.period.end.toEpochDay())
        out.writeShort(snapshot.dailyBalances.size)
        snapshot.dailyBalances.forEach(out::writeLong)
        snapshot.lineBalances.forEach(out::writeLong)
        out.writeShort(snapshot.firstIndex)
        out.writeShort(snapshot.markers.size)
        snapshot.markers.forEach(out::writeShort)
        with(snapshot.summary) {
            out.writeLong(startBalance)
            out.writeLong(endBalance)
            out.writeLong(income)
            out.writeLong(expense)
            out.writeLong(diff)
            out.writeLong(opening)
        }
    }

    fun clearHome() {
        homeFile.delete()
    }

    private val forecastFile get() = File(dir, FORECAST_FILE)

    fun readForecast(): CachedForecast? = read(forecastFile) { input ->
        if (input.readByte() != FORECAST_FORMAT) return@read null
        CachedForecast(
            dataVersion = input.readLong(),
            computedOn = LocalDate.ofEpochDay(input.readLong()),
            monthStartDay = input.readByte().toInt(),
            algorithmVersion = input.readUnsignedShort(),
            balances = LongArray(input.readUnsignedShort()) { input.readLong() },
        )
    }

    fun writeForecast(forecast: CachedForecast) = write(forecastFile) { out ->
        out.writeByte(FORECAST_FORMAT.toInt())
        out.writeLong(forecast.dataVersion)
        out.writeLong(forecast.computedOn.toEpochDay())
        out.writeByte(forecast.monthStartDay)
        out.writeShort(forecast.algorithmVersion)
        out.writeShort(forecast.balances.size)
        forecast.balances.forEach(out::writeLong)
    }

    fun clearForecast() {
        forecastFile.delete()
    }

    private fun <T> read(file: File, block: (DataInputStream) -> T?): T? {
        if (!file.exists()) return null
        val result = runCatching { DataInputStream(file.inputStream().buffered()).use(block) }.getOrNull()
        if (result == null) file.delete()
        return result
    }

    /** 임시 파일에 쓴 뒤 바꿔치기해서, 쓰다 죽어도 반쯤 쓴 파일이 남지 않게 한다. */
    private fun write(file: File, block: (DataOutputStream) -> Unit) {
        val temp = File(file.parentFile, file.name + ".tmp")
        DataOutputStream(temp.outputStream().buffered()).use(block)
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }

    companion object {
        const val HOME_FILE = "home_snapshot.bin"
        const val FORECAST_FILE = "forecast.bin"
        private const val HOME_FORMAT: Byte = 2
        private const val FORECAST_FORMAT: Byte = 1
    }
}
