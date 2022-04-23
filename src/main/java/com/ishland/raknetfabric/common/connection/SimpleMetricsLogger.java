package com.ishland.raknetfabric.common.connection;

import com.ishland.raknetfabric.common.util.MathUtil;
import network.ycc.raknet.RakNet;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * This implementation is only designed to be modified single-threaded
 */
@SuppressWarnings("NonAtomicOperationOnVolatileField")
public class SimpleMetricsLogger implements RakNet.MetricsLogger {

    // ========== Loggers ==========

    private volatile long packetsIn = 0L;
    private volatile long framesIn = 0L;
    private volatile long framesError = 0L;
    private volatile long bytesIn = 0L;
    private volatile long packetsOut = 0L;
    private volatile long framesOut = 0L;
    private volatile long bytesOut = 0L;
    private volatile long bytesRecalled = 0L;
    private volatile long bytesACKd = 0L;
    private volatile long bytesNACKd = 0L;
    private volatile long acksSent = 0L;
    private volatile long nacksSent = 0L;

    private volatile long measureRTTns = 0L;
    private volatile long measureRTTnsStdDev = 0L;
    private volatile long measureBurstTokens = 0L;
    private volatile int currentQueuedBytes = 0;

    @Override
    public void packetsIn(int delta) {
        packetsIn += delta;
    }

    @Override
    public void framesIn(int delta) {
        framesIn += delta;
    }

    @Override
    public void frameError(int delta) {
        framesError += delta;
    }

    @Override
    public void bytesIn(int delta) {
        bytesIn += delta;
    }

    @Override
    public void packetsOut(int delta) {
        packetsOut += delta;
    }

    @Override
    public void framesOut(int delta) {
        framesOut += delta;
    }

    @Override
    public void bytesOut(int delta) {
        bytesOut += delta;
        tick();
    }

    @Override
    public void bytesRecalled(int delta) {
        bytesRecalled += delta;
    }

    @Override
    public void bytesACKd(int delta) {
        bytesACKd += delta;
    }

    @Override
    public void bytesNACKd(int delta) {
        bytesNACKd += delta;
    }

    @Override
    public void acksSent(int delta) {
        acksSent += delta;
    }

    @Override
    public void nacksSent(int delta) {
        nacksSent += delta;
    }

    @Override
    public void measureRTTns(long n) {
        measureRTTns = n;
    }

    @Override
    public void measureRTTnsStdDev(long n) {
        measureRTTnsStdDev = n;
        tick();
    }

    @Override
    public void measureBurstTokens(int n) {
        measureBurstTokens = n;
    }

    @Override
    public void currentQueuedBytes(int bytes) {
        currentQueuedBytes = bytes;
    }

    // ========== Calculations ==========

    private long lastMeasureMillis = System.currentTimeMillis();

    private synchronized void tick() {
        final long measureMillis = System.currentTimeMillis();
        final long deltaTime = measureMillis - lastMeasureMillis;
        if (deltaTime < 990) return; // throttle
        this.lastMeasureMillis = measureMillis;

        tickErrorRate();
        tickRXTX(deltaTime);
    }

    private final DescriptiveStatistics errorStats = new DescriptiveStatistics(16);
    private long lastBytesTotal = 0L;
    private long lastBytesRecalled = 0L;
    private volatile double measureErrorRate = 0.0D;

    private void tickErrorRate() {
        final long bytesTotal = this.bytesIn + this.bytesOut;
        final long bytesRecalled = this.bytesRecalled;

        final long bytesTotalDelta = bytesTotal - lastBytesTotal;
        final long bytesRecalledDelta = bytesRecalled - this.lastBytesRecalled;

        if (bytesTotalDelta != 0) {
            this.errorStats.addValue(bytesRecalledDelta / (double) bytesTotalDelta);
            this.measureErrorRate = this.errorStats.getMean();
        }

        this.lastBytesTotal = bytesTotal;
        this.lastBytesRecalled = bytesRecalled;
    }

    private final DescriptiveStatistics rxStats = new DescriptiveStatistics(8);
    private final DescriptiveStatistics txStats = new DescriptiveStatistics(8);
    private long lastPacketsIn = 0L;
    private long lastPacketsOut = 0L;
    private long lastBytesIn = 0L;
    private long lastBytesOut = 0L;
    private volatile int measureRX = 0;
    private volatile int measureTX = 0;
    private volatile long measureBytesInRate = 0;
    private volatile long measureBytesOutRate = 0;
    private volatile String measureTrafficInFormatted = "...";
    private volatile String measureTrafficOutFormatted = "...";

    private void tickRXTX(long deltaTime) {

        final long packetsIn = this.packetsIn;
        final long packetsOut = this.packetsOut;
        final long bytesIn = this.bytesIn;
        final long bytesOut = this.bytesOut;

        final double timeDeltaS = deltaTime / 1000.0;

        this.rxStats.addValue((packetsIn - this.lastPacketsIn) / timeDeltaS);
        this.txStats.addValue((packetsOut - this.lastPacketsOut) / timeDeltaS);

        this.measureRX = (int) this.rxStats.getMean();
        this.measureTX = (int) this.txStats.getMean();

        this.measureBytesInRate = (long) ((bytesIn - this.lastBytesIn) / timeDeltaS);
        this.measureBytesOutRate = (long) ((bytesOut - this.lastBytesOut) / timeDeltaS);

        this.measureTrafficInFormatted = MathUtil.humanReadableByteCountBin(this.measureBytesInRate) + "/s";
        this.measureTrafficOutFormatted = MathUtil.humanReadableByteCountBin(this.measureBytesOutRate) + "/s";

        this.lastPacketsIn = packetsIn;
        this.lastPacketsOut = packetsOut;
        this.lastBytesIn = bytesIn;
        this.lastBytesOut = bytesOut;
    }

    // ========== Getters ==========

    public long getMeasureRTTns() {
        return measureRTTns;
    }

    public long getMeasureRTTnsStdDev() {
        return measureRTTnsStdDev;
    }

    public double getMeasureErrorRate() {
        return measureErrorRate;
    }

    public int getMeasureRX() {
        return measureRX;
    }

    public int getMeasureTX() {
        return measureTX;
    }

    public int getCurrentQueuedBytes() {
        return currentQueuedBytes;
    }

    public long getMeasureBurstTokens() {
        return measureBurstTokens;
    }

    public long getMeasureBytesInRate() {
        return measureBytesInRate;
    }

    public long getMeasureBytesOutRate() {
        return measureBytesOutRate;
    }

    public String getMeasureTrafficInFormatted() {
        return measureTrafficInFormatted;
    }

    public String getMeasureTrafficOutFormatted() {
        return measureTrafficOutFormatted;
    }

    // ========== Misc ==========

    private MetricsSynchronizationHandler metricsSynchronizationHandler;

    public MetricsSynchronizationHandler getMetricsSynchronizationHandler() {
        return this.metricsSynchronizationHandler;
    }

    public void setMetricsSynchronizationHandler(MetricsSynchronizationHandler metricsSynchronizationHandler) {
        this.metricsSynchronizationHandler = metricsSynchronizationHandler;
    }
}
