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

import h2.core.H2Connection;

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
				SESSION.disconnect();
				storeSession();
			} catch (SQLException e) {
				return Response.status(500)
						.entity(new Error("Could not disconnect.", e.getMessage())).build();
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
		
		if (SESSION != null)
			try {
				SESSION.disconnect();
			} catch (SQLException e) {
				return Response.status(500)
						.entity(new Error("Could not disconnect current session.", e.getMessage())).build();
			}
		
		try {
			SESSION = new H2Connection(data.getUrl(), data.getUsername(), data.getPassword());
			storeSession();
			
			return Response.status(200).entity(data).build();
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
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response disconnect() {
		if (SESSION == null)
			return Response.status(400).entity(new Error("Not even connected.", "")).build();
		
		try {
			closeSession();
			storeSession();
			return Response.status(200).build();
		} catch (SQLException e) {
			return Response.status(500).entity(new Error("Failed to disconnect.", e.getMessage())).build();
		} catch (IOException e) {
			return Response.status(500).entity(new Error("Failed to store session.", e.getMessage())).build();
		}
	}
	
	private void closeSession() throws SQLException {
		if (SESSION == null) return;
		
		SESSION.disconnect();
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
