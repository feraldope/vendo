//MyBatisEmployeeDistribution.java

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

public class MyBatisEmployeeDistribution
{
	///////////////////////////////////////////////////////////////////////////
	public MyBatisEmployeeDistribution (String lastName, int count)
	{
		_lastName = lastName;
		_count = count;
	}

	///////////////////////////////////////////////////////////////////////////
	public MyBatisEmployeeDistribution (MyBatisEmployeeDistribution employee)
	{
		_lastName = employee.getLastName ();
		_count = employee.getCount ();
	}

	///////////////////////////////////////////////////////////////////////////
	public String getLastName ()
	{
		return _lastName;
	}

	///////////////////////////////////////////////////////////////////////////
	public int getCount ()
	{
		return _count;
	}

	///////////////////////////////////////////////////////////////////////////
	@Override
	public String toString ()
	{
		StringBuffer sb = new StringBuffer (getClass ().getName ());
		sb.append (", ").append (getLastName ());
		sb.append (", ").append (getCount ());

		return sb.toString ();
	}


	//members
	private final String _lastName;
	private final int _count;

//	private static Logger _log = LogManager.getLogger ();
}
