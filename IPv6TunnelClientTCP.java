package tcp4;

import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class IPv6TunnelClientTCP {
    private static final int IPV4_HEADER_LENGTH = 20;
    private static final int IPV6_HEADER_LENGTH = 40;

    public static void main(String[] args) {
        String serverIPv4 = "127.0.0.1";  // Replace with the server IPv4 address or "127.0.0.1" for localhost
        int serverPort = 9999;            // Replace with the server TCP port

        try {
            // Message 1
            String message1 = "Hello, this is the first message!";
            byte[] payloadData1 = message1.getBytes(StandardCharsets.UTF_8);
            int trafficClass1 = 0x1E; // Custom Traffic Class for QoS
            int flowLabel1 = 0x00001;  // Custom Flow Label for message 1

            // Send first message
            long startTime1 = System.nanoTime();
            byte[] ipv6Packet1 = createIPv6PacketWithQoSAndData(payloadData1, trafficClass1, flowLabel1);
            byte[] ipv4Packet1 = createIPv4PacketWithEncapsulatedIPv6("192.0.2.1", serverIPv4, ipv6Packet1);
            sendPacketOverTCP(serverIPv4, serverPort, ipv4Packet1);
            long endTime1 = System.nanoTime();
            long duration1 = endTime1 - startTime1;

            // Message 2
            String message2 = "Hello, this is the second message!";
            byte[] payloadData2 = message2.getBytes(StandardCharsets.UTF_8);
            int trafficClass2 = 0x1C; // Different Traffic Class for QoS
            int flowLabel2 = 0x00002;  // Custom Flow Label for message 2

            // Send second message
            long startTime2 = System.nanoTime();
            byte[] ipv6Packet2 = createIPv6PacketWithQoSAndData(payloadData2, trafficClass2, flowLabel2);
            byte[] ipv4Packet2 = createIPv4PacketWithEncapsulatedIPv6("192.0.2.1", serverIPv4, ipv6Packet2);
            sendPacketOverTCP(serverIPv4, serverPort, ipv4Packet2);
            long endTime2 = System.nanoTime();
            long duration2 = endTime2 - startTime2;

            // Output durations for comparison
            System.out.println("Message 1 sent in " + (duration1 / 1_000_000) + " ms with Traffic Class: " + trafficClass1 + " and Flow Label: " + flowLabel1);
            System.out.println("Message 2 sent in " + (duration2 / 1_000_000) + " ms with Traffic Class: " + trafficClass2 + " and Flow Label: " + flowLabel2);

            // Compare efficiency
            if (duration1 < duration2) {
                System.out.println("Message 1 was more efficient.");
            } else if (duration1 > duration2) {
                System.out.println("Message 2 was more efficient.");
            } else {
                System.out.println("Both messages were sent in the same time.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static byte[] createIPv6PacketWithQoSAndData(byte[] payloadData, int trafficClass, int flowLabel) {
        ByteBuffer buffer = ByteBuffer.allocate(IPV6_HEADER_LENGTH + payloadData.length);

        // Combine into version, traffic class, and flow label
        int versionAndQoS = (6 << 28) | (trafficClass << 20) | flowLabel;  // 6 for IPv6 version
        buffer.putInt(versionAndQoS);

        // Payload Length
        buffer.putShort((short) payloadData.length);

        // Next Header (No Next Header in this demo)
        buffer.put((byte) 0);

        // Hop Limit (set a reasonable value)
        buffer.put((byte) 64);

        // Source and Destination IPv6 Addresses (placeholder)
        buffer.put(new byte[16]);  // Source IPv6 address
        buffer.put(new byte[16]);  // Destination IPv6 address

        // Actual data payload
        buffer.put(payloadData);

        return buffer.array();
    }

    private static byte[] createIPv4PacketWithEncapsulatedIPv6(String srcIPv4, String destIPv4, byte[] ipv6Packet) throws Exception {
        ByteBuffer buffer = ByteBuffer.allocate(IPV4_HEADER_LENGTH + ipv6Packet.length);

        // IPv4 Version and Header Length
        buffer.put((byte) 0x45);

        // Type of Service (TOS) - can set custom QoS bits here if desired
        buffer.put((byte) 0);

        // Total Length (IPv4 header + IPv6 packet)
        buffer.putShort((short) (IPV4_HEADER_LENGTH + ipv6Packet.length));

        // Identification, Flags, Fragment Offset
        buffer.putShort((short) 0);  // ID
        buffer.putShort((short) 0);  // Flags + Fragment Offset

        // Time to Live (TTL)
        buffer.put((byte) 64);

        // Protocol (41 for IPv6 encapsulated in IPv4)
        buffer.put((byte) 41);

        // Header Checksum (set to 0 initially, calculated later)
        buffer.putShort((short) 0);

        // Source IPv4 address
        buffer.put(InetAddress.getByName(srcIPv4).getAddress());

        // Destination IPv4 address
        buffer.put(InetAddress.getByName(destIPv4).getAddress());

        // Encapsulated IPv6 packet as payload
        buffer.put(ipv6Packet);

        // Calculate IPv4 header checksum
        byte[] ipv4Packet = buffer.array();
        short checksum = calculateChecksum(ipv4Packet, 0, IPV4_HEADER_LENGTH);
        ipv4Packet[10] = (byte) (checksum >> 8);
        ipv4Packet[11] = (byte) (checksum & 0xFF);

        return ipv4Packet;
    }

    private static void sendPacketOverTCP(String destIPv4, int port, byte[] packet) throws Exception {
        try (Socket socket = new Socket(destIPv4, port);
             OutputStream outputStream = socket.getOutputStream()) {
            outputStream.write(packet);
            outputStream.flush();
            System.out.println("Packet with actual data sent to " + destIPv4 + ":" + port);
        }
    }

    private static short calculateChecksum(byte[] buf, int offset, int length) {
        int sum = 0;
        for (int i = offset; i < offset + length; i += 2) {
            int word = ((buf[i] & 0xFF) << 8) + (buf[i + 1] & 0xFF);
            sum += word;
            if ((sum & 0xFFFF0000) != 0) {
                sum &= 0xFFFF;
                sum++;
            }
        }
        return (short) ~sum;
    }
}


