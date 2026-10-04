package com.anomalydetect.simulator;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Quick test to verify Finnhub WebSocket connection works.
 * Run with: mvn exec:java -Dexec.mainClass="com.anomalydetect.simulator.FinnhubConnectionTest"
 */
public class FinnhubConnectionTest {

    private static final String API_KEY = "db1754pr01qhkqh8i39gdb1754pr01qhkqh8i3a0";
    private static final String WS_URL = "wss://ws.finnhub.io?token=" + API_KEY;

    public static void main(String[] args) throws Exception {
        System.out.println("🔌 Testing Finnhub WebSocket connection...\n");

        CountDownLatch connectedLatch = new CountDownLatch(1);
        CountDownLatch dataLatch = new CountDownLatch(1);

        WebSocketClient client = new WebSocketClient(new URI(WS_URL)) {
            @Override
            public void onOpen(ServerHandshake handshake) {
                System.out.println("✅ Connected to Finnhub!");
                connectedLatch.countDown();

                // Subscribe to AAPL
                String subscribeMsg = "{\"type\":\"subscribe\",\"symbol\":\"AAPL\"}";
                send(subscribeMsg);
                System.out.println("📡 Subscribed to AAPL");
                System.out.println("\n⏳ Waiting for trade data (may take up to 30s during market hours)...\n");
            }

            @Override
            public void onMessage(String message) {
                System.out.println("📥 Received: " + message);
                
                if (message.contains("\"type\":\"trade\"")) {
                    System.out.println("\n🎉 SUCCESS! Received real trade data from Finnhub!");
                    dataLatch.countDown();
                } else if (message.contains("\"type\":\"ping\"")) {
                    System.out.println("   (ping - connection is alive)");
                }
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                System.out.println("🔒 Connection closed: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                System.err.println("❌ Error: " + ex.getMessage());
            }
        };

        client.connect();

        // Wait for connection
        if (!connectedLatch.await(10, TimeUnit.SECONDS)) {
            System.err.println("❌ Failed to connect within 10 seconds");
            System.exit(1);
        }

        // Wait for data (up to 60 seconds - market hours only)
        boolean receivedData = dataLatch.await(60, TimeUnit.SECONDS);

        client.close();

        if (receivedData) {
            System.out.println("\n✅ Finnhub integration is working correctly!");
            System.out.println("   You can now run the full pipeline with real market data.");
        } else {
            System.out.println("\n⚠️  No trade data received within 60 seconds.");
            System.out.println("   This is normal outside US market hours (9:30 AM - 4:00 PM ET).");
            System.out.println("   The connection itself is working - you'll see data during trading hours.");
        }
    }
}
