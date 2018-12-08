import thenguyen.pw.helper.Lib;
import thenguyen.pw.model.Memo;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import com.thedeanda.lorem.*;

public class Main {

    private static Option option;

    private static Queue memoQueue;
    private static final int queueSize = 5000;

    // threads
    private static Thread serverThread;

    // sockets
    private static ServerSocket tcpSocket;
    private static DatagramSocket udpSocket;

    // lorem
    private static Lorem lorem;

    public static void main(String[] args) {

        // init queue
        memoQueue = new LinkedList<Memo>();

        final ArgumentParser argumentParser=ArgumentParsers.newArgumentParser("Main", true)
                .description("Packet Watcher Cli App 1.0").version("version 1.0");

        // mandatory arguments
        argumentParser.addArgument("-m", "--mode")
                .dest("mode")
                .required(true)
                .choices("server", "client")
                .help("Please choose server or client mode");
        argumentParser.addArgument("-p", "--protocol")
                .dest("protocol")
                .required(true)
                .choices("udp", "tcp")
                .help("Please choose tcp or udp protocol");
        argumentParser.addArgument("-t", "--type")
                .dest("type")
                .required(true)
                .choices("auto", "manual")
                .help("Please choose auto or manual trigger type.");

        // optional arguments
        argumentParser.addArgument("-l", "--listen")
                .dest("listenPort")
                .required(false)
                .type(Integer.class)
                .setDefault(0)
                .choices(Arguments.range(2000, 65535))
                .help("Server listening port (required in server mode)");

        // optional arguments
        argumentParser.addArgument("-si", "--serverip")
                .dest("serverIp")
                .required(false)
                .help("Server IP address (required in client mode)");

        argumentParser.addArgument("-sp", "--serverport")
                .dest("serverPort")
                .required(false)
                .type(Integer.class)
                .setDefault(0)
                .choices(Arguments.range(2000, 65535))
                .help("Server port number (required in client mode)");

        argumentParser.addArgument("-ps", "--packetsent")
                .dest("packetSent")
                .required(false)
                .type(Integer.class)
                .setDefault(10)
                .help("Please choose how many packets you want to send out (required in client + auto mode).");

        argumentParser.addArgument("-d", "--delay")
                .dest("packetSent")
                .required(false)
                .type(Integer.class)
                .setDefault(10)
                .help("Please choose maximum delay between 2 consecutive outgoing packets (required in client + auto mode).");

        argumentParser.addArgument("-v", "--verbose")
                .dest("verbose")
                .type(Boolean.class)
                .required(false)
                .nargs("?")
                .setConst(false)
                .help("Enable verbose mode.");

        option = new Option();

        try {
            argumentParser.parseArgs(args, option);

            if(option.mode.equals("server")) {
                startServer();
            } else {
                startClient();
            }
        } catch (ArgumentParserException e) {
            argumentParser.handleError(e);
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("W: Interrupt received, shutdown sockets & threads…");
            try {
                serverThread.interrupt();
                tcpSocket.close();
                udpSocket.close();
            } catch (IOException ignored) {

            }
        }));
    }

    /**
     * Server methods
     */
    private static void startServer() {
        try {
            System.out.println("Start server mode, protocol: " + option.protocol);

            if (option.protocol.equals("tcp")) {
                tcpSocket = new ServerSocket(option.listenPort);

                // create server thread
                serverThread = new Thread(Main::tcpServerRunnableTask);
                serverThread.setDaemon(true);
                serverThread.start();

            } else {
                // can add the actual interface here InetAddress.getByName("25.16.167.171")
                udpSocket = new DatagramSocket(option.listenPort);

                // create server thread
                serverThread = new Thread(new PwUdpServer(udpSocket, memoQueue, queueSize));
                serverThread.setDaemon(true);
                serverThread.start();
            }

            // wait & print receiving memo
            /*while (true) {
                synchronized (memoQueue) {
                    while (memoQueue.isEmpty()) {
                        memoQueue.wait();
                    }
                }

                Memo memo = (Memo) memoQueue.poll();
                if(memo != null)
                    System.out.println(memo.printMemo());
                else
                    System.out.println("NULL VALUE ????");

                synchronized (memoQueue) {
                    memoQueue.notify();
                }
            }*/

            System.in.read();
        } catch (SocketException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.err.println(e.getMessage());
        } /*catch (InterruptedException e) {
            System.err.println(e.getMessage());
        }*/
    }

    private static void tcpServerRunnableTask() {
        System.out.println("Packet Watcher | Start listening for tcp packets");
        try {
            while (true) {
                Socket socket = tcpSocket.accept();
                System.out.println("New tcp client connected");
                Thread newTcpConn = option.type.equals("auto") ? new Thread(new PwMultipleTcpRunnable(socket, memoQueue, queueSize)) :
                        new Thread(new PwSingleTcpRunnable(socket, memoQueue, queueSize));
                newTcpConn.setDaemon(true);
                newTcpConn.start();
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * Client methods
     */
    private static void startClient() {
        System.out.println("Start client mode, protocol: " + option.protocol);
        String userInput;
        Scanner sn = new Scanner(System.in);

        if(option.type.equals("auto")) {
            System.out.println("*****Auto Options*****");
            System.out.println(String.format("Sending out %d packets", option.packetSent));

            System.out.println("Prepare sending out packets");
            autoSendMemo();
            System.out.println("Finish sending out packets");

        } else {
            while (true) {
                //Print the options for the user to choose from
                System.out.println("*****Available Options*****");
                System.out.println("*. Press 1 for sending memo using " + option.protocol + "!");
                System.out.println("*. Press 2 to exit");

                // Prompt the use to make a choice
                System.out.println("Enter your choice:");

                userInput = sn.next();

                // check
                switch (userInput) {
                    case "1":
                        manualSendMemo();
                        break;
                    default:
                        System.out.println("Exit system");
                        System.exit(0);
                }
            }
        }
    }



    private static void autoSendMemo() {
        try {
            // init lorem
            lorem = LoremIpsum.getInstance();
            InetAddress serverAddr = InetAddress.getByName(option.serverIp);

            if(option.protocol.equals("tcp")) {
                autoSendMultipleTcpPacketOneConnection(serverAddr);
            } else {
                autoSendMultipleUdpPackets(serverAddr);
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Cannot send packet! Error " + e.getMessage());
        }
    }

    private static void manualSendMemo() {
        try {
            Scanner sn = new Scanner(System.in);

            // let user input memo
            System.out.println("Enter memo's title ('exit' to exit system):");
            String memoTitle = sn.next();

            // Eat the new line
            sn.nextLine();

            System.out.println("Enter memo's content ('exit' to exit system):");
            String memoContent = sn.next();

            if(!memoTitle.toLowerCase().equals("exit") && !memoContent.toLowerCase().equals("exit")) {
                // create memo
                Memo memo = new Memo(memoTitle, memoContent);

                manualSendPacket(memo);

                System.out.println("Send " + option.protocol + " packet to " + option.serverIp + ":" +
                        option.serverPort + " successfully!");
            } else {
                System.out.println("Exit system");
                System.exit(0);
            }
        } catch (IOException e) {
            System.err.println("Cannot send packet! Error " + e.getMessage());
        }
    }

    private static void manualSendPacket(Memo memo) throws IOException {
        InetAddress serverAddr = InetAddress.getByName(option.serverIp);
        // send memo via packet depends on chosen protocol
        if(option.protocol.equals("tcp")) {
            Socket sendSocket = new Socket(serverAddr, option.serverPort);
            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(sendSocket.getOutputStream()));

            // send tcp packet
            out.writeObject(memo);
            out.flush();

            // close stream & socket
            out.close();
            sendSocket.close();
        } else {
            DatagramSocket sendSocket = new DatagramSocket();

            // serialize memo to byte array
            byte[] memoByteArray = Lib.objectToByteArray(memo);
            DatagramPacket packet = new DatagramPacket(memoByteArray, memoByteArray.length,
                    serverAddr, option.serverPort);

            // send packet
            sendSocket.send(packet);

            // close socket
            sendSocket.close();
        }

        System.out.println("Send " + option.protocol + " packet to " + option.serverIp + ":" +
                option.serverPort + " successfully!");
    }

    private static void autoSendMultipleTcpPacketOneConnection(InetAddress serverAddr) throws IOException, InterruptedException {
        Socket sendSocket = new Socket(serverAddr, option.serverPort);
        ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(sendSocket.getOutputStream()));

        for(int i = 0; i <= option.packetSent; i++) {
            System.out.println(String.format("Sending %s packet#%d - Remain:%d", option.protocol,
                    i, option.packetSent - i));

            Memo memo = new Memo(String.format("Packet %d", i),
                    String.format("Content of packet %d", i));

            // send tcp packet
            out.writeObject(memo);
            out.flush();

            System.out.println("Send " + option.protocol + " packet to " + option.serverIp + ":" +
                    option.serverPort + " successfully!");

            Thread.sleep(100);
        }

        // send closed packet
        Memo quitMemo = new Memo("quit", "quit");

        out.writeObject(quitMemo);
        out.flush();

        // close stream & socket
        out.close();
        sendSocket.close();
    }

    private static void autoSendMultipleUdpPackets(InetAddress serverAddr) throws IOException, InterruptedException {
        DatagramSocket sendSocket = new DatagramSocket();

        for(int i = 0; i <= option.packetSent; i++) {
            System.out.println(String.format("Sending %s packet#%d - Remain:%d", option.protocol,
                    i, option.packetSent - i));

            Memo memo = new Memo(lorem.getWords(3,7),lorem.getWords(6,15));

            // serialize memo to byte array
            byte[] memoByteArray = Lib.objectToByteArray(memo);
            DatagramPacket packet = new DatagramPacket(memoByteArray, memoByteArray.length,
                    serverAddr, option.serverPort);

            // send packet
            sendSocket.send(packet);

            System.out.println("Send " + option.protocol + " packet to " + option.serverIp + ":" +
                    option.serverPort + " successfully!");

            Thread.sleep(100);
        }

        // close socket
        sendSocket.close();
    }
}
