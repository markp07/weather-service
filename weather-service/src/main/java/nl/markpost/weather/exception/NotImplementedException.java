package nl.markpost.weather.exception;

import nl.markpost.weather.constant.GenericErrorCodes;

public class NotImplementedException extends GenericException {

  public NotImplementedException() {
    super(GenericErrorCodes.NOT_IMPLEMENTED);
  }

  public NotImplementedException(String message) {
    super(message, GenericErrorCodes.NOT_IMPLEMENTED);
  }

}
