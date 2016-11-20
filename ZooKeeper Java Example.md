# 简单的Watch客户端

为了介绍Zookeeper JAVA API，我们开发了一个简单的watch客户端.这个客户端会监听一个Znode,并以启动或结束一个可执行程序来做回应.

## 要求

客户端有四个要求：

- 它需要以下参数：

- - Zookeeper服务的地址
  - 然后是被监控节点znode的名字
  - 一个可执行文件的参数

- 它获取与znode相关联的数据并启动可执行文件。

- 如果znode改变，客户端重新获取内容并重新启动可执行文件。

- 如果znode消失，客户端结束这个可执行文件。

## 程序设计

按照惯例，Zookeeper应用分为两个单元，一个维持连接，另一个监控数据。在这个应用里，Executor类维持Zookeeper连接，DataMonitor类监控Zookeeper树形节点的数据。另外，Executor包含主线程和执行逻辑。它负责和用户交互.

# Executor类

Executor对象是实例应用的主容器。它包含Zookeeper对象和DataMonitor.

```java
 // from the Executor class...
    
    public static void main(String[] args) {
        if (args.length < 4) {
            System.err
                    .println("USAGE: Executor hostPort znode filename program [args ...]");
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

    public Executor(String hostPort, String znode, String filename,
            String exec[]) throws KeeperException, IOException {
        this.filename = filename;
        this.exec = exec;
        zk = new ZooKeeper(hostPort, 3000, this);
        dm = new DataMonitor(zk, znode, null, this);
    }

    public void run() {
        try {
            synchronized (this) {
                while (!dm.dead) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
        }
    }
```

