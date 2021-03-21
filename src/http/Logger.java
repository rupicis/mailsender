package http;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Logger {
  private FileOutputStream        zip  = null;
  private ZipOutputStream         zout = null;
  private ByteArrayOutputStream   errb = new ByteArrayOutputStream();
  private PrintStream             err;
  public String                   time = main.Util.timestampU();
  private HashMap<String, byte[]> data = new HashMap<String, byte[]>();
  public String                   type = "GW";

  private void open() throws Exception {
    if (zout != null) return;
    File dir = new File(".", "HTTPLOG");
    if (!dir.exists()) dir.mkdirs();
    zip = new FileOutputStream(new File(dir, time + ".zip"));
    zout = new ZipOutputStream(zip);
  }

  public void log8(String name, String data) throws Exception {
    byte b[] = new byte[data.length()];
    data.getBytes(0, data.length(), b, 0);
    log(name, b);
  }

  public void log(String name, byte entr[]) throws Exception {
    data.put(name, entr);
  }

  public void close() throws Exception {
    open();

    try {
      if (err != null) err.close();
      byte ex[] = errb.toByteArray();
      if (ex.length > 0) log("trace.txt", ex);
      for (Map.Entry<String, byte[]> e : data.entrySet()) {
        zout.putNextEntry(new ZipEntry(e.getKey()));
        zout.write(e.getValue());
        zout.closeEntry();
        zout.flush();
      }
      zout.close();
    }
    catch (Throwable ex) {}
    try {
      zip.close();
    }
    catch (Throwable ex) {}
  }

  public void exeption(Throwable tr) {
    try {
      if (err == null) err = new PrintStream(errb, false, "UTF-8");
      tr.printStackTrace(err);
    }
    catch (Throwable e) {}
  }

  public void trace(String str) {
    try {
      if (err == null) err = new PrintStream(errb, false, "UTF-8");
      err.println(str);
    }
    catch (Throwable e) {}
  }

}
