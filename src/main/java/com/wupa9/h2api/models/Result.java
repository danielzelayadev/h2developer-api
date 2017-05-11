package com.wupa9.h2api.models;

import java.util.ArrayList;

public class Result {
	
	public String statement = "";
	public ArrayList<ArrayList<Entry>> resultSet = new ArrayList<>();
	public ResultStatus status = ResultStatus.SUCCESS;
	public String msg = "";
	
	public void push(ArrayList<Entry> register) {
		resultSet.add(register);
	}

}
