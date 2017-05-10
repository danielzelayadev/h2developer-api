package h2.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import h2.core.datastructs.Cell;
import h2.core.datastructs.Column;
import h2.core.datastructs.Row;
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
	
	public void disconnect() throws SQLException {
		if (conn != null && !conn.isClosed())
			conn.close();
	}
	
	public void execute(String s) throws SQLException {
		conn.createStatement().execute(s);
	}
	
	public Table query(String q) throws SQLException {
		Table table = new Table();
		ResultSet rs = conn.createStatement().executeQuery(q);
		ResultSetMetaData rsmd = rs.getMetaData();
		
		for (int i = 0; i < rsmd.getColumnCount(); i++)
			table.addColumn(new Column(rsmd.getColumnName(i + 1)));
			
        while (rs.next()) {
        	Row r = new Row();
        	for (int i = 0; i < rsmd.getColumnCount(); i++)
        		r.addCell(new Cell(rs.getString(i + 1)));
        	table.insertRow(r);
        }
        
		return table;
	}
	
	@Override
	public String toString() {
		return _conn.toString();
	}
	
}