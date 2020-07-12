package graderio;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQLUtil
{
	private static Connection sqlConnection = null;
	
	public SQLUtil(String filePath) throws SQLException
	{
		boolean needReinit = false;
		if (sqlConnection == null)
		{
			needReinit = true;
		}
		else
		{
			if (sqlConnection.isClosed())
			{
				needReinit = true;
			}
		}
		if (needReinit) 
		{
			try
			{
				Class.forName("org.sqlite.JDBC");
			}
			catch (ClassNotFoundException cnfe)
			{
				cnfe.printStackTrace();
			}
			
			sqlConnection = DriverManager.getConnection("jdbc:sqlite:" + filePath);
		}
	}
	
	public void initTables() throws SQLException
	{
		DatabaseMetaData dbm = sqlConnection.getMetaData();
		ResultSet tables = dbm.getTables(null, null, "postdb", null);
		if (!tables.next()) 
		{
			String query = "CREATE TABLE postdb(id INT, tags VARCHAR(255) NULL, artist VARCHAR(255) NULL, rating INT NULL, score INT NULL, sources VARCHAR(255) NULL, created_at BIGINT NULL);";
			PreparedStatement stmt = sqlConnection.prepareStatement(query);
			stmt.execute();
		}
	}

}