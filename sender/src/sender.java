import java.net.*;
import java.io.*;

public class sender {

    public class packet {
        Integer seqNum;
        Integer packetID;
        Integer checksum;
        public String content;

        public packet() {
            seqNum = 1;
            packetID = 0;
        }

        // Packets contain these items (sequence number, packet ID, checksum, and content)
        public void createPacket(String pContent) {
            content = pContent;
            seqNum = (seqNum + 1) % 2;
            checksum = checksum(content);
            packetID++;
        }


        public Integer checksum(String s) {
            int sum = 0;
            for(char ch : s.toCharArray()){
                sum += ch;
            }
            return sum;
        }
    }
    public static void main(String[] args) {
        if (args.length != 3)
        {
            System.exit(1);
        }
        try {
            new sender().startSender(args[0], Integer.parseInt(args[1]), args[2]);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public boolean validateACK(String ACK, Integer seqNo) {
        return (ACK.equals("ACK" + seqNo));
    }

    public void startSender(String hostName, int portNumber, String fileName) throws IOException {

        Socket socket;
        PrintWriter writer;
        BufferedReader brIn;

        try {
            socket = new Socket(hostName, portNumber);
            writer = new PrintWriter(socket.getOutputStream(), true);
            brIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String ACK;
            try {
                FileInputStream file = new FileInputStream(fileName);
                DataInputStream dis = new DataInputStream(file);
                BufferedReader br = new BufferedReader(new InputStreamReader(dis));

                String input;

                int totalSent = 0;
                String message;
                packet packet = new packet();
                while ((input = br.readLine()) != null) {
                    String[] content_array = input.split("\\s+");
                    int i = 0;
                    while (i < content_array.length) {
                        packet.createPacket(content_array[i]);
                        message = packet.seqNum + " " + packet.packetID + " " + packet.checksum + " " + packet.content;
                        writer.println(message);
                        ACK = brIn.readLine();
                        totalSent++;
                        if (ACK.equals("ACK2")) { // DROP
                            System.out.println("Waiting: " + ACK + ", " + totalSent + ", DROP, resend Packet" + packet.seqNum);
                        }
                        else if (validateACK(ACK, packet.seqNum)) { // PASS
                            i++;
                            System.out.println("Waiting: " + ACK + ", " + totalSent + ", " + ACK + ", no more packets to send");
                        }
                        else if(!validateACK(ACK, packet.seqNum)){ // CORRUPT
                            System.out.println("Waiting: " + ACK + ", " + totalSent + ", " + ACK + ", send Packet" + packet.seqNum);
                        }
                    }
                    writer.println(-1);
                }
                dis.close();
                writer.close();
                brIn.close();
                socket.close();
            } catch (Exception e) {
                System.exit(1);
            }
        } catch (IOException e) {
            System.exit(1);
        }
    }
}