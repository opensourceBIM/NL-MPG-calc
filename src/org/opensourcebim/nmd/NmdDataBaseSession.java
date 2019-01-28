package org.opensourcebim.nmd;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.opensourcebim.ifccollection.MpgElement;
import org.opensourcebim.nmd.NmdDatabaseConfig;

/**
 * Implementation of the NmdDataService to retrieve data from the NMD see :
 * https://www.milieudatabase.nl/ for mor information
 * 
 * @author vijj
 *
 */
public class NmdDataBaseSession implements NmdDataService {

	private static final DateFormat dbDateFormat = new SimpleDateFormat("yyyyMMdd"); // according to docs thi should be
																						// correct
	private Date requestDate = new Date();
	private NmdDatabaseConfig config = null;
	private boolean isConnected = false;
	private String token;

	public NmdDataBaseSession(NmdDatabaseConfig config) {
		this.config = config;
		this.login();
	}
	
	public void setToken(String token) {
		this.token = token;
	}
	
	@Override
	public Date getRequestDate() {
		return this.requestDate;
	}
	
	@Override
	public void setRequestDate(Date newDate) {
		this.requestDate = newDate;
	}

	@Override
	public Boolean getIsConnected() {
		// TODO Auto-generated method stub
		return this.isConnected;
	}

	@Override
	public void login() {
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(
				"https://www.milieudatabase-datainvoer.nl/NMD_30_AuthenticationServer/NMD_30_API_Authentication/getToken");

		httppost.setHeader("refreshToken", config.getToken());
		httppost.setHeader("pAPI_ID", "1");
		httppost.setHeader("Content-Type", "application/x-www-form-urlencoded");

		// Execute and get the response.
		HttpResponse response;
		try {
			response = httpclient.execute(httppost);
			HttpEntity entity = response.getEntity();

			// get the credentials from the response
			if (entity != null) {
				InputStream instream = entity.getContent();
				// the properties from the received input stream.
				Properties props = new Properties();
				props.load(instream);
				
				this.isConnected = true;
			}

		} catch (IOException e1) {
			this.isConnected = false;
			System.out.println("authentication failed");
		}

	}

	@Override
	public void logout() {
		this.setToken("");
		this.isConnected = false;
	}

	@Override
	public List<NmdProductCard> getAllProductSets() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public NmdProductCard retrieveMaterial(MpgElement material) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProfileSet> getSpecsForProducts(List<NmdProductCard> products) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdFaseProfiel> getFaseProfilesByIds(List<Integer> ids) {
		// TODO Auto-generated method stub
		return null;
	}
}
