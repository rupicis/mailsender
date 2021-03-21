package app;

import java.nio.charset.StandardCharsets;

import http.HttpCall;
import http.StaticContent;

public class StatusHandler implements EndpointHandler {

  public boolean handle(HttpCall call) throws Throwable {
    StringBuilder data = new StringBuilder();

    for (MailJob job : MailSender.getPending()) {
      data.append("<tr><td>");
      data.append(job.timestamp);
      data.append("</td><td>");
      data.append(job.from);
      data.append("</td><td>");
      data.append(job.getDescripotion());
      data.append("</td></tr>");
    }
    String template = new String(StaticContent.getEntry("queue.html").array(), StandardCharsets.UTF_8);
    call.output.write(template.replace("${kverijs}", data.toString()).getBytes(StandardCharsets.UTF_8));
    return false;
  }

}
