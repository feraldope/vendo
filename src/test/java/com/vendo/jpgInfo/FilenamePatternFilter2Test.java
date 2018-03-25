//This version uses: @RunWith(value = JUnitParamsRunner.class)

package com.vendo.jpgInfo;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;

@RunWith(value = JUnitParamsRunner.class)
public class FilenamePatternFilter2Test extends FilenamePatternFilter {

	private static final Object[] getInvalidPatterns() {
		return new Object[] {
				"[[",
				"[",
				"[*" };
	}

	private static final Object[] getValidPatterns() {
		return new Object[] {
				new Object[] { "Red*", "red.*" },
				new Object[] { "Green+", "green[\\d]" },
				new Object[] { "Blue+*", "blue[\\d].*" }
		};
	}

	private static final Object[] getAcceptData() {
		return new Object[] {
				new Object[] { "Red*", "redTime", true },
				new Object[] { "Green+", "green6", true },
				new Object[] { "Blue+*", "blue7Date", true },
				new Object[] { "Red+*", "redWeek", false },
				new Object[] { "Green+", "greenHour", false },
				new Object[] { "Blue+*", "bluer7", false }
		};
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void testDefaultCtor() {
		FilenamePatternFilter filter = new FilenamePatternFilter();
		assertEquals(".*", filter._filenamePattern.pattern().toString());
	}

	@Test(expected = RuntimeException.class)
	@Parameters(method = "getInvalidPatterns")
	public void testInvalidPatterns(String pattern) {
		/* FilenamePatternFilter filter = */ new FilenamePatternFilter(pattern);
	}

	@Test
	@Parameters(method = "getValidPatterns")
	public void testValidPatterns(String pattern, String result) {
		FilenamePatternFilter filter = new FilenamePatternFilter(pattern);
		assertEquals(result, filter._filenamePattern.pattern().toString());
	}

	@Test
	@Parameters(method = "getAcceptData")
	public void testAccept(String pattern, String filename, boolean result) throws IOException {
		FilenamePatternFilter filter = new FilenamePatternFilter(pattern);
		Path path = FileSystems.getDefault().getPath(filename);
		assertEquals(result, filter.accept(path));
	}
}
