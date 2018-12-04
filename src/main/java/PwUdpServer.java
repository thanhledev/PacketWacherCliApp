import thenguyen.pw.udp.UdpSenderRunnable;
import thenguyen.pw.model.Memo;
import thenguyen.pw.helper.Lib;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Queue;

public class PwUdpServer extends UdpSenderRunnable {

    // additional properties
    private Queue mQueue;
    private int queueSize;

    public PwUdpServer(DatagramSocket socket, Queue queue, int size) {
        super(socket);
        this.mQueue = queue;
        this.queueSize = size;
    }

    @Override
    public void run() {
        this.current = Thread.currentThread();
        System.out.println("Packet Watcher | Start listening for udp packets");

        try {
            while (true) {

                byte[] pBuffer = new byte[super.PACKET_SIZE];
                DatagramPacket rPacket = new DatagramPacket(pBuffer, pBuffer.length);

                // get packet from socket
                sSocket.receive(rPacket);

                // get memo
                Memo memo = Lib.arrayToMemo(rPacket.getData());

                // add to queue
                synchronized (mQueue) {
                    while (mQueue.size() == queueSize) {
                        mQueue.wait();
                    }
                }

                mQueue.add(memo);

                synchronized (mQueue) {
                    mQueue.notify();
                }
            }
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    @Override
    public void stop() {
        this.current.interrupt();
    }
}
