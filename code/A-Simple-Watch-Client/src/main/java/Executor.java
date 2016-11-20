import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.*;

/**
 * Created by lrkin on 2016/11/20.
 */
public class Executor implements Watcher, Runnable, DataMonitor.DataMonitorListener {

    String znode;
    DataMonitor dm;
    ZooKeeper zk;
    String filename;
    String exec[];
    Process child;

    public Executor(String hostPort, String znode, String filename, String[] exec) throws IOException {
        this.filename = filename;
        this.exec = exec;
        zk = new ZooKeeper(hostPort, 3000, this);
        dm = new DataMonitor(zk, znode, null, this);
    }

    public static void main(String[] args) {
        if (args.length < 4) {
            System.err.println("USAGE:Executor hostPort znode filename program [args...]");
            System.exit(2);
        }
        String hostPort = args[0];
        String znode = args[1];
        String filename = args[2];
        String exec[] = new String[args.length - 3];
        System.arraycopy(args, 3, exec, 0, exec.length);
        try {
            new Executor(hostPort, znode, filename, exec).run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 我们自己处理任何事件，我们只需要转发它们
     *
     * @param event
     */
    public void process(WatchedEvent event) {
        dm.process(event);
    }

    static class StreamWriter extends Thread {
        OutputStream os;
        InputStream is;

        public StreamWriter(InputStream is, OutputStream os) {
            this.os = os;
            this.is = is;
            start();
        }

        @Override
        public void run() {
            byte[] bytes = new byte[80];
            int rc;
            try {
                while ((rc = is.read(bytes)) > 0) {
                    os.write(bytes, 0, rc);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void exists(byte[] data) {
        if (data == null) {
            if (child != null) {
                System.out.println("Killing process");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            child = null;
        } else {
            if (child != null) {
                System.out.println("Stopping child");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(data);
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("Starting child");
                child = Runtime.getRuntime().exec(exec);
                new StreamWriter(child.getInputStream(), System.out);
                new StreamWriter(child.getErrorStream(), System.err);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }

    public void run() {
        try {
            synchronized (this) {
                while (!dm.dead) {
                    wait();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
