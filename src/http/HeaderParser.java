package http;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeaderParser implements HttpParser {
  private HashMap<String, List<String>> data = new HashMap<String, List<String>>();
  private StringBuilder line = new StringBuilder();
  private StringBuilder all = new StringBuilder();
  private boolean r = false;

  public HeaderParser() {
  }

  public String toString() {
    return all.toString();
  }

  public String get(String key) {
    List<String> l = data.get(key.toLowerCase());
    if (l == null)
      return null;
    return l.get(0);
  }

  public String get(String key, String def) {
    List<String> l = data.get(key.toLowerCase());
    if (l == null)
      return def;
    return l.get(0);
  }

  public String[] getAll(String key) {
    List<String> l = data.get(key.toLowerCase());
    if (l == null)
      return null;
    return l.toArray(new String[l.size()]);
  }

  public HttpParser add(HttpCall call, ByteBuffer src) {
    while (src.hasRemaining()) {
      char c = (char) src.get();
      all.append(c);
      if (c == '\r')
        r = true;
      else if (c == '\n') {
        if (!r)
          continue;
        if (line.length() == 0) {
          return call.getBodyHandler();
        }
        int pos = line.indexOf(": ");
        if (pos == -1) {
          call.protocolError(400, "malformed http header");
          return null;
        }
        String key = line.substring(0, pos).toLowerCase();
        List<String> l = data.get(key);
        if (l == null) {
          l = new ArrayList<String>(2);
          data.put(key, l);
        }
        l.add(line.substring(pos + 2).trim());
        r = false;
        line.setLength(0);
        return this;
      } else
        line.append(c);
    }
    return this;
  }

}
