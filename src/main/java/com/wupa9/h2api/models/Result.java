package com.wupa9.h2api.models;

import java.util.HashMap;

public class Result {
	
	public String statement = "";
	public ResultSet resultSet = new ResultSet();
	public ResultStatus status = ResultStatus.SUCCESS;
	public int updateCount = -1;
	public String msg = "";
	public int errorCode = -1;
	
	public void pushColumn(String col) {
		resultSet.columns.add(col);
	}
	
	public void push(HashMap<String, String> rowData) {
		resultSet.data.add(rowData);
	}

}
