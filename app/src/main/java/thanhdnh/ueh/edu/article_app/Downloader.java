package thanhdnh.ueh.edu.article_app;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.ProgressBar;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSink;
import okio.Okio;

public class Downloader {
  public static String cached_file_path = "";

  /** Callback báo tiến trình. percent = -1 nếu server không trả Content-Length. */
  public interface ProgressListener {
    void onProgress(int percent, long downloadedBytes, long totalBytes);
  }

  public static File downloadFile(String url, File cached) {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url).build();

    try (Response response = client.newCall(request).execute()) {
      if (!response.isSuccessful()) return null;
      String contentType = response.header("Content-Type", "");
      String extension = getExtensionFromMimeType(contentType);
      File file = File.createTempFile("downloaded_file", extension, cached);
      if (response.body() != null) {
        BufferedSink sink = Okio.buffer(Okio.sink(file));
        sink.writeAll(response.body().source());
        sink.close();
        return file;
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /**
   * MỚI: tải file về thư mục cache và báo tiến trình (%) liên tục.
   * Hàm chạy ĐỒNG BỘ nên phải gọi trong luồng nền (ExecutorService).
   * listener được gọi ngay trên luồng nền -> muốn cập nhật UI phải runOnUiThread.
   *
   * @return file đã tải trong cacheDir, hoặc null nếu lỗi.
   */
  public static File downloadWithProgress(String url, File cacheDir, String fileName, ProgressListener listener) {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(url).build();

    try (Response response = client.newCall(request).execute()) {
      ResponseBody body = response.body();
      if (!response.isSuccessful() || body == null) return null;

      long totalBytes = body.contentLength(); // -1 nếu không biết
      File file = new File(cacheDir, fileName);

      try (InputStream inputStream = body.byteStream();
           OutputStream outputStream = new FileOutputStream(file)) {
        byte[] buffer = new byte[4096];
        long downloadedBytes = 0;
        int bytesRead;
        int lastPercent = -2;

        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
          downloadedBytes += bytesRead;

          int percent = totalBytes > 0 ? (int) ((downloadedBytes * 100) / totalBytes) : -1;
          if (listener != null && percent != lastPercent) {
            lastPercent = percent;
            listener.onProgress(percent, downloadedBytes, totalBytes);
          }
        }
        outputStream.flush();
      }
      return file;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  /** MỚI: đọc toàn bộ file text (UTF-8). Chuyển từ ArticleData sang đây, đã sửa lỗi trả về null/NPE. */
  public static String readText(File file) {
    StringBuilder builder = new StringBuilder();
    try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        builder.append(line).append("\n");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return builder.toString();
  }

  // ---- Hàm cũ giữ nguyên (tải ảnh có ProgressBar) ----
  public static void downloadWithProgress(String inputurl, Handler mainHandler, Context context, File where2store, ProgressBar progressBar, ImageView imageView) {
    OkHttpClient client = new OkHttpClient();
    Request request = new Request.Builder().url(inputurl).build();

    client.newCall(request).enqueue(new Callback() {
      @Override
      public void onFailure(Call call, IOException e) {
        mainHandler.post(() -> {
          progressBar.setVisibility(ProgressBar.INVISIBLE);
        });
      }

      @Override
      public void onResponse(Call call, Response response) {
        if (!response.isSuccessful()) {
          mainHandler.post(() -> {});
          return;
        }

        long totalBytes = response.body().contentLength();
        InputStream inputStream = response.body().byteStream();
        String contentType = response.header("Content-Type", "");
        String extension = getExtensionFromMimeType(contentType);

        try (OutputStream outputStream = new FileOutputStream(where2store + "/downloaded_file" + extension)) {
          byte[] buffer = new byte[1024];
          long downloadedBytes = 0;
          int bytesRead;

          while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
            downloadedBytes += bytesRead;
            int progress = (int) ((downloadedBytes * 100) / totalBytes);
            mainHandler.post(() -> progressBar.setProgress(progress));
          }
          outputStream.flush();

          mainHandler.post(() -> {
            cached_file_path = where2store + "/downloaded_file" + extension;
            imageView.setImageURI(Uri.parse(cached_file_path));
            progressBar.setVisibility(ProgressBar.INVISIBLE);
          });
        } catch (Exception e) {
          mainHandler.post(() -> {});
        }
      }
    });
  }

  private static String getExtensionFromMimeType(String mimeType) {
    Map<String, String> mimeMap = new HashMap<>();
    mimeMap.put("image/jpeg", ".jpg");
    mimeMap.put("image/png", ".png");
    mimeMap.put("application/json", ".json");
    return mimeMap.getOrDefault(mimeType, "");
  }
}