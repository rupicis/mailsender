package http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

class ChunkedBodyParser extends BodyParser {
  private ByteArrayOutputStream data = new ByteArrayOutputStream();
  private StringBuilder text = new StringBuilder();
  private boolean r;
  private int size = 0;
  private int received = 0;
  private byte chunk[];
  private int pos;

  public InputStream getInputStream() {
    return new ByteArrayInputStream(data.toByteArray());
  }

  public HttpParser add(HttpCall call, ByteBuffer src) {
    received += src.remaining();
    if (received > call.maxInputSize) {
      call.protocolError(400, "maximum request size exceeded");
      return null;
    }
    if (size == 0) {
      if (chunk != null) {
        if (!r && src.hasRemaining()) {
          char c = (char) src.get();
          if (c == '\r')
            r = true;
          else {
            call.protocolError(400, "malformed chunked encoding");
            return null;
          }
        }
        if (r && src.hasRemaining()) {
          char c = (char) src.get();
          if (c == '\n') {
            try {
              data.write(chunk);
            } catch (IOException e) {
            }
            chunk = null;
          } else {
            call.protocolError(400, "malformed chunked encoding");
            return null;
          }
        } else
          return this;
      }
      r = false;
      while (src.hasRemaining()) {
        char c = (char) src.get();
        if (c == '\r')
          r = true;
        else if (c == '\n') {
          if (!r)
            continue;
          try {
            size = Integer.parseInt(text.toString(), 16);
            if (size == 0)
              return null; // end
            if (received + size > call.maxInputSize) {
              call.protocolError(400, "maximum request size exceeded");
              return null;
            }
            chunk = new byte[size];
            pos = 0;
            r = false;
            text = new StringBuilder();
            break;
          } catch (Throwable tr) {
            call.protocolError(400, "invalid chunk size");
            return null;
          }
        } else
          text.append(c);
      }
    }
    for (;;) {
      int readLen = Math.min(size, src.remaining());
      if (readLen == 0)
        break;
      src.get(chunk, pos, readLen);
      pos += readLen;
      size -= readLen;
    }
    return this;
  }

  public byte[] get() {
    return data.toByteArray();
  }
}
