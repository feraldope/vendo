//MyBatisEmployee.java

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

import java.text.SimpleDateFormat;
import java.util.Date;


public class MyBatisEmployee
{
	///////////////////////////////////////////////////////////////////////////
	public MyBatisEmployee (int employeeNumber, String firstName, String lastName, String gender, Date birthDate, Date hireDate)
	{
		_employeeNumber = employeeNumber;
		_firstName = firstName;
		_lastName = lastName;
		_gender = gender;
		_birthDate = birthDate;
		_hireDate = hireDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public MyBatisEmployee (MyBatisEmployee employee)
	{
		_employeeNumber = employee.getEmployeeNumber ();
		_firstName = employee.getFirstName ();
		_lastName = employee.getLastName ();
		_gender = employee.getGender ();
		_birthDate = employee.getBirthDate ();
		_hireDate = employee.getHireDate ();
	}

	///////////////////////////////////////////////////////////////////////////
	public int getEmployeeNumber ()
	{
		return _employeeNumber;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getFirstName ()
	{
		return _firstName;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getLastName ()
	{
		return _lastName;
	}

	///////////////////////////////////////////////////////////////////////////
	public String getGender ()
	{
		return _gender;
	}

	///////////////////////////////////////////////////////////////////////////
	public Date getBirthDate ()
	{
		return _birthDate;
	}

	///////////////////////////////////////////////////////////////////////////
	public Date getHireDate ()
	{
		return _hireDate;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuffer sb = new StringBuffer (getClass ().getName ());
		sb.append (": ").append (getEmployeeNumber ());
		sb.append (", ").append (getLastName ());
		sb.append (", ").append (getFirstName ());
		sb.append (", ").append (getGender ());
		sb.append (", ").append (_dateFormat.format (getBirthDate ()));
		sb.append (", ").append (_dateFormat.format (getHireDate ()));

		return sb.toString ();
	}


	//members
	private final int _employeeNumber;
	private final String _firstName;
	private final String _lastName;
	private final String _gender;
	private final Date _birthDate;
	private final Date _hireDate;

    private static final SimpleDateFormat _dateFormat = new SimpleDateFormat ("MM/dd/yy");

//	private static Logger _log = LogManager.getLogger ();
}
