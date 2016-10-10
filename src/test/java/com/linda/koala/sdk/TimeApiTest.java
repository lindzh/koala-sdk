package com.linda.koala.sdk;

import java.io.File;
import java.util.HashMap;

/**
 * 测试用例
 * @author lindezhi
 * 2016年5月29日 下午1:51:57
 */
public class TimeApiTest {
	
	public static void main(String[] args) {
		
		KoalaClient client = new KoalaClient();
		client.setAppKey("832f0a0abf384a1b905f39610733cb21");
		client.setAppSec("19c043d1dc3b41fb8d89a5dd8594f4e0");
		client.setGatewayUrl("http://test.gateway.aidaojia.com/gateway");
		client.init();
		
		KoalaApi api = new KoalaApi();
		//api名称
		api.setApi("lin.test");
		//api版本
		api.setApiVersion("1.0.0");
		//该api业务参数为空无需填写
		KoalaResult result = client.sendRequest(api);
		
		System.out.println(JSONUtils.toJSON(result));
		
		KoalaApi qiniuApi = new KoalaApi();
		//api名称
		qiniuApi.setApi("adj.c.qiniuUpload");
		//api版本
		qiniuApi.setApiVersion("1.0.0");
		//业务参数列表
		HashMap<String, Object> bodyMap = new HashMap<String,Object>();
		bodyMap.put("file", new File("d:\\fj.jpg"));
		bodyMap.put("fileName", "fj.jpg");
		//设置业务参数
		qiniuApi.setRequestBody(bodyMap);
		//发送请求
		KoalaResult uploadResult = client.sendRequest(qiniuApi);
		//返回值
		System.out.println(JSONUtils.toJSON(uploadResult));
		
		KoalaApi homeApi = new KoalaApi();
		homeApi.setApi("adj.c.home");
		homeApi.setApiVersion("1.0.0");
		HashMap<String, Object> homeBody = new HashMap<String,Object>();
		homeBody.put("communityId", "1");
		homeApi.setRequestBody(homeBody);
		
		HashMap<String, String> urpParams = new HashMap<String,String>();
		urpParams.put("ctid", "1");
		urpParams.put("cmmid", "1");
		
		KoalaResult homeResult = client.sendRequest(homeApi);
		System.out.println(JSONUtils.toJSON(homeResult));
		
		client.close();
		
		
	}
	

}
