package acecardapi.utils;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

import java.math.BigDecimal;

public class NumberUtilities {
  public static void numberIsFloat(String number, Handler<AsyncResult<Float>> resultHandler) {
    try {
      float value  = Float.parseFloat(number);
      resultHandler.handle(Future.succeededFuture(value));
    } catch (NumberFormatException e) {
      resultHandler.handle(Future.failedFuture(""));
    }
  }

  public static void doubleHasNADecimals(int decimalsRequired, Double value, Handler<AsyncResult<Double>> resultHandler) {
    boolean fail = (BigDecimal.valueOf(value).scale() > decimalsRequired);
    if (fail) {
      resultHandler.handle(Future.failedFuture("Value has more than 2 decimals"));
    } else {
      resultHandler.handle(Future.succeededFuture(value));
    }
  }

  public static boolean intMinMaxValue(int value, int minValue, int maxValue) {

    return value >= minValue && value <= maxValue;

  }

  public static boolean doubleMinMaxValue(double value, double minValue, double maxValue) {

    return value >= minValue && value <= maxValue;

  }

}
