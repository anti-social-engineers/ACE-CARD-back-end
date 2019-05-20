package acecardapi.apierrors;

import io.vertx.core.json.JsonObject;

public abstract class FieldViolation extends ApiError {

  String field;

  public FieldViolation(Boolean success, String message) {
    super(success, message);
  }

  @Override
  public JsonObject errorJson() {
    JsonObject jsonObject = super.errorJson();
    jsonObject.put("field", this.field);
    return jsonObject;
  }

}
