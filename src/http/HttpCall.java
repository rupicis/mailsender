package http;

import static main.Util.showtime;
import static main.Util.timestamp;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import app.RequestHandler;

public class HttpCall implements Runnable {
  public RequestMethodParser method;
  public HeaderParser header;
  public BodyParser body;
  public int err;
  public String errDesc;
  public ByteArrayOutputStream output;
  public ByteBuffer directBody;
  public HashMap<String, String> attributes;
  public Logger log;
  public String reqName;
  // non reset
  Connection nconn;

  protected int maxInputSize;
  private HttpParser p;

  private boolean keep;

  public HttpCall() {
    reset();
  }

  public void reset() {
    reqName = "req.txt";
    err = 200;
    errDesc = "OK";
    output = new ByteArrayOutputStream();
    directBody = null;
    attributes = new HashMap<String, String>();
    log = new Logger();
    header = new HeaderParser();
    body = null;
    p = method = new RequestMethodParser();
    maxInputSize = 1024 * 1024;
  }

  public void exception(Throwable tr) {
    log.exeption(tr);
  }

  public void protocolError(int e, String text) {
    err = e;
    errDesc = text;
  }

  public HttpParser getBodyHandler() {
    if ("HEAD".equals(method.method))
      return null;
    String enc = header.get("Transfer-Encoding");
    if (enc != null) {
      if (!"chunked".equals(enc)) {
        protocolError(400, "unsupported transfer encoding");
        return null;
      }
      return body = new ChunkedBodyParser();
    }
    String clen = header.get("Content-Length");
    if (clen != null) {
      try {
        int l = Integer.parseInt(clen);
        if (l > maxInputSize) {
          protocolError(400, "maximum request size exceeded");
          return null;
        }
        return body = new FixedLengthBodyParser(l);
      } catch (Throwable tr) {
        protocolError(400, "invalid content length");
        return null;
      }
    }
    return null; // no body expected
  }

  public void trace(String str) {
    log.trace(str);
  }

  private byte[] responceHeader() throws Exception {
    StringBuilder head = new StringBuilder();
    head.append(method.version + " " + err + " " + errDesc + "\r\n");
    for (Map.Entry<String, String> e : attributes.entrySet())
      head.append(e.getKey() + ": " + e.getValue() + "\r\n");
    head.append("\r\n");
    byte h[] = head.toString().getBytes();
    log.log("resp.http", h);
    return h;
  }

  public boolean parse(ByteBuffer data) {
    while ((p != null) && data.remaining() > 0)
      p = p.add(this, data);
    return p == null;
  }

  public void run() {
    boolean delay = false;
    nconn.setTimeout(60000 * 3);
    try {
      if (header == null)
        log.log8("req.http", method.toString());
      else
        log.log8("req.http", method.toString()
            + header.toString().replaceAll("Pasw: [^\\s]+", "Pasw: ðeit bija sivçns"));
      keep = "Keep-Alive".equals(header.get("Connection"));
      if (err == 200) {
        try {
          delay = new RequestHandler().dispatchRequest(this);
        } catch (BadRequestException bad) {
          exception(bad);
          err = 400;
          errDesc = bad.getMessage();
        } catch (Throwable tr) {
          exception(tr);
          err = 500;
          errDesc = "internal error @" + showtime(timestamp());
          keep = false;
        }
        if (!delay)
          respond();
      }
    } catch (Throwable tr) {
      exception(tr);
    } finally {
      if (!delay)
        endCall();
    }
  }

  public void complete() {
    try {
      respond();
    } catch (Throwable tr) {
      exception(tr);
    } finally {
      endCall();
    }
  }

  private void respond() throws Exception {
    if (keep)
      attributes.put("Connection", "keep-alive");
    if (body != null)
      log.log(reqName, body.get());

    if (directBody != null) {
      attributes.put("Content-Length", "" + directBody.remaining());
      nconn.outgoing(ByteBuffer.wrap(responceHeader()));
      if (directBody.hasRemaining()) {
        nconn.outgoing(directBody);
      }
    } else {
      byte[] resp = output.toByteArray();
      log.log("resp.json", resp);
      attributes.put("Content-Length", "" + resp.length);
      nconn.outgoing(ByteBuffer.wrap(responceHeader()));
      if (resp.length > 0)
        nconn.outgoing(ByteBuffer.wrap(resp));
    }
  }

  private void endCall() {
    try {
      log.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    if (keep) {
      reset();
      p = method = new RequestMethodParser();
      nconn.enableReading(true);
    } else
      nconn.end();
  }

  public String from() {
    return nconn.from;
  }

}
