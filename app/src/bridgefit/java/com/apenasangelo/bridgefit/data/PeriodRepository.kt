package com.apenasangelo.bridgefit.data

import com.apenasangelo.bridgefit.ui.Metric
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import nodomain.freeyourgadget.gadgetbridge.GBApplication
import nodomain.freeyourgadget.gadgetbridge.impl.GBDevice
import nodomain.freeyourgadget.gadgetbridge.activities.HeartRateUtils
import nodomain.freeyourgadget.gadgetbridge.widgets.WidgetVariant
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.ActiveCaloriesWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.ActiveTimeWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.DistanceWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.SleepWidget
import nodomain.freeyourgadget.gadgetbridge.widgets.impl.StepsWidget
import org.slf4j.LoggerFactory
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

/**
 * The time span of a detail chart: D, W, M, Y.
 */
enum class Period { DAY, WEEK, MONTH, YEAR }

/**
 * One day of a metric: its value (a total or an average) and, for ranged metrics, the day's low and high.
 * [value] is null when the day has no data.
 */
data class DailyPoint(val day: LocalDate, val value: Float?, val low: Float? = null, val high: Float? = null)

/**
 * One bar of a period chart: a day for W and M, a month for Y.
 */
data class PeriodBar(val start: LocalDate, val value: Float?, val low: Float? = null, val high: Float? = null)

/**
 * The bars of a period and the figure shown above them (the daily average over the days with data).
 */
data class PeriodSeries(
    val period: Period,
    val from: LocalDate,
    val to: LocalDate,
    val bars: List<PeriodBar>,
    val average: Float?,
    val low: Float?,
    val high: Float?,
)

/**
 * Daily values per metric, for the W/M/Y charts and the Trends card.
 *
 * Totals (steps, distance, move, exercise, sleep) reuse the upstream dashboard widgets day by day, like the upstream
 * period charts do. Sparse metrics (SpO₂, stress, temperature, HRV, heart rate) are read with one range query per
 * device and bucketed by day. Results are cached per (metric, day); [invalidateRecent] drops the days a sync can
 * still change.
 */
object PeriodRepository {
    private val LOG = LoggerFactory.getLogger(PeriodRepository::class.java)
    private val cache = ConcurrentHashMap<Pair<Metric, LocalDate>, DailyPoint>()
    private val limiter = Semaphore(4)
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /**
     * Forgets the last [days] days, which a sync may have backfilled.
     */
    fun invalidateRecent(days: Long = 7) {
        val from = LocalDate.now().minusDays(days)
        cache.keys.removeAll { it.second >= from }
    }

    suspend fun series(metric: Metric, period: Period, day: LocalDate): PeriodSeries {
        val (from, to) = range(period, day)
        val points = daily(metric, from, to)
        val bars = if (period == Period.YEAR) {
            points.groupBy { it.day.withDayOfMonth(1) }.toSortedMap().map { (month, days) ->
                val known = days.filter { it.value != null }
                PeriodBar(
                    month,
                    known.map { it.value!! }.average().toFloat().takeIf { known.isNotEmpty() },
                    known.mapNotNull { it.low }.minOrNull(),
                    known.mapNotNull { it.high }.maxOrNull(),
                )
            }
        } else {
            points.map { PeriodBar(it.day, it.value, it.low, it.high) }
        }
        val known = points.mapNotNull { it.value }
        return PeriodSeries(
            period, from, to, bars,
            average = if (known.isEmpty()) null else known.average().toFloat(),
            low = points.mapNotNull { it.low ?: it.value }.minOrNull(),
            high = points.mapNotNull { it.high ?: it.value }.maxOrNull(),
        )
    }

    /**
     * The W (Monday-based week), M (calendar month) or Y (last 12 months) range that contains [day].
     */
    fun range(period: Period, day: LocalDate): Pair<LocalDate, LocalDate> = when (period) {
        Period.DAY -> day to day
        Period.WEEK -> {
            val start = day.minusDays((day.dayOfWeek.value - 1).toLong())
            start to start.plusDays(6)
        }
        Period.MONTH -> day.withDayOfMonth(1) to day.withDayOfMonth(day.lengthOfMonth())
        Period.YEAR -> day.withDayOfMonth(1).minusMonths(11) to day.withDayOfMonth(day.lengthOfMonth())
    }

    /**
     * The daily points of [metric] from [from] to [to], inclusive. Future days are returned without data.
     */
    suspend fun daily(metric: Metric, from: LocalDate, to: LocalDate): List<DailyPoint> {
        val today = LocalDate.now()
        val days = (0..ChronoUnit.DAYS.between(from, to)).map { from.plusDays(it) }
        val missing = days.filter { it <= today && !cache.containsKey(metric to it) }
        if (missing.isNotEmpty()) {
            when (metric) {
                Metric.ACTIVITY, Metric.STEPS, Metric.DISTANCE, Metric.SLEEP -> loadTotals(metric, missing)
                Metric.EXERCISE -> loadTotals(metric, missing)
                else -> loadSparse(metric, missing.min(), missing.max())
            }
        }
        return days.map { cache[metric to it] ?: DailyPoint(it, null) }
    }

    private suspend fun loadTotals(metric: Metric, days: List<LocalDate>) = coroutineScope {
        days.map { day ->
            async {
                limiter.withPermit {
                    val value: Float? = when (metric) {
                        Metric.STEPS -> FitRepository.load(StepsWidget, day, WidgetVariant.GAUGE)?.value?.total?.toFloat()
                        Metric.DISTANCE -> FitRepository.load(DistanceWidget, day, WidgetVariant.GAUGE)?.value?.total
                        Metric.ACTIVITY -> FitRepository.load(ActiveCaloriesWidget, day, WidgetVariant.GAUGE)?.value?.active?.toFloat()
                        Metric.EXERCISE -> FitRepository.load(ActiveTimeWidget, day, WidgetVariant.GAUGE)?.value?.total?.toFloat()
                        Metric.SLEEP -> FitRepository.load(SleepWidget, day, WidgetVariant.GAUGE)?.value?.total?.toFloat()
                        else -> null
                    }
                    cache[metric to day] = DailyPoint(day, value?.takeIf { it > 0f })
                }
            }
        }.awaitAll()
    }

    /**
     * Reads every sample of [metric] between [from] and [to] in one query per device, and buckets them by day.
     */
    private suspend fun loadSparse(metric: Metric, from: LocalDate, to: LocalDate) = withContext(Dispatchers.IO) {
        val fromSec = from.atStartOfDay(zone).toEpochSecond()
        val toSec = to.plusDays(1).atStartOfDay(zone).toEpochSecond() - 1
        val samples = mutableListOf<Pair<Long, Float>>()
        try {
            GBApplication.acquireDbReadOnly().use { db ->
                val session = db.daoSession
                for (dev in FitRepository.devices()) {
                    val c = dev.deviceCoordinator
                    when (metric) {
                        Metric.SPO2 -> c.getSpo2SampleProvider(dev, session)?.getAllSamples(fromSec * 1000, toSec * 1000)
                            ?.filter { it.spo2 in 50..100 }?.mapTo(samples) { it.timestamp / 1000 to it.spo2.toFloat() }

                        Metric.STRESS -> c.getStressSampleProvider(dev, session)?.getAllSamples(fromSec * 1000, toSec * 1000)
                            ?.filter { it.stress in 1..100 }?.mapTo(samples) { it.timestamp / 1000 to it.stress.toFloat() }

                        Metric.TEMPERATURE -> c.getTemperatureSampleProvider(dev, session)?.getAllSamples(fromSec * 1000, toSec * 1000)
                            ?.filter { it.temperature > 0f }?.mapTo(samples) { it.timestamp / 1000 to it.temperature }

                        Metric.HRV -> {
                            val summaries = c.getHrvSummarySampleProvider(dev, session)?.getAllSamples(fromSec * 1000, toSec * 1000)
                                ?.mapNotNull { s -> s.lastNightAverage?.takeIf { it > 0 }?.let { s.timestamp / 1000 to it.toFloat() } }
                                .orEmpty()
                            if (summaries.isNotEmpty()) {
                                samples += summaries
                            } else {
                                c.getHrvValueSampleProvider(dev, session)?.getAllSamples(fromSec * 1000, toSec * 1000)
                                    ?.filter { it.value > 0 }?.mapTo(samples) { it.timestamp / 1000 to it.value.toFloat() }
                            }
                        }

                        Metric.HEART_RATE -> heartRates(dev, db, fromSec, toSec, samples)
                        else -> Unit
                    }
                }
            }
        } catch (e: Exception) {
            LOG.error("Could not load {} from {} to {}", metric, from, to, e)
        }
        val byDay = samples.groupBy { java.time.Instant.ofEpochSecond(it.first).atZone(zone).toLocalDate() }
        var day = from
        while (!day.isAfter(to)) {
            val values = byDay[day]?.map { it.second }
            cache[metric to day] = if (values.isNullOrEmpty()) {
                DailyPoint(day, null)
            } else {
                DailyPoint(day, values.average().toFloat(), values.min(), values.max())
            }
            day = day.plusDays(1)
        }
    }

    private fun heartRates(
        dev: GBDevice,
        db: nodomain.freeyourgadget.gadgetbridge.database.DBHandler,
        fromSec: Long,
        toSec: Long,
        out: MutableList<Pair<Long, Float>>,
    ) {
        if (!dev.deviceCoordinator.supportsHeartRateMeasurement(dev)) return
        val provider = dev.deviceCoordinator.getSampleProvider(dev, db.daoSession) ?: return
        val utils = HeartRateUtils.getInstance()
        provider.getAllActivitySamples(fromSec.toInt(), toSec.toInt())
            .filter { utils.isValidHeartRateValue(it.heartRate) }
            .mapTo(out) { it.timestamp.toLong() to it.heartRate.toFloat() }
    }
}
