package org.opensourcebim.bcfexport;

public class BcfExportSettings {

	private String author;
	private static BcfExportSettings settings = null;
	
    private BcfExportSettings() {}
    
    public static BcfExportSettings getInstance() {
        if (settings == null) {
            synchronized (BcfExportSettings.class) {
                if (settings == null) {
                	settings = new BcfExportSettings();
                }
            }
        }
        return settings;
    }

	public String getAuthor() {
		return author;
	}
	
	public void setAuthor(String author) {
		this.author = author;
	}
}
