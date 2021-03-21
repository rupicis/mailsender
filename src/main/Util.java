package main;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public final class Util {

  private static Object tslock = new Object();
  private static String lastTimestamp;
  private static int lastsuffix;

  public static String timestampU() {
    String t = timestamp();
    synchronized (tslock) {
      if (!t.equals(lastTimestamp)) {
        lastTimestamp = t;
        lastsuffix = 0;
      }
      return t + ++lastsuffix;
    }
  }

  public static String timestamp() {
    return timestamp(new Date(System.currentTimeMillis()));
  }

  public static String timestamp(Date d) {
    SimpleDateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
    return df.format(d);
  }

  public static String showtime(String d14) {
    if (d14 == null)
      return "";
    if (d14.length() < 14)
      return "";
    // minimal garbage convertation
    char ch[] = new char[19];
    ch[2] = ch[5] = '-';
    ch[10] = ' ';
    ch[13] = ch[16] = ':';
    d14.getChars(6, 8, ch, 0);
    d14.getChars(4, 6, ch, 3);
    d14.getChars(0, 4, ch, 6);
    d14.getChars(8, 10, ch, 11);
    d14.getChars(10, 12, ch, 14);
    d14.getChars(12, 14, ch, 17);
    return new String(ch);
  }

  private static AtomicLong delta = new AtomicLong();

  public static String generateID() {
    return Instant.now().plusNanos(delta.getAndIncrement() & 0xEFFFF).toString();
  }

}
