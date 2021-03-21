package http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.ThreadPoolExecutor;

public class Listener implements Runnable {
  public static ThreadPoolExecutor cpu;

  private boolean cancel, stopped;
  private ServerSocketChannel ss;
  private Selector sel;
  private HashSet<Connection> conn = new HashSet<Connection>();
  private long time;

  public Listener(int port) throws IOException {
    sel = SelectorProvider.provider().openSelector();
    ss = ServerSocketChannel.open();
    ss.configureBlocking(false);
    InetSocketAddress isa = new InetSocketAddress(port);
    ss.socket().bind(isa);
    ss.register(sel, SelectionKey.OP_ACCEPT);
    Thread t = new Thread(this, "NIO server on port " + port);
    t.setContextClassLoader(null);
    t.start();
  }

  long getTime() {
    return time;
  }

  void wake() {
    sel.wakeup();
  }

  public void close() {
    cancel = true;
    while (!stopped) {
      try {
        Thread.sleep(10);
      } catch (Exception e) {
      }
    }
  }

  private void stop() {
    try {
      sel.close();
      for (Connection c : conn)
        c.close();
      ss.socket().close();
      ss.close();
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  public void run() {
    try {
      Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
      while (!cancel)
        selectorLoop();
    } finally {
      stop();
      stopped = true;
    }
  }

  private void selectorLoop() {
    try {
      sel.select(100);
      time = System.currentTimeMillis();
      Iterator<?> selectedKeys = sel.selectedKeys().iterator();
      while (selectedKeys.hasNext()) {
        SelectionKey key = (SelectionKey) selectedKeys.next();
        selectedKeys.remove();
        if (!key.isValid())
          continue;
        if (key.isAcceptable())
          doAccept();
        else if (key.isReadable())
          doRead(key);
        else if (key.isWritable())
          doWrite(key);
      }
      //
      // expire incative
      //
      Iterator<Connection> i = conn.iterator();
      while (i.hasNext()) {
        Connection c = i.next();
        if (c.expired()) {
          c.close();
        }
        if (c.isClosed()) {
          i.remove();
        }
      }
      //
      // register writes
      //
      for (Connection c : conn) {
        c.checkMode();
      }

    } catch (Exception e) {
      e.printStackTrace();
      // cancel = true;
    }
  }

  private void doAccept() throws IOException {
    SocketChannel socketChannel = ss.accept();
    socketChannel.configureBlocking(false);
    socketChannel.socket().setTcpNoDelay(true);
    SelectionKey key = socketChannel.register(sel, SelectionKey.OP_READ);
    Connection c = new Connection(this, socketChannel, key, socketChannel.socket().getInetAddress().getHostAddress());
    conn.add(c);
  }

  private void doRead(SelectionKey key) {
    Connection nc = (Connection) key.attachment();
    nc.read();
  }

  private void doWrite(SelectionKey key) {
    Connection nc = (Connection) key.attachment();
    nc.writeOutput();
  }
}
