package com.wupa9.h2api;

public class Error {

	private String message;
	private String log;
	
	public Error(String msg, String log) {
		message = msg;
		this.log = log;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	
	public String getLog() {
		return log;
	}

	
	public void setLog(String log) {
		this.log = log;
	}
	
	
	
}
