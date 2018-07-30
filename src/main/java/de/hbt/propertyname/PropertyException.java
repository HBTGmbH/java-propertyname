package de.hbt.propertyname;

/**
 * Thrown whenever generating a property name fails.
 */
@SuppressWarnings("serial")
public class PropertyException extends RuntimeException {
	PropertyException(String message, Throwable cause) {
		super(message, cause);
	}
}
