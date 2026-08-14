package com.vendo.albumServlet;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class AlbumFileFilterTest {

    ///////////////////////////////////////////////////////////////////////////
    @Test
    public void testCtor() {
        AlbumFileFilter filter = new AlbumFileFilter(new String[]{"a"}, new String[]{""},false, 0L);
        assertEquals("^a*$", filter.toString());

        filter = new AlbumFileFilter(new String[]{"[a-f]"}, new String[]{""},false, 0L);
        assertEquals("^a*$,^b*$,^c*$,^d*$,^e*$,^f*$", filter.toString());

        filter = new AlbumFileFilter(new String[]{"[a-f]a"}, new String[]{""},false, 0L);
        assertEquals("^aa*$,^ba*$,^ca*$,^da*$,^ea*$,^fa*$", filter.toString());

        try {
            filter = new AlbumFileFilter(new String[]{"[a-F]"}, new String[]{""}, true, 0L); //when useCase = true, lowercase "a" is greater than uppercase "F" so range is undefined
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException ex) {
            assertEquals("end is less that start for range <[a-F]>", ex.getMessage());
        }

        filter = new AlbumFileFilter(new String[]{"[a-F]"}, new String[]{""}, false, 0L); //NOTE useCase = false
        assertEquals("^a*$,^b*$,^c*$,^d*$,^e*$,^f*$", filter.toString());
    }
}
