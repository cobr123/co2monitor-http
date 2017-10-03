package local.co2monitor.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by cobr123 on 05.04.2017.
 */
public class CO2MonitorHttpHandler implements HttpHandler {
    private static final String response = "<html>\n" +
            "<head>\n" +
            "    <title>CO2 meter</title>\n" +
            "    <script src=\"https://yastatic.net/jquery/3.1.1/jquery.min.js\"></script>\n" +
            "    <script src=\"https://code.highcharts.com/stock/highstock.js\"></script>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"container\" style=\"height: 80%; width: 100%\"></div>\n" +
            "    <script>\n" +
            "/**\n" +
            " * Load new data depending on the selected min and max\n" +
            " */\n" +
            "function afterSetExtremes(e) {\n" +
            "    var chart = Highcharts.charts[0];\n" +
            "    chart.showLoading('Loading data from server...');\n" +
            "    console.log('/co2monitor/data?start=' + Math.round(e.min) + '&end=' + Math.round(e.max));\n" +
            "    $.getJSON('/co2monitor/data?start=' + Math.round(e.min) + '&end=' + Math.round(e.max), function (data) {\n" +
            "        console.log(data.length);\n" +
            "        chart.series[0].setData(data.map(function(val){return [val[0],val[1]];}));\n" +
            "        chart.series[1].setData(data.map(function(val){return [val[0],val[2]];}));\n" +
            "        chart.hideLoading();\n" +
            "    });\n" +
            "}\n" +
            "Highcharts.setOptions({\n" +
            "    global: {\n" +
            "        useUTC: false\n" +
            "    }\n" +
            "});\n" +
            "  $.getJSON('/co2monitor/data', function (data) {\n" +
            "    // Add a null value for the end date\n" +
            "    data = [].concat(data, [[new Date().getTime(), null, null, null, null]]);\n" +
            "\n" +
            "    var chart = Highcharts.stockChart('container', {\n" +
            "    chart: {\n" +
            "        zoomType: 'x'\n" +
            "    },\n" +
            "    navigator: {\n" +
            "        adaptToUpdatedData: false,\n" +
            "        series: {\n" +
            "            data: data\n" +
            "        }\n" +
            "    },\n" +
            "    scrollbar: {\n" +
            "        liveRedraw: false\n" +
            "    },\n" +
            "    rangeSelector: {\n" +
            "        buttons: [{\n" +
            "            count: 1,\n" +
            "            type: 'hour',\n" +
            "            text: '1H'\n" +
            "        }, {\n" +
            "            count: 4,\n" +
            "            type: 'hour',\n" +
            "            text: '4H'\n" +
            "        }, {\n" +
            "            count: 8,\n" +
            "            type: 'hour',\n" +
            "            text: '8H'\n" +
            "        }, {\n" +
            "            count: 1,\n" +
            "            type: 'day',\n" +
            "            text: '1d'\n" +
            "        }, {\n" +
            "            count: 1,\n" +
            "            type: 'week',\n" +
            "            text: '1w'\n" +
            "        }, {\n" +
            "            count: 1,\n" +
            "            type: 'month',\n" +
            "            text: '1m'\n" +
            "        }, {\n" +
            "            type: 'all',\n" +
            "            text: 'All'\n" +
            "        }],\n" +
            "        inputEnabled: true, // it supports only days\n" +
            "        selected: 0\n" +
            "    },\n" +
            "        xAxis: {\n" +
            "            events: {\n" +
            "                afterSetExtremes: afterSetExtremes\n" +
            "            },\n" +
            "            minRange: 3600 * 1000 // one hour\n" +
            "        },\n" +
            "        yAxis: [{\n" +
            "            floor: 0,\n" +
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
            "            data: data.map(function(val){return [val[0],val[1]];}),\n" +
            "            dataGrouping: {\n" +
            "                enabled: false\n" +
            "            }\n" +
            "        },\n" +
            "       {\n" +
            "            yAxis: 1,\n" +
            "            name: 'temp',\n" +
            "           tooltip: {\n" +
            "               valueDecimals: 2,\n" +
            "               valueSuffix: ' C'\n" +
            "           },\n" +
            "            visible: false,\n" +
            "            data: data.map(function(val){return [val[0],val[2]];}),\n" +
            "            dataGrouping: {\n" +
            "                enabled: false\n" +
            "            }\n" +
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
            "    }, function(chart){\n" +
            "            // apply the date pickers\n" +
            "            setTimeout(function () {\n" +
            "                $('input.highcharts-range-selector', $(chart.container).parent()).datepicker();\n" +
            "            }, 0);\n" +
            "        });\n" +
            "   chart.xAxis[0].setExtremes(new Date().getTime() - 3600 * 1000, new Date().getTime());\n" +
            "  });\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";

    public void handle(final HttpExchange t) throws IOException {
        try {
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
