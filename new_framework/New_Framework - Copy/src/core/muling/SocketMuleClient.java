package core.muling;

import core.config.MulingConfig;
import org.dreambot.api.utilities.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/**
 * Default socket-based implementation of {@link MuleClient}.
 * This was adapted from the original script-specific implementation.
 */
public class SocketMuleClient implements MuleClient {
    private final MulingConfig config;
    private Socket clientSocket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private String lastError = null;

    public SocketMuleClient(MulingConfig config) {
        this.config = config;
    }

    @Override
    public boolean connect() {
        if (connected) {
            Logger.log("MuleClient: Already connected.");
            return true;
        }
        if (!config.isEnabled()) {
            Logger.log("MuleClient: Muling is disabled in config.");
            lastError = "Muling disabled";
            return false;
        }

        String host = "127.0.0.1";
        int port = config.getPort();

        if (port <= 0 || port > 65535) {
            lastError = "Invalid port number: " + port;
            Logger.error("MuleClient: " + lastError);
            return false;
        }

        if (!verifyServerRunning(host, port)) {
            return false;
        }

        Logger.log("MuleClient: Attempting to connect to mule server at " + host + ":" + port);

        try {
            if (clientSocket != null) {
                try {
                    clientSocket.close();
                } catch (Exception e) {
                    // ignore
                }
                clientSocket = null;
            }

            clientSocket = new Socket();
            clientSocket.setSoTimeout(10000);
            clientSocket.connect(new java.net.InetSocketAddress(host, port), 5000);

            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            connected = true;
            lastError = null;
            Logger.log("MuleClient: Connection established successfully.");

            if (!sendMessage("CONNECT:" + config.getMuleUsername())) {
                Logger.error("MuleClient: Failed to send initial connection message");
                disconnect();
                lastError = "Failed to send initial connection message";
                return false;
            }

            Logger.log("MuleClient: Waiting for connection acknowledgment...");
            long startTime = System.currentTimeMillis();
            long timeout = 5000;
            while (System.currentTimeMillis() - startTime < timeout) {
                String response = readMessage();
                if (response != null && (response.contains("CONNECTED") || response.contains("ACK"))) {
                    Logger.log("MuleClient: Connection acknowledged by server");
                    break;
                }
                org.dreambot.api.utilities.Sleep.sleep(100);
            }

            Logger.log("MuleClient: Initial connection message sent successfully");
            return true;
        } catch (UnknownHostException e) {
            lastError = "Unknown host: " + host;
            Logger.error("MuleClient: Connection failed - " + lastError);
            connected = false;
            return false;
        } catch (IOException e) {
            lastError = "I/O error connecting to " + host + ":" + port + " - Is the mule script running?";
            Logger.error("MuleClient: Connection failed - " + lastError + " (" + e.getMessage() + ")");
            connected = false;
            return false;
        } catch (Exception e) {
            lastError = "Unexpected error: " + e.getMessage();
            Logger.error("MuleClient: Connection failed with unexpected error - " + lastError);
            connected = false;
            return false;
        }
    }

    private boolean verifyServerRunning(String host, int port) {
        Logger.log("MuleClient: Verifying server at " + host + ":" + port);
        int retryCount = 3;
        for (int attempt = 1; attempt <= retryCount; attempt++) {
            Socket testSocket = null;
            try {
                Logger.log("MuleClient: Server verification attempt " + attempt + "/" + retryCount);
                testSocket = new Socket();
                int timeout = 3000 * attempt;
                testSocket.connect(new java.net.InetSocketAddress(host, port), timeout);
                Logger.log("MuleClient: Server verification successful - port " + port + " is open");
                return true;
            } catch (IOException e) {
                Logger.error("MuleClient: Server verification attempt " + attempt + " failed: " + e.getMessage());
                if (attempt < retryCount) {
                    int backoff = 2000 * attempt;
                    Logger.log("MuleClient: Waiting " + (backoff / 1000) + " seconds before retry...");
                    org.dreambot.api.utilities.Sleep.sleep(backoff);
                } else {
                    lastError = "Server verification failed - port " + port + " is not accepting connections";
                    Logger.error("MuleClient: " + lastError);
                    return false;
                }
            } finally {
                if (testSocket != null) {
                    try { testSocket.close(); } catch (IOException ignored) {}
                }
            }
        }
        return false;
    }

    @Override
    public boolean sendMessage(String msg) {
        if (!connected || out == null) {
            Logger.warn("MuleClient: Cannot send message, not connected.");
            lastError = "Not connected";
            return false;
        }
        try {
            Logger.debug("MuleClient: Sending message: " + msg);
            out.println(msg);
            return true;
        } catch (Exception e) {
            lastError = "Error sending message: " + e.getMessage();
            Logger.error("MuleClient: " + lastError);
            disconnect();
            return false;
        }
    }

    @Override
    public String readMessage() {
        if (!connected || in == null) {
            return null;
        }
        try {
            if (in.ready()) {
                String msg = in.readLine();
                if (msg != null) {
                    Logger.debug("MuleClient: Received message: " + msg);
                }
                return msg;
            }
            return null;
        } catch (SocketTimeoutException e) {
            return null;
        } catch (IOException e) {
            lastError = "Error reading message: " + e.getMessage();
            Logger.error("MuleClient: " + lastError);
            disconnect();
            return null;
        }
    }

    @Override
    public void disconnect() {
        if (!connected) return;
        Logger.log("MuleClient: Disconnecting from mule server...");
        try {
            if (out != null) out.close();
            if (in != null) in.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            Logger.error("MuleClient: Error closing socket resources: " + e.getMessage());
        } finally {
            connected = false;
            clientSocket = null;
            out = null;
            in = null;
            Logger.log("MuleClient: Disconnected.");
        }
    }

    @Override
    public boolean isConnected() {
        if (connected && clientSocket != null) {
            return !clientSocket.isClosed() && clientSocket.isConnected();
        }
        return false;
    }

    @Override
    public String getLastError() {
        return lastError;
    }
}
