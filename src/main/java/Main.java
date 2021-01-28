import com.google.gson.Gson;
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
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException, SAXException, ParserConfigurationException {
        int check_in_minutes = Integer.parseInt(getConfig("check_in_minutes"));

        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    List<String> activeBranchNames = getReviews().stream().map((s) -> s.getDisplayText().substring(s.getDisplayText().indexOf(".") + 2, s.getDisplayText().length() - 1)).collect(Collectors.toList());
                    List<String> tomcatWebapps = listTomcatApps();

                    if (tomcatWebapps.size() != 0) {
                        for (String appName : tomcatWebapps) {
                            if (!activeBranchNames.contains(appName)) {
                                shutdownTomcatApp(appName);
                                deleteTomcatApp(appName);
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
        String ccolab_url = getConfig("ccolaborator_url") + "/services/json/v1";
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
        String ccolab_url = getConfig("ccolaborator_url") + "/services/json/v1";
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

    public static void shutdownTomcatApp(String appName) {
        try {
            Process p = Runtime.getRuntime().exec("curl --user " + getConfig("tomcat_user_login") + ":" + getConfig("tomcat_user_password") + " " + getConfig("tomcat_url") + "/manager/text/stop?path=/" + appName);
            InputStream is = p.getInputStream();

            BufferedReader read = new BufferedReader(new InputStreamReader(is));

            read
                    .lines()
                    .forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void deleteTomcatApp(String appName) {
        try {
            Process p = Runtime.getRuntime().exec("curl --user " + getConfig("tomcat_user_login") + ":" + getConfig("tomcat_user_password") + " " + getConfig("tomcat_url") + "/manager/text/undeploy?path=/" + appName);
            InputStream is = p.getInputStream();

            BufferedReader read = new BufferedReader(new InputStreamReader(is));

            read
                    .lines()
                    .forEach(System.out::println);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> listTomcatApps() {
        List<String> appsExclude = Arrays.asList("ROOT", "manager", "docs");
        List<String> result = new ArrayList<>();

        try {
            Process p = Runtime.getRuntime().exec("curl --user " + getConfig("tomcat_user_login") + ":" + getConfig("tomcat_user_password") + " " + getConfig("tomcat_url") + "/manager/text/list");
            InputStream is = p.getInputStream();

            BufferedReader read = new BufferedReader(new InputStreamReader(is));

            read
                    .lines()
                    .forEach(line -> {
                        if (line.contains(":")) {
                            String current = line.substring(line.lastIndexOf(":") + 1);
                            if (!appsExclude.contains(current)) {
                                result.add(current);
                            }
                        }
                    });
            read.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }
}
