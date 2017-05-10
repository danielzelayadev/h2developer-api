package h2.core.datastructs;

import java.util.ArrayList;

public class Row {

	private ArrayList<Cell> cells;
	
	public Row() {
		cells = new ArrayList<>();
	}

	public Cell getCell(int indx) {
		return cells.get(indx);
	}
	
	public void setCell(int indx, Cell c) {
		cells.remove(indx);
		cells.add(indx, c);
	}
	
	public void addCell(Cell c) {
		cells.add(c);
	}
	
	public void addCell(int i, Cell c) {
		cells.remove(i);
		cells.add(i, c);
	}
	
	public void removeCell(int i) {
		cells.remove(i);
	}
	
	protected int getCellCount() {
		return cells.size();
	}
	
	@Override
	public String toString() {
		StringBuilder str = new StringBuilder("");
		
		for (int i = 0; i < cells.size(); i++) {
			str.append(cells.get(i));
			str.append('\t');
		}
		
		return str.toString();
	}
	
}
