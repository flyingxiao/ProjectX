package cn.pandora.x.crawl;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

public class CrawlHomePage {

	public static void main(String[] args) {

		CloseableHttpClient httpclient = HttpClients.createDefault();

		try {
			HttpHost target = new HttpHost("www.ibm.com"); // 302

			// ����httpget.
			HttpGet httpget = new HttpGet("/");
			RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
			httpget.setConfig(requestConfig);
			
			// httpget.setHeader("User-Agent",
			// "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 1.7; .NET CLR 1.1.4322; CIBA; .NET CLR 2.0.50727)");

			System.out.println("executing request to " + target);

			// ִ��get����.
			CloseableHttpResponse response = httpclient
					.execute(target, httpget);

			try {
				// ��ȡ��Ӧʵ��
				HttpEntity entity = response.getEntity();
				System.out.println("--------------------------------------");
				// ��ӡ��Ӧ״̬
				System.out.println(response.getStatusLine());
				
				// ����Ƿ��ض���
				int statuscode = response.getStatusLine().getStatusCode();
				if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY)
						|| (statuscode == HttpStatus.SC_MOVED_PERMANENTLY)
						|| (statuscode == HttpStatus.SC_SEE_OTHER)
						|| (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
					// ��ȡ�µ�URL��ַ
					Header[] header = response.getHeaders("location");
					if (header != null) {
						String newuri = header[0].getValue();
						if ((newuri == null) || (newuri.equals("")))
							newuri = "/";
						HttpGet redirect = new HttpGet(newuri);
						response = httpclient.execute(redirect);
						System.out.println("Redirect:"
								+ response.getStatusLine());
						redirect.releaseConnection();
					} else
						System.out.println("Invalid redirect");
				}
				
				// ��ȡ��Ӧʵ��
				entity = response.getEntity();
				System.out.println("--------------------------------------");
				// ��ӡ��Ӧ״̬
				System.out.println(response.getStatusLine());
				
				if (entity != null) {
					// ��ӡ��Ӧ���ݳ���
					System.out.println("Response content length: "
							+ entity.getContentLength());

					// ��ӡ��Ӧ����
					// System.out.println("Response content: " +
					// EntityUtils.toString(entity, "UTF-8"));
					System.out.println("Response content type: "
							+ entity.getContentType());
					System.out.println("Response content encoding: "
							+ entity.getContentEncoding());
					System.out.println("Response content CharSet: "
							+ EntityUtils.getContentCharSet(entity));
					System.out.println("Response content length: "
							+ EntityUtils.toByteArray(entity).length);
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

	/**
	 * ��ȡ�ض���֮�����ַ��Ϣ
	 * 
	 * @see HttpClientȱʡ���Զ�����ͻ����ض���
	 * @see ��������ҳA��,���豻�ض�����B��ҳ,��ôHttpClient���Զ�����B��ҳ������
	 * @see ����ȡ��B��ҳ�ĵ�ַ
	 *      ,����Ҫ����HttpContext����,HttpContextʵ�����ǿͻ��������ڶ��������Ӧ�Ľ�����,����״̬��Ϣ��
	 * @see �����Լ�Ҳ��������HttpContext�����һЩ������Ҫ����Ϣ,�Ա��´������ʱ���ܹ�ȡ����Щ��Ϣ��ʹ��
	 */
	public static void getRedirectInfo() {
		HttpClient httpClient = new DefaultHttpClient();
		HttpContext httpContext = new BasicHttpContext();
		HttpGet httpGet = new HttpGet("http://127.0.0.1:8088/blog/main.jsp");
		try {
			// ��HttpContext������Ϊ��������execute()����,��HttpClient���������Ӧ���������е�״̬��Ϣ�洢��HttpContext��
			HttpResponse response = httpClient.execute(httpGet, httpContext);
			// ��ȡ�ض���֮���������ַ��Ϣ,��"http://127.0.0.1:8088"
			HttpHost targetHost = (HttpHost) httpContext
					.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			// ��ȡʵ�ʵ���������URI,���ض���֮���"/blog/admin/login.jsp"
			HttpUriRequest realRequest = (HttpUriRequest) httpContext
					.getAttribute(ExecutionContext.HTTP_REQUEST);
			System.out.println("������ַ:" + targetHost);
			System.out.println("URI��Ϣ:" + realRequest.getURI());
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				System.out.println("��Ӧ����:"
						+ EntityUtils.toString(entity, ContentType
								.getOrDefault(entity).getCharset()));
				EntityUtils.consume(entity);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}
}
