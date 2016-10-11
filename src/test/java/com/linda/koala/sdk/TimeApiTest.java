package com.linda.koala.sdk;

import java.io.File;
import java.util.HashMap;

/**
 * 测试用例
 * @author lindezhi
 * 2016年5月29日 下午1:51:57
 */
public class TimeApiTest {
	
	public static void main(String[] args) throws InterruptedException {
		
		KoalaClient client = new KoalaClient();
		client.setAppKey("e2c2ab71e885408ea213a4f465745529d");
		client.setAppSec("77bb4062f3654556664d1c4ef3508ff73");
		client.setGatewayUrl("http://api.mtop.toabao.com/openapi");
		client.init();
		System.out.println(client.getServerTime());
		Thread.currentThread().sleep(500);
		System.out.println(client.getServerTime());
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
		qiniuApi.setApi("taobao.c.qiniuUpload");
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
		homeApi.setApi("taobao.c.home");
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
