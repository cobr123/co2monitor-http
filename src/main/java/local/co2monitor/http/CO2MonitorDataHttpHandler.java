package local.co2monitor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

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
            final Calendar cal = Calendar.getInstance();
            final Date today = new Date();
            final String todayStr = new SimpleDateFormat("dd.MM.yyyy").format(today);
            final String todayPath = new SimpleDateFormat("yyyy\\MM\\dd").format(today);
            final StringBuilder sb = new StringBuilder();
            int cnt = 0;
            sb.append("[");
            for (final String line : Files.readAllLines(Paths.get("d:\\co2mini-data-logger\\" + todayPath + ".CSV"))) {
                if (cnt > 1) {
                    sb.append(",");
                }
                if (cnt > 0) {
                    sb.append("[");
                    //Time,Co2(PPM),Temp,RH(%)
                    //10:26:39,649,0.00,0.00
                    final String[] parts = line.split(",");
                    cal.setTime(df.parse(todayStr + " " + parts[0]));
                    sb.append(cal.getTimeInMillis());
                    sb.append(",");
                    sb.append(parts[1]);
                    sb.append(",");
                    sb.append(parts[2]);
                    sb.append("]");
                }
                ++cnt;
            }
            sb.append("]");
            final String response = sb.toString(); //"[[1,0.7695],[2,0.7648],[3,0.7645]]";
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
}
