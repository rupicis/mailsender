package app;

import http.HttpCall;
import http.StaticContent;

class InvalidUrl implements EndpointHandler {

  public boolean handle(HttpCall call) throws Exception {
    String query = call.method.query;
    if (query.startsWith("/"))
      query = query.substring(1);
    int ch = query.indexOf('?');
    if (ch != -1)
      query = query.substring(0, ch);
    if (query.length() == 0)
      query = "index.html";
    StaticContent.respond(call, query);
    return false;
  }

}
