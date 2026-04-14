import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.util.Iterator;
import java.util.Set;

// Use only the below modules for DNS related operations. 
// You can only use DNS libraries that pack and unpack DNS packets or parse zone files. 
// You can not use libraries that actually interpret those packets for you, 
// manage a DNS cache, or check for certain errors.


import org.xbill.DNS.ARecord;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Flags;
import org.xbill.DNS.Header;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Section;
import org.xbill.DNS.TextParseException;

public class Server {
    private String rootIp;
    private String domain;
    private DatagramChannel channel;
    private int port;

    public Server(String rootIp, String domain, int port) throws IOException {
        this.rootIp = rootIp;
        this.domain = domain;
        this.channel = DatagramChannel.open();
        this.channel.socket().bind(new InetSocketAddress(port));
        this.port = this.channel.socket().getLocalPort();
        this.channel.configureBlocking(false);
        log("Bound to port " + this.port);
    }

    private void log(String message) {
        System.err.println(message);
    }

    private void send(InetSocketAddress addr, Message message) throws IOException {
        log("Sending message:\n" + message);
        byte[] data = message.toWire();
        ByteBuffer buffer = ByteBuffer.wrap(data);
        channel.send(buffer, addr);
    }

    private void recv() throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(65535);
        InetSocketAddress addr = (InetSocketAddress) channel.receive(buffer);
        if (addr == null) {
            return; 
        }
        buffer.flip();
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);

        Message request = new Message(data);
        log("Received message:\n" + request);

        Message response = new Message(request.getHeader().getID());
        Header header = response.getHeader();
        header.setFlag(Flags.QR); 
        header.setFlag(Flags.AA); 
        header.setFlag(Flags.RD);
        header.setFlag(Flags.RA); 

        for (Record rec : request.getSectionArray(Section.QUESTION)) {
            response.addRecord(rec, Section.QUESTION);
        }

        // YOU WILL NEED TO ACTUALLY DO SOMETHING SMART HERE
        // WE ARE JUST REPLYING WITH A FIXED RESPONSE
        try {
            Name name = Name.fromString("abc.com.");
            Record answer = new ARecord(name, DClass.IN, 60L, InetAddress.getByName("1.2.3.4"));
            response.addRecord(answer, Section.ANSWER);
        } catch (TextParseException e) {
            log("Error parsing name: " + e.getMessage());
        }

        send(addr, response);
    }

    public void run() throws IOException {
        Selector selector = Selector.open();
        channel.register(selector, SelectionKey.OP_READ);

        while (true) {
            int readyChannels = selector.select(100);
            if (readyChannels == 0) {
                continue;
            }
            Set<SelectionKey> selectedKeys = selector.selectedKeys();
            Iterator<SelectionKey> iter = selectedKeys.iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                if (key.isReadable()) {
                    recv();
                }
                iter.remove();
            }
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Server <root_ip> <zone> [--port <port>]");
            System.exit(1);
        }
        String rootIp = args[0];
        String zone = args[1];
        int port = 0; 
        for (int i = 2; i < args.length; i++) {
            if (args[i].equals("--port")) {
                if (i + 1 < args.length) {
                    try {
                        port = Integer.parseInt(args[i + 1]);
                        i++; 
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number: " + args[i + 1]);
                        System.exit(1);
                    }
                } else {
                    System.err.println("Expected port number after --port");
                    System.exit(1);
                }
            }
        }
        try {
            Server server = new Server(rootIp, zone, port);
            server.run();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
