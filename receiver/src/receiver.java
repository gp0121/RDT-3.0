import java.net.*;
import java.io.*;

public class receiver {

    public class packet {

        Integer seqNo;
        Integer packetID;
        Integer checksum;
        public String content;
        public boolean isLastMessage = false;

        public packet() {
            seqNo = 1;
            packetID = 0;
        }

        // Sum the ascii characters of the word
        public Integer generateChecksum(String s) {
            int sum = 0;
            for(char ch : s.toCharArray()){
                sum += ch;
                if(ch == '.'){
                    isLastMessage = true;
                }
            }
            return sum;
        }

        // Parse the message and break up by spaces
        public void parseMessage(String pcontent) {
            String[] splited = pcontent.split("\\s+");
            for (int i = 0; i < splited.length; i++) {
                seqNo = Integer.parseInt(splited[0]);
                packetID = Integer.parseInt(splited[1]);
                checksum = Integer.parseInt(splited[2]);
                content = splited[3];
            }
        }

        // Check the checksum to see whether or not it is corrupt
        public String validateMessage() {
            Integer newChecksum = generateChecksum(content);
            if (newChecksum.equals(checksum)) {
                return "ACK" + seqNo.toString();
            }
            else {
                seqNo = (seqNo + 1) % 2;
                return "ACK" + seqNo;
            }
        }
    }

    public static void main(String[] args) {
        if (args.length != 2)
        {
            System.err.println("Usage: java receiver <host name> <port number>");
            System.exit(1);
        }

        try {
            new receiver().startReceiver(args[0], Integer.parseInt(args[1]));
        } catch (Exception e) {
            System.out.println("Something falied: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void startReceiver(String hostName, int portNumber) throws IOException {

        Socket socket = null;
        PrintWriter writerOut = null;
        BufferedReader bufferIn = null;

        try {
            socket = new Socket(hostName, portNumber);
            writerOut = new PrintWriter(socket.getOutputStream(), true);
            bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String input, output;
            int totalReceived = 0;
            StringBuilder msg = new StringBuilder();
            System.out.println("Waiting... connect sender");

            // Create new packet object
            packet pak = new packet();
            while ((input = bufferIn.readLine()) != null) {
                // Terminate receiver
                if (input.equals("-1")) {
                    writerOut.println(-1);
                    break;
                }

                // Split message by spaces (parse)
                pak.parseMessage(input);
                msg.append(pak.content).append(" ");
                totalReceived++;
                // Print out current sequence number, total packets received, message, and ACK to be transmitted
                output = "Waiting " + pak.seqNo + ", " + totalReceived + ", " + input + ", " + pak.validateMessage();
                System.out.println(output);
                // Validate message (from packet object) - checks the checksum and then returns the proper ACK
                writerOut.println(pak.validateMessage());
                // If last message, then return the full message :)
                if (pak.isLastMessage) {
                    System.out.println("Message: " + msg);
                }
            }

        } catch (UnknownHostException e) {
            System.err.println("Cannot find the host: " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't read/write from the connection: " + e.getMessage());
            System.exit(1);
        } finally {
            writerOut.close();
            bufferIn.close();
            socket.close();
        }
    }
}