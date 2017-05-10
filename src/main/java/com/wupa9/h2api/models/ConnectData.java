package com.wupa9.h2api.models;

public class ConnectData extends Connection {

	/**
	 * 
	 */
	private static final long serialVersionUID = -9072305369901801104L;
	
	private String username, password;
	
	public ConnectData() {}
	
	public ConnectData(String url, String username, String password) {
		super(url);
		setUsername(username);
		setPassword(password);
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	
	@Override
	public String toString() {
		return "url: " + getUrl() + ", username: " + username + ", password: " + password;
	}
	
}
