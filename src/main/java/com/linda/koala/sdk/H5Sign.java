package com.linda.koala.sdk;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;

/**
 * 
 * @author lindezhi
 *
 */
public class H5Sign implements BaseSign{

	/**
	 * 对key反向排序，然后组装字符转 keyvaluekeyvalue
	 * z->y->x
	 */
	@Override
	public String genSignbefore(TreeMap<String, String> signMap, String appSec) {
		List<String> names = new ArrayList<String>();
		names.addAll(signMap.keySet());
        //降序排序
        Collections.sort(names);
        Collections.reverse(names);
        
        StringBuilder signBuilder = new StringBuilder(appSec);
        for(String key:names){
        	signBuilder.append(key).append(signMap.get(key));
        }
        signBuilder.append(appSec);
		return signBuilder.toString();
	}

	/**
	 * 生成签名
	 * 先base64编码，然后再执行sha1hex
	 */
	@Override
	public String sign(String str) {
		try{
			byte[] tmpSign = Base64.encodeBase64(str.getBytes("utf-8"));
			return DigestUtils.sha1Hex(tmpSign);
		}catch(UnsupportedEncodingException e){
			throw new KoalaException(e);
		}
	}

}
