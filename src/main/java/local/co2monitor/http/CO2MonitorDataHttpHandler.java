package local.co2monitor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private static final DateFormat df = new SimpleDateFormat("dd.MM.yyyy H:m:s");

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

    private static String getData(final Map<String, String> queryParams) {
        final long startTimeInMillis = Long.parseLong(queryParams.getOrDefault("start", "0"));
        final long endTimeInMillis = Long.parseLong(queryParams.getOrDefault("end", "" + Calendar.getInstance().getTimeInMillis()));
        final long range = endTimeInMillis - startTimeInMillis;
        final long stepInSeconds = getStepInSeconds(range);

        final StringBuilder sb = new StringBuilder();
        final String loggerDir = nvl(System.getenv("co2mini-data-logger"), "d:\\co2mini-data-logger\\");
        final File dir = new File(loggerDir);
        final File[] yearDirs = dir.listFiles();

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
                                                    if (sb.length() > 1) {
                                                        sb.append(",");
                                                    }
                                                    readCSVFile(sb, dayFile.getName().split("\\.")[0] + "." + monthDir.getName() + "." + yearDir.getName(), dayFile.toPath(), startTimeInMillis, endTimeInMillis, stepInSeconds);

                                                    if (sb.charAt(sb.length() - 1) == ',') {
                                                        sb.deleteCharAt(sb.length() - 1);
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

    private static void readCSVFile(
            final StringBuilder sb
            , final String ddMMyyyy
            , final Path filePath
            , final long startTimeInMillis
            , final long endTimeInMillis
            , final long stepInSeconds
    ) throws IOException {
        long prev = startTimeInMillis;
        List<Double> ppm = new ArrayList<>();
        List<Double> temperature = new ArrayList<>();
        final Calendar cal = Calendar.getInstance();
        long toSkip = 1;
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
                    cal.setTime(df.parse(ddMMyyyy + " " + parts[0]));
                    final long timeInMillis = cal.getTimeInMillis();
                    if (timeInMillis >= startTimeInMillis && timeInMillis <= endTimeInMillis) {
                        if ((timeInMillis - prev) / 1000 > stepInSeconds && !ppm.isEmpty()) {
                            sb.append("[");
                            sb.append(timeInMillis);
                            sb.append(",");
                            sb.append(ppm.stream().mapToDouble(l -> l).average().getAsDouble());
                            sb.append(",");
                            sb.append(temperature.stream().mapToDouble(l -> l).average().getAsDouble());
                            sb.append("]");
                            sb.append(",");

                            ppm = new ArrayList<>();
                            temperature = new ArrayList<>();
                            prev = timeInMillis;
                        } else {
                            ppm.add(Double.parseDouble(parts[1]));
                            temperature.add(Double.parseDouble(parts[2]));
                        }
                    }
                }
            } catch (final ParseException e) {
                e.printStackTrace();
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
}
