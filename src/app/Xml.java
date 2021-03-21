package app;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PushbackInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import javax.xml.XMLConstants;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class Xml {
  private static final int QUOTA = 0x100000;
  public Document doc;
  public Node pos;
  private boolean skipNullas;

  static ThreadLocal<DocumentBuilder> builder = new ThreadLocal<DocumentBuilder>() {
    @Override
    protected DocumentBuilder initialValue() {
      try {
        DocumentBuilderFactory f = DocumentBuilderFactory
            .newInstance();
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        f.setFeature("http://xml.org/sax/features/external-general-entities", false);
        f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        f.setNamespaceAware(true);
        DocumentBuilder b = f.newDocumentBuilder();
        b.setErrorHandler(new ErrorHandler() {
          public void warning(SAXParseException e) throws SAXException {
          }

          public void fatalError(SAXParseException e) throws SAXException {
            throw e;
          }

          public void error(SAXParseException e) throws SAXException {
            throw e;
          }
        });
        return b;
      } catch (ParserConfigurationException exc) {
        throw new IllegalArgumentException(exc);
      }
    }
  };

  static ThreadLocal<Transformer> transformer = new ThreadLocal<Transformer>() {
    @Override
    protected Transformer initialValue() {
      try {
        TransformerFactory tf = TransformerFactory
            .newInstance();
        // tf.setAttribute("indent-number",
        // new
        // Integer(2));
        Transformer t = tf.newTransformer();
        t.setOutputProperty(OutputKeys.INDENT, "yes");

        // t.setOutputProperty(OutputKeys.ENCODING,
        // "windows-1257");

        t.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        return t;
      } catch (TransformerConfigurationException e) {
        throw new IllegalArgumentException(e);
      }
    }
  };

  static ThreadLocal<HashMap<String, Validator>> schema = new ThreadLocal<HashMap<String, Validator>>() {
    @Override
    protected HashMap<String, Validator> initialValue() {
      return new HashMap<String, Validator>();
    }
  };

  public Xml() throws Exception {
    DocumentBuilder b = builder.get();
    b.reset();
    doc = b.newDocument();
    pos = doc;
  }

  /*
   * public Xml(String schema) throws Exception { DocumentBuilder b =
   * builder.get(); b.reset(); doc =
   * b.getDOMImplementation().createDocument(schema, "Document", null); // doc =
   * b.newDocument(); pos = doc; }
   */

  public Xml(byte data[]) throws Exception {
    this(new ByteArrayInputStream(data));
  }

  public Xml(String data) throws Exception {
    DocumentBuilder b = builder.get();
    b.reset();
    doc = b.parse(new InputSource(new StringReader(data)));
    pos = doc.getDocumentElement();
  }

  public void skipNullas() {
    skipNullas = true;
  }

  private boolean isGazets(PushbackInputStream data) throws IOException {
    byte signature[] = new byte[3];
    int x = data.read(signature);
    if (x < 1)
      return false;
    data.unread(signature, 0, x);
    if (x != 3)
      return false;
    if (signature[0] != 31)
      return false;
    if (signature[1] != -117)
      return false;
    return signature[2] == 8;
  }

  public Xml(InputStream data) throws Exception {
    this(data, QUOTA);
  }

  public Xml(InputStream data, int quota) throws Exception {
    PushbackInputStream check = new PushbackInputStream(data, 3);
    DocumentBuilder b = builder.get();
    b.reset();
    if (isGazets(check)) {
      GZIPInputStream gz = null;
      try {
        gz = new GZIPInputStream(check);
        doc = b.parse(new QuotedInputStream(gz, quota));
      } finally {
        try {
          gz.close();
        } catch (Throwable ex) {
        }
      }
    } else
      doc = b.parse(check);
    pos = doc.getDocumentElement();
  }

  public Xml(File data) throws Exception {
    DocumentBuilder b = builder.get();
    b.reset();
    doc = b.parse(data);
    pos = doc.getDocumentElement();
  }

  public Xml(Xml me, Node pos) {
    doc = me.doc;
    this.pos = pos;
  }

  public void describe() {
    System.out.println("node:" + pos.getLocalName() + " ns:" + pos.getNamespaceURI() + " nn:" + pos.getNodeName());
    NamedNodeMap aa = pos.getAttributes();
    if (aa == null)
      return;
    for (int i = 0; i < aa.getLength(); i++) {
      Node n = aa.item(i);
      System.out.println("attr:" + n.getLocalName() + " ns:" + n.getNamespaceURI() + " nn:" + n.getNodeName());
    }
  }

  public String getOne(String... path) {
    for (String p : path) {
      try {
        return get(p).get();
      } catch (NullPointerException e) {
      }
    }
    return "";
  }

  public String getOne2(String... path) {
    for (String p : path) {
      try {
        get(p).get();
        return getOptional2(p);
      } catch (NullPointerException e) {
      }
    }
    return "";
  }

  public Xml get(String path) {
    Node n = pos;
    if (path.startsWith(".")) {
      path = path.substring(1);
      n = doc.getDocumentElement();
    }
    for (String tag : path.split("\\.")) {
      n = get0(n, tag);
      if (n == null)
        return null;
    }
    return new Xml(this, n);
  }

  public String getOptional(String path) {
    try {
      return get(path).get();
    } catch (NullPointerException ex) {
      return "";
    }
  }

  public String getOptional2(String path) {
    String ret = "";
    try {
      for (Xml x : list(path))
        ret = ret + x.get();
      return ret;
    } catch (NullPointerException ex) {
      return "";
    }
  }

  public Xml[] list(String path) {
    ArrayList<Xml> ret = new ArrayList<Xml>();
    Node n = pos;
    if (path.startsWith(".")) {
      path = path.substring(1);
      n = doc.getDocumentElement();
    }
    String tags[] = path.split("\\.");
    for (String tag : tags) {
      n = get0(n, tag);
      if (n == null)
        return new Xml[0];
    }
    String name = tags[tags.length - 1];
    if (name.equals(tag(n))) {
      ret.add(new Xml(this, n));
    }
    for (;;) {
      n = n.getNextSibling();
      if (n == null)
        break;
      if (name.equals(tag(n)))
        ret.add(new Xml(this, n));
    }
    return ret.toArray(new Xml[0]);
  }

  public Xml[] list() {
    ArrayList<Xml> ret = new ArrayList<Xml>();
    NodeList nl = pos.getChildNodes();

    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if ("#text".equals(n.getNodeName()))
        continue;
      ret.add(new Xml(this, n));
    }
    return ret.toArray(new Xml[0]);
  }

  private void declareAsID(Xml pos, String name) {
    if (pos.attr(name) != null)
      ((Element) pos.pos).setIdAttribute(name, true);
    for (Xml c : pos.list())
      declareAsID(c, name);
  }

  public void declareAsID(String name) {
    declareAsID(new Xml(this, doc), name);
  }

  private Node get0(Node from, String name) {
    NodeList nl = from.getChildNodes();
    for (int i = 0; i < nl.getLength(); i++) {
      Node n = nl.item(i);
      if (name.equals(tag(n)))
        return n;
    }
    return null;
  }

  public String get() {
    Node n = pos.getFirstChild();
    if (n == null)
      return "";
    return n.getNodeValue();
  }

  public String name() {
    return tag(pos);
  }

  public void setName(String name) {
    doc.renameNode(pos, pos.getNamespaceURI(), name);
  }

  public String attr(String name) {
    try {
      NamedNodeMap map = pos.getAttributes();
      return map.getNamedItem(name).getFirstChild().getNodeValue();
    } catch (Throwable tr) {
      return null;
    }
  }

  public Xml setAttr(String name, String text) throws Exception {
    ((Element) pos).setAttribute(name, text);
    return this;
  }

  public Xml setAttrNS(String name, String text, String urla) throws Exception {
    ((Element) pos).setAttributeNS(urla, name, text);
    return this;
  }

  private String tag(Node node) {
    String n = node.getNodeName();
    int ch = n.indexOf(':');
    if (ch == -1)
      return n;
    return n.substring(ch + 1);
  }

  public Xml add(Xml from) {
    Node child = doc.importNode(from.pos, true);
    pos.appendChild(child);
    return new Xml(this, child);
  }

  public Xml add(String tag) throws Exception {
    Element child = doc.createElementNS(pos.getNamespaceURI(), tag);
    pos.appendChild(child);
    return new Xml(this, child);
  }

  public Xml addNS(String tag, String urla) throws Exception {
    Element child = doc.createElementNS(urla, tag);
    pos.appendChild(child);
    return new Xml(this, child);
  }

  public void add_split(String tag, String text, int size) throws Exception {
    if (text == null)
      return;
    Xml pos = this;
    if (tag.contains(".")) {
      int ch = tag.lastIndexOf(".");
      pos = add1(tag.substring(0, ch));
      tag = tag.substring(ch + 1);
    }
    for (;;) {
      int len = text.length() > size ? size : text.length();
      if (len == 0)
        break;
      pos.add(tag).set(text.substring(0, len));
      text = text.substring(len);
    }
  }

  public void addAttr(String tag, String text, String attr, String aval) throws Exception {
    if (text == null)
      return;
    if (text.length() == 0)
      return;
    Xml n = add1(tag, text);
    n.setAttr(attr, aval);
  }

  public void add(String tag, String text) throws Exception {
    if (text == null)
      return;
    if (skipNullas && text.length() == 0)
      return;
    if (tag.contains(".")) {
      add1(tag, text);
      return;
    }
    Element child = doc.createElementNS(pos.getNamespaceURI(), tag);
    pos.appendChild(child);
    new Xml(this, child).set(text);
  }

  public Xml add1(String path, String text) throws Exception {
    if (text == null)
      return null;
    if (skipNullas && text.length() == 0)
      return new Xml();
    // if (text.length() == 0) return
    Xml x;
    if (path.startsWith(".")) {
      path = path.substring(1);
      x = new Xml(this, doc.getDocumentElement());
    } else
      x = this;
    for (String tag : path.split("\\.")) {
      Xml down = x.get(tag);
      if (down == null)
        down = x.add(tag);
      x = down;
    }
    x.set(text);
    return x;
  }

  public Xml add1(String path) throws Exception {
    Xml x;
    if (path.startsWith(".")) {
      path = path.substring(1);
      x = new Xml(this, doc.getDocumentElement());
    } else
      x = this;
    for (String tag : path.split("\\.")) {
      Xml down = x.get(tag);
      if (down == null)
        down = x.add(tag);
      x = down;
    }
    return x;
  }

  public void set(String val) throws Exception {
    if (val == null)
      val = "";

    NodeList children = pos.getChildNodes();
    boolean success = false;
    if (children != null) {
      for (int i = 0; i < children.getLength(); i++) {
        Node childNode = children.item(i);
        if ((childNode.getNodeType() == Node.TEXT_NODE) || (childNode.getNodeType() == Node.CDATA_SECTION_NODE)) {
          childNode.setNodeValue(val);
          success = true;
        }
      }
    }
    if (!success) {
      Text textNode = doc.createTextNode(val);
      pos.appendChild(textNode);
    }
  }

  private static String expand(int nr, int len) {
    String num = "" + nr;
    while (num.length() < len)
      num = "0" + num;
    return num;
  }

  public static String getDate(String date) throws Exception {
    XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
    return expand(cal.getYear(), 4) + expand(cal.getMonth(), 2) + expand(cal.getDay(), 2);
  }

  public static String getTime(String date) throws Exception {
    XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar(date);
    return expand(cal.getYear(), 4) + expand(cal.getMonth(), 2) + expand(cal.getDay(), 2) + expand(cal.getHour(), 2)
        + expand(cal.getMinute(), 2) + expand(cal.getSecond(), 2);
  }

  public String toString() {
    try {
      StringWriter writer = new StringWriter();
      transformer.get().transform(new DOMSource(doc), new StreamResult(writer));
      return writer.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  public String subString() {
    try {
      StringWriter writer = new StringWriter();
      transformer.get().transform(new DOMSource(pos), new StreamResult(writer));
      return writer.toString();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return "";
  }

  public byte[] subBytes() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      transformer.get().transform(new DOMSource(pos), new StreamResult(out));
      return out.toByteArray();

    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public byte[] getBytes() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      transformer.get().transform(new DOMSource(doc), new StreamResult(out));
      return out.toByteArray();
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return new byte[0];
  }

  public void write(OutputStream out) throws Exception {
    transformer.get().transform(new DOMSource(doc), new StreamResult(out));
  }

  public String validate(InputStream xsd) {
    try {
      SchemaFactory f = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      Source s = new StreamSource(xsd);
      Schema schema = f.newSchema(s);
      Validator validator = schema.newValidator();
      validator.validate(new DOMSource(pos));
      return null;
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public String validate(String resourceName) {
    try {
      HashMap<String, Validator> shemas = schema.get();
      Validator val = shemas.get(resourceName);
      if (val == null) {
        SchemaFactory f = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Source s = new StreamSource(getClass().getClassLoader().getResourceAsStream(resourceName));
        Schema schema = f.newSchema(s);
        val = schema.newValidator();
        shemas.put(resourceName, val);
      }
      val.reset();
      val.validate(new DOMSource(pos));
      return null;
    } catch (Exception e) {
      return e.getMessage();
    }
  }

  public boolean buulis(String tag) {
    return Boolean.valueOf(getOptional(tag));
  }

  public void remove() {
    pos.getParentNode().removeChild(pos);
  }

  private class QuotedInputStream extends InputStream {
    private int size, quota, size2;
    private InputStream src;

    public QuotedInputStream(InputStream in, int quota) {
      src = in;
      this.quota = quota;
    }

    public int read() throws IOException {
      size++;
      if (size > quota)
        throw new IOException("data size limit exceeded");
      return src.read();
    }

    public int read(byte b[]) throws IOException {
      int l = src.read(b, 0, b.length);
      size += l;
      if (size > quota)
        throw new IOException("data size limit exceeded");
      return l;
    }

    public int read(byte b[], int off, int len) throws IOException {
      int l = src.read(b, off, len);
      size += l;
      if (size > quota)
        throw new IOException("data size limit exceeded ");
      return l;
    }
  }
}
