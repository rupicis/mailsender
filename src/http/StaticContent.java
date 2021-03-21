package http;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Hashtable;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class StaticContent {
  private static Hashtable<String, ByteBuffer> body = new Hashtable<String, ByteBuffer>();
  private static Hashtable<String, ByteBuffer> zbody = new Hashtable<String, ByteBuffer>();
  private static Hashtable<String, String> type = new Hashtable<String, String>();

  public static void AddZip(InputStream pack) {
    try {
      ZipInputStream zip = new ZipInputStream(pack);
      for (;;) {
        ZipEntry z = zip.getNextEntry();
        if (z == null)
          break;
        if (z.isDirectory())
          continue;
        ByteArrayOutputStream o = new ByteArrayOutputStream(2048);
        byte data[] = new byte[8192];
        while (true) {
          int n = zip.read(data, 0, 8192);
          if (n == -1)
            break;
          o.write(data, 0, n);
        }
        String name = z.getName();
        if (name.toLowerCase().endsWith(".jar"))
          AddZip(new ByteArrayInputStream(o.toByteArray()));
        else {
          byte entry[] = o.toByteArray();
          ByteBuffer buf = ByteBuffer.allocate(entry.length);
          buf.order(ByteOrder.nativeOrder());
          buf.put(entry);
          buf.flip();
          body.put(name, buf);
          ByteArrayOutputStream bout = new ByteArrayOutputStream();
          GZIPOutputStream gz = new GZIPOutputStream(bout);
          gz.write(entry);
          gz.close();
          byte gzipped[] = bout.toByteArray();
          buf = ByteBuffer.allocate(gzipped.length);
          buf.order(ByteOrder.nativeOrder());
          buf.put(gzipped);
          buf.flip();
          zbody.put(name, buf);

          File file = new File(name);
          String mimeType = URLConnection.guessContentTypeFromName(file.getName());
          if (mimeType == null)
            if (name.endsWith(".js"))
              mimeType = "application/javascript";
          if (name.endsWith(".svg"))
            mimeType = "image/svg+xml";
          if (name.endsWith(".css"))
            mimeType = "text/css";
          if (mimeType != null)
            type.put(name, mimeType);
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }

  }

  public static ByteBuffer getEntry(String name) {
    return body.get(name);
  }

  public static ByteBuffer getZipEntry(String name) {
    return zbody.get(name);
  }

  public static String getType(String name) {
    return type.get(name);
  }

  public static void respond(HttpCall call, String page) {
    ByteBuffer data;
    String accept = call.header.get("accept-encoding");
    boolean zipped = accept == null ? false : accept.contains("gzip");

    data = zipped ? getZipEntry(page) : getEntry(page);
    if (data == null) {
      call.err = 404;
      call.errDesc = "Invalid URL";
    } else {
      String mime = getType(page);
      if (mime == null)
        call.attributes.remove("Content-Type");
      else
        call.attributes.put("Content-Type", mime);
      if (zipped)
        call.attributes.put("Content-Encoding", "gzip");
      call.directBody = data.asReadOnlyBuffer();
    }
  }

}
