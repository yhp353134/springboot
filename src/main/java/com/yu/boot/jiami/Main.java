package com.yu.boot.jiami;

import okhttp3.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import java.net.URI;
import java.util.concurrent.TimeUnit;


public class Main {

    public static void main(String[] args) throws Exception {

        // 初始化OkHttp Client, 注意：重用同一个客户端对象才能达到最佳性能
        OkHttpClient client1 = initOkHttpClient();

        // 初始化Apache HttpClient, 注意：重用同一个客户端对象才能达到最佳性能
        HttpClient client = initApacheHttpClient();

        // 按照接口文档准备参数，这里以xcloud_xfraud接口为例
        String name = "张三";
        String mobile = "13800138000";
        String id = "21043219870123456X";

        // 手机号以SHA256形式传入，姓名/身份证这两个字段也可支持
        String mobile_sha256 = DigestUtils.sha256Hex(mobile);

        URI uri = new URIBuilder()
                // 按照接入文档的服务地址设置正确的URL
                .setScheme("https").setHost("api.creditx.com").setPath("/xcloud_xfraud")
                .setParameter("user_name", name)
                // SHA256形式传入的参数名称带有_sha256后缀
                .setParameter("mobile_number_sha256", mobile_sha256)
                .setParameter("identity_number", id)
                .build();

        // 使用OkHttp Client调用接口
        callWithOkHttpClient(client1, uri);

        // 使用Apache HttpClient调用接口
        callWithApacheHttpClient(client, uri);
    }

    /**
     * 使用AccessId方式验证，需要设置AccessId和AccessKey。
     */
    public static final String accessId = "asdf";
    public static final String accessKey = "fdsa";

    /**
     * 使用证书方式验证时，需要导入客户端证书和服务器CA证书。证书申请
     * 步骤请参阅接口对接文档。
     */

    // 准备好客户密钥client.key和氪信返回的客户证书client.crt，执行以
    // 下命令生成Java需要的.p12格式文件:
    //   openssl pkcs12 -export -clcerts -in client.crt -inkey client.key -out client.p12
    // client_passwd为执行命令时填写的密码。
    public static final String client_file = "./client.p12";
    public static final String client_passwd = "craiditx";

    // 如果氪信返回了CA证书ca.crt，执行以下命令生成.jks格式文件:
    //   keytool -keystore truststore.jks -alias craiditx -import -trustcacerts -file ca.crt
    // server_passwd为执行命令时填写的密码。
    public static final String server_file = "./truststore.jks";
    public static final String server_passwd = "craiditx";


    /**
     * 初始化OkHttp客户端。
     *
     * 注意：每个OkHttp3的客户端对象都有独立的连接池，使用同一个客户端
     * 对象发起多次查询才能达到最佳性能。若没有正确使用，在并发请求量
     * 过大时可能会导致请求被服务器拒绝(返回HTTP 429错误码)。
     */
    static OkHttpClient initOkHttpClient() throws Exception {

        // 并发连接数设置，目前氪信服务器支持单IP最大50个并发连接
        Dispatcher dispatcher = new Dispatcher();
        // 性能参考：行为风险评分接口的平均响应时间约300ms，黑名单接口
        // 的平均响应时间约250ms，10个并发连接可以达到>25qps
        dispatcher.setMaxRequestsPerHost(10);

        // 连接池设置，10s后释放长连接
        ConnectionPool cp = new ConnectionPool(5, 10, TimeUnit.SECONDS);

        /** AccessID方式验证，使用以下代码 **/
        OkHttpClient client1 = new OkHttpClient.Builder()
            .addInterceptor(new CraiditXAuthOkHttpInterceptor(accessId, accessKey))
            .dispatcher(dispatcher)
            .connectionPool(cp)
            .build();

        /** 证书方式验证，使用以下代码 **/
        CraiditXSSLContext context = new CraiditXSSLContext()
            // 如果氪信返回了CA证书，则需要引入
            // .addServerCAFile(server_file, server_passwd)
            // 设置客户端证书
            .setClientCert(client_file, client_passwd);
        OkHttpClient client = new OkHttpClient.Builder()
            .sslSocketFactory(context.getSocketFactory(), context.getTrustManager())
            .dispatcher(dispatcher)
            .connectionPool(cp)
            .build();

        return client;
    }

    static void callWithOkHttpClient(OkHttpClient client, URI uri)
        throws Exception
    {
        HttpUrl url = HttpUrl.get(uri);
        Request request = new Request.Builder().url(url).build();
        /** 注意：调用接口时重用client对象才能达到最佳性能 **/
        Response response = client.newCall(request).execute();

        // 检查HTTP状态码，200是正常返回，否则需检查响应内容里的错误消息
        System.out.println(response.code());

        // AccessID方式签名比对不通过时，输出服务器端计算的待签名字符
        // 串，可与CraiditXAuthOkHttpInterceptor.intercept()中的变量
        // stringToSign对比
        if (response.header("X-CrX-String-To-Sign") != null) {
            System.out.println("---- SIGNATURE VERIFICATION FAIL! ----");
            String strToSign = response.header("X-CrX-String-To-Sign");
            System.out.println(strToSign.substring(1, strToSign.length()-1).replace("\\n", "\n"));
            System.out.println("--------------------------------------");
        }

        System.out.println(response.body().string());
    }


    /**
     * 初始化Apache Http客户端，暂不支持客户证书方式验证。
     *
     * 注意：每个客户端对象都有独立的连接池，使用同一个客户端对象发起
     * 多次查询才能达到最佳性能。若没有正确使用，在并发请求量过大时可
     * 能会导致请求被服务器拒绝(返回HTTP 429错误码)。
     */
    static HttpClient initApacheHttpClient() throws Exception {
        final PoolingHttpClientConnectionManager cm =
            new PoolingHttpClientConnectionManager();
        // 并发连接数设置，目前氪信服务器支持单IP最大50个并发连接
        // 性能参考：行为风险评分接口的平均响应时间约300ms，黑名单接口
        // 的平均响应时间约250ms，10个并发连接可以达到>25qps
        cm.setMaxTotal(10);

        /** AccessID方式验证，使用以下代码 **/
        HttpClient client = HttpClients.custom()
            .addInterceptorLast(new CraiditXAuthApacheHttpInterceptor(accessId, accessKey))
            .setConnectionManager(cm)
            .build();

        return client;
    }

    static void callWithApacheHttpClient(HttpClient client, URI uri)
        throws Exception
    {
        HttpGet request = new HttpGet(uri);
        HttpResponse response = client.execute(request);

        // 检查HTTP状态码，200是正常返回，否则需检查响应内容里的错误消息
        System.out.println(response.getStatusLine().getStatusCode());

        // AccessID方式签名比对不通过时，输出服务器端计算的待签名字符
        // 串，可与CraiditXAuthInterceptor.intercept()中的变量
        // stringToSign对比
        if (response.containsHeader("X-CrX-String-To-Sign")) {
            System.out.println("---- SIGNATURE VERIFICATION FAIL! ----");
            String strToSign = response.getFirstHeader("X-CrX-String-To-Sign").getValue();
            System.out.println(strToSign.substring(1, strToSign.length()-1).replace("\\n", "\n"));
            System.out.println("--------------------------------------");
        }

        System.out.println(EntityUtils.toString(response.getEntity(), "UTF-8"));
    }



}
