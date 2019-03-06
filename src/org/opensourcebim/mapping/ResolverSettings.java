package org.opensourcebim.mapping;

public class ResolverSettings {

	// if the number of results exceeds this ratio times the total amount of products add a warning.
	public static double tooManyOptionsRatio = 0.25;
	
	// if the # of results exceeds this number add a warning
	public static double tooManyOptionsAbsNum = 15;
	
	// determines how often words should occur in the keyWord database to be taken into account
	public static int keyWordOccurenceMininum = 2;
	
	// determines how long a word should be to be included
	public static int minWordLengthForSimilarityCheck = 2;
	
	// value that describes which productCards to include based on their similarity score.
	public static double cutOffSimilarityRatio = 0.1;
	
	public static String splitChars = " |-|,";
	
}
