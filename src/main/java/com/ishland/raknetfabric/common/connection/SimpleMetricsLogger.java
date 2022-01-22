package com.ishland.raknetfabric.common.connection;

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

    // ========== Calculations ==========

    private synchronized void tick() {
        tickErrorRate();
        tickRXTX();
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
    private long lastFramesIn = 0L;
    private long lastFramesOut = 0L;
    private long lastMeasureMillis = System.currentTimeMillis();
    private volatile int measureRX = 0;
    private volatile int measureTX = 0;

    private void tickRXTX() {
        final long framesIn = this.framesIn;
        final long framesOut = this.framesOut;
        final long measureMillis = System.currentTimeMillis();

        final long deltaTime = measureMillis - lastMeasureMillis;
        if (deltaTime < 980) return; // throttle

        final double timeDeltaS = deltaTime / 1000.0;

        this.rxStats.addValue((framesIn - this.lastFramesIn) / timeDeltaS);
        this.txStats.addValue((framesOut - this.lastFramesOut) / timeDeltaS);

        this.measureRX = (int) this.rxStats.getMean();
        this.measureTX = (int) this.txStats.getMean();

        this.lastFramesIn = framesIn;
        this.lastFramesOut = framesOut;
        this.lastMeasureMillis = measureMillis;
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
}
