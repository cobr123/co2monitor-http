package local.co2monitor.http;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by cobr123 on 05.04.2017.
 */
public class CO2MonitorDataHttpHandler implements HttpHandler {
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final int STATUS_OK = 200;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final DateFormat dateTimeFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
    private static final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    public void handle(final HttpExchange httpExchange) throws IOException {
        try {
            final String response = getData(queryToMap(httpExchange.getRequestURI().getQuery()));
            httpExchange.getResponseHeaders().set(HEADER_CONTENT_TYPE, String.format("application/json; charset=%s", CHARSET));
            final byte[] rawResponseBody = response.getBytes(CHARSET);
            httpExchange.sendResponseHeaders(STATUS_OK, rawResponseBody.length);
            httpExchange.getResponseBody().write(rawResponseBody);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            httpExchange.close();
        }
    }

    private Map<String, String> queryToMap(final String query) {
        final Map<String, String> result = new HashMap<>();
        if (query != null) {
            for (final String param : query.split("&")) {
                final String pair[] = param.split("=");
                if (pair.length > 1) {
                    result.put(pair[0], pair[1]);
                } else {
                    result.put(pair[0], "");
                }
            }
        }
        return result;
    }

    private static String nvl(final String value, final String defValue) {
        if (value == null || value.isEmpty()) {
            return defValue;
        } else {
            return value;
        }
    }

    private static long getStepInSeconds(final long range) {
        if (range / 1000 < 24 * 60 * 60) {
            return 1;
        } else if (range / 1000 < 2 * 24 * 60 * 60) {
            return 60;
        } else if (range / 1000 < 14 * 24 * 60 * 60) {
            return 5 * 60;
        } else if (range / 1000 < 31 * 24 * 60 * 60) {
            return 10 * 60;
        } else {
            return 15 * 60;
        }
    }

    private static Date getZeroTimeDate(final Date date) {
        final Calendar calendar = Calendar.getInstance();

        calendar.setTime(date);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTime();
    }

    private static boolean isWithinRange(final Date date, final long startTimeInMillis, final long endTimeInMillis) {
        return !(date.before(getZeroTimeDate(new Date(startTimeInMillis))) || date.after(getZeroTimeDate(new Date(endTimeInMillis))));
    }

    private static String getData(final Map<String, String> queryParams) {
        final long startTimeInMillis = Long.parseLong(queryParams.getOrDefault("start", "0"));
        final long endTimeInMillis = Long.parseLong(queryParams.getOrDefault("end", "" + Calendar.getInstance().getTimeInMillis()));
        final long range = endTimeInMillis - startTimeInMillis;
        final long stepInSeconds = getStepInSeconds(range);

        final StringBuilder sb = new StringBuilder();
        final String loggerDir = nvl(System.getenv("co2mini-data-logger"), "d:\\co2mini-data-logger\\");
        final File dir = new File(loggerDir);
        final File[] yearDirs = dir.listFiles();
        final Calendar cal = Calendar.getInstance();

        sb.append("[");
        if (yearDirs != null) {
            for (final File yearDir : yearDirs) {
                if (yearDir.isDirectory()) {
                    final File[] monthDirs = yearDir.listFiles();
                    if (monthDirs != null) {
                        for (final File monthDir : monthDirs) {
                            if (monthDir.isDirectory()) {
                                final File[] dayFiles = monthDir.listFiles();
                                if (dayFiles != null) {
                                    for (final File dayFile : dayFiles) {
                                        if (dayFile.isFile()) {
                                            if (dayFile.getName().endsWith(".CSV")) {
                                                try {
                                                    final String ddMMyyyy = dayFile.getName().split("\\.")[0] + "." + monthDir.getName() + "." + yearDir.getName();
                                                    cal.setTime(dateFormat.parse(ddMMyyyy));
                                                    if (isWithinRange(cal.getTime(), startTimeInMillis, endTimeInMillis)) {
                                                        if (sb.length() > 1) {
                                                            sb.append(",");
                                                        }
                                                        readCSVFile(sb, ddMMyyyy, startTimeInMillis, endTimeInMillis, stepInSeconds);

                                                        if (sb.charAt(sb.length() - 1) == ',') {
                                                            sb.deleteCharAt(sb.length() - 1);
                                                        }
                                                    }
                                                } catch (final Exception e) {
                                                    e.printStackTrace();
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        sb.append("]");
        return sb.toString();
    }

    //данные текущего дня читаем напрямую
    private static List<Data> getData(final String ddMMyyyy){
        if(ddMMyyyy.equals(dateFormat.format(new Date()))) {
            return readCSVFile(ddMMyyyy);
        } else {
            return cache.getUnchecked(ddMMyyyy);
        }
    }
    private static void readCSVFile(
            final StringBuilder sb
            , final String ddMMyyyy
            , final long startTimeInMillis
            , final long endTimeInMillis
            , final long stepInSeconds
    ) throws IOException {
        long prev = startTimeInMillis;
        List<Double> ppm = new ArrayList<>();
        List<Double> temperature = new ArrayList<>();

        final List<Data> list = getData(ddMMyyyy);

        for (final Data data : list) {
            //Time,Co2(PPM),Temp,RH(%)
            //10:26:39,649,0.00,0.00
            if (data.getTimeInMillis() >= startTimeInMillis && data.getTimeInMillis() <= endTimeInMillis) {
                if ((data.getTimeInMillis() - prev) / 1000 > stepInSeconds && !ppm.isEmpty()) {
                    sb.append("[");
                    sb.append(data.getTimeInMillis());
                    sb.append(",");
                    sb.append(ppm.stream().mapToDouble(l -> l).average().getAsDouble());
                    sb.append(",");
                    sb.append(temperature.stream().mapToDouble(l -> l).average().getAsDouble());
                    sb.append("]");
                    sb.append(",");

                    ppm = new ArrayList<>();
                    temperature = new ArrayList<>();
                    prev = data.getTimeInMillis();
                } else {
                    ppm.add(data.getCO2Ppm());
                    temperature.add(data.getTemperature());
                }
            }
        }
        if (!ppm.isEmpty()) {
            sb.append("[");
            sb.append(prev);
            sb.append(",");
            sb.append(ppm.stream().mapToDouble(l -> l).average().getAsDouble());
            sb.append(",");
            sb.append(temperature.stream().mapToDouble(l -> l).average().getAsDouble());
            sb.append("]");
            sb.append(",");
        }
    }

    private static final LoadingCache<String, List<Data>> cache = CacheBuilder.newBuilder().build(
            new CacheLoader<String, List<Data>>() {
                @Override
                public List<Data> load(final String key) {
                    return readCSVFile(key);
                }
            });

    private static List<Data> readCSVFile(final String ddMMyyyy) {
        final String loggerDir = nvl(System.getenv("co2mini-data-logger"), "d:\\co2mini-data-logger\\");
        final String[] dateParts = ddMMyyyy.split("\\.");
        final Path filePath = Paths.get(loggerDir, dateParts[2], dateParts[1], dateParts[0] + ".CSV");
        System.out.println(filePath.toAbsolutePath().toString());

        final List<Data> list = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        long toSkip = 1;
        try {
            for (final String line : Files.readAllLines(filePath)) {
                if (toSkip > 0) {
                    toSkip--;
                    continue;
                }
                try {
                    //Time,Co2(PPM),Temp,RH(%)
                    //10:26:39,649,0.00,0.00
                    final String[] parts = line.split(",");
                    if (parts.length == 4) {
                        cal.setTime(dateTimeFormat.parse(ddMMyyyy + " " + parts[0]));
                        final long timeInMillis = cal.getTimeInMillis();
                        final double co2ppm = Double.parseDouble(parts[1]);
                        final double temperature = Double.parseDouble(parts[2]);

                        list.add(new Data(timeInMillis, co2ppm, temperature));
                    }
                } catch (final ParseException e) {
                    e.printStackTrace();
                }
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
        return list;
    }
}
