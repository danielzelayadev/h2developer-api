package h2.core.datastructs;

public class Column {

	private String name;
	
	public Column(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	@Override
	public String toString() {
		return name;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Column)
			return name.equals(((Column)obj).name);
		if (obj instanceof String)
			return name.equals((String)obj);
		
		return super.equals(obj);
	}
	
}
