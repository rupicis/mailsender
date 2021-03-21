package app;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
  public static String host = "";
  public static String port = "";
  public static String user = "";
  public static String pasw = "";
  public static String email = "";

  public static void load() {
    try (InputStream input = new FileInputStream("config.cfg")) {
      Properties prop = new Properties();
      prop.load(input);
      prop.forEach((k, v) -> {
        try {
          Config.class.getField((String) k).set(null, v);
        } catch (Throwable tr) {
        }
      });
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
