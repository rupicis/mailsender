package http;

import java.nio.ByteBuffer;
import java.util.StringTokenizer;

public class RequestMethodParser implements HttpParser {
  private StringBuilder text = new StringBuilder();
  private boolean r = false;

  String method;
  public String query;
  String version;

  public String toString() {
    return text.toString() + "\r\n";
  }

  public HttpParser add(HttpCall call, ByteBuffer src) {
    while (src.hasRemaining()) {
      char c = (char) src.get();
      if (c == '\r')
        r = true;
      else if (c == '\n') {
        if (!r)
          continue;
        StringTokenizer t = new StringTokenizer(text.toString());
        try {
          method = t.nextToken().toUpperCase();
        } catch (Throwable tr) {
          call.protocolError(400, "no request method");
          return null;
        }
        try {
          query = t.nextToken();
        } catch (Throwable tr) {
          query = "/";
        }
        try {
          version = t.nextToken();
        } catch (Throwable tr) {
          version = "HTTP 1.0";
        }
        if (invalidMethod()) {
          call.protocolError(400, "unsupported request method");
          return null;
        }
        if (invalidVersion()) {
          call.protocolError(400, "unsupported protocol version");
          return null;
        }
        return call.header;
      } else
        text.append(c);
    }
    return this;
  }

  private boolean invalidVersion() {
    if ("HTTP/1.0".equals(version))
      return false;
    if ("HTTP/1.1".equals(version))
      return false;
    return true;
  }

  private boolean invalidMethod() {
    if ("POST".equals(method))
      return false;
    if ("GET".equals(method))
      return false;
    return true;
  }

  public String getPath() {
    int offset = query.indexOf('?');
    if (offset == -1)
      return query;
    return query.substring(0, offset);
  }

}
