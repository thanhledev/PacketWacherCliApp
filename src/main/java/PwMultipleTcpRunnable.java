import thenguyen.pw.tcp.TcpSenderRunnable;
import thenguyen.pw.model.Memo;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.Socket;
import java.util.Queue;

public class PwMultipleTcpRunnable extends TcpSenderRunnable {

    // additional properties
    private Queue mQueue;
    private int queueSize;

    public PwMultipleTcpRunnable(Socket socket, Queue queue, int size) {
        super(socket);
        this.mQueue = queue;
        this.queueSize = size;
    }

    @Override
    public void run() {
        try {
            current = Thread.currentThread();
            ObjectInputStream inStream = new ObjectInputStream(new BufferedInputStream(sSocket.getInputStream()));
            int count = 1;
            while (true) {
                try {
                    // get memo
                    Memo memo = (Memo) inStream.readObject();
                    if (memo.getTitle().equals("quit") || memo.getContent().equals("quit")) {
                        break;
                    } else {
                        System.out.println(String.format("%s received %d packets.",current.getName(),++count));
                    }

                    // add to queue
                    /*synchronized (mQueue) {
                        while (mQueue.size() == queueSize) {
                            mQueue.wait();
                        }
                    }

                    mQueue.add(memo);

                    synchronized (mQueue) {
                        mQueue.notify();
                    }*/

                } catch (ClassNotFoundException e) {
                    System.err.println(e.getMessage());
                } /*catch (InterruptedException e) {
                    System.err.println(e.getMessage());
                }*/
            }
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void stop() {
        current.interrupt();
    }
}
