package http;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Connection {

  //
  //
  // http handler part
  //
  //
  HttpCall call = new HttpCall();

  // forwarded ip

  public boolean outgoing(ByteBuffer data) {
    if (isClosed())
      return false;
    write(data);
    return true;
  }

  public void input(ByteBuffer data) {
    try {
      setTimeout(30000);
      data.flip();
      if (call.parse(data)) {
        enableReading(false);
        Listener.cpu.execute(call);
      }
    }

    catch (Throwable ex) {
      ex.printStackTrace();
      close();
    }
  }

  //
  //
  // NioSocket part
  //
  //
  //

  public static final int MAX_INPUT = 8192;

  private Listener srv;
  private SocketChannel chn;
  private long time;
  private int timeout = 60000;
  private ByteBuffer rbuf;
  private SelectionKey key;
  private BlockingQueue<ByteBuffer> output;
  private boolean readytoread, closed;
  Object userdata;
  String from;

  public Connection(Listener srv, SocketChannel chn, SelectionKey key, String from) {
    this.srv = srv;
    this.chn = chn;
    this.key = key;
    this.from = from;
    key.attach(this);
    rbuf = ByteBuffer.allocate(MAX_INPUT);
    rbuf.order(ByteOrder.LITTLE_ENDIAN);
    output = new ArrayBlockingQueue<ByteBuffer>(32);
    readytoread = true;
    activity();
    call.nconn = this;
  }

  public long activity() {
    time = srv.getTime();
    return time;
  }

  public void setTimeout(int val) {
    timeout = val;
  }

  public boolean expired() {
    return srv.getTime() > time + timeout;
  }

  public String getFrom() {
    return from;
  }

  public void enableReading(boolean val) {
    readytoread = val;
    srv.wake();
  }

  public void checkMode() {
    try {
      if (!output.isEmpty())
        key.interestOps(SelectionKey.OP_WRITE);
      else
        key.interestOps(readytoread ? SelectionKey.OP_READ : 0);
    } catch (CancelledKeyException ex) {
    }
  }

  public boolean isClosed() {
    return closed;
  }

  public void close() {
    if (closed)
      return;
    closed = true;
    output.clear();
    key.cancel();
    try {
      chn.close();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void end() {
    while (output.size() > 0)
      try {
        Thread.sleep(10);
      } catch (Exception e) {
      }
    close();
  }

  public void write(ByteBuffer data) {
    if (closed)
      return;
    try {
      output.put(data);
    } catch (Exception ex) {
    }
    srv.wake();
  }

  public void writeOutput() {
    while (!output.isEmpty()) {
      ByteBuffer buf = output.peek();
      try {
        chn.write(buf);
      } catch (IOException ex) {
        // ex.printStackTrace();
        close();
        return;
      }
      if (buf.remaining() > 0)
        break;
      output.poll();
      activity();
    }
  }

  public void read() {
    try {
      rbuf.clear();
      int n = chn.read(rbuf);
      if (n > 0) {
        activity();
        input(rbuf);
      } else if (n == -1)
        close();
    } catch (Throwable e) {
      close();
    }
  }

}
