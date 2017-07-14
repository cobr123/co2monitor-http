package local.co2monitor.http;

/**
 * Created by cobr123 on 14.07.2017.
 */
public final class Data {
    private final long timeInMillis;
    private final double co2ppm;
    private final double temperature;

    public Data(final long timeInMillis, final double co2ppm, final double temperature) {
        this.timeInMillis = timeInMillis;
        this.co2ppm = co2ppm;
        this.temperature = temperature;
    }

    public long getTimeInMillis() {
        return timeInMillis;
    }

    public double getCO2Ppm() {
        return co2ppm;
    }

    public double getTemperature() {
        return temperature;
    }
}
