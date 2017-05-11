package h2.core.datastructs;

import java.util.ArrayList;
import java.util.Iterator;

public class Row implements Iterable<String> {

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
	
	public String get(int cellNo) {
		return getCell(cellNo).getValue();
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

	@Override
	public Iterator<String> iterator() {
		return new Iterator<String>() {
			
			private int indx = 0;
			
			@Override
			public String next() {
				return cells.get(indx++).getValue();
			}
			
			@Override
			public boolean hasNext() {
				return indx < cells.size();
			}
		};
	}
	
}
