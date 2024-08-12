package de.hdg.keklist.util;

import com.google.gson.Gson;
import com.google.gson.JsonParser;
import de.hdg.keklist.Keklist;
import lombok.Getter;
import okhttp3.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class IpUtil {

    private final OkHttpClient client = new OkHttpClient();
    private static final Gson gson = new Gson();

    private static long xtls = System.currentTimeMillis(); // Time until rate limit reset
    private static int xrl = 45; // Requests left per minute


    @Getter
    private final String ip;

    public IpUtil(@NotNull  String ip) {
        this.ip = ip;
    }

    @NotNull
    public CompletableFuture<IpData> getIpData() {
        boolean local = (new IpAddressMatcher("127.0.0.0/8").matches(ip) ||
                new IpAddressMatcher("172.16.0.0/12").matches(ip) ||
                new IpAddressMatcher("192.168.0.0/16").matches(ip) ||
                new IpAddressMatcher("10.0.0.0/8").matches(ip));


        if (System.currentTimeMillis() > xtls) {
            xrl = 45;
            xtls = System.currentTimeMillis() + 60000; // Gets reset in request callback
        }

        if (xrl <= 0) {
            Keklist.getInstance().getSLF4JLogger().warn(Keklist.getTranslations().get("ip.rate-limit"));
            return CompletableFuture.failedFuture(new IOException("Rate limit reached"));
        }


        Request request = new Request.Builder()
                .url(getUrl(local ? "" : ip))
                .build();

        IpFutureCallback callback = new IpFutureCallback();
        client.newCall(request).enqueue(callback);

        return callback;
    }


    @Contract(pure = true)
    private @NotNull String getUrl(@NotNull String ip) {
        // https://ip-api.com/docs/api:json
        return "http://ip-api.com/json/" + ip + "?fields=3862299"; // status,message,continent,continentCode,country,countryCode,regionName,city,timezone,isp,org,as,mobile,proxy,hosting,query
    }


    private static class IpFutureCallback extends CompletableFuture<IpData> implements Callback {

        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            super.completeExceptionally(e);
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
            if (!response.isSuccessful()) {
                super.completeExceptionally(new IOException("Unexpected code " + response));
                return;
            }

            ResponseBody body = response.body();
            String json = body.string();

            if(JsonParser.parseString(json).getAsJsonObject().get("status").getAsString().equals("fail")) {
                super.completeExceptionally(new IOException("HTTP API failed with status: " + JsonParser.parseString(json).getAsJsonObject().get("message").getAsString()));
                return;
            }

            String xrlHeader = response.header("X-Rl");
            String xtlsHeader = response.header("X-Tsl");

            if(xrlHeader != null)
                xrl = Integer.parseInt(xrlHeader);

            if(xtlsHeader != null)
                xtls = System.currentTimeMillis() + (Integer.parseInt(xtlsHeader) * 1000L);

            IpData ipData = gson.fromJson(json, IpData.class);

            super.complete(ipData);
        }
    }

    public record IpData(String continent, String continentCode, String country, String countryCode,
                          String regionName, String city, String timezone, String isp, String org, String as,
                          boolean mobile, boolean proxy, boolean hosting, String query) {}


}
