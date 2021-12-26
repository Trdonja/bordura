package http;

public class HttpException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6869552171456940628L;
	
	private int status; // return status code, appropriate to be given as a response to the requestor
	
	public HttpException(int status) {
		super();
		this.status = status;
	}
	
	public HttpException(int status, String msg) {
		super(msg);
		this.status = status;
	}
	
	public int returnStatusCode() {
		return status;
	}

}
