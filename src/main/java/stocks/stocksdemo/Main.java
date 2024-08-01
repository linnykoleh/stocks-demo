package stocks.stocksdemo;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class Main {

    private static final String MEASUREMENT_ID = "G-B4LPFB9MW3"; // Your Measurement ID
    private static final String API_SECRET = "JGAoPHXfR6uI0KyCEveZQw"; // Your API Secret
    private static final String EVENT_NAME = "EVENT_EXCHANGE_RATE";
    private static final String CURRENCY_FROM = "UAH";
    private static final String CURRENCY_TO = "USD";
    private static final long PUSH_INTERVAL_MS = 10000; // 10 seconds for testing, it could be 1 hour as per requirements
    private static final String GA_URL = "https://www.google-analytics.com/mp/collect?measurement_id=" + MEASUREMENT_ID + "&api_secret=" + API_SECRET;

    public static void main(String[] args) {
        System.out.println("Worker started. Pushing info to Google Analytics 4 every " + PUSH_INTERVAL_MS + " ms");

        var timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    sendEvent();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 0, PUSH_INTERVAL_MS);
    }

    private static JSONObject getCurrenciesList(String from, String to) throws Exception {
        var urlString = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json";
        var url = new URL(urlString);
        var conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");

        var sc = new Scanner(url.openStream());
        var inline = new StringBuilder();
        while (sc.hasNext()) {
            inline.append(sc.nextLine());
        }
        sc.close();

        var jsonArray = new JSONArray(inline.toString());
        for (var i = 0; i < jsonArray.length(); i++) {
            var item = jsonArray.getJSONObject(i);
            if (item.getString("cc").equals(to)) {
                var result = new JSONObject();
                result.put("from", from);
                result.put("to", to);
                result.put("exchangeRate", item.getDouble("rate"));
                return result;
            }
        }

        throw new RuntimeException("No exchange rate information found for " + to);
    }

    private static void sendEvent() throws Exception {
        var currencyData = getCurrenciesList(CURRENCY_FROM, CURRENCY_TO);
        var clientId = UUID.randomUUID().toString();

        var payload = new JSONObject();
        payload.put("client_id", clientId);

        var events = new JSONArray();
        JSONObject event = new JSONObject();
        event.put("name", EVENT_NAME);

        var params = new JSONObject();
        params.put("from", currencyData.getString("from"));
        params.put("to", currencyData.getString("to"));
        params.put("exchangeRate", currencyData.getDouble("exchangeRate"));
        event.put("params", params);

        events.put(event);
        payload.put("events", events);

        var url = new URL(GA_URL);
        var conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");

        var os = conn.getOutputStream();
        os.write(payload.toString().getBytes());
        os.flush();

        System.out.println("Response code: " + conn.getResponseCode());
        conn.disconnect();

        System.out.println("Sent: " + currencyData.getDouble("exchangeRate") + " for FROM: " + currencyData.getString("from") + " to TO: " + currencyData.getString("to"));
    }
}
