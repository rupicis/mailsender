package app;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MailSender implements Runnable {
  private static BlockingQueue<MailJob> output = new ArrayBlockingQueue<MailJob>(1000);

  public static void start() {
    new Thread(new MailSender(), "Mail sender").start();
  }

  static void enqueue(MailJob msg) throws Exception {
    output.put(msg);
  }

  public void run() {
    for (;;) {
      try {
        loop();
        Thread.sleep(1000);
      } catch (Throwable tr) {
        tr.printStackTrace();
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
        }
      }
    }

  }

  public static MailJob[] getPending() {
    return output.toArray(new MailJob[0]);
  }

  private void loop() {

    if (output.isEmpty())
      return;
    Instant expire = Instant.now().minus(Duration.ofSeconds(30));
    output.forEach(job -> {
      if (job.timestamp.isBefore(expire))
        if (job.stuckcallback != null) {
          job.stuckcallback.run();
          job.stuckcallback = null;
          job.donecallback = null;
        }
    });

    try (Smtp conn = new Smtp()) {
      MailJob job = output.peek();
      if (job == null)
        return;
      conn.addText("this is some payment file");
      conn.addFile("payment.xml", job.payment);
      conn.send(Config.user.replace('-', '@'), Config.email, "Payment message from:" + job.from);
      if (job.donecallback != null)
        job.donecallback.run();
      output.poll();
    } catch (Throwable tr) {
      System.out.println("mail fail:" + tr.getMessage());
    }
  }

}
