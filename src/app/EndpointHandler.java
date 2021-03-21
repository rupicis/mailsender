package app;

import http.HttpCall;

interface EndpointHandler {
  public boolean handle(HttpCall call) throws Throwable;
}
