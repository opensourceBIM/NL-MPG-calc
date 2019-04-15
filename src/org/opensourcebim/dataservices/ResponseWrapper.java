package org.opensourcebim.dataservices;

import org.apache.http.StatusLine;

/**
 * wrapper class to return the repsonse status together with a deserialized object
 * instead of the json response normally available.
 *
 * @param <T> any deserialized object
 */
public class ResponseWrapper<T> {

	private T object;
	private StatusLine status;
	
	public ResponseWrapper(T wrappedObject, StatusLine status) {
		this.setObject(wrappedObject);
		this.setStatus(status);
	}
	
	public T getObject() {
		return object;
	}
	private void setObject(T object) {
		this.object = object;
	}
	public StatusLine getStatus() {
		return status;
	}
	private  void setStatus(StatusLine status) {
		this.status = status;
	}
	
	public boolean succes() {
		return this.status.getStatusCode() == 200;
	}
}
