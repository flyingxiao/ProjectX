package cn.xiao.crawl;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpProxyAvailable {

	public static void main(String[] args) {
		
		CloseableHttpClient httpclient = HttpClients.createDefault();
		
        try {
        	HttpHost target = new HttpHost("www.kuaidaili.com");
//            HttpHost proxy = new HttpHost("106.3.40.67", 8080);
//            HttpHost proxy = new HttpHost("106.38.251.62", 8088);
            HttpHost proxy = new HttpHost("122.225.106.40", 80);

            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        	
            // 创建httpget.
            HttpGet httpget = new HttpGet("/free/inha/3");
            httpget.setConfig(config);
            
            System.out.println("executing request to " + target + " via " + proxy);
            
            // 执行get请求.
            CloseableHttpResponse response = httpclient.execute(target, httpget);
            try {
                // 获取响应实体
                HttpEntity entity = response.getEntity();
                System.out.println("--------------------------------------");
                // 打印响应状态
                System.out.println(response.getStatusLine());
                if (entity != null) {
                    // 打印响应内容长度
                    System.out.println("Response content length: " + entity.getContentLength());
                    // 打印响应内容
                    System.out.println("Response content: " + EntityUtils.toString(entity, "UTF-8"));
                }
                System.out.println("------------------------------------");
            } finally {
                response.close();
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭连接,释放资源
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

}
