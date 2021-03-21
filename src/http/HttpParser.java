package http;

import java.nio.ByteBuffer;

public interface HttpParser {
  public HttpParser add(HttpCall call, ByteBuffer data);
}
