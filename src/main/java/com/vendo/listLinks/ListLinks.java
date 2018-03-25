//ListLinks.java

//Example program to list links from a URL
//original from: http://jsoup.org/cookbook/extracting-data/example-list-links

package com.vendo.listLinks;

import org.jsoup.Jsoup;
import org.jsoup.helper.Validate;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;


public class ListLinks
{
	///////////////////////////////////////////////////////////////////////////
	public static void main (String[] args) throws IOException
	{
		Validate.isTrue (args.length == 1, "usage: supply url to fetch");
		String url = args[0];
		print ("Fetching %s...", url);

		Document doc = Jsoup.connect (url).get ();
		Elements media = doc.select ("[src]");
		Elements imports = doc.select ("link[href]");
		Elements links = doc.select ("a[href]");

		Collections.sort (media, new ElementComparator ("abs:src"));
		Collections.sort (imports, new ElementComparator ("abs:href"));
		Collections.sort (links, new ElementComparator ("abs:href"));

		print ("\nMedia: (%d)", media.size ());
		for (Element src : media) {
			if (src.tagName ().compareToIgnoreCase ("img") == 0) {
				print (" %s: <%s> %sx%s (%s)",
									src.tagName (), src.attr ("abs:src"), src.attr ("width"), src.attr ("height"), trim (src.attr ("alt"), 20));
			} else {
				print (" %s: <%s>", src.tagName (), src.attr ("abs:src"));
			}
		}

		print ("\nImports: (%d)", imports.size ());
		for (Element link : imports) {
			print (" %s: <%s> (%s)", link.tagName (), link.attr ("abs:href"), link.attr ("rel"));
		}

		print ("\nLinks: (%d)", links.size ());
		for (Element link : links) {
			print (" %s: <%s> (%s)", link.tagName (), link.attr ("abs:href"), trim (link.text (), 35));
		}
	}

	///////////////////////////////////////////////////////////////////////////
	private static class ElementComparator implements Comparator<Element>
	{
		public ElementComparator (String compareKey)
		{
			_compareKey = compareKey;
		}

		public int compare (Element element1, Element element2)
		{
			//sort on tagName, then _compareKey
			int diff = element1.tagName ().compareToIgnoreCase (element2.tagName ());

			if (diff != 0) {
				return diff;
			} else {
				return element1.attr (_compareKey).compareToIgnoreCase (element2.attr (_compareKey));
			}
		}

		private String _compareKey;
	}

	///////////////////////////////////////////////////////////////////////////
	private static void print (String msg, Object... args)
	{
		System.out.println (String.format (msg, args));
	}

	///////////////////////////////////////////////////////////////////////////
	private static String trim (String string, int width)
	{
		if (string.length () > width) {
			string = string.substring (0, width - 1) + "*";
		}

		return string;
	}
}
