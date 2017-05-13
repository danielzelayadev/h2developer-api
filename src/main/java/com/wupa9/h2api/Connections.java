package com.wupa9.h2api;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wupa9.h2api.models.ConnectData;
import com.wupa9.h2api.models.Connection;
import com.wupa9.h2api.models.Result;
import com.wupa9.h2api.models.ResultStatus;
import com.wupa9.h2api.models.SchemaTreeNode;
import com.wupa9.h2api.models.SessionData;
import com.wupa9.h2api.models.UserTreeNode;

import h2.core.H2Connection;
import h2.core.datastructs.Column;
import h2.core.datastructs.Row;
import h2.core.datastructs.StatementResult;
import h2.core.datastructs.Table;

/**
 * Connections resource
 */
@Path("connections")
public class Connections {
	
	private static H2Connection SESSION = new H2Connection();
	private static ArrayList<Connection> CONNS = new ArrayList<>();
	private static final String DATA_HOME = "data"; 
	private static final String SESSION_FILE = DATA_HOME + "/session"; 
	private static final String CONNECTIONS_FILE = DATA_HOME + "/connections"; 
	
	public Connections() {
		try {
			Class.forName("org.h2.Driver");
			loadSession();
			loadConnections();
			System.out.println("Session: " + SESSION);
			System.out.println("Connections:\n" + CONNS);
		} catch (ClassNotFoundException | IOException | SQLException e) {
			e.printStackTrace();
		}
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getConnections() {
		return Response.status(200).entity(CONNS).build();
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response newConnection(ConnectData data) {
		if (CONNS.contains(data))
			return Response.status(400)
					.entity(new Error("Connection already saved.", "")).build();
		
		try {
			H2Connection c = new H2Connection(data.getUrl(), data.getUsername(), data.getPassword());
			c.disconnect();
			
			CONNS.add(new Connection(data.getUrl()));
			storeConnections();
			
			return Response.status(200).entity(data).build();
		} catch (SQLException e) {
			return Response.status(500).
					entity(new Error("Connection failed.", e.getMessage())).build();
		} catch (IOException e) {
			CONNS.remove(CONNS.size() - 1);
			return Response.status(500).
					entity(new Error("Failed to add connection.", e.getMessage())).build();
		}
	}
	
	@DELETE
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON })
	public Response removeConnection(Connection conn) {
		if (!CONNS.contains(conn))
			return Response.status(200).entity("Nothing changed.").build();
		
		if (SESSION != null && conn.getUrl().equals(SESSION.getConn().getUrl())) {
			try {
				closeSession();
				storeSession();
			} catch (IOException e) {
				return Response.status(500)
						.entity(new Error("Failed to store session change.", e.getMessage())).build();
			}
		}
		
		CONNS.remove(conn);
		try {
			storeConnections();
			return Response.status(200).build();
		} catch (IOException e) {
			CONNS.add(conn);
			return Response.status(500)
					.entity(new Error("Could not store connections.", e.getMessage())).build();
		}
	}
	
	@POST
	@Path("connect")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response connect(ConnectData data) {
		if (!CONNS.contains(data))
			return Response.status(400).entity(new Error("Unknown connection url.", "")).build();
		
		try {
			SESSION = new H2Connection(data.getUrl(), data.getUsername(), data.getPassword());
			SESSION.disconnect();
			storeSession();
			
			return Response.status(200).entity(getDBTree()).build();
		} catch (SQLException e) {
			return Response.status(500)
					.entity(new Error("Could not connect.", e.getMessage())).build();
		} catch (IOException e) {
			return Response.status(500)
					.entity(new Error("Could not store session.", e.getMessage())).build();
		}
	}
	
	@POST
	@Path("disconnect")
	@Produces(MediaType.APPLICATION_JSON)
	public Response disconnect() {
		if (SESSION == null)
			return Response.status(400).entity(new Error("Not even connected.", "")).build();
		
		try {
			closeSession();
			storeSession();
			return Response.status(200).build();
		} catch (IOException e) {
			return Response.status(500).entity(new Error("Failed to store session.", e.getMessage())).build();
		}
	}
	
	@GET
	@Path("session")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSession() {
		SessionData sd = new SessionData();
		
		if (SESSION != null) {
			sd.conn = new ConnectData(SESSION.getConn().getUrl(), 
					SESSION.getConn().getUsername(), SESSION.getConn().getPassword());
			try {
				sd.dbTree = getDBTree();
			} catch (SQLException e) {
				return Response.status(500)
						.entity(new Error("Failed to fetch DB Tree.", e.getMessage()))
						.build();
			}
		}
		
		return Response.status(200).entity(sd).build();
	}
	
	@POST
	@Path("run")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	public Response run(String stmt) {
		if (SESSION == null)
			return Response.status(403)
					.entity(new Error("You must connect to a db first.", ""))
					.build();
		
		try {
			SESSION.connect();
		} catch (SQLException e1) {
			return Response.status(500).entity(new Error("Connection failed.",e1.getMessage())).build();
		}
		
		Result res = new Result();
		res.statement = stmt;
		
		try {
			StatementResult sres = SESSION.execute(stmt);
			
			res.status = ResultStatus.SUCCESS;
			
			if (sres.updateCount != -1)
				res.updateCount = sres.updateCount;
			else {
				Table t = sres.table;
				ArrayList<Column> cols = t.getColumns();
				
				for (Column c : cols)
					res.pushColumn(c.getName());
				
				for (Row row : t) {
					int i = 0;
					HashMap<String, String> rowData = new HashMap<>();
					
					for (String val : row)
						rowData.put(t.getColumn(i++).getName(), val);
					
					res.push(rowData);
				}
			}
			
		} catch (SQLException e) {
			res.errorCode = e.getErrorCode();
			res.msg = e.getMessage();
			res.status = ResultStatus.FAILED;
		}
		
		try {
			SESSION.disconnect();
		} catch (SQLException e) {
			return Response.status(500).entity(new Error("Failed to disconnect.", e.getMessage())).build();
		}
		
		return Response.status(200).entity(res).build();
	}
	
	private ArrayList<UserTreeNode> getDBTree() throws SQLException {
		if (SESSION == null)
			return null;
		
		SESSION.connect();
		
		ArrayList<UserTreeNode> dbTree = new ArrayList<>();
		
		Table users = SESSION.query("select name from information_schema.users");
		
		for (Row row : users) {
			UserTreeNode utn = new UserTreeNode();
			
			utn.user = row.get(0);
			
			Table schemas = SESSION
					.query("select schema_name from	information_schema.schemata " +
						   "where schema_owner='" + utn.user + "'");
			
			for (Row _row : schemas) {
				SchemaTreeNode stn = new SchemaTreeNode();
				
				stn.schema = _row.get(0);
				
				Table constants = SESSION
						.query("select constant_name from information_schema.constants " +
							   "where constant_schema='" + stn.schema + "'");
				
				for (Row cRow : constants)
					stn.constants.add(cRow.get(0));
				
				Table constraints = SESSION
						.query("select constraint_name from information_schema.constraints " +
							   "where constraint_schema='" + stn.schema + "'");
				
				for (Row cRow : constraints)
					stn.constraints.add(cRow.get(0));
				
				Table functions = SESSION
						.query("select alias_name from information_schema.function_aliases " +
							   "where alias_schema='" + stn.schema + "'");
				
				for (Row cRow : functions)
					stn.functions.add(cRow.get(0));
				
				Table indexes = SESSION
						.query("select index_name from information_schema.indexes " +
							   "where table_schema='" + stn.schema + "'");
				
				for (Row cRow : indexes)
					stn.indexes.add(cRow.get(0));
				
				Table sequences = SESSION
						.query("select sequence_name from information_schema.sequences " +
							   "where sequence_schema='" + stn.schema + "'");
				
				for (Row cRow : sequences)
					stn.sequences.add(cRow.get(0));
				
				Table tables = SESSION
						.query("select table_name from information_schema.tables " +
							   "where table_schema='" + stn.schema + "'");
				
				for (Row cRow : tables)
					stn.tables.add(cRow.get(0));
				
				Table triggers = SESSION
						.query("select trigger_name from information_schema.triggers " +
							   "where trigger_schema='" + stn.schema + "'");
				
				for (Row cRow : triggers)
					stn.triggers.add(cRow.get(0));
				
				Table views = SESSION
						.query("select table_name from information_schema.views " +
							   "where table_schema='" + stn.schema + "'");
				
				for (Row cRow : views)
					stn.views.add(cRow.get(0));
				
				
				utn.schemas.add(stn);
			}
					
			dbTree.add(utn);
		}
		
		SESSION.disconnect();
		
		return dbTree;
	}
	
	private void closeSession() {
		SESSION = null;
	}
	
	private void loadSession() throws FileNotFoundException, IOException, ClassNotFoundException, SQLException {
		File f = new File(SESSION_FILE);
		
		if (f.length() <= 0) {
			SESSION = null;
			return;
		}
		
		ObjectInputStream is = new ObjectInputStream(new FileInputStream(f));
		
		h2.core.datastructs.Connection c = (h2.core.datastructs.Connection) is.readObject();
		SESSION = c == null ? null : new H2Connection(c);
		
		if (SESSION != null)
			SESSION.disconnect();
		
		is.close();
	}
	
	public void storeSession() throws FileNotFoundException, IOException {
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(SESSION_FILE));
		os.writeObject(SESSION == null ? null : SESSION.getConn());
		os.close();
	}
	
	private void storeConnections() throws FileNotFoundException, IOException {
		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(CONNECTIONS_FILE));
		os.writeObject(CONNS);
		os.close();
	}
    
	@SuppressWarnings({ "unchecked" })
	private void loadConnections() throws FileNotFoundException, IOException, ClassNotFoundException {
		File f = new File(CONNECTIONS_FILE);
		
		if (f.length() <= 0) {
			CONNS = new ArrayList<>();
			return;
		}
		
		ObjectInputStream is = new ObjectInputStream(new FileInputStream(CONNECTIONS_FILE));
		
		CONNS = (ArrayList<Connection>) is.readObject();
		
		is.close();
	}
	
}
