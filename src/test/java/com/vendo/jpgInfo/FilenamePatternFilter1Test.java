//This version uses: @RunWith(value = Parameterized.class)

package com.vendo.jpgInfo;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(value = Parameterized.class)
public class FilenamePatternFilter1Test extends FilenamePatternFilter {

	@Parameter
	public String pattern;

	@Parameters(name = "{index}: pattern - {0}")
	public static Object[] data() {
		return new Object[] {
				"[[",
				"[",
				"[*"
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
	public void testInvalidPatterns() {
		/* FilenamePatternFilter filter = */ new FilenamePatternFilter(pattern);
	}
}
