import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        String path_to_tomcat_webapps = getConfig("path_to_tomcat_webapps");
        int check_in_minutes = Integer.parseInt(getConfig("check_in_minutes"));

        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    List<String> branchNames = getReviews().stream().map((s) -> s.getDisplayText().substring(s.getDisplayText().indexOf(".") + 1)).collect(Collectors.toList());

                    File dir = new File(path_to_tomcat_webapps);
                    File[] warFiles = dir.listFiles((d, name) -> name.endsWith(".war"));

                    if (warFiles != null) {
                        for (File file : warFiles) {
                            String fileName = file.getName();
                            String warName = fileName.substring(0, fileName.indexOf("."));

                            if (!branchNames.contains(fileName)) {
                                if (file.delete()) {
                                    FileUtils.deleteDirectory(new File(path_to_tomcat_webapps + "\\" + warName));
                                    System.out.println("SUCCESSFUL! war. file named " + fileName + " deleted. Directory " + warName + " deleted succesfully");
                                } else {
                                    System.out.println("CANT DELETE " + fileName);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };

        Timer timer = new Timer("Timer");

        timer.scheduleAtFixedRate(task, 0, check_in_minutes * 60000);
    }

    public static List<Review> getReviews() throws IOException, ParserConfigurationException, SAXException {
        String ccolab_url = getConfig("ccolaborator_url");
        String ccolaborator_login = getConfig("ccolaborator_username");

        URL url = new URL(ccolab_url);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);

        byte[] out = ("[{\"command\":\"SessionService.authenticate\",\"args\": {\"login\":\"" + ccolaborator_login + "\",\"ticket\":\"" + getCcolabTicket() + "\"}},{\"command\": \"ReviewService.findReviews\",\"args\": {\"findPlace \": 0,\"searchText\":\" \"}}]").getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(
                http.getInputStream()));

        JSONArray results = new JSONArray(in.readLine());
        JSONArray reviews = new JSONObject(results.get(1).toString()).getJSONObject("result").getJSONArray("reviews");

        List<Review> result = new ArrayList<>();

        Gson gson = new Gson();
        for (Object review : reviews) {
            Review currentReview = gson.fromJson(review.toString(), Review.class);
            if (currentReview.getReviewPhase().equals("ANNOTATING") || currentReview.getReviewPhase().equals("PLANNING")) {
                result.add(currentReview);
            }
        }

        in.close();
        return result;
    }

    public static String getCcolabTicket() throws IOException, ParserConfigurationException, SAXException {
        String ccolab_url = getConfig("ccolaborator_url");
        String ccolaborator_login = getConfig("ccolaborator_username");
        String ccolaborator_password = getConfig("ccolaborator_password");

        URL url = new URL(ccolab_url);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);
        byte[] out = ("[{\"command\":\"SessionService.getLoginTicket\",\"args\": {\"login\":\"" + ccolaborator_login + "\",\"password\":\"" + ccolaborator_password + "\"}}]").getBytes(StandardCharsets.UTF_8);
        int length = out.length;

        http.setFixedLengthStreamingMode(length);
        http.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        http.connect();
        try (OutputStream os = http.getOutputStream()) {
            os.write(out);
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(
                http.getInputStream()));

        JSONObject jsonObj = new JSONObject(in.readLine().replace("[", "").replace("]", ""));
        in.close();
        return jsonObj.getJSONObject("result").get("loginTicket").toString();
    }

    public static String getConfig(String parameter) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File("config.xml"));
        return document.getElementsByTagName(parameter).item(0).getTextContent();
    }
}
