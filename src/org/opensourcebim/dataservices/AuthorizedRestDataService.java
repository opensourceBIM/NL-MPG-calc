package org.opensourcebim.dataservices;

import java.util.List;

import org.apache.http.client.methods.HttpGet;
import org.bimserver.shared.reflector.KeyValuePair;

/**
 * Class to handle authorization to a rest API for the NMD connectionn
 * @author vijj
 * ToDo: there are currently a lot of methods in here that should be in a lower level class. move these.
 */
public class AuthorizedRestDataService extends RestDataService  {

	private String token;	
		
	@Override
	protected HttpGet createHttpGetRequest(String path, List<KeyValuePair> params) {
		HttpGet request = new HttpGet(createUri(path, params));
		request.addHeader("Access_Token", this.token);
		return request;
	}
	
	public void setToken(String token) {
		this.token = token;
	}
}
