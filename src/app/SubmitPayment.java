package app;

import java.nio.charset.StandardCharsets;

import http.HttpCall;
import http.ParamsRequest;
import http.StaticContent;
import main.Util;

public class SubmitPayment implements EndpointHandler {

  public boolean handle(HttpCall call) throws Throwable {
    ParamsRequest req = new ParamsRequest(call);
    String remitter = req.get("rname");
    String amount = req.get("amount");

    Xml root = new Xml().addNS("Document", "urn:iso:std:iso:20022:tech:xsd:pain.001.001.03");
    root.setAttrNS("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance", "http://www.w3.org/2000/xmlns/");
    root = root.add("CstmrCdtTrfInitn");
    root.add("GrpHdr.MsgId", Util.generateID());
    root.add("GrpHdr.CreDtTm", Util.generateID());
    root.add("GrpHdr.NbOfTxs", "1");
    root.add("GrpHdr.CtrlSum", amount);
    root.add("GrpHdr.InitgPty.Nm", remitter);

    Xml payment = root.add("PmtInf");
    payment.add("PmtInfId", "1");
    payment.add("NbOfTxs", "1");
    payment.add("CtrlSum", amount);
    payment.add("PmtTpInf.SvcLvl.Cd", "NURG");
    payment.add("PmtTpInf.CtgyPurp.Cd", "OTHR");
    payment.add("Dbtr.Nm", remitter);
    payment.add("Dbtr.Id.PrvtId.Othr.Id", req.get("rid"));
    payment.add("DbtrAcct.Id.IBAN", req.get("racc"));
    payment.add("DbtrAgt.FinInstnId.BIC", "PARXLV22");
    payment.add("ChrgBr", "SHAR");

    Xml cdtr = payment.add("CdtTrfTxInf");
    cdtr.add("PmtTpInf.SvcLvl.Cd", "SEPA");
    cdtr.addAttr("Amt.InstAmt", amount, "Ccy", "EUR");
    cdtr.add("CdtrAgt.FinInstnId.BIC", req.get("bank"));
    cdtr.add("Cdtr.Nm", req.get("bname"));
    cdtr.add("Cdtr.Id.PrvtId.Othr.Id", req.get("bid"));
    cdtr.add("CdtrAcct.Id.IBAN", req.get("bacc"));
    cdtr.add("RmtInf.Ustrd", req.get("details"));

    MailJob job = new MailJob();
    job.from = call.from();
    job.payment = root.toString().getBytes(StandardCharsets.UTF_8);
    job.donecallback = () -> {
      StaticContent.respond(call, "done.html");
      call.complete();
    };
    job.stuckcallback = () -> {
      StaticContent.respond(call, "stuck.html");
      call.complete();
    };
    MailSender.enqueue(job);
    return true;
  }
}
