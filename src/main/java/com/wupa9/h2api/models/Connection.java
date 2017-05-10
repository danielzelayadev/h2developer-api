package com.wupa9.h2api.models;

import java.io.Serializable;

public class Connection implements Serializable {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 9113892149426371973L;
	
	private String url;
	
	public Connection(){}
	
	public Connection(String url) {
		setUrl(url);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
	
	@Override
	public String toString() {
		return url;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Connection) {
			Connection c = (Connection)obj;
			return url.equals(c.url);
		}
		
		return super.equals(obj);
	}
	
}
