package de.hbt.propertyname;

/**
 * Thrown whenever generating a property name fails.
 */
@SuppressWarnings("serial")
public class PropertyNameException extends RuntimeException {
	PropertyNameException(String message, Throwable cause) {
		super(message, cause);
	}
}
