package com.wupa9.h2api.models;

import java.util.ArrayList;
import java.util.HashMap;

public class SchemaTreeNode {

	public String schema = "";
	public String ddl    = "";
	public String query  = "";
	public HashMap<String, String> templates = new HashMap<>();
	
	public ArrayList<DBObject> constants   = new ArrayList<>();
	public ArrayList<DBObject> constraints = new ArrayList<>();
	public ArrayList<DBObject> functions   = new ArrayList<>();
	public ArrayList<DBObject> indexes     = new ArrayList<>();
	public ArrayList<DBObject> sequences   = new ArrayList<>();
	public ArrayList<DBObject> tables      = new ArrayList<>();
	public ArrayList<DBObject> triggers    = new ArrayList<>();
	public ArrayList<DBObject> views       = new ArrayList<>();
	
}
