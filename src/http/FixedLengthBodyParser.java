package http;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

class FixedLengthBodyParser extends BodyParser {

  private byte data[];
  private int pos;

  FixedLengthBodyParser(int size) {
    data = new byte[size];
  }

  public InputStream getInputStream() {
    return new ByteArrayInputStream(data);
  }

  public HttpParser add(HttpCall call, ByteBuffer src) {
    int len = Math.min(data.length - pos, src.remaining());
    src.get(data, pos, len);
    pos += len;
    return pos == data.length ? null : this;
  }

  public byte[] get() {
    return data;
  }
}
