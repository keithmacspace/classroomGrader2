package net.cdonald.googleClassroom.googleClassroomInterface;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.cdonald.googleClassroom.gui.DebugLogDialog;
import net.cdonald.googleClassroom.listenerCoordinator.LongQueryResponder;
import net.cdonald.googleClassroom.model.ClassroomData;
import net.cdonald.googleClassroom.model.SQLDataBase;

public abstract class ClassroomDataFetcher extends LongQueryResponder<ClassroomData> implements DataFetchListener {

	private List<ClassroomData> dbAdd;
	private Set<String> readFromDB;
	protected GoogleClassroomCommunicator authorize;
	private String dataBaseTable;
	private Class<? extends Enum<?>> tableLabelEnum;
	private SQLDataBase dataBase;

	protected Exception communicationException;
	private SQLException databaseException;
	private Exception miscException;


	public ClassroomDataFetcher(GoogleClassroomCommunicator authorize) {
		super();
		this.authorize = authorize;
		communicationException = null;
		databaseException = null;
		miscException = null;
		dbAdd = new ArrayList<ClassroomData>();
		readFromDB = new HashSet<String>();
	}
	
	protected abstract ClassroomData newData(Map<String, String> initData);

	@Override
	public void retrievedInfo(ClassroomData data) {
		publish(data);
	}

	protected void readDataBase(String dataBaseName, String dataBaseTable, Class<? extends Enum<?>> tableLabelEnum) {
		if (dataBaseName != null) {

			this.dataBaseTable = dataBaseTable;
			this.tableLabelEnum = tableLabelEnum;
			this.dataBase = new SQLDataBase();
			try {

				dataBase.connect(dataBaseName);
				List<Map<String, String>> data = null;
				data = dataBase.load(dataBaseTable, tableLabelEnum);
				if (data != null) {
					for (Map<String, String> dbInfo : data) {
						ClassroomData temp = newData(dbInfo);
						publish(temp);
					}
				}
			} catch (SQLException e) {	
				DebugLogDialog.appendException(e);
				databaseException = e;
			} catch (Exception x) {
				miscException = x;
				DebugLogDialog.appendException(x);
			}
		}
	}

	@Override
	protected void process(List<ClassroomData> chunks) {

		int currentSize = chunks.size();
		if (currentSize != 0) {
			List<ClassroomData> sendToListener = new ArrayList<ClassroomData>();
			for (int i = 0; i < currentSize; i++) {
				ClassroomData data = chunks.get(i);
				// If it is in our readFromDB list, that means we got it from the database
				// already & the listener added it
				if (readFromDB == null || readFromDB.contains(data.getId()) == false) {
					sendToListener.add(data);
				}
				if (data.isRetrievedFromGoogle() == true) {
					dbAdd.add(data);
				} else {
					// Start out adding everything in the database to the remove list
					// In done(), we'll go through and remove all the ones we're adding
					// to the database leaving with only the last few to remove
					readFromDB.add(data.getId());
				}
			}
			getListener().process(sendToListener);
		}
	}

	@Override
	protected void done() {
		try {
			get();
		}catch(Exception e) {
			communicationException = e;
			e.printStackTrace();
		}
		if (shouldPossiblyRetry()) {
			if (JOptionPane.showConfirmDialog(null, communicationException.getMessage() + "\n\nRetry?",  "Error Communicating With The Classroom",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
				LongQueryResponder<?> responder = newInstance();
				responder.setListener(this.getListener());
				responder.execute();
				return;
			}
				
			
		}

		if (communicationException != null) {
			System.err.println("google classroom error " + communicationException.getMessage());
		}
		if (databaseException != null) {
			System.err.println("local database error " + databaseException.getMessage());
		}
		if (dataBase != null) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {

					if (databaseException == null && miscException == null) {
						try {
							dataBase.save(dataBaseTable, tableLabelEnum, dbAdd);
							for (ClassroomData dataCheck : dbAdd) {
								readFromDB.remove(dataCheck.getId());
							}
							if (readFromDB.size() != 0 && dbAdd.size() != 0) {
								sendRemove(readFromDB);
								dataBase.delete(dataBaseTable, tableLabelEnum, readFromDB);
							}

						} catch (SQLException e) {							
							databaseException = e;
							DebugLogDialog.appendException(e);
						} catch (Exception x) {
							miscException = x;
							DebugLogDialog.appendException(x);
						}
						dataBase.disconnect();
					}

				}
			});
		}
		super.done();
	}

	public Exception getCommunicationException() {
		return communicationException;
	}

	public SQLException getDatabaseException() {
		return databaseException;
	}

	public Exception getMiscException() {
		return miscException;
	}
	
	public boolean shouldPossiblyRetry() {
		return (communicationException != null);
	}
	
}
