package h2.core.datastructs;

public class StatementResult {

	public String statement = "";
	public int updateCount = -1;
	public Table table =  null;
	
	public StatementResult(){}
	public StatementResult(int uc, Table t) {
		updateCount = uc;
		table = t;
	}
}
