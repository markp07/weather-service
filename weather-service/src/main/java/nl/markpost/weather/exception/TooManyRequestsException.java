package nl.markpost.weather.exception;

import lombok.Getter;
import nl.markpost.weather.constant.GenericErrorCodes;

@Getter
public class TooManyRequestsException extends GenericException {

  public TooManyRequestsException() {
    super(GenericErrorCodes.TOO_MANY_REQUESTS);
  }

  public TooManyRequestsException(String message) {
    super(message, GenericErrorCodes.TOO_MANY_REQUESTS);
  }

  public TooManyRequestsException(String message, Exception exception) {
    super(message, GenericErrorCodes.TOO_MANY_REQUESTS, exception);
  }
}
