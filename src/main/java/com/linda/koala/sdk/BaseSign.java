package com.linda.koala.sdk;

import java.util.TreeMap;

/**
 * 
 * @author lindezhi
 * 签名
 *
 */
public interface BaseSign {
	
	public String genSignbefore(TreeMap<String, String> signMap,String sec);
	
	public String sign(String str);

}
