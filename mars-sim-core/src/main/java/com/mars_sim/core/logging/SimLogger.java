/*
 * Mars Simulation Project
 * SimLogger.java
 * @date 2022-06-27
 * @author Barry Evans
 */

package com.mars_sim.core.logging;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.mars_sim.core.Entity;


/**
 * This is a logger class similar to Java Logger that is Simulation aware
 * to handle common formatting.
 * This actor as an Adapter to the underlying Java Logger.
 */
public class SimLogger {

	/**
	 * TimeAndCount keeps track of the between time and the number of times the message has appeared.
	 */
	private static class TimeAndCount {
		protected long startTime;
		protected int count;

		TimeAndCount() {
			this.startTime = System.currentTimeMillis();
			this.count = 1;
		}
	}

	private static Map<String, SimLogger> loggers = new HashMap<>();
	private static Map<String, TimeAndCount> lastLogged = new ConcurrentHashMap<>();

	private static final String OPEN_BRACKET = " [x";
	private static final String CLOSED_BRACKET = "]";
	private static final String CLOSED_BRACKET_SPACE = "] ";
	private static final String COLON = " : [";
	private static final String DASH = " - ";
	private static final String QUESTION = "?";
	private static final long DEFAULT_WARNING_TIME = 0;
	public static final long DEFAULT_SEVERE_TIME = 0;
	private static final long DEFAULT_INFO_TIME = 0;

	private String sourceName;

	private Logger rootLogger;


	/**
	 * Gets the logger instance.
	 * 
	 * @param name
	 * @return
	 */
	public static SimLogger getLogger(String name) {
		SimLogger result = null;
		synchronized (loggers) {
			result = loggers.computeIfAbsent(name, k -> new SimLogger(name));

		}
		return result;
	}

	/**
	 * Constructor.
	 * 
	 * @param name
	 */
	private SimLogger(String name) {
		rootLogger = Logger.getLogger(name);

		sourceName = name.substring(name.lastIndexOf(".") + 1, name.length());
	}

	public String getSourceName() {
		return sourceName;
	}

	/**
	 * Logs given <code>message</code> to given <code>logger</code> as long as:
	 * <ul>
	 * <li>A message (from same class and line number) has not already been logged
	 * within the past <code>timeBetweenLogs</code>.</li>
	 * <li>The given <code>level</code> is active for given
	 * <code>logger</code>.</li>
	 * </ul>
	 * Note: If messages are skipped, they are counted. When
	 * <code>timeBetweenLogs</code> has passed, and a repeat message is logged, the
	 * count will be displayed.
	 *
	 * @param actor           Unit that is the Actor in the message.
	 * @param level           Level to log.
	 * @param timeBetweenLogs Milliseconds to wait between similar log messages.
	 * @param message         The actual message to log.
	 */
	public void log(Entity actor, Level level, long timeBetweenLogs, String message) {
		baseLog(actor, level, timeBetweenLogs, message, null);
	}

	/**
	 * Does the actual logging to the logger.
	 * 
	 * @param actor
	 * @param level
	 * @param timeBetweenLogs
	 * @param message
	 * @param t
	 */
	private void baseLog(Entity actor, Level level, long timeBetweenLogs, String message,
			Throwable t) {
		if (!rootLogger.isLoggable(level)) {
			return;
		}

		long dTime = timeBetweenLogs;

		String uniqueIdentifier = getUniqueIdentifer(actor);
		TimeAndCount lastTimeAndCount = lastLogged.get(uniqueIdentifier);
		StringBuilder outputMessage = null;
		if (lastTimeAndCount != null) {
			synchronized (lastTimeAndCount) {
				long now = System.currentTimeMillis();
				if (now - lastTimeAndCount.startTime < dTime) {
					// Increment count only since the message in the same and is within the time prescribed
					lastTimeAndCount.count++;
					return;
				}

				// Print the log statement with counts
				outputMessage = new StringBuilder(sourceName);
				outputMessage.append(OPEN_BRACKET).append(lastTimeAndCount.count).append(CLOSED_BRACKET);
			}
		}
		else {
			// First time for this message
			outputMessage = new StringBuilder(sourceName);
		}

		// Add body, contents Settlement, Unit nickname message"
		outputMessage.append(COLON);
		if (actor == null) {
			// Actor unknown
			outputMessage.append("System").append(CLOSED_BRACKET_SPACE);
		}
		else {
			// Has an Actor
			String context = actor.getContext();
			if (context == null) {
				outputMessage.append(actor.getName()).append(CLOSED_BRACKET_SPACE);
			}
			else {
				outputMessage.append(context);
				outputMessage.append(CLOSED_BRACKET_SPACE).append(actor.getName()).append(DASH);
			}
		}
		outputMessage.append(message);

		if (t == null) {
			rootLogger.log(level, outputMessage::toString);
		}
		else {
			rootLogger.log(level, outputMessage.toString(), t);
		}

		// Register the message
		lastLogged.put(uniqueIdentifier, new TimeAndCount());
	}

	/**
	 * Returns the line.
	 *
	 * @return
	 */
	private static String getUniqueIdentifer(Entity actor) {
		StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
		String loggerClass = SimLogger.class.getName();
		String nickName = (actor != null ? actor.getName() : "unknown");

		// Skip the first frame as it is the "getStackTrace" call
		for (int idx = 1; idx < stackTrace.length; idx++) {
			StackTraceElement ste = stackTrace[idx];
			if (!ste.getClassName().equals(loggerClass)) {
				// We have now file/line before entering SimLogger.
				StringBuilder key = new StringBuilder();
				key.append(ste.getFileName()).append(ste.getLineNumber()).append(nickName);
				return key.toString();
			}
		}
		return QUESTION;
	}

	/**
	 * Logs directly without formatting.
	 * 
	 * @param level
	 * @param message
	 */
	public void log(Level level, String message) {
		rootLogger.log(level, () -> sourceName + " : " + message);
	}

	/**
	 * Logs directly without formatting.
	 * 
	 * @param level
	 * @param message
	 * @param e Exception
	 */
	private void rootLog(Level level, String message, Throwable e) {
		rootLogger.log(level, sourceName + " : " + message, e);
	}
	
	/**
	 * Logs directly without formatting.
	 * 
	 * @param message
	 */
	public void fine(String message) {
		log(Level.FINE, message);
	}

	/**
	 * Helper method just to log a fine message. Message timeout is predefined.
	 * 
	 * @param actor
	 * @param string
	 */
	public void fine(Entity actor, String string) {
		baseLog(actor, Level.FINE, DEFAULT_INFO_TIME, string, null);
	}

	/**
	 * Helper method just to log a fine message.
	 * 
	 * @param actor
	 * @param timeBetweenLogs Milliseconds to wait between similar log messages.
	 * @param string
	 */
	public void fine(Entity actor, long timeBetweenLogs, String string) {
		baseLog(actor, Level.FINE, timeBetweenLogs, string, null);
	}
	
	/**
	 * Logs directly without formatting.
	 * 
	 * @param message
	 */
	public void info(String message) {
		log(Level.INFO, message);
	}

	/**
	 * Helper method just to log an info message.
	 * 
	 * @param actor
	 * @param timeBetweenLogs Milliseconds to wait between similar log messages.
	 * @param string
	 */
	public void info(Entity actor, long timeBetweenLogs, String string) {
		baseLog(actor, Level.INFO, timeBetweenLogs, string, null);
	}

	/**
	 * Helper method just to log a info message. Message timeout is predefined.
	 * 
	 * @param actor
	 * @param string
	 */
	public void info(Entity actor, String string) {
		baseLog(actor, Level.INFO, DEFAULT_INFO_TIME, string, null);
	}

	/**
	 * Helper method just to log a info message. Message timeout is predefined.
	 * 
	 * @param timeBetweenLogs
	 * @param message
	 */
	public void info(long timeBetweenLogs, String message) {
		baseLog(null, Level.INFO, timeBetweenLogs, sourceName + " : " + message, null);
	}
	
	/**
	 * Logs directly without formatting.
	 * 
	 * @param message
	 */
	public void warning(String message) {
		log(Level.WARNING, message);
	}

	/**
	 * Helper method just to log a warning message.
	 * 
	 * @param actor
	 * @param timeBetweenLogs Milliseconds to wait between similar log messages.
	 * @param string
	 */
	public void warning(Entity actor, long timeBetweenLogs, String string) {
		baseLog(actor, Level.WARNING, timeBetweenLogs, string, null);
	}

	/**
	 * Helper method just to log a warning message. Message  timeout is predefined.
	 * 
	 * @param actor
	 * @param string
	 */
	public void warning(Entity actor, String string) {
		baseLog(actor, Level.WARNING, DEFAULT_WARNING_TIME, string, null);
	}

	public void warning(long timeBetweenLogs, String message) {
		baseLog(null, Level.WARNING, timeBetweenLogs, sourceName + " : " + message, null);
	}

	/**
	 * Log directly without formatting.
	 * 
	 * @param message
	 */
	public void severe(String message) {
		log(Level.SEVERE, message);
	}

	/**
	 * Log directly without formatting.
	 * 
	 * @param message
	 */
	public void severe(String message, Throwable e) {
		rootLog(Level.SEVERE, message, e);
	}

	/**
	 * Helper method just to log a severe message. Message timeout is predefined.
	 * 
	 * @param actor
	 * @param message
	 */
	public void severe(Entity actor, String string) {
		baseLog(actor, Level.SEVERE, DEFAULT_SEVERE_TIME, string, null);
	}

	/**
	 * Helper method just to log a severe message.
	 * 
	 * @param actor
	 * @param timeBetweenLogs Milliseconds to wait between similar log messages.
	 * @param string
	 * @param e
	 */
	public void severe(Entity actor, long timeBetweenLogs, String string, Exception e) {
		baseLog(actor, Level.SEVERE, timeBetweenLogs, string, e);
	}

	/**
	 * Helper method just to log a severe message.
	 * 
	 * @param actor
	 * @param timeBetweenLogs Milliseconds to wait between similar log messages.
	 * @param string
	 */
	public void severe(Entity actor, long timeBetweenLogs, String string) {
		baseLog(actor, Level.SEVERE, timeBetweenLogs, string, null);
	}

	/**
	 * Helper method just to log a severe message. Message timeout is predefined.
	 * 
	 * @param actor
	 * @param message
	 * @param reason
	 */
	public void severe(Entity actor, String message, Throwable reason) {
		baseLog(actor, Level.SEVERE, DEFAULT_SEVERE_TIME, message, reason);
	}

	/**
	 * Logs directly without formatting.
	 * 
	 * @param message
	 */
	public void config(String message) {
		log(Level.CONFIG, message);
	}

	/**
	 * Helper method just to log a config message. Message timeout is predefined.
	 * 
	 * @param actor
	 * @param message
	 */
	public void config(Entity actor, String message) {
		baseLog(actor, Level.CONFIG, 0, message, null);
	}

	public boolean isLoggable(Level level) {
		return rootLogger.isLoggable(level);
	}

}
