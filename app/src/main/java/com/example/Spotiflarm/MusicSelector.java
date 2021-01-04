package com.example.Spotiflarm;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.mappers.JsonObject;
import com.spotify.sdk.android.auth.AuthorizationClient;
import com.spotify.sdk.android.auth.AuthorizationRequest;
import com.spotify.sdk.android.auth.AuthorizationResponse;
import com.squareup.picasso.Picasso;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.Layout;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MusicSelector extends AppCompatActivity {

    private static final int REQUEST_CODE = 1337;
    String accToken;
    private static SpotifyAppRemote mSpotifyAppRemote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_music_selector);

        // Get auth token to access spotify Web API
        getAuthToken();

    }

    private void requestUserLibraryTracks(){


        // set label for list
        TextView musicListLabel = findViewById(R.id.musicListLabel);
        musicListLabel.setText(R.string.list_user_lib);

        // where to add our spotify resources for user to see
        LinearLayout verticalLayout = findViewById(R.id.verticalLayout);
        int heightEntry = verticalLayout.getHeight()/10;



        // pass base url and access token
        JsonObjectRequestBuilder builder = new JsonObjectRequestBuilder("https://api.spotify.com/v1/me/tracks",accToken);

        builder.setOnResponse(new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {

                // add each track to the verticalLayout as a button

                try {
                    JSONArray items = response.getJSONArray("items");
                    for(int i = 0; i < items.length(); i ++){
                        JSONObject item = items.getJSONObject(i);
                        JSONObject track = item.getJSONObject("track");
                        String trackname = track.getString("name");
                        JSONArray artists = track.getJSONArray("artists");

                        String uri = track.getString("uri");
                        JSONObject album = track.getJSONObject("album");
                        JSONArray images = album.getJSONArray("images");
                        JSONObject image;
                        String imageURL = "";

                        // get the first image only
                        if(images.length() > 0){
                            image = images.getJSONObject(0);
                            imageURL = image.getString("url");
                        }


                        // horizontal entry for this track
                        LinearLayout entry = new LinearLayout(getApplicationContext());
                        entry.setOrientation(LinearLayout.HORIZONTAL);
                        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightEntry);
                        layoutParams.setMargins(20 , 20, 20, 20);
                        entry.setLayoutParams(layoutParams);


                        // create text for track title
                        TextView textview = new TextView(getApplicationContext());
                        textview.setText(trackname);
                        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        layoutParams.setMargins(0, 10, 0, 10);
                        textview.setLayoutParams(layoutParams);
                        textview.setTextColor(Color.WHITE);

                        // autoscroll feature - if the text is longer than the length of textview
                        textview.setHorizontallyScrolling(true);
                        textview.setSingleLine();
                        textview.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                        textview.setMarqueeRepeatLimit(-1);
                        textview.setSelected(true);

                        // create text for track artists
                        TextView artisttextview = new TextView(getApplicationContext());
                        String artistString = "";
                        for(int j = 0; j < artists.length(); j++){
                            if(j > 0){
                                artistString += ", ";
                            }

                            artistString += artists.getJSONObject(j).getString("name");

                        }

                        artisttextview.setText(artistString);
                        artisttextview.setLayoutParams(layoutParams);
                        artisttextview.setTextColor(Color.WHITE);

                        // autoscroll feature - if the text is longer than the length of textview
                        artisttextview.setHorizontallyScrolling(true);
                        artisttextview.setSingleLine();
                        artisttextview.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                        artisttextview.setMarqueeRepeatLimit(-1);
                        artisttextview.setSelected(true);

                        // to vertically place track-name and artist-name textviews next to the album photo
                        LinearLayout innerlayout = new LinearLayout(getApplicationContext());
                        innerlayout.setOrientation(LinearLayout.VERTICAL);
                        layoutParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,LinearLayout.LayoutParams.MATCH_PARENT);
                        layoutParams.setMargins(20, 0, 0, 0);
                        innerlayout.setLayoutParams(layoutParams);

                        innerlayout.addView(textview);
                        innerlayout.addView(artisttextview);

                        ImageView imageView = new ImageView(getApplicationContext());
                        imageView.setAdjustViewBounds(true);
                        imageView.setMaxWidth(320);

                        Picasso.get().load(imageURL).fit().placeholder(R.drawable.ic_launcher_foreground).error(R.drawable.ic_launcher_foreground).into(imageView);

                        entry.addView(imageView);
                        entry.addView(innerlayout);

                        // on click function - play the track
                        entry.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {

                                //update alarm object in MainActivity
                                MainActivity.mainActivityInstance.alarmToChange.spotify_res_uri = uri;
                                MainActivity.mainActivityInstance.alarmToChange.spotify_res_name = trackname;
                                //update text of button
                                MainActivity.mainActivityInstance.musicButtonToChange.setText(trackname);

                                // play the resource
                                if(mSpotifyAppRemote == null || !mSpotifyAppRemote.isConnected()){
                                    connectAppRemote(uri);
                                }
                                else{
                                    mSpotifyAppRemote.getPlayerApi().play(uri);
                                }
                            }
                        });

                        // add horiz layout to vert layout
                        verticalLayout.addView(entry);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });

        builder.setOnErrorResponse(new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });

        builder.addParam("limit", "11");
        JsonObjectRequest req = builder.build();
        MainActivity.queue.add(req);


    }


    private void sendSpotifyRequest(){

    }

    private void connectAppRemote(String uri){

        ConnectionParams connectionParams = new ConnectionParams.Builder(MainActivity.CLIENT_ID)
                .setRedirectUri(MainActivity.REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.connect(this, connectionParams,
                // Define anonymous listener
                new Connector.ConnectionListener() {
                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        // Now you can start interacting with App Remote
                        mSpotifyAppRemote.getPlayerApi().play(uri);
                    }
                    @Override
                    public void onFailure(Throwable throwable) {
                        // Something went wrong when attempting to connect! Handle errors here
                        throwable.printStackTrace();
                    }
                }
        );
    }


    // Call when user needs auth token
    private void getAuthToken(){
        AuthorizationRequest.Builder builder;
        AuthorizationRequest request;

        builder = new AuthorizationRequest.Builder(MainActivity.CLIENT_ID, AuthorizationResponse.Type.TOKEN, MainActivity.REDIRECT_URI);

        // Auth token should allow us to do all of this:
        builder.setScopes(new String[]{"user-top-read","playlist-read-private","user-read-private",
                "user-read-recently-played","user-library-read"});
        request = builder.build();

        // Open login activity for user to login
        AuthorizationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    // Called when Spotify authorization has a result
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // Check if result comes from the correct activity
        if (requestCode == REQUEST_CODE) {
            AuthorizationResponse response = AuthorizationClient.getResponse(resultCode, intent);

            switch (response.getType()) {
                // Response was successful and contains auth token
                case TOKEN:
                    // Handle successful response
                    this.accToken = response.getAccessToken();
                    // load user's library tracks by default
                    requestUserLibraryTracks();
                    break;

                // Auth flow returned an error
                case ERROR:
                    // Handle error response
                    System.err.println(response.getError());
                    break;

                // Most likely auth flow was cancelled
                default:
                    // Handle other cases
                    System.err.println("Auth flow was cancelled");
            }
        }
    }


    @Override
    // When app closes
    protected void onStop() {
        super.onStop();

        // stop playing music (if playing) and disconnect remote
        mSpotifyAppRemote.getPlayerApi().pause();
        SpotifyAppRemote.disconnect(mSpotifyAppRemote);

    }

}