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
        	HttpHost target = new HttpHost("www.baidu.com", 443, "https");
            HttpHost proxy = new HttpHost("117.162.116.4", 8123);

            RequestConfig config = RequestConfig.custom().setProxy(proxy).build();
        	
            // ����httpget.
            HttpGet httpget = new HttpGet("www.baidu.com");
            httpget.setConfig(config);
            
            System.out.println("executing request to " + target + " via " + proxy);
            
            // ִ��get����.
            CloseableHttpResponse response = httpclient.execute(target, httpget);
            try {
                // ��ȡ��Ӧʵ��
                HttpEntity entity = response.getEntity();
                System.out.println("--------------------------------------");
                // ��ӡ��Ӧ״̬
                System.out.println(response.getStatusLine());
                if (entity != null) {
                    // ��ӡ��Ӧ���ݳ���
                    System.out.println("Response content length: " + entity.getContentLength());
                    // ��ӡ��Ӧ����
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
            // �ر�����,�ͷ���Դ
            try {
                httpclient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
	}

}
