package chess.net;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 * One end of a two-player network connection. The same class is used by both
 * computers once connected -- only how they OPEN it differs:
 *
 *   host(port)       opens a ServerSocket and waits (blocks) for the other
 *                    player to connect. The one who hosts.
 *   join(ip, port)   connects (blocks) to a waiting host at that address.
 *
 * Reading is blocking, so it runs on its own daemon thread (listen); each line
 * received is handed to the callback. Writing is a simple println.
 *
 * NOTE: host() and join() BLOCK, so the UI must call them on a background
 * thread (a SwingWorker), never on the event thread, or the window freezes.
 */
public class NetLink {
    private final Socket sock;
    private final BufferedReader in;
    private final PrintWriter out;
    private volatile boolean open = true;

    private NetLink(Socket s) throws IOException {
        sock = s;
        in  = new BufferedReader(new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
        out = new PrintWriter(new OutputStreamWriter(s.getOutputStream(), StandardCharsets.UTF_8), true);
    }

    /** Host: wait for one opponent to connect, then return the link. */
    public static NetLink host(int port) throws IOException {
        ServerSocket ss = new ServerSocket();
        ss.setReuseAddress(true);
        ss.bind(new InetSocketAddress(port));
        try {
            Socket s = ss.accept();              // blocks until the opponent joins
            return new NetLink(s);
        } finally {
            ss.close();                          // stop listening once connected
        }
    }

    /** Join: connect to a waiting host (10s timeout). */
    public static NetLink join(String host, int port) throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 10000);
        return new NetLink(s);
    }

    public void send(String line) { out.println(line); }

    /** Read one line right now (used for the one-time START handshake). */
    public String readLineBlocking() throws IOException { return in.readLine(); }

    /** Start the background reader. Every line goes to onMsg; a drop sends "DISCONNECT". */
    public void listen(Consumer<String> onMsg) {
        Thread t = new Thread(() -> {
            try {
                String line;
                while (open && (line = in.readLine()) != null) onMsg.accept(line);
            } catch (IOException ignore) {
            } finally {
                if (open) onMsg.accept(Protocol.DISCONNECT);
            }
        }, "net-reader");
        t.setDaemon(true);
        t.start();
    }

    public void close() {
        open = false;
        try { sock.close(); } catch (IOException ignore) {}
    }

    /** Best-guess LAN IPv4 address to show the host (e.g. 192.168.0.12). */
    public static String localIp() {
        try {
            for (NetworkInterface ni : java.util.Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress ia : java.util.Collections.list(ni.getInetAddresses())) {
                    if (ia instanceof Inet4Address && ia.isSiteLocalAddress()) return ia.getHostAddress();
                }
            }
        } catch (Exception ignore) {}
        return "127.0.0.1";
    }
}