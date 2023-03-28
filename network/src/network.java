import java.io.*;
import java.net.*;
import java.util.*;


public class network {

    public static class packet {
        Integer seqNum;
        Integer packetID;
        Integer checksum;
        public String content;

        public packet() {
            seqNum = 1;
            packetID = 0;
        }
        public void parseMessage(String msg) {
            String[] splitMsg = msg.split("\\s+");
            seqNum = Integer.parseInt(splitMsg[0]);
            packetID = Integer.parseInt(splitMsg[1]);
            checksum = Integer.parseInt(splitMsg[2]);
            content = splitMsg[3];
        }
    }
    static List<Object> threadList = new ArrayList<>();
    static ServerSocket serverSocket;

    public static void main(String[] args){

        if (args.length != 1)
        {
            System.exit(1);
        }

        try {
            serverSocket = new ServerSocket(Integer.parseInt(args[0]));
            Socket receiverSocket = serverSocket.accept();
            new MessageThread(receiverSocket).start();
            Socket senderSocket = serverSocket.accept();
            new MessageThread(senderSocket).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static class MessageThread extends Thread {
        private Socket socket;
        MessageThread mt = null;
        int ID;
        PrintWriter writer;

        public MessageThread(Socket socket) {
            this.socket = socket;
            threadList.add(this);
            ID = threadList.size() - 1;
        }

        public void run() {
            String input;

            try {
                writer = new PrintWriter(socket.getOutputStream(), true);
                BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                while ((input = br.readLine()) != null) {
                    if (input.equals("-1")) {
                        if (ID == 1) {
                            mt = (MessageThread)threadList.get(0);
                            mt.writer.println("-1");
                        }
                        break;
                    }

                    String[] splitMsg = input.split("\\s+");
                    double x = Math.random();

                    if (x < 0.5 || splitMsg.length == 1) // PASS
                    {
                        if (splitMsg[0].contains("ACK")) {
                            System.out.println("Received: " + splitMsg[0] + ", PASS");
                        }
                        else {
                            System.out.println("Received: ACK" + splitMsg[0] + ", PASS");
                        }
                        mt = ID == 0 ? (MessageThread)threadList.get(1) : (MessageThread)threadList.get(0);
                        mt.writer.println(input);
                    }
                    else if (x <= 0.75) // CORRUPT
                    {
                        packet p = new packet();
                        p.parseMessage(input);
                        p.checksum += 1;
                        System.out.println("Received: Packet" + splitMsg[0] + ", " + splitMsg[1] + ", CORRUPT");
                        mt = ID == 0 ? (MessageThread)threadList.get(1) : (MessageThread)threadList.get(0);
                        mt.writer.println(p.seqNum + " "  + p.packetID + " " + p.checksum + " " + p.content);
                    }
                    else // DROP
                    {
                        System.out.println("Received: Packet" + splitMsg[0] + ", " + splitMsg[1] + ", DROP");
                        writer.println("ACK2");
                    }
                }
                socket.close();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}