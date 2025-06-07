package core.muling;

/**
 * Generic interface for mule client implementations.
 */
public interface MuleClient {
    /** Establishes a connection to the mule server. */
    boolean connect();
    /** Sends a message to the mule server. */
    boolean sendMessage(String msg);
    /** Reads a message from the mule server if available. */
    String readMessage();
    /** Disconnects from the mule server. */
    void disconnect();
    /** Returns true if the client is currently connected. */
    boolean isConnected();
    /** Returns the last error message, or null if none. */
    String getLastError();
}
