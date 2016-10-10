package com.linda.koala.sdk;

/**
 * koala返回值
 * @author lindezhi
 * 2016年5月28日 上午11:31:32
 * @param <T>
 */
public class KoalaResult {

	private int code;

	private String msg;

	private String data;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

}
