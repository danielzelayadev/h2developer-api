package h2.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import h2.core.datastructs.StatementResult;
import h2.core.datastructs.Table;

public class H2Connection {
	
	private Connection conn;
	private h2.core.datastructs.Connection _conn;
	
	public H2Connection(){}
	
	public H2Connection(String url) throws SQLException {
		setConnection(url, "", "");
	}
	
	public H2Connection(String url, String username, String password) throws SQLException {
		setConnection(url, username, password);
	}
	
	public H2Connection(h2.core.datastructs.Connection conn) throws SQLException {
		setConnection(conn);
	}
	
	public h2.core.datastructs.Connection getConn() {
		return _conn;
	}
	
	public void setConnection(h2.core.datastructs.Connection conn) throws SQLException {
		this.setConnection(conn.getUrl(), conn.getUsername(), conn.getPassword());
		_conn = conn;
	}
	
	public void setConnection(String url, String username, String password) throws SQLException {
		this.conn = DriverManager.getConnection(url, username, password);
		_conn = new h2.core.datastructs.Connection(url, username, password);
	}
	
	public void connect() throws SQLException {
		if (conn != null && conn.isClosed())
			this.conn = DriverManager.getConnection(_conn.getUrl(), _conn.getUsername(), _conn.getPassword());
	}
	
	public void disconnect() throws SQLException {
		if (conn != null && !conn.isClosed())
			conn.close();
	}
	
	public StatementResult execute(String sql) throws SQLException {
		StatementResult result = new StatementResult();
		Statement stmt = conn.createStatement();
		
		if (stmt.execute(sql))
			result.table = new Table(stmt.getResultSet());
		else
			result.updateCount = stmt.getUpdateCount();
		
		stmt.close();
		
		return result;
	}
	
	public Table query(String q) throws SQLException {
		return new Table(conn.createStatement().executeQuery(q));
	}
	
	@Override
	public String toString() {
		return _conn.toString();
	}
	
}