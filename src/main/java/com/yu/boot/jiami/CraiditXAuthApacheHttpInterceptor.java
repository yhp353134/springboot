package com.yu.boot.jiami;

import org.apache.http.*;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.apache.http.protocol.HttpContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;


class CraiditXAuthApacheHttpInterceptor implements HttpRequestInterceptor {

    private final String accessId;
    private final String accessKey;
    private final String[] headerPattern;
    private final SecretKeySpec hmacKey;

    public CraiditXAuthApacheHttpInterceptor(String accessId, String accessKey) {
        this(accessId, accessKey, "Content-;Host;Date;X-CrX-");
    }

    /**
     * @param accessId       氪信分配的用户名
     * @param accessKey      氪信分配的密码
     * @param headerPatterns 参与签名计算的HTTP头字段名称
     */
    public CraiditXAuthApacheHttpInterceptor(
        String accessId, String accessKey, String headerPatterns
    ) {
        this.accessId = accessId;
        this.accessKey = accessKey;
        this.headerPattern = headerPatterns.toLowerCase().split(";");
        this.hmacKey = new SecretKeySpec(accessKey.getBytes(), "HmacSHA256");
    }

    /**
     * 将query中的参数名称和值编码，并按名称重新排序后用"&"连接，得到
     * 正规化的query字符串。编码结果应等价与JavaScript中的
     * {@code encodeURIComponent}函数。
     *
     * encodeURIComponent(<QueryParam1>)+"="+encodeURIComponent(<value>)+"&"+
     * encodeURIComponent(<QueryParam2>)+"="+encodeURIComponent(<value>)+"&"+
     * ...
     *
     * @see http://www.w3school.com.cn/jsref/jsref_encodeURIComponent.asp
     */
    private String normalizeQuery(HttpRequestWrapper req) {
        String query = req.getURI().getRawQuery();
        if (query == null) return "";
        query = query.replace("%21", "!").replace("%27", "'")
            .replace("%28", "(").replace("%29", ")").replace("%7E", "~");
        String[] query_list = query.split("&");
        Arrays.sort(query_list);
        return String.join("&", query_list);
    }

    /**
     * 根据{@code headerPattern}选出要参与签名HTTP头(名称全部视为小写)，
     * 并按名称重新排序后用换行符连接，得到正规化的HTTP头字符串。
     *
     * lower(<HeaderName1>)+":"+trim(<value>)+"\n"+
     * lower(<HeaderName2>)+":"+trim(<value>)+"\n"+
     * ...
     */
    private String[] normalizeHeaders(HttpRequestWrapper req) {
        List<String> headerName = new ArrayList();
        String host = null, date = null;
        if (!req.containsHeader("host")) {
            headerName.add("host");
            host = req.getURI().getHost();
            req.addHeader("Host", host);
        }
        if (!req.containsHeader("date")) {
            headerName.add("date");
            date = HEADER_DATE_FORMAT.format(new Date());
            req.addHeader("Date", date);
        }

        Header[] headers = req.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            String name = headers[i].getName().toLowerCase();
            if (!headerName.contains(name) &&
                headerMatches(name, this.headerPattern)) {
                headerName.add(name);
            }
        }
        Collections.sort(headerName);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headerName.size(); i++) {
            String name = headerName.get(i);
            String value =
                (name == "host" && host != null) ? host :
                (name == "date" && date != null) ? date :
                req.getFirstHeader(name).getValue();
            sb.append(name).append(":").append(value).append("\n");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        return new String[]{ sb.toString(), String.join(";", headerName) };
    }

    private String bodyHash(HttpRequestWrapper req)
        throws IOException, NoSuchAlgorithmException
    {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        if (req instanceof HttpEntityEnclosingRequest) {
            InputStream is = ((HttpEntityEnclosingRequest) req).getEntity().getContent();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                md.update(buffer, 0, length);
            }
        }
        return hex(md.digest());
    }

    private String hmacSHA256(String str, String key)
        throws NoSuchAlgorithmException, InvalidKeyException
    {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(this.hmacKey);
        byte[] result = mac.doFinal(str.getBytes());
        return hex(result);
    }

    private static String hex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++){
            String temp = Integer.toHexString(bytes[i] & 0xFF);
            if (temp.length() == 1) {
                sb.append("0");
            }
            sb.append(temp);
        }
        return sb.toString();
    }

    public void process(final HttpRequest req, final HttpContext context)
        throws HttpException, IOException
    {
        HttpRequestWrapper request = (HttpRequestWrapper) req;

        String nonce = UUID.randomUUID().toString();
        String[] header = normalizeHeaders(request);

        // 待签名字符串的格式为
        //    ${AccessId} + "\n"
        //  + ${Nonce} + "\n"
        //  + ${HTTP方法} + "\n"
        //  + ${HTTP URL路径} + "\n"
        //  + ${正规化的query字符串} + "\n"
        //  + ${正规化的HTTP头} + "\n"
        //  + ${HTTP body的哈希值}
        StringBuilder stringToSign = new StringBuilder();
        stringToSign
            .append(accessId).append('\n')
            .append(nonce).append('\n')
            .append(request.getMethod()).append('\n')
            .append(request.getURI().getPath()).append('\n')
            .append(normalizeQuery(request)).append('\n')
            .append(header[0]).append('\n');
        try {
            stringToSign.append(bodyHash(request));
        } catch(NoSuchAlgorithmException e) {
            throw new HttpException("Fail to generate SHA256 for HTTP body", e);
        }
        // System.out.println("---- STRING TO SIGN ----");
        // System.out.print(stringToSign.toString());
        // System.out.println("\n------------------------");

        String signature = "";
        try {
            signature = hmacSHA256(stringToSign.toString(), this.accessKey);
        } catch(NoSuchAlgorithmException e) {
            throw new HttpException("Fail to generate signature", e);
        } catch(InvalidKeyException e) {
            throw new HttpException("Fail to generate signature", e);
        }

        String auth = String.format(
            "CX-HMAC-%s AccessId=%s Nonce=%s SignedHeaders=%s Signature=%s",
            "SHA256", this.accessId, nonce, header[1], signature);
        request.addHeader("Authorization", auth);
    }

    private static boolean headerMatches(String s, String[] pats) {
        for (int i = 0; i < pats.length; i++) {
            String p = pats[i];
            if (s.equals(p) || p.endsWith("-") && s.startsWith(p))
                return true;
        }
        return false;
    }

    private static SimpleDateFormat HEADER_DATE_FORMAT =
        new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
    static {
        HEADER_DATE_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT"));
    }
}
