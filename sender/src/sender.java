import java.net.*;
import java.io.*;

public class sender {

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

        // Packets contain these items (sequence number, packet ID, checksum, and content)
        public void createPacket(String pContent) {
            content = pContent;
            seqNo = (seqNo + 1) % 2;
            checksum = generateChecksum(content);
            packetID++;
        }

        // Generates the message
        public String generateMessage() {
            return seqNo + " " + packetID + " " + checksum + " " + content;
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

    // Function to return whether or not the ACK is the same from the network
    public boolean validateACK(String ACK, Integer seqNo) {
        return ("ACK" + seqNo.toString()).equals(ACK);
    }

    public void startSender(String hostName, int portNumber, String fileName) throws IOException {

        Socket socket;
        PrintWriter writerOut;
        BufferedReader bufferIn;

        try {
            socket = new Socket(hostName, portNumber);
            writerOut = new PrintWriter(socket.getOutputStream(), true);
            bufferIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String ACK;

            try {
                // Code to read in the file
                FileInputStream fstream = new FileInputStream(fileName);
                DataInputStream dataIn = new DataInputStream(fstream);
                BufferedReader buffReader = new BufferedReader(new InputStreamReader(dataIn));

                String input;

                int totalSent = 0;
                String message = "";

                // Create new packet object
                packet pak = new packet();
                while ((input = buffReader.readLine()) != null) {
                    String[] content_array = input.split("\\s+");
                    int i = 0;
                    while (i < content_array.length) {
                        // Create the message
                        pak.createPacket(content_array[i]);
                        // Generate the message based on the format
                        message = pak.generateMessage();
                        // Send message to network
                        writerOut.println(message);
                        ACK = bufferIn.readLine();

                        totalSent++;
                        //System.out.println(ACK + " " + pak.seqNo);
                        if (ACK.equals("ACK2")) { // DROP
                            System.out.println("Waiting: " + ACK + ", " + totalSent + ", DROP, resend Packet" + pak.seqNo);
                        }
                        else if (validateACK(ACK, pak.seqNo)) { // PASS
                            i++;
                            System.out.println("Waiting: " + ACK + ", " + totalSent + ", " + ACK + ", no more packets to send");
                        }
                        else { // CORRUPT
                            System.out.println("Waiting: " + ACK + ", " + totalSent + ", " + ACK + ", send Packet" + pak.seqNo);
                        }
                    }
                    writerOut.println(-1);
                }
                dataIn.close();
                writerOut.close();
                bufferIn.close();
                socket.close();
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        } catch (UnknownHostException e) {
            System.err.println("Cannot find the host: " + hostName);
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Couldn't read/write from the connection: " + e.getMessage());
            System.exit(1);
        }
    }
}