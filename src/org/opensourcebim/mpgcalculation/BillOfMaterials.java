package org.opensourcebim.mpgcalculation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class BillOfMaterials {
	private List<String> annotations;
	private List<Map<String, Object>> bomEntries;
	
	public BillOfMaterials() {
		this.annotations = new ArrayList<>();
		this.bomEntries = new ArrayList<>();
	}

	public List<String> getAnnotations() {
		return this.annotations;
	}
	
	public void addAnnotation(String annotation) {
		this.annotations.add(annotation);
	}
	
	public List<Map<String, Object>> getEntries() {
		return this.bomEntries;
	}
	
	public void addEntry(Map<String, Object> entry) {
		this.bomEntries.add(entry);
	}
	
	public String printToCsv(String delimiter) {
		StringBuilder sb = new StringBuilder();
		annotations.forEach(a -> {
			sb.append("# ");
			sb.append(a);
			sb.append(System.lineSeparator());
		});
		
		List<String> headers = bomEntries.stream()
				.flatMap(m -> m.keySet().stream())
				.distinct().collect(Collectors.toList());
		sb.append(String.join(delimiter, headers));
		sb.append(System.lineSeparator());
		
		bomEntries.stream().forEach(m -> {
			List<String> entries = headers.stream()
					.map(h -> m.keySet().contains(h) ? m.get(h).toString() : "")
					.collect(Collectors.toList());
			sb.append(String.join(delimiter, entries));
			sb.append(System.lineSeparator());
		});

		return sb.toString();
	}
}
