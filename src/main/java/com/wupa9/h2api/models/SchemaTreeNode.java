package com.wupa9.h2api.models;

import java.util.ArrayList;

public class SchemaTreeNode {

	public String schema = "";
	
	public ArrayList<String> constants = new ArrayList<>();
	public ArrayList<String> constraints = new ArrayList<>();
	public ArrayList<String> functions = new ArrayList<>();
	public ArrayList<String> indexes = new ArrayList<>();
	public ArrayList<String> sequences = new ArrayList<>();
	public ArrayList<String> tables = new ArrayList<>();
	public ArrayList<String> triggers = new ArrayList<>();
	public ArrayList<String> views = new ArrayList<>();
	
}
