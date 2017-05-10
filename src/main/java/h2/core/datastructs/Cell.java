package h2.core.datastructs;

public class Cell {

	private String value = "";
	
	public Cell(String value) {
		this.setValue(value);
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public String getValue() {
		return value;
	}
	
	@Override
	public String toString() {
		return value;
	}
	
}