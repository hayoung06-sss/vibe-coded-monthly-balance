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
        firstIndex == other.firstIndex &&
        markers.contentEquals(other.markers) &&
        summary == other.summary

    override fun hashCode(): Int = arrayOf<Any>(
        dataVersion, writtenOn, monthStartDay, balance, period,
        dailyBalances.contentHashCode(), firstIndex, markers.contentHashCode(), summary,
    ).contentHashCode()
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
        HomeSnapshot(dataVersion, writtenOn, monthStartDay, balance, period, daily, firstIndex, markers, summary)
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
        private const val HOME_FORMAT: Byte = 1
    }
}
