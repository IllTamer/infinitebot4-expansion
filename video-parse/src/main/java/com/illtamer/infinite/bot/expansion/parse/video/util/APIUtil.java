package com.illtamer.infinite.bot.expansion.parse.video.util;

import com.illtamer.perpetua.sdk.util.Maps;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.InterruptedIOException;
import java.util.Map;

public final class APIUtil {

    private static final Map<String, String> headers = Maps.of(
            "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/88.0.4324.150 Safari/537.36"
    );

    public static String getBody(String url, boolean enableRedirect) throws InterruptedIOException {
        int status;
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            HttpGet httpGet = new HttpGet(url);
            headers.forEach(httpGet::addHeader);
            httpGet.setConfig(RequestConfig.custom()
                    .setConnectTimeout(3000)
                    .setSocketTimeout(3000)
                    .setRedirectsEnabled(enableRedirect)
                    .build());
            try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
                status = response.getStatusLine().getStatusCode();
                if (status == HttpStatus.SC_OK) {
                    HttpEntity responseEntity = response.getEntity();
                    return EntityUtils.toString(responseEntity);
                } else if (!enableRedirect && (status == HttpStatus.SC_MOVED_TEMPORARILY || status == HttpStatus.SC_MOVED_PERMANENTLY)) {
                    return EntityUtils.toString(response.getEntity());
                }
            } catch (InterruptedIOException e) {
                throw new InterruptedIOException(e.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


}
