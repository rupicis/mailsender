package app;

import http.HttpCall;

public class RequestHandler {

  public boolean dispatchRequest(HttpCall call) throws Throwable {

    EndpointHandler svc = null;

    switch (call.method.getPath()) {
    case "/submit":
      svc = new SubmitPayment();
      break;
    case "/status":
      svc = new StatusHandler();
      break;
    }

    if (svc == null)
      svc = new InvalidUrl();
    call.attributes.put("Content-Type", "text/html");
    return svc.handle(call);
  }
}
