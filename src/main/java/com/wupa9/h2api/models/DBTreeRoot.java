package com.wupa9.h2api.models;

import java.util.ArrayList;
import java.util.HashMap;

public class DBTreeRoot {
	
	public ArrayList<SchemaTreeNode> userSchemas = new ArrayList<>();
	public ArrayList<UserTreeNode> otherUsers = new ArrayList<>();
	public HashMap<String, String> templates = new HashMap<>();

}
