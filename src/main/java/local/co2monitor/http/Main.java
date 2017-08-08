package local.co2monitor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

/**
 * Created by cobr123 on 05.04.2017.
 */
final public class Main {
    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 1);
        server.createContext("/", new HttpHandler() {
            public void handle(final HttpExchange t) throws IOException {
                final String response = "<html><header><META HTTP-EQUIV=REFRESH CONTENT=\"1; URL=/co2monitor\"></header></html>";
                t.sendResponseHeaders(200, response.length());
                try (final OutputStream os = t.getResponseBody()) {
                    os.write(response.getBytes());
                }
            }
        });
        CO2MonitorDataHttpHandler.getData(0, Long.MAX_VALUE);
        server.createContext("/co2monitor", new CO2MonitorHttpHandler());
        server.createContext("/co2monitor/data", new CO2MonitorDataHttpHandler());
        server.setExecutor(null); // creates a default executor
        server.start();
    }
}
