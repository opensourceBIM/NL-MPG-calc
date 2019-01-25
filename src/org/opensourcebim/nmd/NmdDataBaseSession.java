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
import org.opensourcebim.ifccollection.MpgMaterial;

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
	private boolean isConnected = false;
	private String token;

	public NmdDataBaseSession(String authToken) {
		this.setToken(authToken);
		this.login();
	}
	
	private String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	@Override
	public void login() {
		HttpClient httpclient = HttpClients.createDefault();
		HttpPost httppost = new HttpPost(
				"https://www.milieudatabase-datainvoer.nl/NMD_30_AuthenticationServer/NMD_30_API_Authentication/getToken");

		httppost.setHeader("refreshToken", this.getToken());
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
			System.out.println("no response received.");
		}

	}

	@Override
	public void logout() {
		this.setToken("");
	}

	@Override
	public NmdProductCard retrieveMaterial(MpgMaterial material) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdProductCard> getAllProductSets() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<MaterialSpecification> getSpecsForProducts(List<NmdProductCard> products) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<NmdBasisProfiel> getPhaseProfiles(List<Integer> ids) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setRequestDate(Date newDate) {
		this.requestDate = newDate;
	}
}
