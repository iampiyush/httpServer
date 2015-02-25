package com.hike.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;


public class HttpServer extends Thread {

  // private static final String HTML_START = "<html>" + "<title>Hike HTTP Server</title>" +
  // "<body>";

  // private static final String HTML_END = "</body>" + "</html>";

  public static final String GET = "GET";
  public static final String POST = "POST";
  public static final String SLEEP = "sleep";
  public static final String TIMEOUT = "timeout";
  public static final String CONN_ID = "connid";
  public static final String SERVER_STATUS = "server-status";
  public static final String KILL = "kill";
  public static final Integer HTTP_OK = 200;

  private Socket connectedClient = null;
  private BufferedReader inputFromClient = null;
  private DataOutputStream outputToClient = null;



  public HttpServer(Socket client) {
    connectedClient = client;
  }

  public void run() {
    System.out.println("The Client " + connectedClient.getInetAddress() + ":"
        + connectedClient.getPort() + " is connected");

    try {
      inputFromClient = new BufferedReader(new InputStreamReader(connectedClient.getInputStream()));
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    OutputStream os = null;
    try {
      os = connectedClient.getOutputStream();
    } catch (IOException e2) {
      // TODO Auto-generated catch block
      e2.printStackTrace();
    }
    outputToClient = new DataOutputStream(os);

    String requestString = null;
    try {
      requestString = inputFromClient.readLine();
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    String headerLine = requestString;
    String httpMethod = "";
    String httpQueryString = "";
    StringTokenizer tokenizer = null;
    if (headerLine != null && !headerLine.isEmpty()) {
      tokenizer = new StringTokenizer(headerLine);
      httpMethod = tokenizer.nextToken();
      httpQueryString = tokenizer.nextToken();
    }

    StringBuffer responseBuffer = new StringBuffer();

    try {
      while (inputFromClient.ready()) {
        // Read the HTTP complete HTTP Query
        responseBuffer.append(requestString + "<BR>");
        requestString = inputFromClient.readLine();
      }
    } catch (IOException e1) {
      // TODO Auto-generated catch block
      e1.printStackTrace();
    }

    if (httpMethod.equals(GET)) {
      System.out.println("httpQueryString == " + httpQueryString);
      // String query = requestUrl.getQuery();
      Map<String, String> queryParamMap = new LinkedHashMap<>();
      String query = new String();

      if (httpQueryString.startsWith("/")) {

        if (httpQueryString.contains("?")) {
          query = httpQueryString.split("\\?")[1];
        }
        try {
          if (query != null && !query.isEmpty())
            queryParamMap = splitQuery(query);
        } catch (UnsupportedEncodingException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }

        if (httpQueryString.contains(SLEEP)) {
          sleepResponse(HTTP_OK, queryParamMap);
        } else if (httpQueryString.contains(SERVER_STATUS)) {
          serverStatusResponse(HTTP_OK);
        }
      }
    }

    else if (httpMethod.equals(POST)) {

      Map<String, String> queryParamMap = new LinkedHashMap<>();
      String query = new String();

      if (httpQueryString.startsWith("/")) {

        if (httpQueryString.contains("?")) {
          query = httpQueryString.split("\\?")[1];
        }
        try {
          if (query != null && !query.isEmpty())
            queryParamMap = splitQuery(query);
        } catch (UnsupportedEncodingException e1) {
          // TODO Auto-generated catch block
          e1.printStackTrace();
        }

        if (httpQueryString.contains(KILL)) {
          killResponse(HTTP_OK, queryParamMap);
        }
      }
    }
  }

  /**
   * functionality to kill request 
   * @param statusCode
   * @param queryParamMap
   */
  public void killResponse(int statusCode, Map<String, String> queryParamMap) {
    Integer connectionId = -1;
    if (queryParamMap != null) {
      if (queryParamMap.containsKey(CONN_ID)) {
        connectionId = Integer.parseInt(queryParamMap.get(CONN_ID));
        Global.serverStatusMap.remove(connectionId);
        Global.timeOfRequestMap.remove(connectionId);
        Global.threadMap.get(connectionId).interrupt();
        Global.threadMap.remove(connectionId);
      }
      String responseString = "{\"stat\":\"ok\"}";
      try {
        sendResponse(HTTP_OK, responseString);
      } catch (Exception e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }
  }

  /**
   * api for sleep functionality
   * @param statusCode
   * @param queryParamMap
   */
  public void sleepResponse(int statusCode, Map<String, String> queryParamMap) {
    Long timeOut = 0l;
    Integer connectionId = -1;
    if (queryParamMap != null) {
      if (queryParamMap.containsKey(TIMEOUT)) {
        timeOut = Long.parseLong(queryParamMap.get(TIMEOUT));
      }
      if (queryParamMap.containsKey(CONN_ID)) {
        connectionId = Integer.parseInt(queryParamMap.get(CONN_ID));
      }
    }
    Long timeOfRequest = System.currentTimeMillis();
    Global.timeOfRequestMap.put(connectionId, timeOfRequest);
    Global.serverStatusMap.put(connectionId, timeOut * 1000);
    try {

      Global.threadMap.put(connectionId, Thread.currentThread());
      Thread.sleep(timeOut * 1000);

    } catch (InterruptedException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    String responseString = "";

    try {
      if (Global.serverStatusMap.containsKey(connectionId))
        responseString = "{\"stat\":\"ok\"}";
      else
        responseString = "{\"stat\":\"killed\"}";
      Global.serverStatusMap.remove(connectionId);
      Global.timeOfRequestMap.remove(connectionId);
      Global.threadMap.remove(connectionId);
      sendResponse(HTTP_OK, responseString);
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * api for server-status map
   * @param statusCode http status code
   */
  public void serverStatusResponse(int statusCode) {

    for (Map.Entry<Integer, Long> entry : Global.serverStatusMap.entrySet()) {
      Long curVal = entry.getValue();
      Integer key = entry.getKey();
      Long newval =
          curVal - System.currentTimeMillis() + Global.timeOfRequestMap.get(entry.getKey());
      System.out.println("oldvalue at key " + entry.getKey() + "was " + entry.getValue()
          + "and new value is " + newval);
      Global.serverStatusMap.put(key, newval);
    }

    try {
      System.out.println("serverStatusMap == " + Global.serverStatusMap);
      sendResponse(statusCode, Global.serverStatusMap.toString());
    } catch (Exception e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }


  /**
   * 
   * @param statusCode http response code
   * @param responseString response to be returned
   * @throws Exception
   */
  public void sendResponse(int statusCode, String responseString) throws Exception {
    String statusLine = null;
    String serverdetails = "Server: hike HTTP Server";
    String contentLengthLine = null;
    String contentTypeLine = "Content-Type: text/html" + "\r\n";

    if (statusCode == HTTP_OK)
      statusLine = "HTTP/1.1 200 OK" + "\r\n";
    else
      statusLine = "HTTP/1.1 404 Not Found" + "\r\n";

    // responseString = /* HttpServer.HTML_START + */responseString /* + HttpServer.HTML_END */;
    contentLengthLine = "Content-Length: " + responseString.length() + "\r\n";

    outputToClient.writeBytes(statusLine);
    outputToClient.writeBytes(serverdetails);
    outputToClient.writeBytes(contentTypeLine);
    outputToClient.writeBytes(contentLengthLine);
    outputToClient.writeBytes("Connection: close\r\n");
    outputToClient.writeBytes("\r\n");
    outputToClient.writeBytes(responseString);
    // outputToClient.close();
  }

  /**
   * 
   * @param query queryString from which query map to be created 
   * @return map with all query params
   * @throws UnsupportedEncodingException
   */
  public static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
    Map<String, String> query_pairs = new LinkedHashMap<String, String>();
    String[] pairs = query.split("&");
    for (String pair : pairs) {
      int idx = pair.indexOf("=");
      query_pairs.put(URLDecoder.decode(pair.substring(0, idx), "UTF-8"),
          URLDecoder.decode(pair.substring(idx + 1), "UTF-8"));
    }
    return query_pairs;
  }


  public static void main(String args[]) throws Exception {
    @SuppressWarnings("resource")
    ServerSocket Server = new ServerSocket(8080, 10000, InetAddress.getByName("127.0.0.1"));
    System.out.println("hike http server on port 8080");

    while (true) {
      Socket connected = Server.accept();
      if (connected != null)
        (new HttpServer(connected)).start();
    }
  }

}
