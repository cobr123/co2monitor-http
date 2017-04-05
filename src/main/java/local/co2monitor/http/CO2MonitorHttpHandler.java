package local.co2monitor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by cobr123 on 05.04.2017.
 */
public class CO2MonitorHttpHandler implements HttpHandler {
    public void handle(final HttpExchange t) throws IOException {
        try {
            final String response = "<html>\n" +
                    "<head>\n" +
                    "    <title>CO2 meter</title>\n" +
                    "    <script src=\"https://code.jquery.com/jquery-3.1.1.min.js\"></script>\n" +
                    "    <script src=\"https://code.highcharts.com/stock/highstock.js\"></script>\n" +
                    "</head>\n" +
                    "<body>\n" +
                    "    <div id=\"container\" style=\"height: 80%; width: 100%\"></div>\n" +
                    "    <script>\n" +
                    "Highcharts.setOptions({\n" +
                    "    global: {\n" +
                    "        useUTC: false\n" +
                    "    }\n" +
                    "});\n" +
                    "  $.getJSON('/co2monitor/data', function (data) {\n" +
                    "    Highcharts.stockChart('container', {\n" +
                    "    rangeSelector: {\n" +
                    "        buttons: [{\n" +
                    "            count: 1,\n" +
                    "            type: 'hour',\n" +
                    "            text: '1H'\n" +
                    "        }, {\n" +
                    "            count: 1,\n" +
                    "            type: 'day',\n" +
                    "            text: '1d'\n" +
                    "        }, {\n" +
                    "            count: 1,\n" +
                    "            type: 'week',\n" +
                    "            text: '1w'\n" +
                    "        }, {\n" +
                    "            type: 'all',\n" +
                    "            text: 'All'\n" +
                    "        }],\n" +
                    "        selected: 0\n" +
                    "    },\n" +
                    "        yAxis: [{\n" +
                    "            title: {\n" +
                    "                text: 'ppm'\n" +
                    "            },\n" +
                    "            plotLines: [{\n" +
                    "                value: 800,\n" +
                    "                color: 'yellow',\n" +
                    "                dashStyle: 'shortdash',\n" +
                    "                width: 2\n" +
                    "            }, {\n" +
                    "                value: 1200,\n" +
                    "                color: 'red',\n" +
                    "                dashStyle: 'shortdash',\n" +
                    "                width: 2\n" +
                    "            }]\n" +
                    "        }, { // Secondary yAxis\n" +
                    "        title: {\n" +
                    "            text: 'temp'\n" +
                    "        },\n" +
                    "        opposite: true\n" +
                    "    }],\n" +
                    "        series: [{\n" +
                    "            name: 'ppm',\n" +
                    "            data: data.map(function(val){return [val[0],val[1]];})\n" +
                    "        },{\n" +
                    "            yAxis: 1,\n" +
                    "            name: 'temp',\n" +
                    "           tooltip: {\n" +
                    "               valueDecimals: 2,\n" +
                    "               valueSuffix: ' C'\n" +
                    "           },\n" +
                    "            visible: false,\n" +
                    "            data: data.map(function(val){return [val[0],val[2]];})\n" +
                    "        }],\n" +
                    "    legend: {\n" +
                    "        enabled: true\n" +
                    "    },\n" +
                    "    plotOptions: {\n" +
                    "        series: {\n" +
                    "            animation: false\n" +
                    "        }\n" +
                    "    },\n" +
                    "    credits: {\n" +
                    "      enabled: false\n" +
                    "    }\n" +
                    "    });\n" +
                    "  });" +
                    "    </script>\n" +
                    "</body>\n" +
                    "</html>";
            t.sendResponseHeaders(200, response.length());
            try (final OutputStream os = t.getResponseBody()) {
                os.write(response.getBytes());
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            t.close();
        }
    }

}
