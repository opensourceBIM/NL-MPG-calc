package org.opensourcebim.ifccollection;

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
	 * @param code string in form i.j.k where i, j and k have to be left padded integer values
	 * @return a flag to indicate whether the above description of the code param holds.
	 */
	public static Boolean isNlsfbCode(String code) {
		if (code == null) {return false;}
		List<String> split = Arrays.asList(code.split("\\."));
		return split.size() > 0 && split.stream().anyMatch(i -> i.matches("-?(0|[1-9]\\d*)"));
	}
	
	public Boolean isSubCategoryOf(NlsfbCode code) {
		Boolean majorMatch = this.majorId == code.getMajorId() ;
		if (this.mediorId == null || !majorMatch) {
			return false;
		}
		return this.mediorId == code.mediorId;
	}

	public String print() {
		return getMajorId().toString() + "." + mediorId.toString() + 
				(minorId == null ? "" : "." + minorId.toString()); 
	}

	public Integer getMajorId() {
		return majorId;
	}
	
	public Integer getMediorId() {
		// TODO Auto-generated method stub
		return mediorId;
	}
	
	public Integer getMinorId() {
		return minorId;
	}
}
