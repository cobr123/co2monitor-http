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
import java.util.Calendar;

/**
 * Created by cobr123 on 05.04.2017.
 */
public class CO2MonitorDataHttpHandler implements HttpHandler {
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final int STATUS_OK = 200;
    private static final Charset CHARSET = StandardCharsets.UTF_8;

    private static final DateFormat df = new SimpleDateFormat("dd.MM.yyyy H:m:s");

    public void handle(final HttpExchange t) throws IOException {
        try {
            final String response = getData();
            t.getResponseHeaders().set(HEADER_CONTENT_TYPE, String.format("application/json; charset=%s", CHARSET));
            final byte[] rawResponseBody = response.getBytes(CHARSET);
            t.sendResponseHeaders(STATUS_OK, rawResponseBody.length);
            t.getResponseBody().write(rawResponseBody);
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            t.close();
        }
    }

    public static String getData() {
        final StringBuilder sb = new StringBuilder();
        final File dir = new File("d:\\co2mini-data-logger\\");
        final File[] yearDirs = dir.listFiles();

        int cnt = 0;
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
                                                    if (cnt > 0) {
                                                        sb.append(",");
                                                    }
                                                    readCSVFile(sb, dayFile.getName().split("\\.")[0] + "." + monthDir.getName() + "." + yearDir.getName(), dayFile.toPath());
                                                    ++cnt;
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

    public static void readCSVFile(final StringBuilder sb, final String ddMMyyyy, final Path filePath) throws IOException, ParseException {
        final Calendar cal = Calendar.getInstance();
        int cnt = 0;
        for (final String line : Files.readAllLines(filePath)) {
            if (cnt > 1) {
                sb.append(",");
            }
            if (cnt > 0) {
                sb.append("[");
                //Time,Co2(PPM),Temp,RH(%)
                //10:26:39,649,0.00,0.00
                final String[] parts = line.split(",");
                cal.setTime(df.parse(ddMMyyyy + " " + parts[0]));
                sb.append(cal.getTimeInMillis());
                sb.append(",");
                sb.append(parts[1]);
                sb.append(",");
                sb.append(parts[2]);
                sb.append("]");
            }
            ++cnt;
        }
    }
}
