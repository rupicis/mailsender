package app;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

public class Smtp implements Closeable {
  private Session session;
  private Transport tr;

  private ArrayList<MimeBodyPart> part = new ArrayList<MimeBodyPart>();

  public Smtp() throws Exception {
    Properties props = new Properties();
    props.put("mail.smtp.host", Config.host);
    props.put("mail.smtp.port", Config.port);
    props.put("mail.smtp.connectiontimeout", "" + 30000);
    props.put("mail.smtp.timeout", "" + 30000);
    props.put("mail.smtp.writetimeout", "" + 30000);
    props.put("mail.smtp.auth", "true");
    props.put("mail.smtp.starttls.enable", "true"); // TLS

    session = Session.getInstance(props, new javax.mail.Authenticator() {
      protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(Config.user, Config.pasw);
      }
    });
    tr = session.getTransport("smtp");
    tr.connect();
  }

  public void close() {
    try {
      tr.close();
    } catch (Throwable tr) {
      tr.printStackTrace();
    }
  }

  public void addText(String text) throws Exception {
    MimeBodyPart p = new MimeBodyPart();
    p.setContent(text, "text/plain; charset=\"utf-8\"");
    part.add(p);
  }

  public void addHtml(String text) throws Exception {
    MimeBodyPart p = new MimeBodyPart();
    p.setContent(text, "text/html; charset=\"UTF-8\"");
    part.add(p);
  }

  public void addFile(String filename, byte data[]) throws Exception {
    MimeBodyPart p = new MimeBodyPart();
    ByteArrayDataSource ds = new ByteArrayDataSource(data, "application/octet-stream");
    p.setDataHandler(new DataHandler(ds));
    p.setFileName(filename);
    part.add(p);
  }

  public void send(String from, String to, String subj) throws Exception {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
    message.setSubject(subj, "utf-8");
    MimeMultipart mp = new MimeMultipart();
    for (MimeBodyPart p : part)
      mp.addBodyPart(p);
    part.clear();
    message.setContent(mp);
    //    message.setHeader("X-Mailer", "smtpsend");
    message.setSentDate(new Date());
    message.saveChanges();
    tr.sendMessage(message, message.getAllRecipients());
  }

  public void sendHtml(String from, String to, String subj, String body) throws Exception {
    MimeMessage message = new MimeMessage(session);
    message.setFrom(new InternetAddress(from));
    message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
    message.setSubject(subj, "utf-8");
    message.setContent(body, "text/html");
    //    message.setHeader("X-Mailer", "smtpsend");
    message.setSentDate(new Date());
    message.saveChanges();
    tr.sendMessage(message, message.getAllRecipients());
  }

}
