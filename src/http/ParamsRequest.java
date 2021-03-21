package http;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParamsRequest {

  private Map<String, List<String>> query_pairs;

  public ParamsRequest(HttpCall call) {
    query_pairs = new HashMap<String, List<String>>();
    try {
      String content = "";
      if ("POST".equals(call.method.method)) {
        content = new String(call.body.get());
      } else {
        int offset = call.method.query.indexOf('?');
        if (offset == -1)
          return;
        content = call.method.query.substring(offset + 1);
      }
      for (String pair : content.split("&")) {
        int idx = pair.indexOf("=");
        String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
        if (!query_pairs.containsKey(key)) {
          query_pairs.put(key, new ArrayList<String>());
        }
        String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
            : "";
        query_pairs.get(key).add(value);
      }
    } catch (Exception e) {
    }
  }

  public String get(String key) throws BadRequestException {
    List<String> node = query_pairs.get(key);
    if (node == null)
      throw new BadRequestException("Missing parameter:" + key);
    return node.get(0);
  }

  public int getInt(String key) throws BadRequestException {
    List<String> node = query_pairs.get(key);
    if (node == null)
      throw new BadRequestException("Missing parameter:" + key);
    try {
      return Integer.parseInt(node.get(0));
    } catch (Throwable tr) {
      throw new BadRequestException("Illegal value");
    }
  }

  public List<String> getAll(String key) throws Exception {
    return query_pairs.get(key);
  }

  public int getIntOptional(String key) throws BadRequestException {
    List<String> node = query_pairs.get(key);
    if (node == null)
      return 0;
    try {
      return Integer.parseInt(node.get(0));
    } catch (Throwable tr) {
      throw new BadRequestException("Illegal value");
    }
  }

  public String getOptional(String key, String def) {
    List<String> node = query_pairs.get(key);
    if (node == null)
      return def;
    return node.get(0);
  }
}
