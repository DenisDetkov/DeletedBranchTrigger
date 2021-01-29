import com.google.gson.Gson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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
    public static String ccolaborator_url;
    public static String ccolaborator_username;
    public static String ccolaborator_password;
    public static String tomcat_url;
    public static String tomcat_user_login;
    public static String tomcat_user_password;
    public static int check_in_minutes;

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        actualizeConfigs();
        TimerTask task = new TimerTask() {
            public void run() {
                try {
                    actualizeConfigs();
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

    public static List<Review> getReviews() throws IOException {
        String ccolab_url = ccolaborator_url + "/services/json/v1";

        URL url = new URL(ccolab_url);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);

        byte[] out = ("[{\"command\":\"SessionService.authenticate\",\"args\": {\"login\":\"" + ccolaborator_username + "\",\"ticket\":\"" + getCcolabTicket() + "\"}},{\"command\": \"ReviewService.findReviews\",\"args\": {\"findPlace \": 0,\"searchText\":\" \"}}]").getBytes(StandardCharsets.UTF_8);
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

    public static String getCcolabTicket() throws IOException {
        String ccolab_url = ccolaborator_url + "/services/json/v1";

        URL url = new URL(ccolab_url);
        URLConnection con = url.openConnection();
        HttpURLConnection http = (HttpURLConnection) con;
        http.setRequestMethod("POST"); // PUT is another valid option
        http.setDoOutput(true);
        byte[] out = ("[{\"command\":\"SessionService.getLoginTicket\",\"args\": {\"login\":\"" + ccolaborator_username + "\",\"password\":\"" + ccolaborator_password + "\"}}]").getBytes(StandardCharsets.UTF_8);
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

    public static void shutdownTomcatApp(String appName) {
        try {
            Process p = Runtime.getRuntime().exec("curl --user " + tomcat_user_login + ":" + tomcat_user_password + " " + tomcat_url + "/manager/text/stop?path=/" + appName);
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
            Process p = Runtime.getRuntime().exec("curl --user " + tomcat_user_login + ":" + tomcat_user_password + " " + tomcat_url + "/manager/text/undeploy?path=/" + appName);
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
            Process p = Runtime.getRuntime().exec("curl --user " + tomcat_user_login + ":" + tomcat_user_password + " " + tomcat_url + "/manager/text/list");
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

    public static void actualizeConfigs() throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new File("config.xml"));
        Node parrentNode = document.getDocumentElement();

        NodeList nodeList = parrentNode.getChildNodes();
        for (int i = 0; i < nodeList.getLength(); i++) {
            Node currentNode = nodeList.item(i);
            if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
                switch (currentNode.getNodeName()) {
                    case "ccolaborator_url":
                        ccolaborator_url = currentNode.getTextContent();
                        break;
                    case "check_in_minutes":
                        check_in_minutes = Integer.parseInt(currentNode.getTextContent());
                        break;
                    case "ccolaborator_username":
                        ccolaborator_username = currentNode.getTextContent();
                        break;
                    case "ccolaborator_password":
                        ccolaborator_password = currentNode.getTextContent();
                        break;
                    case "tomcat_url":
                        tomcat_url = currentNode.getTextContent();
                        break;
                    case "tomcat_user_login":
                        tomcat_user_login = currentNode.getTextContent();
                        break;
                    case "tomcat_user_password":
                        tomcat_user_password = currentNode.getTextContent();
                }
            }
        }
    }
}
