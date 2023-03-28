import java.net.*;
import java.io.*;

public class receiver {

    public class packet {

        Integer seqNum;
        Integer packetID;
        Integer checksum;
        public String content;
        public boolean isLastMessage = false;

        public packet() {
            seqNum = 1;
            packetID = 0;
        }
        public Integer checkSum(String s) {
            int sum = 0;
            for(char ch : s.toCharArray()){
                sum += ch;
                if(ch == '.'){
                    isLastMessage = true;
                }
            }
            return sum;
        }

        public void parseMessage(String msg) {
            String[] splitMsg = msg.split("\\s+");
            seqNum = Integer.parseInt(splitMsg[0]);
            packetID = Integer.parseInt(splitMsg[1]);
            checksum = Integer.parseInt(splitMsg[2]);
            content = splitMsg[3];
        }

        public String checkChecksum() {
            boolean isValid = checkSum(content).equals(checksum);
            if (!isValid) {
                seqNum = (seqNum + 1) % 2;
            }
            return "ACK" + seqNum;
        }
    }

    public static void main(String[] args) {
        if (args.length != 2)
        {
            System.exit(1);
        }
        try {
            new receiver().startReceiver(args[0], Integer.parseInt(args[1]));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void startReceiver(String hostName, int portNumber) throws IOException {

        Socket socket = null;
        PrintWriter writer = null;
        BufferedReader br = null;

        try {
            socket = new Socket(hostName, portNumber);
            writer = new PrintWriter(socket.getOutputStream(), true);
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String input;
            int count = 0;
            String msg = "";

            packet packet = new packet();
            while ((input = br.readLine()) != null) {
                if (input.equals("-1")) {
                    writer.println(-1);
                    break;
                }
                packet.parseMessage(input);
                msg = msg + packet.content + " ";
                count++;
                System.out.println("Waiting " + packet.seqNum + ", " + count + ", " + input + ", " + packet.checkChecksum());
                writer.println(packet.checkChecksum());
                if (packet.isLastMessage) {
                    System.out.println("Message: " + msg);
                }
            }

        } catch (IOException e) {
            System.exit(1);
        }
    }
}