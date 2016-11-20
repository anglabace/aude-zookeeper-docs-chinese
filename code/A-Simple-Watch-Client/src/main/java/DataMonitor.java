import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.Arrays;

/**
 * Created by lrkin on 2016/11/20.
 */
public class DataMonitor implements Watcher, AsyncCallback.StatCallback {
    ZooKeeper zk;
    String znode;
    Watcher chainedWatcher;
    boolean dead;
    DataMonitorListener listener;
    byte prevData[];

    public DataMonitor(ZooKeeper zk, String znode, Watcher chainedWatcher, DataMonitorListener listener) {
        this.zk = zk;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;
        //在开始的时候检测znode节点是否存在.这就是所谓的事件驱动
        zk.exists(znode, true, this, null);
    }

    /**
     * 如果其他的类要使用DataMonitor,就需要实现这个接口
     */
    public interface DataMonitorListener {
        /**
         * 节点的存在状态改变了
         *
         * @param data
         */
        void exists(byte data[]);

        /**
         * ZooKeeper的session已经不再有效了
         *
         * @param rc
         */
        void closing(int rc);
    }

    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (rc) {
            case KeeperException.Code.Ok:
                exists = true;
                break;
            case KeeperException.Code.NoNode:
                exists = false;
                break;
            case KeeperException.Code.SessionExpired:
            case KeeperException.Code.NoAuth:
                dead = true;
                listener.closing(rc);
                return;
            default:
                //重试错误
                zk.exists(znode, true, this, null);
                return;
        }
        byte b[] = null;
        if (exists) {
            try {
                b = zk.getData(znode, false, null);
            } catch (KeeperException e) {
//                我们不需要担心现在恢复。 watch回调将启动任何异常处理
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }
        if ((b == null && b != prevData) || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;
        }

    }

    public void process(WatchedEvent event) {
        String path = event.getPath();
        if (event.getType() == Event.EventType.None) {
            //我们被告知连接的状态已经改变
            switch (event.getState()) {
                case SyncConnected:
                    //在这个特定的例子中，我们不需要在这里做任何事情
                    //- watch会自动重新注册服务器,在client断连时触发的wathces,会被按顺序的传送
                    break;
                case Expired:
                    //结束
                    dead = true;
                    listener.closing(KeeperException.Code.SessionExpired);
                    break;
            }
        } else {
            if (path != null && path.equals(znode)) {
                //有事情在节点上发生了变化，让我们来了解一下
                zk.exists(znode, true, this, null);
            }
        }
        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }


}
