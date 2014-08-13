package org.dmfs.ngrams;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;


/**
 * Generator for N-grams from a given String.
 * 
 * @author Marten Gajda <marten@dmfs.org>
 */
public final class NGramGenerator
{
	/**
	 * A {@link Pattern} that matches anything that doesn't belong to a word or number.
	 */
	private final static Pattern SEPARATOR_PATTERN = Pattern.compile("[^\\p{L}\\p{M}\\d]+");

	/**
	 * A {@link Pattern} that matches anything that doesn't belong to a word.
	 */
	private final static Pattern SEPARATOR_PATTERN_NO_NUMBERS = Pattern.compile("[^\\p{L}\\p{M}]+");

	private final int mN;
	private final int mMinWordLen;
	private boolean mAllLowercase = true;
	private boolean mReturnNumbers = true;
	private Locale mLocale = Locale.getDefault();


	public NGramGenerator(int n)
	{
		this(n, 1);
	}


	public NGramGenerator(int n, int minWordLen)
	{
		mN = n;
		mMinWordLen = minWordLen;
	}


	public NGramGenerator setAllLowercase(boolean lowercase)
	{
		mAllLowercase = lowercase;
		return this;
	}


	/**
	 * Sets the {@link Locale} to use when converting the input string to lower case. This has no effect when {@link #setAllLowercase(boolean)} is called with
	 * <code>false</code>.
	 * 
	 * @param locale
	 *            The {@link Locale} to user for the conversion to lower case.
	 * @return This instance.
	 */
	public NGramGenerator setLocale(Locale locale)
	{
		mLocale = locale;
		return this;
	}


	/**
	 * Get all N-grams contained in the given String.
	 * 
	 * @param data
	 *            The String to analyze.
	 * @return A {@link Set} containing all N-grams.
	 */
	public Set<String> getNgrams(String data)
	{
		if (mAllLowercase)
		{
			data = data.toLowerCase(mLocale);
		}

		String[] words = mReturnNumbers ? SEPARATOR_PATTERN.split(data) : SEPARATOR_PATTERN_NO_NUMBERS.split(data);

		Set<String> result = new HashSet<String>(128);

		for (String word : words)
		{
			getNgrams(word, result);
		}

		return result;
	}


	public void getNgrams(String word, Set<String> ngrams)
	{
		final int len = word.length();
		final int minWordLen = mMinWordLen;

		if (len < minWordLen)
		{
			return;
		}

		final int n = mN;
		final int last = Math.max(1, len - n + 1);

		for (int i = 0; i < last; ++i)
		{
			ngrams.add(word.substring(i, Math.min(i + n, len)));
		}
	}
}
