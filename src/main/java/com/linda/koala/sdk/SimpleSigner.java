package com.linda.koala.sdk;

import java.util.TreeMap;

/**
 * 默认不签名，不加密
 * @author lindezhi
 * 2016年10月10日 下午5:32:03
 */
public class SimpleSigner implements BaseSign{

	@Override
	public String genSignbefore(TreeMap<String, String> signMap, String sec) {
		return "test-unique no sign";
	}

	@Override
	public String sign(String str) {
		return "koala";
	}

}
