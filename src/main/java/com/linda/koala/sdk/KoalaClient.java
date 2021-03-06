package com.linda.koala.sdk;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.log4j.Logger;

/**
 * koala java client
 * @author lindezhi
 * 2016年5月28日 下午12:16:55
 * 说明：http请求所有参数使用utf8编码
 */
public class KoalaClient {
	
	/**
	 * 开放平台分配的appkey
	 */
	private String appKey;
	
	/**
	 * 开放平台分配的sec
	 */
	private String appSec;
	
	/**
	 * 开放平台url
	 */
	private String gatewayUrl;
	
	/**
	 * http请求连接池限制
	 */
	private int maxConnections = 20;
	
	private int httpDefaultMaxPerRoute = 10;
	
	/**
	 * 发送http请求的工具
	 */
	private HttpComponent httpComponent;
	
	/**
	 * 风控参数
	 */
	private static final KoalaDS koalaDS;

	/**
	 * 系统参数
	 */
	private static final String osName;
	
	/**
	 * 系统版本
	 */
	private static final String osVersion;
	
	/**
	 * 设备号
	 */
	private static final String hwid;
	
	private AtomicInteger flagIndex = new AtomicInteger(100);
	
	
	private Timer timer = null;
	
	private AtomicLong time = null;
	
	/**
	 * 是否开启时间同步，避免时间不同步导致api不能访问
	 */
	private boolean useKopTime = true;
	
	private AtomicLong lastSyncTime = new AtomicLong(0);
	
	private final long SYNC_INTERVAL_6_HOUR = 1000*60*60*6;
	
	private final long SYNC_INTERVAL = SYNC_INTERVAL_6_HOUR;
	
	/**
	 * 签名算法
	 */
	private BaseSign signer = new SimpleSigner();
	
	Logger logger = Logger.getLogger("KoalaClient");
	
	/**
	 * 请求上下文flag，用于日志打印，方便排查问题
	 */
	private ThreadLocal<String> requestFlag = new ThreadLocal<String>(){
		
		@Override
		protected String initialValue() {
			return genRandomString();
		}
	};
	
	private String genRandomString(){
		int index = flagIndex.incrementAndGet();
		if(index>100000){
			flagIndex.set(100);
		}
		return "KoalaReuest_"+(System.currentTimeMillis()/54000)+"_"+index;
	}
	
	/**
	 * 初始化变量，安全控制
	 */
	static {
		koalaDS = new KoalaDS();
		osName = System.getProperty("os.name");
		osVersion = System.getProperty("os.version");
		hwid = "Aidaojia-SDK";
	}
	
	/**
	 * 启动调用方法
	 */
	public void init(){
		httpComponent = new HttpComponent(maxConnections,httpDefaultMaxPerRoute);
		if(useKopTime){
			timer = new Timer();
			time = new AtomicLong(0);
			timer.scheduleAtFixedRate(new TimerTask(){
				private AtomicBoolean inSync = new AtomicBoolean(false);
				@Override
				public void run() {
					long now = System.currentTimeMillis();
					//每6个小时同步一次系统时间
					if(now-lastSyncTime.get()> SYNC_INTERVAL&& inSync.compareAndSet(false, true)){
						requestKoalaTimeApi(null);
					}else{
						time.addAndGet(10);
					}
				}
			}, new Date(), 10);
			//同步系统时间
			this.requestKoalaTimeApi(null);
		}
	}
	
	/**
	 * kop.time 1.0
	 * @param body
	 * @return {"code":200,"data":{"time":1476171893751},"msg":"OK"}
	 */
	private long requestKoalaTimeApi(Map<String,Object> body){
		KoalaApi api = new KoalaApi();
		api.setApi("kop.time");
		api.setApiVersion("1.0");
		api.setRequestBody(body);
		//该api业务参数为空无需填写
		KoalaResult result = this.sendRequest(api);
		if(result.getCode()==200){
			Map timeMap = JSONUtils.fromJSON(result.getData(), Map.class);
			long serverTime = (Long)timeMap.get("time");
			//系统时间+15ms的返回时间
			long clientTime = serverTime+15;
			time.set(clientTime);
			lastSyncTime.set(System.currentTimeMillis());
			return clientTime;
		}else{
			throw new KoalaException("invalid koala config result "+JSONUtils.toJSON(result));
		}
	}
	
	/**
	 * 报告bug 使用kop.time api
	 * @param api
	 * @param version
	 * @param appkey
	 * @param sign
	 * @param bug
	 */
	public void reportBug(String api,String version,String appkey,String sign,String bug){
		HashMap<String,Object> map = new HashMap<String,Object>();
		map.put("r_api", api);
		map.put("r_version", version);
		map.put("r_appkey", appkey);
		map.put("r_sign", sign);
		map.put("r_bug", bug);
		this.requestKoalaTimeApi(map);
	}
	
	/**
	 * 结束执行方法
	 */
	public void close(){
		if(httpComponent!=null){
			httpComponent.close();
		}
		if(useKopTime){
			this.timer.cancel();
		}
	}
	
	public KoalaResult sendRequest(KoalaApi request){
		if(appKey==null||appSec==null||gatewayUrl==null){
			throw new KoalaException("please init appkey and appsec and url first");
		}
		if(this.useKopTime){
			request.setTimestamp(time.get());
		}else{
			request.setTimestamp(System.currentTimeMillis());
		}
		try{
			requestFlag.set(this.genRandomString());
			KoalaRequest koalaRequest = this.build(request);
			HttpResponseMeta response = null;
			if(koalaRequest.getFileBody()==null||koalaRequest.getFileBody().size()==0){
				//发送一般请求，使用post body
				response = this.sendSimpleRequest(koalaRequest);
			}else{
				//发送文件请求，使用multi part
				response = this.sendMultiRequest(koalaRequest);
			}
			return this.parseResponse(response);
		}catch(RuntimeException e){
			logger.error(this.toLog(e.getMessage()),e);
			throw e;
		}finally{
			requestFlag.remove();
		}
	}
	
	/**
	 * 生成统一的flag，方便查询
	 * @param msg
	 * @return
	 */
	private String toLog(String msg){
		return msg +",flag="+requestFlag.get();
	}
	
	/**
	 * 结果转换与预处理，上传错误等
	 * @param response
	 * @return
	 */
	private KoalaResult parseResponse(HttpResponseMeta response){
		KoalaResult result = new KoalaResult();
		if(response!=null){
			if(response.getStatusCode()!=200){
				result.setCode(999402);
				result.setMsg("服务器开小差啦，请稍后再试");
				logger.info(this.toLog("[Koala_Client] response:"+response.getResponseAsString()+" httpStatusCode:"+response.getStatusCode()+" result:"+JSONUtils.toJSON(result)));
				return result;
			}
			Map resultMap = JSONUtils.fromJSON(response.getResponseAsString(), Map.class);
			logger.info(this.toLog("[Koala_Client] response:"+response.getResponseAsString()+" httpStatusCode:"+200));
			Integer code = (Integer)resultMap.get("code");
			result.setCode(code);
			String msg = (String)resultMap.get("msg"); 
			result.setMsg(msg);
			Object data = resultMap.get("data");
			if(data!=null){
				result.setData(JSONUtils.toJSON(data));
			}else{
				result.setData(null);
			}
			return result;
		}else{
			logger.info(this.toLog("[Koala_Client] response:null"));
		}
		return null;
	}
	
	private HttpResponseMeta sendSimpleRequest(KoalaRequest request){
		Map<String, String> body = request.getBody();
		if(body==null){
			body = new HashMap<String,String>();
		}
		String json = JSONUtils.toJSON(body);
		try{
			String koalaBody = URLEncoder.encode(json, "utf-8");
			logger.info(this.toLog("[Koala_Client] httprequest url:"+request.getUrl()+" urlParams:"+JSONUtils.toJSON(request.getUrlParams())
					+" headers:"+JSONUtils.toJSON(request.getHeaders())+" body:"+koalaBody));
			return httpComponent.httpPost(request.getUrl(), request.getUrlParams(), request.getHeaders(), koalaBody);
		}catch(Exception e){
			throw new KoalaException(e);
		}
	}
	
	private HttpResponseMeta sendMultiRequest(KoalaRequest request){
		Map<String, byte[]> fileBody = request.getFileBody();
		if(fileBody==null||fileBody.size()<1){
			return this.sendSimpleRequest(request);
		}
		Map<String, String> body = request.getBody();
		if(body==null){
			body = new HashMap<String,String>();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		MultipartEntityBuilder entityBuilder = MultipartEntityBuilder.create();
		//string key
		Set<String> bodyKeys = body.keySet();
		ContentType contentType = ContentType.create("text/plain", "utf-8");
		for(String key:bodyKeys){
			String text = body.get(key);
			entityBuilder.addTextBody(key, text, contentType);
			sb.append(key+":"+text+",");
		}
		//file key
		Set<String> binKeys = fileBody.keySet();
		for(String key:binKeys){
			byte[] bytes = fileBody.get(key);
			entityBuilder.addBinaryBody(key, bytes, contentType, "ffff.a");
			sb.append(key+":bin["+bytes.length+"],");
		}
		sb.append("}");
		HttpEntity entity = entityBuilder.build();
		try{
			logger.info(this.toLog("[Koala_Client] httprequest url:"+request.getUrl()+" urlParams:"+JSONUtils.toJSON(request.getUrlParams())
					+" headers:"+JSONUtils.toJSON(request.getHeaders())+" body:"+sb.toString()));
			return httpComponent.httpPost(request.getUrl(), request.getUrlParams(), request.getHeaders(), entity);
		}catch(Exception e){
			throw new KoalaException(e);
		}
	}
	
	/**
	 * 生成请求参数
	 * @param request
	 * @return
	 */
	private KoalaRequest build(KoalaApi request){
		KoalaRequest koalaRequest = new KoalaRequest();
		koalaRequest.setUrl(gatewayUrl);

		HashMap<String, String> headerMap = new HashMap<String,String>();
		//风控参数
		if(koalaDS!=null){
			String json = JSONUtils.toJSON(koalaDS);
			try {
				String kopds = URLEncoder.encode(json, "utf-8");
				headerMap.put(Constant.HEAD_KOPDS_KEY, kopds);
			} catch (UnsupportedEncodingException e) {
				throw new KoalaException(e);
			}
		}
		if(request.getUserAgent()!=null){
			headerMap.put(Constant.HEAD_USER_AGENT_KEY, request.getUserAgent());
		}else{
			headerMap.put(Constant.HEAD_USER_AGENT_KEY, Constant.USER_AGENT);
		}
		koalaRequest.setHeaders(headerMap);
		
		//签名使用map
		TreeMap<String, String> signMap = new TreeMap<String,String>();
		
		HashMap<String, String> urlStrParams = new HashMap<String,String>();
		
		//--------------------------url 排除 sign 参数，方便签名---------------------------
		//可选url参数
		Map<String, String> params = request.getUrlParams();
		if(params!=null){
			Set<String> keys = params.keySet();
			for(String key:keys){
				String value = params.get(key);
				if(value!=null){
					urlStrParams.put(key, value);
				}
			}
		}
		//平台规定参数
		urlStrParams.put(Constant.URL_API_NAME, request.getApi());
		urlStrParams.put(Constant.URL_API_VERSION, request.getApiVersion());
		urlStrParams.put(Constant.URL_APP_KEY, appKey);
		urlStrParams.put(Constant.URL_APP_VERSION, Constant.APP_VERSION);
		urlStrParams.put(Constant.URL_HW_ID, hwid);
		urlStrParams.put(Constant.URL_MOBILE_TYPE, Constant.MOBILE_TYPE+"_"+osName);
		urlStrParams.put(Constant.URL_OS_TYPE, Constant.OS_TYPE+"");
		urlStrParams.put(Constant.URL_OS_VERSION, osVersion);
		
//		urlParams.put(Constant.URL_SIGN, request.get);
		
		urlStrParams.put(Constant.URL_TIMESTAMP, request.getTimestamp()+"");
		urlStrParams.put(Constant.URL_TTID, Constant.TTID);
		if(request.getUserId()!=null){
			urlStrParams.put(Constant.URL_USER_ID, request.getUserId());
		}
		if(request.getUserRole()!=null){
			urlStrParams.put(Constant.URL_USER_ROLE, request.getUserRole());
		}
		if(request.getToken()!=null){
			urlStrParams.put(Constant.URL_TOKEN, request.getToken());
		}
		signMap.putAll(urlStrParams);
		
		//-------------------body参数，签名前---------------------------------------
		HashMap<String, String> bodyStrMap = new HashMap<String,String>();
		HashMap<String, byte[]> bodyBinMap = new HashMap<String,byte[]>();
		
		Map<String, Object> body = request.getRequestBody();
		if(body!=null){
			Set<String> keys = body.keySet();
			for(String key:keys){
				Object value = body.get(key);
				if(value!=null){
					if(this.isJavaLangType(value.getClass())){
						bodyStrMap.put(key, value.toString());
					}else if(this.isFile(value.getClass())){
						if(value.getClass()==byte[].class){
							bodyBinMap.put(key, (byte[])value);
						}else{
							File file = (File)value;
							try {
								FileInputStream fis = new FileInputStream(file);
								ByteArrayOutputStream bos = new ByteArrayOutputStream();
								httpComponent.copy(fis, bos);
								byte[] bytes = bos.toByteArray();
								bodyBinMap.put(key, bytes);
								bos.close();
								fis.close();
							} catch (FileNotFoundException e) {
								throw new KoalaException(e);
							} catch (IOException e) {
								throw new KoalaException(e);
							}
						}
					}else{
						throw new KoalaException("invalid request body key "+key+" type:"+value.getClass());
					}
				}
			}
		}
		
		signMap.putAll(bodyStrMap);
		Set<String> binKeys = bodyBinMap.keySet();
		for(String binKey:binKeys){
			byte[] bs = bodyBinMap.get(binKey);
			signMap.put(binKey, "b["+bs.length+"]");
		}
		
		//签名生成
		String signBefore = signer.genSignbefore(signMap, this.appSec);
		String sign = signer.sign(signBefore);

		HashMap<String, Object> urlParams = new HashMap<String,Object>();
		urlParams.putAll(urlStrParams);
		urlParams.put(Constant.URL_SIGN, sign);
		
		koalaRequest.setSign(sign);
		koalaRequest.setUrlParams(urlParams);
		koalaRequest.setFileBody(bodyBinMap);
		koalaRequest.setBody(bodyStrMap);
		koalaRequest.setSignBefore(signBefore);
		
		logger.info(this.toLog("[Koala_Client] buildSign details:"+JSONUtils.toJSON(koalaRequest)+" api:"+JSONUtils.toJSON(request)));
		
		return koalaRequest;
	}
	
	private static boolean isJavaLangType(Class type){
		return isPrimitiveType(type)
				||type==Integer.class||type==Long.class||type==Short.class
				||type==Byte.class||type==Boolean.class||type==Character.class
				||type==Double.class||type==Float.class
				||type==String.class;
	}
	
	private static boolean isPrimitiveType(Class type){
		return type==int.class||type==long.class||type==short.class
				||type==byte.class||type==boolean.class||type==char.class
				||type==double.class||type==float.class;
	}

	private static boolean isFile(Class clazz){
		return clazz==File.class||clazz==byte[].class;
	}
	
	/**
	 * 方便生成签名使用
	 * @param body
	 * @return
	 */
    private Map<String, String> beanToMap(Object body) {
        Map<String, String> bodyMap = new TreeMap<String, String>();
        if (null != body) {
            Class<?> clazz = body.getClass();
            Field[] fields = clazz.getDeclaredFields();
            if (null != fields && fields.length > 0) {
                for (int index = 0; index < fields.length; index++) {
                    Field field = fields[index];
                    field.setAccessible(true);
                    try {
                        Object value = field.get(body);
                        if (null != value) {
                            bodyMap.put(field.getName(), String.valueOf(value));
                        }
                    } catch (Exception e) {
                    	throw new KoalaException(e);
                    }
                }
            }
        }
        return bodyMap;
    }
    
    /**
     * init后调用
     * 获取服务器时间，同步服务器时间
     * @return
     */
    public long getServerTime(){
    	if(useKopTime){
    		return time.get();
    	}else{
    		return System.currentTimeMillis();
    	}
    }
	
	public String getAppKey() {
		return appKey;
	}

	public void setAppKey(String appKey) {
		this.appKey = appKey;
	}

	public String getAppSec() {
		return appSec;
	}

	public void setAppSec(String appSec) {
		this.appSec = appSec;
	}

	public String getGatewayUrl() {
		return gatewayUrl;
	}

	public void setGatewayUrl(String gatewayUrl) {
		this.gatewayUrl = gatewayUrl;
	}

	public int getMaxConnections() {
		return maxConnections;
	}

	public void setMaxConnections(int maxConnections) {
		this.maxConnections = maxConnections;
	}

	public int getHttpDefaultMaxPerRoute() {
		return httpDefaultMaxPerRoute;
	}

	public void setHttpDefaultMaxPerRoute(int httpDefaultMaxPerRoute) {
		this.httpDefaultMaxPerRoute = httpDefaultMaxPerRoute;
	}

	public boolean isUseKopTime() {
		return useKopTime;
	}

	public void setUseKopTime(boolean useKopTime) {
		this.useKopTime = useKopTime;
	}
}
