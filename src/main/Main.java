package main;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import app.Config;
import app.MailSender;
import http.Listener;
import http.StaticContent;

public class Main {

  public static void main(String args[]) {

    try {
      Listener.cpu = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
      Config.load();
      MailSender.start();
      StaticContent.AddZip(Main.class.getResourceAsStream("/static.zip"));
      int port = 80;
      if (args.length > 0)
        try {
          port = Integer.parseInt(args[0]);
        } catch (Throwable tr) {
        }
      new Listener(port);
      System.out.println("http on " + port);
    } catch (Throwable tr) {
      tr.printStackTrace();
    }
  }
}
