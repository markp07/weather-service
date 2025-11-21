package nl.markpost.weather.exception;


import nl.markpost.weather.constant.GenericErrorCodes;

public class UnauthorizedException extends GenericException {

  public UnauthorizedException() {
    super(GenericErrorCodes.UNAUTHORIZED);
  }

  public UnauthorizedException(String message) {
    super(message, GenericErrorCodes.UNAUTHORIZED);
  }

  public UnauthorizedException(String message, Exception exception) {
    super(message, GenericErrorCodes.UNAUTHORIZED, exception);
  }
}
