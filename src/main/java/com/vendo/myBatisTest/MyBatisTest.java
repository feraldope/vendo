//MyBatisTest.java

//MyBatis doc: http://www.mybatis.org/mybatis-3/

//uses MySQL (MariaDB) employee sample database

/*
mysql -u root -proot -e "show databases"
mysql -u root -proot employees -e "show tables"
mysql -u root -proot employees -e "describe employees"
+------------+---------------+------+-----+---------+-------+
| Field      | Type          | Null | Key | Default | Extra |
+------------+---------------+------+-----+---------+-------+
| emp_no     | int(11)       | NO   | PRI | NULL    |       |
| birth_date | date          | NO   |     | NULL    |       |
| first_name | varchar(14)   | NO   |     | NULL    |       |
| last_name  | varchar(16)   | NO   |     | NULL    |       |
| gender     | enum('M','F') | NO   |     | NULL    |       |
| hire_date  | date          | NO   |     | NULL    |       |
+------------+---------------+------+-----+---------+-------+
*/

package com.vendo.myBatisTest;

import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class MyBatisTest
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String args[])
	{
		MyBatisTest app = new MyBatisTest ();

		if (!app.processArgs (args))
			System.exit (1); //processArgs displays error

		app.run ();
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

				} else {
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
				}

			} else {
/*
				//check for other args
				if (_model == null) {
					_model = arg;

				} else if (_outputPrefix == null) {
					_outputPrefix = arg;

				} else {
*/
					displayUsage ("Unrecognized argument '" + args[ii] + "'", true);
//				}
			}
		}

/*
		//check for required args and handle defaults
//TODO - verify _destDir exists, and is writable??
		if (_destDir == null)
			_destDir = getCurrentDirectory ();
		_destDir = appendSlash (_destDir);
//		if (_Debug)
//			_log.debug ("_destDir = " + _destDir);
*/

		return true;
	}

	///////////////////////////////////////////////////////////////////////////
	private void displayUsage (String message, Boolean exit)
	{
		String msg = new String ();
		if (message != null)
			msg = message + NL;

		msg += "Usage: " + _AppName + " [/debug] TBD";
		System.err.println ("Error: " + msg + NL);

		if (exit)
			System.exit (1);
	}

	///////////////////////////////////////////////////////////////////////////
	private boolean run ()
	{
		String resource = "com/vendo/myBatisTest/mybatis-test-config.xml";
		
		try (InputStream inputStream = Resources.getResourceAsStream (resource)) {
			_sqlSessionFactory = new SqlSessionFactoryBuilder ().build (inputStream);
			_log.debug ("MyBatisTest.run: loaded mybatis config from " + resource);

		} catch (Exception ee) {
			_log.error ("MyBatisTest.run: SqlSessionFactoryBuilder.build failed on " + resource);
			_log.error (ee);
		}

		try (SqlSession session = _sqlSessionFactory.openSession ()) {
			MyBatisEmployeeMapper employeeMapper = session.getMapper (MyBatisEmployeeMapper.class);

			//select

			System.out.println (NL + "**** testing selectAllEmployees");
			List<MyBatisEmployee> employees = employeeMapper.selectAllEmployees ();
			System.out.println ("employees.size() = " + employees.size () + " (" + employees.getClass ().getName () + ")");
			for (int ii = 0; ii < Math.min (10, employees.size ()); ii++) {
				System.out.println (employees.get (ii));
			}

			System.out.println (NL + "**** testing selectEmployeesByIds");
			List<Integer> employeeNumbers = Arrays.asList (10101, 10102, 10103, 10104, 10105);
			employees = employeeMapper.selectEmployeesByIds (employeeNumbers);
			System.out.println ("employees.size() = " + employees.size () + " (" + employees.getClass ().getName () + ")");
			for (int ii = 0; ii < employees.size (); ii++) {
				System.out.println (employees.get (ii));
			}
			MyBatisEmployee employee = employees.get (0);

			System.out.println (NL + "**** testing selectEmployeesByFirstLastName (" + employee.getFirstName () + " " + employee.getLastName ()+ ")");
			employees = employeeMapper.selectEmployeesByFirstLastName (employee.getFirstName (), employee.getLastName ());
			System.out.println ("employees.size() = " + employees.size () + " (" + employees.getClass ().getName () + ")");
			for (int ii = 0; ii < employees.size (); ii++) {
				System.out.println (employees.get (ii));
			}

			System.out.println (NL + "**** testing selectEmployeeMinBirthDate");
			employees = employeeMapper.selectEmployeeMinBirthDate ();
			System.out.println ("employees.size() = " + employees.size () + " (" + employees.getClass ().getName () + ")");
			for (int ii = 0; ii < employees.size (); ii++) {
				System.out.println (employees.get (ii));
			}

			System.out.println (NL + "**** testing selectEmployeeDistributionByLastName");
			List<MyBatisEmployeeDistribution> employeeDists = employeeMapper.selectEmployeeDistributionByLastName ("M");
			System.out.println ("employeeDists.size() = " + employeeDists.size () + " (" + employeeDists.getClass ().getName () + ")");
			for (int ii = 0; ii < Math.min (10, employeeDists.size ()); ii++) {
				System.out.println (employeeDists.get (ii));
			}

			System.out.println (NL + "**** testing getNextEmployeeNumber");
			int nextEmployeeNumber = employeeMapper.getNextEmployeeNumber ();
			System.out.println ("nextEmployeeNumber = " + nextEmployeeNumber);

			//insert

			System.out.println (NL + "**** testing insertEmployee");
			MyBatisEmployee employee1 = employees.get (0);
			MyBatisEmployee employee2 = new MyBatisEmployee (nextEmployeeNumber, //employee number
															"Employee", //first name
															"Oldest", //last name
															employee1.getGender (),
															employee1.getBirthDate (),
															employee1.getHireDate ());
			System.out.println ("inserting: " + employee2);
			int rowsAffected = employeeMapper.insertEmployee (employee2);
			session.commit ();
			System.out.println ("rowsAffected = " + rowsAffected);

			System.out.println (NL + "**** testing selectEmployeeById(" + nextEmployeeNumber + ")");
			employee = employeeMapper.selectEmployeeById (nextEmployeeNumber);
			System.out.println (employee);

			System.out.println (NL + "**** testing deleteEmployeeById(" + nextEmployeeNumber + ")");
			rowsAffected = employeeMapper.deleteEmployeeById (nextEmployeeNumber);
			session.commit ();
			System.out.println ("rowsAffected = " + rowsAffected);

			System.out.println (NL + "**** testing selectEmployeeById(" + nextEmployeeNumber + ")");
			employee = employeeMapper.selectEmployeeById (nextEmployeeNumber);
			System.out.println (employee);

			//insert/update

			System.out.println (NL + "**** testing insertUpdateEmployee");
			MyBatisEmployee employee3 = new MyBatisEmployee (nextEmployeeNumber, //employee number
															"Employee", //first name
															"Oldest", //last name
															employee1.getGender (),
															employee1.getBirthDate (),
															new Date ()); //hire date
			System.out.println ("inserting: " + employee3);
			rowsAffected = employeeMapper.insertUpdateEmployee (employee3);
			session.commit ();
			System.out.println ("rowsAffected = " + rowsAffected);

			System.out.println (NL + "**** testing selectEmployeeById(" + employee3.getEmployeeNumber () + ")");
			employee = employeeMapper.selectEmployeeById (employee3.getEmployeeNumber () );
			System.out.println (employee);

		} catch (Exception ee) {
			_log.error ("MyBatisTest.run: ", ee);
		}

		return true;
	}

//unused
//	///////////////////////////////////////////////////////////////////////////
//	public static Date addDays (Date date, int days)
//	{
//		Calendar cal = Calendar.getInstance ();
//		cal.setTime (date);
//		cal.add (Calendar.DATE, days); //minus number would decrement the days
//		return cal.getTime ();
//	}

//unused
//	///////////////////////////////////////////////////////////////////////////
//	private static String getCurrentDirectory ()
//	{
//		Path file = FileSystems.getDefault ().getPath ("");
//		String dir = file.toAbsolutePath ().toString ();
//
//		return dir;
//	}


	//private members
	private SqlSessionFactory _sqlSessionFactory = null;

	private static Logger _log = LogManager.getLogger ();

	//global members
	public static boolean _Debug = false;

	public static final String _AppName = "MyBatisTest";
	public static final String NL = System.getProperty ("line.separator");
}
