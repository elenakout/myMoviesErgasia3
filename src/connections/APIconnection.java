package connections;

import controllers.FavoriteListJpaController;
import controllers.GenreJpaController;
import controllers.MovieJpaController;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonValue;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import models.Genre;
import models.Movie;

public class APIconnection {

    public static final String baseURL = "https://api.themoviedb.org/3/";
    public static final String api_key = "b59af98651d790c970eaec576c0be18a";
    URL genres_url;
    URL url;
    int page = 1;
    EntityManagerFactory emf = Persistence.createEntityManagerFactory("myMovies_testPU");
    GenreJpaController gjc = new GenreJpaController(emf);
    Genre genre = new Genre();
    MovieJpaController mjc = new MovieJpaController(emf);
    Movie mov = new Movie();
    FavoriteListJpaController fjc = new FavoriteListJpaController(emf);
    DateFormat formatter = new SimpleDateFormat("yyyy-mm-dd");

    public APIconnection() { 
        try {
            this.genres_url = new URL(baseURL + "genre/movie/list?api_key=" + api_key + "&language=en-US");
            this.url = new URL( baseURL + "discover/movie?api_key=" + api_key +"&language=en-US&sort_by=popularity.desc&release_date.gte=2000&with_genres=28%7C878%7C10749&page=" + page);
        } catch (MalformedURLException ex) {
            Logger.getLogger(APIconnection.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void getGenres() {
        // GET GENRES από το API
        try (InputStream is = genres_url.openStream();
                JsonReader rdr = Json.createReader(is)) {

            JsonObject obj = rdr.readObject();

            JsonArray results = obj.getJsonArray("genres");

            for (JsonObject result : results.getValuesAs(JsonObject.class)) {

                if (result.getInt("id") == 28 || result.getInt("id") == 878 || result.getInt("id") == 10749) {
                    genre.setId(result.getInt("id"));
                    genre.setName(result.getString("name", ""));
                    try {
                        gjc.create(genre);
                    } catch (Exception ex) {
                        Logger.getLogger(APIconnection.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        } catch (IOException ex) {
        }
    }
    
    public void getMovies() throws ParseException{
        for (int i = 1; i<77; i++){
            try {
                this.url = new URL( baseURL + "discover/movie?api_key=" + api_key +"&language=en-US&sort_by=popularity.desc&release_date.gte=2000&with_genres=28%7C878%7C10749&page=" + i);
            } catch (MalformedURLException ex) {
                Logger.getLogger(APIconnection.class.getName()).log(Level.SEVERE, null, ex);
            }
            try (InputStream is = url.openStream();
                JsonReader rdr = Json.createReader(is)) {

            JsonObject obj = rdr.readObject();

            JsonArray results = obj.getJsonArray("results");

            for (JsonObject result : results.getValuesAs(JsonObject.class)) {
                // Set Id
                    mov.setId(result.getInt("id"));
                    // Set Title
                    mov.setTitle(result.getString("title"));
                    // Set Rating
                     mov.setRating(Float.parseFloat(result.getJsonNumber("vote_average").toString()));
                     // Set Date
                    java.util.Date date = formatter.parse(result.getString("release_date"));
                    java.sql.Date sqlDate = new java.sql.Date(date.getTime()); 
                    mov.setReleaseDate(sqlDate); 
                    mov.setOverview(result.getString("overview"));
                    // Set Genre Id
                    mov.setGenreId(one_id(result.getJsonArray("genre_ids")));

                    try {
                        mjc.create(mov);
                    } catch (Exception ex) {
                        Logger.getLogger(APIconnection.class.getName()).log(Level.SEVERE, null, ex);
                    }
            }
        } catch (IOException ex) {
        }
            
        }
        
    }
  
    protected Genre one_id(JsonArray arr){ 
        Genre genre = new Genre();
        
        for (JsonValue arr1 : arr) {
            if ("28".equals(arr1.toString()) || "878".equals(arr1.toString())|| "10749".equals(arr1.toString())) {
              genre.setId(Integer.parseInt(arr1.toString())); 
              break;
            }
        }
        return genre;  
    }
  
    public void clearMovies(){
        mjc.clearTbl("Movie.deleteAll");
        gjc.clearTbl("Genre.deleteAll");
        fjc.clearTbl("FavoriteList.deleteAll");
    }
}// </class>

