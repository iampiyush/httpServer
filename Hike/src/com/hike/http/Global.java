package com.hike.http;

import java.util.concurrent.ConcurrentHashMap;

public class Global {

  public static ConcurrentHashMap<Integer, Long> serverStatusMap = new ConcurrentHashMap<Integer, Long>();
  public static ConcurrentHashMap<Integer, Long> timeOfRequestMap = new ConcurrentHashMap<Integer, Long>();
  public static ConcurrentHashMap<Integer, Thread> threadMap = new ConcurrentHashMap<>();
}
