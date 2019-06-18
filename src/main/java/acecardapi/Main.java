/*

  Copyright 2019 Aaron Beetstra, team Anti-Social Engineers

  Simple class used to quickly launch the webserver.
  Do not use in production.

 */


package acecardapi;

import io.vertx.core.Vertx;

public class Main {

  public static void main(String[] args) {
    //System.out.println(decrypt("NNBIKUGVghiiKRCDNcsNJI8IchIJjcdTNESZnJ/mLBY=0000", "C*F-JaNdRgUjXn2r5u8x/A?D(G+KbPeS"));
    Vertx.vertx().deployVerticle(MainVerticle.class.getName());
  }

}
