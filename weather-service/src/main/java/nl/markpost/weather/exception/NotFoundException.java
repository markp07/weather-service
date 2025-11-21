package nl.markpost.weather.exception;

import nl.markpost.weather.constant.GenericErrorCodes;

public class NotFoundException extends GenericException {

  public NotFoundException() {
    super(GenericErrorCodes.NOT_FOUND);
  }

  public NotFoundException(String message) {
    super(message, GenericErrorCodes.NOT_FOUND);
  }

}
