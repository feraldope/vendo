//MyBatisEmployeeMapper.java

package com.vendo.myBatisTest;

import java.util.List;

import org.apache.ibatis.annotations.Param;


public interface MyBatisEmployeeMapper {

	public List<MyBatisEmployee> selectAllEmployees ();

	public List<MyBatisEmployee> selectEmployeesByFirstLastName (@Param("first_name") String firstName, @Param("last_name") String lastName);

	public MyBatisEmployee selectEmployeeById (@Param("emp_no") int id);

	public List<MyBatisEmployee> selectEmployeesByIds (@Param("emp_nos") List<Integer> ids);

	public List<MyBatisEmployee> selectEmployeeMinBirthDate ();

	public List<MyBatisEmployeeDistribution> selectEmployeeDistributionByLastName (@Param("gender") String gender);

	public int getNextEmployeeNumber ();

	public int insertEmployee (MyBatisEmployee employee);

	public int insertUpdateEmployee (MyBatisEmployee employee);

	public int deleteEmployeeById (@Param("emp_no") int id);
}
