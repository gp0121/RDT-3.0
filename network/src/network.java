import java.io.*;
import java.net.*;
import java.util.*;


public class network {

    public static class packet {

        Integer seqNo;
        Integer packetID;
        Integer checksum;
        public String content;
        public boolean isLastMessage = false;

        public packet() {
            seqNo = 1;
            packetID = 0;
        }

        // Generates the message
        public String generateMessage() {
            return seqNo + " " + packetID + " " + checksum + " " + content;
        }

        // Sum the ascii characters of the word
        public Integer generateChecksum(String s) {
            int asciiInt;
            int sum = 0;
            for (int i = 0; i < s.length(); i++) {
                asciiInt = (int) s.charAt(i);
                sum = sum + asciiInt;

                //System.out.println(s.charAt(i) + "   " + asciiInt);
                if ((int) s.charAt(i) == 46) {
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

        // Corrupt the checksum by adding 1
        public void corruptChecksum() {
            checksum = checksum + 1;
        }

        // Check the checksum to see whether or not it is corrupt
        public String validateMessage() {
            Integer newChecksum = generateChecksum(content);
            if (newChecksum.equals(checksum)) {
                return "ACK" + seqNo.toString();
            }
            else {
                if (seqNo == 0) {
                    seqNo = 1;
                }
                else {
                    seqNo = 0;
                }
                return "ACK" + seqNo.toString();
            }
        }

        // Alternates the 0 and 1
        public void getSequenceNum() {
            if (seqNo == 0) {
                seqNo = 1;
            }
            else {
                seqNo = 0;
            }
        }

    }

    // Array list for the threads
    static List<Object> allMessages = new ArrayList<Object>();
    static ServerSocket serverSocket;
    //static boolean listening = true;

    public static void main(String[] args) throws IOException {
        int portNumber;

        if (args.length != 1)
        {
            System.err.println("Usage: java network <port number>");
            System.exit(1);
        }

        portNumber = Integer.parseInt(args[0]);

        try {
            serverSocket = new ServerSocket(portNumber);
            System.out.println("Waiting... connect receiver");
            //while (listening) {
            new MessageThread(serverSocket.accept()).start();
            new MessageThread(serverSocket.accept()).start();
            //}
        } catch (Exception e) {
            System.out.println("I/O failure: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static class MessageThread extends Thread {
        private Socket socket = null;
        MessageThread mt = null;
        int ID = 0;

        public MessageThread(Socket socket) {
            this.socket = socket;
            allMessages.add(this);
            ID = allMessages.size() - 1;
        }

        public void run() {
            String input = "";
            String ACK0 = "ACK0";
            String ACK1 = "ACK1";
            String ACK2 = "ACK2";

            try {
                PrintWriter writerOut = null;
                BufferedReader readerIn = null;
                writerOut = new PrintWriter(socket.getOutputStream(), true);
                readerIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                System.out.println("Get connection from: " + socket.getRemoteSocketAddress().toString());
                //System.out.println("Message Thread - Is connected: " + socket.isConnected());

                while ((input = readerIn.readLine()) != null) {
                    if (input.equals("-1")) {
                        // If sender thread, then send -1 to the receiver
                        if (ID == 1) {
                            mt = ID == 0 ? (MessageThread)allMessages.get(1) : (MessageThread)allMessages.get(0);
                            mt.send("-1");
                            //sendToOtherThread("-1");
                        }
                        break;
                    }

                    String[] splitedMsg = input.split("\\s+");
                    //System.out.println(input);

                    // Random - PASS, CORRUPT, DROP
                    double x = Math.random();

                    // If pass, then send the packet to the receiver
                    if (x < 0.5 || splitedMsg.length == 1) // PASS
                    {
                        //System.out.println(input);
                        if (splitedMsg[0].contains("ACK")) {
                            System.out.println("Received: " + splitedMsg[0] + ", PASS");
                        }
                        else {
                            System.out.println("Received: ACK" + splitedMsg[0] + ", PASS");
                        }
                        mt = ID == 0 ? (MessageThread)allMessages.get(1) : (MessageThread)allMessages.get(0);
                        mt.send(input);
                        //sendToOtherThread(input);
                    }
                    // If corrupt, corrupt the checksum, generate the message and send to other thread
                    else if (x >= 0.5 && x <= 0.75) // CORRUPT
                    {
                        //System.out.println(input);
                        packet p = new packet();
                        p.parseMessage(input);
                        p.corruptChecksum();
                        System.out.println("Received: Packet" + splitedMsg[0] + ", " + splitedMsg[1] + ", CORRUPT");
                        mt = ID == 0 ? (MessageThread)allMessages.get(1) : (MessageThread)allMessages.get(0);
                        mt.send(p.generateMessage());
                        //sendToOtherThread(p.generateMessage());
                    }
                    // If drop, send ACK2 back to sender
                    else // DROP
                    {
                        //System.out.println(input);
                        System.out.println("Received: Packet" + splitedMsg[0] + ", " + splitedMsg[1] + ", DROP");
                        writerOut.println(ACK2);
                    }
                }
                //socket.shutdownInput();
                //socket.shutdownOutput();
                socket.close();
                //System.out.println("Socket close? " + socket.isClosed());
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Send message
        public void send(String pMessage) {
            PrintWriter writerOut = null;

            try {
                writerOut = new PrintWriter(socket.getOutputStream(), true);
                writerOut.println(pMessage);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        // This method keeps track of which thread it is. It sends to the opposite thread.
        public void sendToOtherThread(String pMessage) {
            mt = ID == 0 ? (MessageThread)allMessages.get(1) : (MessageThread)allMessages.get(0);
            mt.send(pMessage);
        }
    }

}