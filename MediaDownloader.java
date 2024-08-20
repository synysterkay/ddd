package com.raymond.redditdownloader;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.gson.Gson;
import com.raymond.redditdownloader.downloads.DownloadsFragment;
import com.raymond.redditdownloader.history.HistoryFragment;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONArray;
import org.json.JSONException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MediaDownloader extends AppCompatActivity {
    private static Context context;
    private static final OkHttpClient httpClient = new OkHttpClient();
    private static File outputFile;
    private static AppCompatActivity appCompatActivity;
    private static DownloadsFragment downloadsFragment;
    private static HistoryFragment historyFragment;
    private static Boolean isShare;
    private static String jsonData;
    private static JSONArray mainArray;
    private static JSONArray children;

    public MediaDownloader(Context context, boolean bool) {
        MediaDownloader.context = context;
        appCompatActivity = (AppCompatActivity) context;
        isShare = bool;
        if (!isShare) {
            historyFragment = (HistoryFragment) (appCompatActivity).getSupportFragmentManager().findFragmentByTag("fragmentHistory");
            downloadsFragment = (DownloadsFragment) (appCompatActivity).getSupportFragmentManager().findFragmentByTag("fragmentDownload");
        }
    }

    public static File download(String urlInput) {
        try {
            if (isValid(urlInput)) {
                if (sendGET(urlInput)) {
                    String fileName = titleGrab(children);
                    if (getDomain(jsonData).contains("giphy")) {
                    } else {
                        switch (getDomain(jsonData)) {
                            case "v.redd.it":
                                String redditFallbackURL = redditFallbackGrab(children);
                                outputFile = mux(redditFallbackURL, fileName);
                                break;
                            case "gfycat.com":
                                String gfycatURL = getGfycatGiant(children);
                                outputFile = downloadMedia(gfycatURL, fileName, ".mp4");
                                break;
                            case "i.redd.it":
                                String url = imageURLGrab(children);
                                if (url.substring(url.lastIndexOf(".")).contains("gif")) {
                                    outputFile = downloadMedia(url, fileName, ".gif");
                                } else {
                                    outputFile = downloadMedia(url, fileName, ".png");
                                }
                                break;
                            case "i.imgur.com":
                                String imgurURL = imageURLGrab(children);
                                String extension;
                                imgurURL = "https://" + imgurURL.substring(imgurURL.indexOf("i.imgur"));
                                switch (imgurURL.substring(imgurURL.lastIndexOf("."))) {
                                    case ".gif":
                                        extension = ".gif";
                                        break;
                                    case ".gifv":
                                        extension = ".mp4";
                                        imgurURL = imgurURL.substring(0, imgurURL.lastIndexOf(".")) + ".mp4";
                                        break;
                                    case ".mp4":
                                        extension = ".mp4";
                                        break;
                                    default:
                                        extension = ".png";
                                }
                                outputFile = downloadMedia(imgurURL, fileName, extension);
                                break;
                        }
                    }
                    if (outputFile != null) {
                        addToHistory(urlInput);
                        if (!isShare) {
                            downloadsFragment.downloadDialog.dismiss();
                            addMedia(outputFile);
                        }
                        return outputFile;
                    } else {
                        Toast("Failed to download media");
                        if (!isShare) {
                            enableButton();
                        }
                    }
                }
            } else if (!isShare) enableButton();
        } catch (Exception e) {
            Log.e("MediaDownloader", "Error during download: " + e.getMessage());
            e.printStackTrace();
            Toast("An error occurred during download");
            if (!isShare) {
                enableButton();
            }
        }
        return outputFile;
    }

    public static boolean isValid(String url) {
        String redditRegex = "https?://(www.)?\\b(reddit.com|redd.it)\\b\\b(/r/)\\b[-a-zA-Z0-9@:%._+~&?#/=]{1,512}";
        String urlRegex = "(https?://){1}[-a-zA-Z0-9@:%_+~&?#./=]{1,512}";
        if (isMatch(url, urlRegex)) {
            try {
                URL urlInput = new URL(url);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Toast("URL is invalid");
                if (!isShare) {
                    enableButton();
                }
                return false;
            }
            if (!isMatch(url, redditRegex)) {
                Toast("URL is not a reddit URL/post");
                if (!isShare) {
                    enableButton();
                }
                return false;
            } else return true;
        } else if (url.length() == 0) {
            Toast("Please enter a URL");
            enableButton();
            return false;
        } else if (!url.substring(0,4).equalsIgnoreCase("http")) {
            url = "https://" + url;
            if (!isShare) {
                setURL(url);
            }
            isValid(url);
            return false;
        } else {
            return false;
        }
    }

    private static boolean isMatch(String in, String pattern) {
        try {
            Pattern pattern1 = Pattern.compile(pattern);
            Matcher matcher = pattern1.matcher(in);
            return matcher.matches();
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static boolean sendGET(String url) throws IOException {
        if (new URL(url).getQuery() != null) { url = removeQuery(url); }
        final Request request = new Request.Builder()
                .url(url + ".json")
                .build();
        try {
            Response response = httpClient.newCall(request).execute();
            jsonData = response.body().string();
            mainArray = new JSONArray(jsonData);
            children = mainArray.getJSONObject(0).getJSONObject("data").getJSONArray("children");
            return true;
        } catch (JSONException e) {
            if (!isShare) enableButton();
            return false;
        }
    }

    private static File downloadMedia(String url, String fileName, String extension) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        Response response = httpClient.newCall(request).execute();
        ContextWrapper contextWrapper = new ContextWrapper(context);
        File directory = contextWrapper.getExternalFilesDir(null);
        Log.d("TAG", "downloadMedia: " + directory);
        outputFile = new File(directory, fileName + extension);
        Log.d("TAG", "downloadMedia: " + extension);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFile);
        InputStream in = response.body().byteStream();
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            fileOutputStream.write(buf, 0, len);
        }
        in.close();
        fileOutputStream.close();
        return outputFile;
    }

    private static void enableButton() {
        final Button button = downloadsFragment.downloadDialog.findViewById(R.id.downloadButton);
        ((MainActivity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                button.setEnabled(true);
            }
        });
    }

    private static void setURL(final String url) {
        final TextView textView = downloadsFragment.downloadDialog.findViewById(R.id.urlDownload);
        ((MainActivity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.setText(url);
            }
        });
    }

    private static String removeQuery(String url) {
        return url.substring(0, url.indexOf("?"));
    }

    private static String redditFallbackGrab(JSONArray children) throws JSONException {
        return children.getJSONObject(0).getJSONObject("data").getJSONObject("secure_media").getJSONObject("reddit_video").getString("fallback_url");
    }

    private static String imageURLGrab(JSONArray children) throws JSONException {
        return children.getJSONObject(0).getJSONObject("data").getString("url_overridden_by_dest");
    }

    private static String titleGrab(JSONArray children) throws JSONException {
        String title = children.getJSONObject(0).getJSONObject("data").getString("title");
        return removeSlash(title);
    }

    private static String removeSlash(String temp) {
        if (temp.contains("/")) {
            temp = temp.substring(0, temp.indexOf("/")) + "⁄" +temp.substring(temp.indexOf("/") + 1);
            removeSlash(temp);
        }
        return temp;
    }

    private static void Toast(final String toast) {
        if (!isShare) {
            ((MainActivity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
                }
            });
        } else Toast.makeText(context, toast, Toast.LENGTH_SHORT).show();
    }

    public static void addToHistory(String url) {
        LinkedList<String> linkedList = new LinkedList<>();
        SharedPreferences sharedPreferences = context.getSharedPreferences("history", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        url = url.substring(url.indexOf("/r/"));
        Gson gson = new Gson();
        String jsonText = sharedPreferences.getString("key", null);
        if (jsonText != null) {
            linkedList = gson.fromJson(jsonText, LinkedList.class);
        }
        int mDataSize = linkedList.size();
        linkedList.addLast(url);
        jsonText = gson.toJson(linkedList);
        editor.putString("key", jsonText);
        editor.apply();
        if (!isShare) {
            historyFragment.recyclerView.getAdapter().notifyItemInserted(mDataSize);
            historyFragment.recyclerView.smoothScrollToPosition(mDataSize);
        }
    }

    private static void addMedia(File filePath) {
        if (downloadsFragment != null && downloadsFragment.imageDirList != null) {
            MediaObjects mediaObjects = new MediaObjects();
            mediaObjects.setMediaPath(filePath.getAbsolutePath());
            downloadsFragment.imageDirList.add(mediaObjects);
        } else {
            Log.e("MediaDownloader", "downloadsFragment or imageDirList is null");
        }
    }

    private static String getDomain(String jsonData) throws JSONException {
        JSONArray mainArray = new JSONArray(jsonData);
        JSONArray children = mainArray.getJSONObject(0).getJSONObject("data").getJSONArray("children");
        return children.getJSONObject(0).getJSONObject("data").getString("domain");
    }

    private static String getGfycatGiant (JSONArray children) throws JSONException {
        String url = children.getJSONObject(0).getJSONObject("data").getJSONObject("secure_media").getJSONObject("oembed").getString("thumbnail_url");
        Log.d("TAG", url);
        url = "https://giant." + url.substring(url.indexOf("gfycat.com"), url.indexOf("-size")) + ".mp4";
        return url;
    }

    private static Boolean isGif (JSONArray children) throws JSONException {
        return Boolean.valueOf(children.getJSONObject(0).getJSONObject("data").getJSONObject("secure_media").getJSONObject("reddit_video").getString("is_gif"));
    }

    private static Boolean hasAudio(String fallbackURL) {
        Boolean result = false;
        String redditAudioURL = fallbackURL.substring(0, fallbackURL.indexOf("DASH")) + "DASH_audio.mp4";
        Request request = new Request.Builder()
                .url(redditAudioURL)
                .build();
        try {
            Response response = httpClient.newCall(request).execute();
            String body = response.body().string();
            Log.d("TAG", "hasAudio: " + body);
            if (!body.contains("Denied")) {
                result = true;
            }
        } catch (IOException e) {
            Log.e("MediaDownloader", "Error checking for audio: " + e.getMessage());
        }
        return result;
    }

    private static File mux(String redditFallbackURL, String fileName) throws IOException, JSONException {
        ContextWrapper contextWrapper = new ContextWrapper(context);
        File directory = contextWrapper.getExternalFilesDir(null);
        File videoFile;
        File audioFile = null;
        File output = new File(directory + "/" + fileName + ".mp4");
        output.createNewFile();
        videoFile = downloadMedia(redditFallbackURL, "video", ".mp4");
        try {
            MediaMuxer muxer = new MediaMuxer(directory + "/" + fileName + ".mp4", MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            // ... (rest of the existing code)

            muxer.stop();
            muxer.release();
        } catch (IOException e) {
            Log.e("MediaDownloader", "Error during muxing: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Clean up resources if needed
            if (videoFile != null && videoFile.exists()) {
                videoFile.delete();
            }
            if (audioFile != null && audioFile.exists()) {
                audioFile.delete();
            }
        }

        return output;
    }


}