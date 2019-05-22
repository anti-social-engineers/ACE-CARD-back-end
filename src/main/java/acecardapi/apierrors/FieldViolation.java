package acecardapi.apierrors;

import io.vertx.core.json.JsonObject;

public abstract class FieldViolation extends ApiError {

  String field;

  public FieldViolation(Boolean success, String type) {
    super(success, type);
  }

  @Override
  public JsonObject errorJson() {
    JsonObject jsonObject = super.errorJson();
    jsonObject.put("field", this.field);
    return jsonObject;
  }

}
