package app;

import java.time.Instant;

class MailJob {
  byte payment[];
  String from;
  Instant timestamp = Instant.now();
  Runnable donecallback;
  Runnable stuckcallback;

  public String getDescripotion() {
    return donecallback == null ? "Offline pending" : "User waiting";
  }
}
