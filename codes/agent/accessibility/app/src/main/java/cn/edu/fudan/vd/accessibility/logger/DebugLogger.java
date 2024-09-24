package cn.edu.fudan.vd.accessibility.logger;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DebugLogger {
	private static final String TAG = "RecordAndReplayAccessibility";
	private static Map<Level, Method> methodMap;
	private static boolean logcatValid;
	private static String lastMessage;

	static {
		methodMap = new HashMap<>();
		logcatValid = true;
		try {
			initMethodMap();
		}
		catch (Exception e) {
			e.printStackTrace();
			logcatValid = false;
		}
	}

	public static void log(DebugInfo info) {
		log(info.generateMessage());
	}

	public static void log(Level level, DebugInfo info) {
		log(level, info.generateMessage());
	}
	
	public static void log(Level level, DebugInfo info, Object... args) {
		log(level, info.generateMessage(args));
	}

	public static void log(String message) {
		log(Level.DEBUG, message);
	}

	public static void log(Level level, String message) {
		// Avoid printing the same message for multiple times
		String complexMessage = addMethodStackInfo(message);
		if (lastMessage != null && lastMessage.equals(complexMessage))
			return;
		else
			lastMessage = complexMessage;

		// Print the message with a proper output stream
		if (logcatValid) {
			try {
				printWithLogcat(level, complexMessage);
			}
			catch (Exception e) {
				System.out.println("Start to use 'System.out' rather than 'Logcat' to print log messages.");
				logcatValid = false;
				printWithSystemOut(level, complexMessage);
			}
		}
		else {
			printWithSystemOut(level, complexMessage);
		}
	}

	private static String addMethodStackInfo(String message) {
		StackTraceElement[] elements = Thread.currentThread().getStackTrace();
		if (elements != null) {
			List<String> filteredList = getFilteredClassNameList();
			for (StackTraceElement element : elements) {
				String completeClassName = element.getClassName();
				String simpleClassName = getSimpleClassName(completeClassName);
				if (!filteredList.contains(completeClassName)) {
					String methodName = element.getMethodName();
					return String.format("[%s.%s()] %s", simpleClassName, methodName, message);
				}
			}
		}
		return String.format("[Unknown stack] %s", message);
	}

	private static void printWithLogcat(Level level, String message) throws Exception {
		Method method = methodMap.get(level);
		assert method != null;
		method.invoke(null, TAG, message);
	}

	private static void printWithSystemOut(Level level, String message) {
		System.out.printf("[%s][%s]%s\n", TAG, level.getText(), message);
	}

	private static void initMethodMap() throws Exception {
		Class<?> clazz = Class.forName("android.util.Log");
		for (Level level : Level.values()) {
			Method method = clazz.getMethod(level.getMethod(), String.class, String.class);
			methodMap.put(level, method);
		}
	}

	private static List<String> getFilteredClassNameList() {
		List<String> classList = new ArrayList<String>();
		classList.add("dalvik.system.VMStack");
		classList.add(Thread.class.getName());
		classList.add(DebugLogger.class.getName());
		return classList;
	}

	private static String getSimpleClassName(String className) {
		String segments[] = className.split("\\.");
		return segments[segments.length - 1];
	}

	public enum Level {
		VERBOSE("v", "VERBOSE"), DEBUG("d", "DEBUG"), INFORMATION("i", "INFORMATION"), WARN("w", "WARN"), ERROR("e", "ERROR");

		private String method;
		private String text;

		private Level(String method, String text) {
			this.method = method;
			this.text = text;
		}

		public String getMethod() {
			return method;
		}

		public String getText() {
			return text;
		}
	}
}
