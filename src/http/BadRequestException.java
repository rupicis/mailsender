package http;

public class BadRequestException extends Exception {

  public BadRequestException(String msg) {
    super(msg);
  }
}
