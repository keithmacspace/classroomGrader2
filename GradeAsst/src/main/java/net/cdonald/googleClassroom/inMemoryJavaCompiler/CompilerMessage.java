package net.cdonald.googleClassroom.inMemoryJavaCompiler;

public class CompilerMessage {
	private String studentId;
	private boolean successful;
	private String compilerMessage;

	public CompilerMessage(String studentId, boolean success, String initCompilerMessage) {
		this.studentId = studentId;
		this.successful = success;
		if (initCompilerMessage != null) {
			int lineIndex = initCompilerMessage.indexOf("line=");
			int messageIndex = initCompilerMessage.indexOf("message=");
			if (lineIndex != -1 || messageIndex != -1) {
				if (lineIndex != -1) {
					// Going to -1 in both of these to chop off the ] part of the message
					compilerMessage = initCompilerMessage.substring(lineIndex, initCompilerMessage.length() - 1);
				} else if (messageIndex != -1) {
					compilerMessage = initCompilerMessage.substring(messageIndex, initCompilerMessage.length() - 1);
				}
				compilerMessage = compilerMessage.replace("message=", "");
			} else {
				compilerMessage = initCompilerMessage;
			}
		}
	}

	public CompilerMessage(String studentId, boolean success) {
		this(studentId, success, null);
	}

	public String getStudentId() {
		return studentId;
	}

	public boolean isSuccessful() {
		return successful;
	}

	public String getCompilerMessage() {
		return compilerMessage;
	}
}
