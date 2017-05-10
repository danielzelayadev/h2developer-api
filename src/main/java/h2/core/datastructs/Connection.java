package h2.core.datastructs;

import java.io.Serializable;

public class Connection implements Serializable{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5610670858576020992L;
	
	private String url, username, password;
	
	public Connection(String url, String username, String password) {
		setUrl(url);
		setUsername(username);
		setPassword(password);
	}
	
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
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
		return "url: " + url + ", username: " + username + ", password: " + password;
	}
	
}
