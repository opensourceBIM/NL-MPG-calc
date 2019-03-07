package org.opensourcebim.mapping;

public class ResolverSettings {

	// if the number of results exceeds this ratio times the total amount of products add a warning.
	public static double tooManyOptionsRatio = 0.25;
	
	// if the # of results exceeds this number add a warning
	public static double tooManyOptionsAbsNum = 15;
	
	// determines how often words should occur in the keyWord database to be taken into account
	public static int keyWordOccurenceMininum = 4;
	
	// determines how long a word should be to be included
	public static int minWordLengthForSimilarityCheck = 3;
	
	// value that describes which productCards to include based on their similarity score.
	public static double cutOffSimilarityRatio = 0.1;
	
	// regex statement to determine which characters need to be split on. 
	public static String splitChars = " |-|,|:|;|_";

	// penalisation factor to reduce overuse of keywords in description.
	public static double descriptionLengthPenaltyCoefficient = 0.3;

	// regex pattern to replace non literal characters
	public static String numericReplacePattern = "[^a-zA-Z]";
	
}
