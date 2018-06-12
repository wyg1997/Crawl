package sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

public class Mysql
{
	public static ConnectionPool connPool = new ConnectionPool("com.mysql.jdbc.Driver", "jdbc:mysql://localhost/crawl?useUnicode=true&characterEncoding=UTF-8&useSSL=true", "root", "wang123.0han");
	private static ArrayList<String> temp;

	public static void Creat()
	{
		try
		{
			connPool.createPool();
		}
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static String query(String sql, String key) throws SQLException
	{

		Connection conn = connPool.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		String res = null;
		while (rs.next())
		{
			res = rs.getString(key);
		}
		rs.close();
		stmt.close();
		connPool.returnConnection(conn);
		return res;
	}

	public static String query(String sql) throws SQLException
	{

		Connection conn = connPool.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		String res = null;
		while (rs.next())
		{
			res = rs.getString(1);
		}
		rs.close();
		stmt.close();
		connPool.returnConnection(conn);
		return res;
	}

	public static ArrayList<String> select(String sql) throws SQLException
	{

		ArrayList<String> temp = new ArrayList<String>();
		Connection conn = connPool.getConnection();
		Statement stmt = conn.createStatement();
		ResultSet rs = stmt.executeQuery(sql);
		while (rs.next())
		{
			temp.add(rs.getString(1));
		}
		rs.close();
		stmt.close();
		connPool.returnConnection(conn);
		return temp;
	}

	public static void add(String sql) throws SQLException
	{
		Connection conn = connPool.getConnection();
		Statement stmt = conn.createStatement();
		stmt.execute(sql);
		stmt.close();
		connPool.returnConnection(conn);
	}

	public static void update(String sql) throws SQLException
	{
		Connection conn = connPool.getConnection();
		Statement stmt = conn.createStatement();
		stmt.executeUpdate(sql);
		stmt.close();
		connPool.returnConnection(conn);
	}

	public static void delete(String sql) throws SQLException
	{
		Connection conn = connPool.getConnection();
		Statement stmt = conn.createStatement();
		stmt.execute(sql);
		stmt.close();
		connPool.returnConnection(conn);
	}
}
