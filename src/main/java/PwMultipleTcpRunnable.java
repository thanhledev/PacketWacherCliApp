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
        current = Thread.currentThread();

        while (true) {
            try {
                ObjectInputStream inStream = new ObjectInputStream(new BufferedInputStream(sSocket.getInputStream()));

                // get memo
                Memo memo = (Memo) inStream.readObject();

                if(memo.getTitle().equals("quit") || memo.getContent().equals("quit")) {
                    break;
                }

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

            } catch (IOException e) {
                System.err.println(e.getMessage());
            } catch (ClassNotFoundException e) {
                System.err.println(e.getMessage());
            } catch (InterruptedException e) {
                System.err.println(e.getMessage());
            }
        }
    }

    @Override
    public void stop() {
        current.interrupt();
    }
}