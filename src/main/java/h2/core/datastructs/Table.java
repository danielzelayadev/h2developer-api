package h2.core.datastructs;

import java.util.ArrayList;
import java.util.Iterator;

public class Table implements Iterable<Row> {

	private ArrayList<Column> columns;
	private ArrayList<Row> rows;
	
	public Table() {
		columns = new ArrayList<>();
		rows = new ArrayList<>();
	}
	
	public void addColumn(Column c) {
		columns.add(c);
		for (Row row : rows)
			row.addCell(new Cell(""));
	}

	public void addColumn(int i, Column c) {
		columns.add(i, c);
		for (Row row : rows)
			row.addCell(i, new Cell(""));
	}
	
	public void removeColumn(int i) {
		columns.remove(i);
		for (Row row : rows)
			row.removeCell(i);
	}
	
	public void removeColumn(String name) {
		for (int i = 0; i < columns.size(); i++)
			if (columns.get(i).getName().equals(name))
				removeColumn(i);
	}
	
	public void setColumn(int i, Column c) {
		removeColumn(i);
		addColumn(i, c);
	}
	
	public Column getColumn(int i) {
		return columns.get(i);
	}
	
	public Column getColumn(String name){
		for (Column col : columns)
			if (col.getName().equals(name))
				return col;
		return null;
	}
	
	public Row getRow(int i) {
		return rows.get(i);
	}
	
	public void insertRow(Row r) {
		rows.add(r);
	}
	
	public void updateRow(int i, Row r) {
		rows.remove(i);
		rows.add(i, r);
	}
	
	public void deleteRow(int i) {
		rows.remove(i);
	}
	
	public int getColumnCount() {
		return columns.size();
	}
	
	public int getRowCount() {
		return rows.size();
	}
	
	public String get(String colName, int rowNo) {
		return rows
				.get(rowNo)
				.getCell(columns.indexOf(colName))
				.getValue();
	}

	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("");
		
		for (int i = 0; i < columns.size(); i++) {
			str.append(columns.get(i).getName());
			str.append('\t');
		}
		
		for (int i = 0; i < rows.size(); i++) {
			str.append('\n');
			str.append(rows.get(i));
		}
		
		return str.toString();
	}

	@Override
	public Iterator<Row> iterator() {
		return new Iterator<Row>() {
			
			private int indx = 0;

			@Override
			public boolean hasNext() {
				return indx < rows.size();
			}

			@Override
			public Row next() {
				return rows.get(indx++);
			}
			
		};
	}
	
}
