//JdbcTest.java - goes against MySQL Sakila test database

//Example usage:
// TBD

package com.vendo.jdbcTest;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;

//import com.mysql.jdbc.exceptions.*;


public class JdbcTest
{
	private enum Database {NotSet, MySQL, Postgres}

	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		JdbcTest jdbcTest = new JdbcTest ();

		if (!jdbcTest.processArgs (args)) {
			System.exit (1); //processArgs displays error
		}

		jdbcTest.run ();
	}

	///////////////////////////////////////////////////////////////////////////
	private Boolean processArgs (String args[])
	{
		for (int ii = 0; ii < args.length; ii++) {
			String arg = args[ii];

			//check for switches
			if (arg.startsWith ("-") || arg.startsWith ("/")) {
				arg = arg.substring (1, arg.length ());

				if (arg.equalsIgnoreCase ("debug") || arg.equalsIgnoreCase ("dbg")) {
					_Debug = true;

				} else if (arg.equalsIgnoreCase ("database") || arg.equalsIgnoreCase ("db")) {
					try {
						_dbName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("host") || arg.equalsIgnoreCase ("h")) {
					try {
						_hostName = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("user") || arg.equalsIgnoreCase ("u")) {
					try {
						_dbUser = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("pass") || arg.equalsIgnoreCase ("p")) {
					try {
						_dbPass = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else if (arg.equalsIgnoreCase ("sql") || arg.equalsIgnoreCase ("s")) {
					try {
						_sql = args[++ii];
					} catch (ArrayIndexOutOfBoundsException exception) {
						displayUsage ("Missing value for /" + arg, true);
					}

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_filename == null)
					_filename = arg;

				else
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
			}
		}

		if (false) {
			//mysql - handle defaults
			_database = Database.MySQL;

			if (_hostName == null) {
				_hostName = "localhost";
			}

			if (_dbName == null) {
				_dbName = "sakila";
			}

			if (_dbUser == null) {
				_dbUser = "root";
			}

			if (_dbPass == null) {
				_dbPass = "root";
			}

			if (_sql == null) {
				_sql = "select f.title, r.rental_date, r.return_date, timestampdiff (day, r.rental_date, r.return_date) as days" +
					   " from film f, rental r, inventory i" +
					   " where f.film_id = i.film_id and i.inventory_id = r.inventory_id and f.title = 'AFRICAN EGG'" +
					   " and r.rental_date < ? " +
					   " order by r.rental_date";
			}

			_jdbcDriver = "com.mysql.cj.jdbc.Driver";
			_dbUrl = "jdbc:mysql://localhost/" + _dbName;

		} else {
			//postgres - handle defaults
			_database = Database.Postgres;

			//handle defaults
			if (_hostName == null) {
				_hostName = "ricda13wd01";
			}

			if (_dbName == null) {
				_dbName = "capman";
			}

			if (_dbUser == null) {
				_dbUser = "postgres";
			}

			if (_dbPass == null) {
				_dbPass = "password";
			}

			if (_sql == null) {
				_sql = "select 1";
			}

			_jdbcDriver = "org.postgresql.Driver";
			_dbUrl = "jdbc:postgresql://" + _hostName + ":5432/" + _dbName;
		}

		System.out.println ("hostname: " + _hostName);
		System.out.println ("database: " + _database);
		System.out.println ("dbName: " + _dbName);
		System.out.println ("dbUser: " + _dbUser);
		System.out.println ("dbPass: " + _dbPass);

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null) {
			msg = message + NL;
		}

		msg += "Usage: " + _AppName + " [/debug] [/database <database name>] [/user <username>] [/pass <password] [/sql <sql statement>]";
		System.err.println ("Error: " + msg + NL);

		if (exit) {
			System.exit (1);
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private void run ()
	{
		System.out.println ("Using defaults for: " + _database);

		Connection conn = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {
			Class.forName (_jdbcDriver);

			if (_trace) {
				System.err.println ("calling: DriverManager.getConnection (" + _dbUrl + ")");
			}
			conn = DriverManager.getConnection (_dbUrl, _dbUser, _dbPass);

			if (_trace) {
				System.err.println ("calling: Connection.prepareStatement");
			}
			ps = conn.prepareStatement (_sql);

			if (_database == Database.MySQL) {
				Timestamp now = new Timestamp (new GregorianCalendar ().getTimeInMillis ());
				ps.setTimestamp (1, now);

				if (_trace) {
					System.err.println ("calling: PreparedStatement.executeQuery");
				}
				rs = ps.executeQuery ();

				if (_trace) {
					System.err.println ("calling: ResultSet.next in loop");
				}

				while (rs.next ()) {
					String title = rs.getString ("title");
					int days = rs.getInt ("days");

					//can read database type TIMESTAMP into either Java Date or Java Timestamp
					Date rentalDate = new Date (rs.getTimestamp ("rental_date").getTime ());
					Timestamp returnDate = rs.getTimestamp ("return_date");

					System.out.println ("title: " + title + ", rental: " + _dateFormat.format (rentalDate) + ", return: " + returnDate + ", days: " + days);
				}

			} else { //_database == Database.Postgres
				if (_trace) {
					System.err.println ("calling: PreparedStatement.executeQuery");
				}
				rs = ps.executeQuery ();

				if (_trace) {
					System.err.println ("calling: ResultSet.next in loop");
				}
			}

			if (_database == Database.MySQL) {
				while (rs.next ()) {
					String title = rs.getString ("title");
					int days = rs.getInt ("days");

					//can read database type TIMESTAMP into either Java Date or Java Timestamp
					Date rentalDate = new Date (rs.getTimestamp ("rental_date").getTime ());
					Timestamp returnDate = rs.getTimestamp ("return_date");

					System.out.println ("title: " + title + ", rental: " + _dateFormat.format (rentalDate) + ", return: " + returnDate + ", days: " + days);
				}

			} else { //_database == Database.Postgres
				while (rs.next ()) {
					int value = rs.getInt (1);

					System.out.println ("value: " + value);
				}
			}

		} catch (SQLException ee) {
			int errorCode = ee.getErrorCode ();
			String sqlState = ee.getSQLState ();
			String message = ee.getMessage ();
			System.err.println ("Exception: errorCode=" + errorCode + ", sqlState='" + sqlState + "', message='" + message + "'");

/* example output
[from bad database name]
Exception: errorCode=1049, sqlState='42000', message='Unknown database 'bad''

[from bad username or password]
Exception: errorCode=1045, sqlState='28000', message='Access denied for user 'root'@'localhost' (using password: YES)'

[from bad SQL syntax]
Exception: errorCode=1064, sqlState='42000', message='You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near 'a' at line 1'
*/

			if (errorCode == 1064 && message.contains ("SQL syntax")) {
				System.err.println ("Error: " + message);
				System.err.println ("sql='" + _sql + "'");
			}

			ee.printStackTrace ();

		} catch (Exception ee) {
			ee.printStackTrace ();

		} finally {
			try {
				if (rs != null) {
					rs.close ();
				}
			} catch (SQLException ee) {} //catch and ignore exception

			try {
				if (ps != null) {
					ps.close ();
				}
			} catch (SQLException ee) {} //catch and ignore exception

			try {
				if (conn != null) {
					conn.close ();
				}
			} catch (SQLException ee) {} //catch and ignore exception
		}
	}


	//private members
	private Database _database = Database.NotSet;
	private String _hostName = null;
	private String _dbName = null;
	private String _dbUser = null;
	private String _dbPass = null;
	private String _dbUrl = null;
	private String _jdbcDriver = null;
	private String _sql = null;

	//global members
	public static boolean _Debug = false;
	public static boolean _trace = true;

	private final SimpleDateFormat _dateFormat = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss"); //match default format used by mysql.exe

	public static final String _AppName = "JdbcTest";
	public static final String NL = System.getProperty ("line.separator");
}
