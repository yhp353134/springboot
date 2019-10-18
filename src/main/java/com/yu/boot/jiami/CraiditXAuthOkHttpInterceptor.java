package com.yu.boot.jiami;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.ByteString;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;


class CraiditXAuthOkHttpInterceptor implements Interceptor {

    private final String accessId;
    private final String accessKey;
    private final String[] headerPattern;

    public CraiditXAuthOkHttpInterceptor(String accessId, String accessKey) {
        this(accessId, accessKey, "Content-;Host;Date;X-CrX-");
    }

    /**
     * @param accessId       氪信分配的用户名
     * @param accessKey      氪信分配的密码
     * @param headerPatterns 参与签名计算的HTTP头字段名称，支持末尾位
     *                       于分隔符"-"之后的通配符"*"，如"X-CrX-*"。
     */
    public CraiditXAuthOkHttpInterceptor(
        String accessId, String accessKey, String headerPatterns
    ) {
        this.accessId = accessId;
        this.accessKey = accessKey;
        this.headerPattern = headerPatterns.toLowerCase().split(";");
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
    private String normalizeQuery(Request req) {
        String query = req.url().encodedQuery();
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
    private String[] normalizeHeaders(Request req, Request.Builder bld) {
        List<String> headerName = new ArrayList();
        String host = null, date = null;
        if (req.header("host") == null) {
            headerName.add("host");
            host = req.url().host();
            bld.header("Host", host);
        }
        if (req.header("date") == null) {
            headerName.add("date");
            date = HEADER_DATE_FORMAT.format(new Date());
            bld.header("Date", date);
        }

        for (String name : req.headers().names()) {
            if (headerMatches(name, this.headerPattern))
                headerName.add(name);
        }
        Collections.sort(headerName);

        StringBuilder sb = new StringBuilder();
        for (String name : headerName) {
            String value =
                (name == "host" && host != null) ? host :
                (name == "date" && date != null) ? date :
                req.header(name);
            sb.append(name).append(":").append(value).append("\n");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }

        return new String[]{ sb.toString(), String.join(";", headerName) };
    }

    private String bodyHash(RequestBody body) throws IOException {
        Buffer buf = new Buffer();
        if (body != null) {
            body.writeTo(buf);
        }
        return buf.sha256().hex();
    }

    @Override public Response intercept(Interceptor.Chain chain) throws IOException {
        Request req = chain.request();
        Request.Builder authReqBuilder = req.newBuilder();

        String nonce = UUID.randomUUID().toString();
        String[] header = normalizeHeaders(req, authReqBuilder);

        // 待签名字符串的格式为
        //    ${AccessId} + "\n"
        //  + ${Nonce} + "\n"
        //  + ${HTTP方法} + "\n"
        //  + ${HTTP URL路径} + "\n"
        //  + ${正规化的query字符串} + "\n"
        //  + ${正规化的HTTP头} + "\n"
        //  + ${HTTP body的哈希值}
        Buffer stringToSign = new Buffer();
        stringToSign
            .writeUtf8(this.accessId).writeUtf8("\n")
            .writeUtf8(nonce).writeUtf8("\n")
            .writeUtf8(req.method()).writeUtf8("\n")
            .writeUtf8(req.url().encodedPath()).writeUtf8("\n")
            .writeUtf8(normalizeQuery(req)).writeUtf8("\n")
            .writeUtf8(header[0]).writeUtf8("\n")
            .writeUtf8(bodyHash(req.body()));
        // System.out.println("---- STRING TO SIGN ----");
        // stringToSign.writeTo(System.out);
        // System.out.println("\n------------------------");
        String signature = stringToSign
            .hmacSha256(ByteString.encodeUtf8(this.accessKey)).hex();

        String auth = String.format(
            "CX-HMAC-%s AccessId=%s Nonce=%s SignedHeaders=%s Signature=%s",
            "SHA256", this.accessId, nonce, header[1], signature);
        authReqBuilder.header("Authorization", auth);

        return chain.proceed(authReqBuilder.build());
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
