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

			// 创建httpget.
			HttpGet httpget = new HttpGet("/");
			RequestConfig requestConfig = RequestConfig.custom().setRedirectsEnabled(false).build();
			httpget.setConfig(requestConfig);
			
			// httpget.setHeader("User-Agent",
			// "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; QQDownload 1.7; .NET CLR 1.1.4322; CIBA; .NET CLR 2.0.50727)");

			System.out.println("executing request to " + target);

			// 执行get请求.
			CloseableHttpResponse response = httpclient
					.execute(target, httpget);

			try {
				// 获取响应实体
				HttpEntity entity = response.getEntity();
				System.out.println("--------------------------------------");
				// 打印响应状态
				System.out.println(response.getStatusLine());
				
				// 检查是否重定向
				int statuscode = response.getStatusLine().getStatusCode();
				if ((statuscode == HttpStatus.SC_MOVED_TEMPORARILY)
						|| (statuscode == HttpStatus.SC_MOVED_PERMANENTLY)
						|| (statuscode == HttpStatus.SC_SEE_OTHER)
						|| (statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
					// 读取新的URL地址
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
				
				// 获取响应实体
				entity = response.getEntity();
				System.out.println("--------------------------------------");
				// 打印响应状态
				System.out.println(response.getStatusLine());
				
				if (entity != null) {
					// 打印响应内容长度
					System.out.println("Response content length: "
							+ entity.getContentLength());

					// 打印响应内容
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
			// 关闭连接,释放资源
			try {
				httpclient.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 获取重定向之后的网址信息
	 * 
	 * @see HttpClient缺省会自动处理客户端重定向
	 * @see 即访问网页A后,假设被重定向到了B网页,那么HttpClient将自动返回B网页的内容
	 * @see 若想取得B网页的地址
	 *      ,就需要借助HttpContext对象,HttpContext实际上是客户端用来在多次请求响应的交互中,保持状态信息的
	 * @see 我们自己也可以利用HttpContext来存放一些我们需要的信息,以便下次请求的时候能够取出这些信息来使用
	 */
	public static void getRedirectInfo() {
		HttpClient httpClient = new DefaultHttpClient();
		HttpContext httpContext = new BasicHttpContext();
		HttpGet httpGet = new HttpGet("http://127.0.0.1:8088/blog/main.jsp");
		try {
			// 将HttpContext对象作为参数传给execute()方法,则HttpClient会把请求响应交互过程中的状态信息存储在HttpContext中
			HttpResponse response = httpClient.execute(httpGet, httpContext);
			// 获取重定向之后的主机地址信息,即"http://127.0.0.1:8088"
			HttpHost targetHost = (HttpHost) httpContext
					.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
			// 获取实际的请求对象的URI,即重定向之后的"/blog/admin/login.jsp"
			HttpUriRequest realRequest = (HttpUriRequest) httpContext
					.getAttribute(ExecutionContext.HTTP_REQUEST);
			System.out.println("主机地址:" + targetHost);
			System.out.println("URI信息:" + realRequest.getURI());
			HttpEntity entity = response.getEntity();
			if (null != entity) {
				System.out.println("响应内容:"
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
