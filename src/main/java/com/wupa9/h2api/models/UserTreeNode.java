package com.wupa9.h2api.models;

import java.util.ArrayList;
import java.util.HashMap;

public class UserTreeNode {
	
	public String user  = "";
	public String ddl   = "";
	public String query = "";
	public HashMap<String, String> templates = new HashMap<>();
	public ArrayList<SchemaTreeNode> schemas = new ArrayList<>();
	
}
