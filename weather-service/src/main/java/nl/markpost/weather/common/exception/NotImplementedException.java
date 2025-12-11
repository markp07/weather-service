package nl.markpost.weather.common.exception;

import nl.markpost.weather.common.constant.GenericErrorCodes;

public class NotImplementedException extends GenericException {

  public NotImplementedException() {
    super(GenericErrorCodes.NOT_IMPLEMENTED);
  }

  public NotImplementedException(String message) {
    super(message, GenericErrorCodes.NOT_IMPLEMENTED);
  }

}
