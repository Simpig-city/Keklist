package de.hdg.keklist.api;

/**
 * This class is used to handle exceptions thrown by the API.
 *
 * @author SageSphinx63920
 * @since 1.0
 */
public class APIExeption extends Exception{

    public APIExeption(String message) {
        super(message);
    }

    public APIExeption(String message, Throwable cause) {
        super(message, cause);
    }

    public APIExeption(Throwable cause) {
        super(cause);
    }

    public APIExeption(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
