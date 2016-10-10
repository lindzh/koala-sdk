package com.linda.koala.sdk;

/**
 * http请求异常
 * @author lindezhi
 * 2016年5月28日 上午10:22:01
 */
public class HttpException extends RuntimeException{

	private static final long serialVersionUID = -7986057373963720777L;

	public HttpException() {
		super();
	}

	public HttpException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public HttpException(String message, Throwable cause) {
		super(message, cause);
	}

	public HttpException(String message) {
		super(message);
	}

	public HttpException(Throwable cause) {
		super(cause);
	}
	
}