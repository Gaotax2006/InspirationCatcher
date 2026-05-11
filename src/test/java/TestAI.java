import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class TestAI {
    public static void main(String[] args) {
        testDeepSeekConnection();
    }

    private static void testDeepSeekConnection() {
        String apiKey = System.getenv("DEEPSEEK_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.out.println("请设置环境变量 DEEPSEEK_API_KEY");
            return;
        }

        try {
            URL url = new URL("https://api.deepseek.com/v1/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // 设置请求头
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setDoOutput(true);

            // 简单测试请求体
            String requestBody = """
                {
                    "model": "deepseek-chat",
                    "messages": [
                        {"role": "user", "content": "你好"}
                    ],
                    "max_tokens": 50,
                    "temperature": 0.7
                }
                """;

            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = requestBody.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            // 获取响应码
            int responseCode = conn.getResponseCode();
            System.out.println("响应码: " + responseCode);

            if (responseCode == 200) {
                System.out.println("✅ API连接成功！");
            } else {
                System.out.println("❌ API连接失败，错误码: " + responseCode);
                // 读取错误信息
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        System.out.println("错误信息: " + line);
                    }
                }
            }

            conn.disconnect();

        } catch (Exception e) {
            System.out.println("❌ 异常: " + e.getMessage());
            e.printStackTrace();
        }
    }
}