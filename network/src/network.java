import java.io.*;
import java.net.*;
import java.util.*;


public class network {

    public static class packet {

        Integer seqNo;
        Integer packetID;
        Integer checksum;
        public String content;

        public packet() {
            seqNo = 1;
            packetID = 0;
        }

        // Generates the message
        public String generateMessage() {
            return seqNo + " " + packetID + " " + checksum + " " + content;
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

    }

    // Array list for the threads
    static List<Object> allMessages = new ArrayList<>();
    static ServerSocket serverSocket;
    //static boolean listening = true;

    public static void main(String[] args){

        if (args.length != 1)
        {
            System.err.println("Usage: java network <port number>");
            System.exit(1);
        }


        try {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
            System.out.println("Waiting... connect receiver");
            //while (listening) {
            Socket receiverSocket = serverSocket.accept();
            new MessageThread(receiverSocket).start();
            Socket senderSocket = serverSocket.accept();
            new MessageThread(senderSocket).start();
            //}
        } catch (Exception e) {
            System.out.println("I/O failure: " + e.getMessage());
            e.printStackTrace();
        }
    }


    public static class MessageThread extends Thread {
        private Socket socket;
        MessageThread mt = null;
        int ID = 0;

        public MessageThread(Socket socket) {
            this.socket = socket;
            allMessages.add(this);
            ID = allMessages.size() - 1;
        }

        public void run() {
            String input = "";
            String ACK2 = "ACK2";

            try {
                PrintWriter writerOut;
                BufferedReader readerIn;
                writerOut = new PrintWriter(socket.getOutputStream(), true);
                readerIn = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                System.out.println("Get connection from: " + socket.getRemoteSocketAddress().toString());

                while ((input = readerIn.readLine()) != null) {
                    if (input.equals("-1")) {
                        if (ID == 1) {
                            mt = (MessageThread)allMessages.get(0);
                            mt.send("-1");
                        }
                        break;
                    }

                    String[] splitedMsg = input.split("\\s+");

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
                    }
                    // If corrupt, corrupt the checksum, generate the message and send to other thread
                    else if (x <= 0.75) // CORRUPT
                    {
                        packet p = new packet();
                        p.parseMessage(input);
                        p.checksum += 1;
                        System.out.println("Received: Packet" + splitedMsg[0] + ", " + splitedMsg[1] + ", CORRUPT");
                        mt = ID == 0 ? (MessageThread)allMessages.get(1) : (MessageThread)allMessages.get(0);
                        mt.send(p.generateMessage());
                    }
                    // If drop, send ACK2 back to sender
                    else // DROP
                    {
                        System.out.println("Received: Packet" + splitedMsg[0] + ", " + splitedMsg[1] + ", DROP");
                        writerOut.println(ACK2);
                    }
                }
                socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }

        // Send message
        public void send(String pMessage) {
            PrintWriter writerOut;
            try {
                writerOut = new PrintWriter(socket.getOutputStream(), true);
                writerOut.println(pMessage);
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}