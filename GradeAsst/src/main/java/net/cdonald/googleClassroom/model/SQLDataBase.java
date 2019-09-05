package net.cdonald.googleClassroom.model;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.cdonald.googleClassroom.gui.DebugLogDialog;

public class SQLDataBase {

	private Connection conn;

	public SQLDataBase() {
		super();

		conn = null;
	}

	public void connect(String url) throws SQLException {
		if (conn == null) {

			url = "jdbc:sqlite:" + url;
			conn = DriverManager.getConnection(url);
			//System.out.println("Connection established");
		}
	}

	public void disconnect() {
		if (conn != null) {
			try {
				conn.close();
				//System.out.println("Connection closed");
				conn = null;
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}



	public static String[] getDBNames(Class<? extends Enum<?>> e) {
		return Arrays.stream(e.getEnumConstants()).map(Enum::name).toArray(String[]::new);
	}
	

	private String buildSQL(String[] tableLabels, String postFieldString, String postBuildString,
			boolean useFieldNames) {
		String sqlString = "";
		int count = 0;

		for (String fieldName : tableLabels) {
			if (count != 0) {
				sqlString += ", ";
			}
			if (useFieldNames) {
				sqlString += fieldName.toString();
			}
			sqlString += postFieldString;
			count++;
		}

		return sqlString + postBuildString;
	}

	public void simpleSave(String tableName, Class<? extends Enum<?>> tableLabelEnum, Map<String, String> values) throws SQLException {
		String[] tableLabels = getDBNames(tableLabelEnum);
		conditionallyAddTable(tableName, tableLabels);
		PreparedStatement checkStmt = createCheckStatement(tableName, tableLabels);
		PreparedStatement insertStmt = createInsertStatement(tableName, tableLabels);
		PreparedStatement updateStmt = createUpdateStatement(tableName, tableLabels);
		String [] data = new String[tableLabels.length];

		int count = 0;
		for (String tableLabel : tableLabels) {
			data[count] = values.get(tableLabel);
			count++;
		}
		saveToDB(checkStmt, insertStmt, updateStmt, data);
		updateStmt.close();
		insertStmt.close();
		checkStmt.close();
		
	}
	
	public void save(String tableName, Class<? extends Enum<?>> tableLabelEnum, List<ClassroomData> dataList)
			throws SQLException {
		String[] tableLabels = getDBNames(tableLabelEnum);
		conditionallyAddTable(tableName, tableLabels);
		PreparedStatement checkStmt = createCheckStatement(tableName, tableLabels);
		PreparedStatement insertStmt = createInsertStatement(tableName, tableLabels);
		PreparedStatement updateStmt = createUpdateStatement(tableName, tableLabels);

		for (ClassroomData data : dataList) {
			String[] mainData = data.getDBValues();
			saveToDB(checkStmt, insertStmt, updateStmt, mainData);
		}

		updateStmt.close();
		insertStmt.close();
		checkStmt.close();
	}

	
	
	private PreparedStatement createInsertStatement(String tableName, String [] tableLabels) throws SQLException {
		String insertValues = "values (" + buildSQL(tableLabels, "?", ")", false);
		String insertSQL = "insert into " + tableName + " (" + buildSQL(tableLabels, "", ") " + insertValues, true);
		PreparedStatement insertStmt = conn.prepareStatement(insertSQL);
		return insertStmt;
	}
	
	private PreparedStatement createUpdateStatement(String tableName, String [] tableLabels) throws SQLException {
		String updateSQL = "update " + tableName + " set "
				+ buildSQL(tableLabels, "=?", " where " + tableLabels[0] + "=?", true);
		PreparedStatement updateStmt = conn.prepareStatement(updateSQL);
		return updateStmt;
	}
	
	private PreparedStatement createCheckStatement(String tableName, String [] tableLabels) throws SQLException {
		PreparedStatement checkStmt = conn
				.prepareStatement("SELECT count(*) as count FROM " + tableName + " WHERE " + tableLabels[0] + "=?");
		return checkStmt;
	}
	
	private void saveToDB(PreparedStatement checkStmt, PreparedStatement insertStmt, PreparedStatement updateStmt, String [] mainData) throws SQLException {		
		String id = mainData[0];
		checkStmt.setString(1, id);		
		ResultSet checkResult = checkStmt.executeQuery();
		checkResult.next();
		int count = checkResult.getInt(1);
		int col = 1;
		PreparedStatement stmtToUse = null;
		if (count == 0) {
			stmtToUse = insertStmt;
		} else {
			stmtToUse = updateStmt;
		}

		for (String value : mainData) {
			stmtToUse.setString(col++, value);
		}

		// Update has one last field, we use the id a second time for the query
		if (count != 0) {
			stmtToUse.setString(col++, id);
		}
		stmtToUse.executeUpdate();
	}

	public void delete(String tableName, Class<? extends Enum<?>> tableLabelEnum, Set<String> fieldValues) {
		if (fieldValues.size() != 0) {
			String[] tableLabels = getDBNames(tableLabelEnum);
			String fieldName = tableLabels[0];
			String statement = "DELETE FROM " + tableName + " WHERE ";
			boolean first = true;
			for (String fieldValue : fieldValues) {
				if (!first) {
					statement += " OR ";
				}
				statement += fieldName + "='" + fieldValue.toString() + "'";
				first = false; 
			}			
			//System.err.println(statement);
			try {
				PreparedStatement removeStmt = conn.prepareStatement(statement);
				removeStmt.execute();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private  Map<String, String> fillMap(ResultSet results, String [] tableLabels) throws SQLException {		
		Map<String, String> fields = new HashMap<String, String>();
		for (String labelName : tableLabels) {
			String value = results.getString(labelName);
			// System.err.println("putting " + tableName + " " + labelName + " = " + value);
			fields.put(labelName, value);
		}
		return fields;
	}

	public Map<String, String> loadTableEntry(String tableName, Class<? extends Enum<?>> tableLabelEnum, String fieldName, String id)
			throws SQLException {
		String[] tableLabels = getDBNames(tableLabelEnum);
		// System.err.println("loading " + tableName + Arrays.toString(tableLabels));
		conditionallyAddTable(tableName, tableLabels);
		final String selectSQL = "SELECT * FROM " + tableName + " WHERE " + fieldName + "=?";
	
		PreparedStatement stmt = conn.prepareStatement(selectSQL);
		stmt.setString(1, id);
		
		return fillMap(stmt.executeQuery(), tableLabels);
	}

	public List<Map<String, String>> load(String tableName, Class<? extends Enum<?>> tableLabelEnum)
			throws SQLException {

		String[] tableLabels = getDBNames(tableLabelEnum);
		// System.err.println("loading " + tableName + Arrays.toString(tableLabels));
		conditionallyAddTable(tableName, tableLabels);
		final String selectSQL = "SELECT " + buildSQL(tableLabels, "", " FROM " + tableName, true);
		Statement selectStmt = conn.createStatement();
		ResultSet results = selectStmt.executeQuery(selectSQL);
		List<Map<String, String>> fullList = new ArrayList<Map<String, String>>();
		while (results.next()) {
			fullList.add(fillMap(results, tableLabels));
		}
		selectStmt.close();
		// System.err.println("done loading " + tableName);
		return fullList;

	}

	private void conditionallyAddTable(String tableName, String[] tableLabels) {
		try {
			if (conn != null) {
				boolean found = false;
				DatabaseMetaData dbmd = conn.getMetaData();
				String table[] = { "TABLE" };
				ResultSet rs = dbmd.getTables(null, null, null, table);
				while (rs.next()) {
					if (rs.getString("TABLE_NAME").compareToIgnoreCase(tableName) == 0) {
						found = true;
						break;
					}
				}
				if (found == false) {
					String sql = "CREATE TABLE " + tableName + " (";

					for (String label : tableLabels) {
						sql += " " + label + " TEXT, ";
					}
					sql += " PRIMARY KEY (" + tableLabels[0] + "))";
					PreparedStatement stmt = conn.prepareStatement(sql);
					stmt.executeUpdate();
				}
			}

		} catch (SQLException e) {
			// System.err.print("SQL Error " + e.getMessage());
		}
	}
	
	public static void main(String [] args) {
		SQLDataBase dbTest = new SQLDataBase();
		try {
			dbTest.connect("C:\\Users\\kdmacdon\\Documents\\Teals\\GoogleClassroomData\\AP_Computer_Science_A\\class.db");
			Map<String, String> entry = dbTest.loadTableEntry("Assignments", ClassroomData.fieldNames.class, "ID", "34979458003");			
			System.out.println(entry);
			dbTest.disconnect();
		} catch (SQLException e) {
			System.out.println(e.getMessage());
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	/*
	 * public void saveToFile(File file) throws IOException { FileOutputStream out =
	 * new FileOutputStream(file); ObjectOutputStream stream = new
	 * ObjectOutputStream(out);
	 * 
	 * Person [] persons = students.toArray(new Person[students.size()]);
	 * stream.writeObject(persons);
	 * 
	 * stream.writeObject(persons);
	 * 
	 * stream.close(); }
	 * 
	 * public boolean loadFromFile(File file) throws IOException { FileInputStream
	 * in = new FileInputStream(file); ObjectInputStream stream = new
	 * ObjectInputStream(in);
	 * 
	 * try { Person [] persons =(Person[]) stream.readObject(); students.clear();
	 * students.addAll(Arrays.asList(persons)); } catch (ClassNotFoundException x) {
	 * stream.close(); return false; } stream.close(); return true; }
	 */
}
