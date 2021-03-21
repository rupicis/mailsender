package http;

import java.io.InputStream;

public abstract class BodyParser implements HttpParser {
  public abstract InputStream getInputStream();

  public abstract byte[] get();
}
