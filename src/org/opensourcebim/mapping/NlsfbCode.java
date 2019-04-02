package org.opensourcebim.mapping;

import java.util.Arrays;
import java.util.List;

public class NlsfbCode {

	private Integer majorId;
	Integer mediorId;
	Integer minorId;

	public NlsfbCode(String code) {

		if (!isNlsfbCode(code)) {
			System.err.println("Invalid NlsfbCode");
			majorId = -1;
			mediorId = -1;
			minorId = -1;
		} else {
			String[] split = code.split("\\.");
			majorId = Integer.parseInt(split[0].trim());

			if (split.length > 1) {
				mediorId = Integer.parseInt(split[1].trim());
			}

			if (split.length > 2) {
				minorId = Integer.parseInt(split[2].trim());
			}
		}
	}

	/**
	 * Check whether the string can be parsed to a NLsfb code
	 * 
	 * @param code string in form i.j.k where i, j and k have to be left padded
	 *             integer values
	 * @return a flag to indicate whether the above description of the code param
	 *         holds.
	 */
	public static Boolean isNlsfbCode(String code) {
		if (code == null) {
			return false;
		}
		List<String> split = Arrays.asList(code.split("\\."));
		return split.size() > 0 && split.stream().anyMatch(i -> i.matches("-?(0|[1-9]\\d*)"));
	}

	/**
	 * Check that the input code is an overlapping category of this code.
	 * 
	 * example 1: this = 22.1 code = 22. => true. example 2: this = 22.2 code = 22.1
	 * => false. example 3: this = 22.2 code = 22.2 => true.
	 * 
	 * @param code the potential parent category
	 * @return flag to indicate that this NLSfb object is a (sub) category of the
	 *         input Nlsfb object
	 */
	public Boolean isSubCategoryOf(NlsfbCode code) {
		return this.majorId == code.getMajorId() && compareSubCodes(code.getMediorId(), this.getMediorId());
	}

	/**
	 * Should return true when an integer B is a derived (sub)code of integer A
	 * example: (1,11) => true example (1,21) => false example (1,1) => true
	 * 
	 * @param codeA 'parent' code
	 * @param codeB possible 'child' code
	 * @return flag to indicate b is a child of a
	 */
	private Boolean compareSubCodes(Integer codeA, Integer codeB) {
		if (codeA == null && codeB != null) {
			return true;
		}

		int[] digitsA = String.valueOf(codeA).chars().toArray();
		int[] digitsB = String.valueOf(codeB).chars().toArray();

		if (digitsA.length > digitsB.length) {
			return false;
		}

		for (int i = 0; i < digitsA.length; i++) {
			if (digitsA[i] != digitsB[i]) {
				return false;
			}
		}

		return true;
	}

	public String print() {
		String res = "";
		if (getMajorId() != null) {
			res += getMajorId().toString();
			if (getMediorId() != null && getMediorId() > 0) {
				res += "." + mediorId.toString();
				if (getMinorId() != null && getMinorId() > 0) {
					res += "." + getMinorId().toString();
				}
			}
		}
		return res;
	}

	public Integer getMajorId() {
		return majorId;
	}

	public Integer getMediorId() {
		return mediorId;
	}

	public Integer getMinorId() {
		return minorId;
	}
}
