package es.alsanchez.goearautocomplete;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class GoearAutocomplete
{
    private static final String GOOGLE_SERVICE_URL_TEMPLATE = "http://clients1.google.com/complete/search?q=%s&nolabels=t&client=youtube&ds=yt";
    private static final String GOEAR_SERVICE_URL = "http://www.goear.com/action/suggest/sounds";

    public static String[] search(String query) throws IOException, JSONException
    {
        if("".equals(query))
        {
            return new String[0];
        }

        final String[] googleAutocompleteMatches = getGoogleAutocompleteMatches(query);
        return getGoearAutocompleteMatches(query, googleAutocompleteMatches);
    }

    private static String[] getGoogleAutocompleteMatches(String query) throws IOException, JSONException
    {
        // Perform the request to the Google service
        final String url = String.format(GOOGLE_SERVICE_URL_TEMPLATE, query);
        final HttpClient client = new DefaultHttpClient();
        final HttpGet request = new HttpGet(url);

        // Retrieve the response
        final HttpResponse response = client.execute(request);
        String responseString = streamToString(response.getEntity().getContent());

        // Strip the JSONP function call from the result
        responseString = responseString.substring(responseString.indexOf('(') + 1);
        responseString = responseString.substring(0, responseString.length() - 2);

        // Parse the result as a JSON array and collect the matches
        final List<String> matches = new ArrayList<String>();
        JSONArray jsonResponse = new JSONArray(responseString);
        jsonResponse = jsonResponse.getJSONArray(1);
        for(int i=0;i<jsonResponse.length();i++)
        {
            final String match = jsonResponse.getJSONArray(i).getString(0);
            matches.add(match);
        }

        // Return the found matches
        return matches.toArray(new String[matches.size()]);
    }

    private static String[] getGoearAutocompleteMatches(String query, String[] allMatches) throws IOException, JSONException
    {
        // Perform the request
        final HttpClient client = new DefaultHttpClient();
        final HttpPost request = new HttpPost(GOEAR_SERVICE_URL);
        request.setHeader(new BasicHeader("Content-Type", "application/x-www-form-urlencoded"));
        request.setEntity(new StringEntity(buildPostData(allMatches, query)));

        // Retrieve the response
        final HttpResponse response = client.execute(request);
        final String responseString = streamToString(response.getEntity().getContent());

        // Parse the response and return the results
        return parseGoearMatches(responseString);
    }

    private static String buildPostData(String[] strings, String query)
            throws JSONException
    {
        final StringBuilder builder = new StringBuilder();
        appendUrlParameter(builder, "data[]", query);

        for(int i=0;i<strings.length;i++)
        {
            final String result = strings[i];
            appendUrlParameter(builder, String.format("data[1][%s][]", i), result);
            appendUrlParameter(builder, String.format("data[1][%s][]", i), "0");
        }

        appendUrlParameter(builder, "data[]", query);
        return builder.toString();
    }

    private static String[] parseGoearMatches(String html)
    {
        final List<String> matches = new ArrayList<String>();

        try
        {
            final Pattern regex = Pattern.compile(">([^<]+)</a>", Pattern.MULTILINE);
            final Matcher regexMatcher = regex.matcher(html);
            while (regexMatcher.find())
            {
                for (int i = 1; i <= regexMatcher.groupCount(); i++)
                {
                    matches.add(regexMatcher.group(i));
                }
            }
        }
        catch (PatternSyntaxException ignored)
        {

        }

        return matches.toArray(new String[matches.size()]);
    }

    private static void appendUrlParameter(StringBuilder builder, String name, String value)
    {
        builder.append("&")
                .append(URLEncoder.encode(name))
                .append("=")
                .append(URLEncoder.encode(value));
    }

    private static String streamToString(InputStream stream) throws IOException
    {
        BufferedReader reader = null;

        try
        {
            final StringBuilder builder = new StringBuilder();

            String line;
            reader = new BufferedReader(new InputStreamReader(stream));
            while((line = reader.readLine()) != null)
            {
                builder.append(line).append("\n");
            }
            return builder.toString();
        }
        finally
        {
            if(reader != null)
            {
                reader.close();
            }
        }
    }
}
