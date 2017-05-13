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
import com.wupa9.h2api.models.DBObject;
import com.wupa9.h2api.models.DBTreeRoot;
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
	
	private DBTreeRoot getDBTree() throws SQLException {
		if (SESSION == null)
			return null;
		
		SESSION.connect();
		
		DBTreeRoot dbTree = new DBTreeRoot();
		
		dbTree.templates.put("createSchema", "CREATE SCHEMA IF NOT EXISTS <NAME> AUTHORIZATION <USER_NAME>");
		dbTree.templates.put("createUser", "CREATE USER <NAME> PASSWORD '<PASSWORD>' [ADMIN]");
		dbTree.templates.put("setPassword", "ALTER USER <NAME> SET PASSWORD '<NEW_PASS>'");
		dbTree.templates.put("setAdmin", "ALTER USER <NAME> ADMIN [TRUE|FALSE]");
		
		Table allUsers = SESSION.query("select name, admin from information_schema.users");
		
		for (Row row : allUsers) {
			UserTreeNode utn = getUserNodeFromRow(row);
			
			if (utn.user.equalsIgnoreCase(SESSION.getConn().getUsername())) {
				dbTree.userSchemas = utn.schemas;
				continue;
			}
			
			dbTree.otherUsers.add(utn);
		}
		
		SESSION.disconnect();
		
		return dbTree;
	}
	
	private UserTreeNode getUserNodeFromRow(Row row) throws SQLException {
		UserTreeNode utn = new UserTreeNode();
		
		utn.user = row.get(0);
		utn.ddl = "CREATE USER IF NOT EXISTS " + utn.user + 
				" SALT '059f80ec7337ca19' HASH 'd92312214feaddf304154ec8b944a63116a542699ee4d2fb5a3b4e0b200cffea'";
		if (row.get(1).equals("true")) utn.ddl += " " + row.get(1);
		utn.query = "SELECT * FROM INFORMATION_SCHEMA.USERS WHERE NAME='" + utn.user + "'";
		utn.templates.put("setAdmin", "ALTER USER " + utn.user + " ADMIN [TRUE|FALSE]");
		utn.templates.put("rename", "ALTER USER " + utn.user + " RENAME TO <NEW_NAME>");
		utn.templates.put("setPassword", "ALTER USER " + utn.user + " SET PASSWORD '<NEW_PASS>'");
		utn.templates.put("drop", "DROP USER " + utn.user);
		utn.templates.put("createSchema", "CREATE SCHEMA IF NOT EXISTS <NAME> AUTHORIZATION <USER_NAME>");
		
		Table schemas = SESSION
				.query("select schema_name, schema_owner from	information_schema.schemata " +
					   "where schema_owner='" + utn.user + "'");
		
		for (Row _row : schemas)
			utn.schemas.add(getSchemaNodeFromRown(_row));
				
		return utn;
	}
	
	private SchemaTreeNode getSchemaNodeFromRown(Row row) throws SQLException {
		SchemaTreeNode stn = new SchemaTreeNode();
		
		stn.schema = row.get(0);
		stn.ddl = "CREATE SCHEMA IF NOT EXISTS " + stn.schema + " AUTHORIZATION " + row.get(1);
		stn.query = "select * from information_schema.schemata where schema_name='" + stn.schema +"'";
		
		stn.templates.put("createConstant", "CREATE CONSTANT IF NOT EXISTS <NAME> VALUE <EXPRESSION>");
		stn.templates.put("createConstraint", "ALTER TABLE IF EXISTS <TABLE_NAME> ADD CONSTRAINT "
				+ "<CONSTRAINT_NAME> [CHECK <EXPRESSION>|UNIQUE(<COL_NAME>)|FOREIGN KEY"
				+ "(<COL_NAME>) REFERENCES <TABLE_NAME>(<COL_NAME>)|PRIMARY KEY(<COL_NAME>)] [CHECK|NOCHECK]");
		stn.templates.put("createFunction", "CREATE ALIAS <NAME> FOR \"<JAVA_CLASS_METHOD>\"");
		stn.templates.put("createIndex", "CREATE INDEX <NAME> ON <TABLE_NAME>(<COL_NAME>)");
		stn.templates.put("createSequence", "CREATE SEQUENCE <NAME> START WITH <LONG>"
				+ "\n\tINCREMENT BY <LONG> MINVALUE <LONG> MAXVALUE <LONG>");
		stn.templates.put("createTable", "CREATE TABLE <NAME>(<COL_NAME> <DATA_TYPE> <CONSTRAINTS>,...)");
		stn.templates.put("createTrigger", "CREATE TRIGGER <NAME> [BEFORE|AFTER|INSTEAD OF] [INSERT|UPDATE|DELETE|SELECT|ROLLBACK]"
					+ " ON <TABLE_NAME> FOR EACH ROW CALL \"<TRIGGER_CLASS_NAME>\"");
		stn.templates.put("createView", "CREATE VIEW <VIEW_NAME> AS SELECT * FROM <TABLE_NAME> WHERE <COL_NAME> < 100");
		stn.templates.put("rename", "ALTER SCHEMA " + stn.schema + " RENAME TO <NEW_NAME>");
		stn.templates.put("drop", "DROP SCHEMA " + stn.schema);
		
		Table constants = SESSION
				.query("select constant_name, sql, constant_schema from information_schema.constants " +
					   "where constant_schema='" + stn.schema + "'");
		
		for (Row cRow : constants) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = "CREATE CONSTANT IF NOT EXISTS " + cRow.get(2) + "."  + cRow.get(0) + " VALUE " + cRow.get(1);
			obj.query = "select * from information_schema.constants where constant_name='".toUpperCase() + obj.name + "'";
			obj.templates.put("drop", "DROP CONSTANT IF EXISTS " + cRow.get(2) + "." + obj.name);
			stn.constants.add(obj);
		}
		
		Table constraints = SESSION
				.query("select constraint_name, sql, table_name, constraint_schema from information_schema.constraints " +
					   "where constraint_schema='" + stn.schema + "'");
		
		for (Row cRow : constraints) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = cRow.get(1);
			obj.query = "select * from information_schema.constraints where constraint_name='".toUpperCase() + obj.name + "'";
			obj.templates.put("rename", "ALTER TABLE " + cRow.get(3) + "." + cRow.get(2) + " RENAME CONSTRAINT " + obj.name + " TO <NEW_NAME>");
			obj.templates.put("drop", "ALTER TABLE " + cRow.get(3) + "." + cRow.get(2) + " DROP CONSTRAINT " + obj.name);
			stn.constraints.add(obj);
		}
		
		Table functions = SESSION
				.query("select alias_name, source, alias_schema from information_schema.function_aliases " +
					   "where alias_schema='" + stn.schema + "'");
		
		for (Row cRow : functions) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = "CREATE ALIAS " + cRow.get(2) + "." + obj.name + " AS $$ " + cRow.get(1) + "$$";
			obj.query = "select * from information_schema.function_aliases where alias_name='".toUpperCase() + obj.name + "'";
			obj.templates.put("drop", "DROP ALIAS " + cRow.get(2) + "." + obj.name);
			stn.functions.add(obj);
		}
		
		Table indexes = SESSION
				.query("select index_name, sql, table_schema from information_schema.indexes " +
					   "where table_schema='" + stn.schema + "'");
		
		for (Row cRow : indexes) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = cRow.get(1);
			obj.query = "select * from information_schema.indexes where index_name='".toUpperCase() + obj.name + "'";
			obj.templates.put("rename", "ALTER INDEX " + cRow.get(2) + "." + obj.name +" RENAME TO <NEW_NAME>");
			obj.templates.put("drop", "DROP INDEX IF EXISTS " + cRow.get(2) + "." + obj.name);
			stn.indexes.add(obj);
		}
		
		Table sequences = SESSION
				.query("select sequence_name, increment, min_value, max_value, sequence_schema from information_schema.sequences " +
					   "where sequence_schema='" + stn.schema + "'");
		
		for (Row cRow : sequences) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = "CREATE SEQUENCE IF NOT EXISTS " + cRow.get(4) + "." + obj.name + " INCREMENT BY " + 
					cRow.get(1) + " MINVALUE " + cRow.get(2) + " MAXVALUE " + cRow.get(3);
			obj.query = "select * from information_schema.sequences where sequence_name='".toUpperCase() + obj.name + "'";
			obj.templates.put("edit", "ALTER SEQUENCE " + cRow.get(4) + "." + obj.name + 
					" RESTART WITH <LONG>\n\tINCREMENT BY <LONG> MINVALUE <LONG> MAXVALUE <LONG>");
			obj.templates.put("drop", "DROP SEQUENCE IF EXISTS " + cRow.get(4) + "." + obj.name);
			stn.sequences.add(obj);
		}
		
		Table tables = SESSION
				.query("select table_name, sql, table_schema from information_schema.tables " +
					   "where table_schema='" + stn.schema + "'");
		
		for (Row cRow : tables) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = cRow.get(1);
			obj.query = "select * from ".toUpperCase() + cRow.get(2) + "." + obj.name;
			obj.templates.put("addColumn", "ALTER TABLE " + cRow.get(2) + "." + obj.name + " ADD <COL_NAME> <DATA_TYPE>");
			obj.templates.put("editColumn", "ALTER TABLE " + cRow.get(2) + "." + obj.name + " ALTER COLUMN <COL_NAME> <DATA_TYPE>");
			obj.templates.put("dropColumn", "ALTER TABLE " + cRow.get(2) + "." + obj.name + " DROP <COL NAME1>,<COL_NAME2>,...");
			obj.templates.put("addConstraint", "ALTER TABLE " + cRow.get(2) + "." + obj.name + " ADD CONSTRAINT [CHECK <EXPRESSION>|UNIQUE(<COL_NAME>)|FOREIGN KEY"
				+ "(<COL_NAME>) REFERENCES <TABLE_NAME>(<COL_NAME>)|PRIMARY KEY(<COL_NAME>)] [CHECK|NOCHECK]");
			obj.templates.put("renameConstraint", "ALTER TABLE " + cRow.get(2) + "." + obj.name + " RENAME CONSTRAINT <OLD_NAME> TO <NEW_NAME>");
			obj.templates.put("dropConstraint", "ALTER TABLE " + cRow.get(2) + "." + obj.name + " DROP CONSTRAINT <CONSTRAINT_NAME>");
			obj.templates.put("rename", "ALTER TABLE " + cRow.get(2) + "." + obj.name +" RENAME TO <NEW_NAME>");
			obj.templates.put("drop", "DROP TABLE " + cRow.get(2) + "." + obj.name + " [RESTRICT|CASCADE]");
			stn.tables.add(obj);
		}
		
		Table triggers = SESSION
				.query("select trigger_name, sql, trigger_schema from information_schema.triggers " +
					   "where trigger_schema='" + stn.schema + "'");
		
		for (Row cRow : triggers) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = cRow.get(1);
			obj.query = "select * from information_schema.triggers where trigger_name='".toUpperCase() + obj.name + "'";
			obj.templates.put("drop", "DROP TRIGGER IF EXISTS " + cRow.get(2) + "." + obj.name);
			stn.triggers.add(obj);
		}
		
		Table views = SESSION
				.query("select table_name, view_definition, table_schema from information_schema.views " +
					   "where table_schema='" + stn.schema + "'");
		
		for (Row cRow : views) {
			DBObject obj = new DBObject();
			obj.name = cRow.get(0);
			obj.ddl = cRow.get(1);
			obj.query = "select * from information_schema.views where table_name='".toUpperCase() + obj.name + "'";
			obj.templates.put("edit", "ALTER VIEW IF EXISTS " + cRow.get(2) + "." + obj.name + " RECOMPILE");
			obj.templates.put("drop", "DROP VIEW IF EXISTS " + cRow.get(2) + "." + obj.name + " [RESTRICT|CASCADE]");
			stn.views.add(obj);
		}
		
		return stn;
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
