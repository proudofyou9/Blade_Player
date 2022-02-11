package v.blade.library;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;
import v.blade.ui.LibraryFragment;

public class Library
{
    private static final String LIBRARY_FILE = "/library.json";
    private static final int LIBRARY_CACHE_VERSION = 1;

    /*
     * In order to update the library, we need to add 'objects' from every source
     * For that, we have to find when the names are the same -> same object
     * HashMaps allows that in constant time, much better than O(n) on a list
     * However, in the end we will need a list ; see below the sorted lists, generated
     * at the end of library modification
     */
    private static HashMap<String, Artist> library_artists = new HashMap<>();
    private static HashMap<String, Album> library_albums = new HashMap<>();
    private static HashMap<String, Song> library_songs = new HashMap<>();
    private static ArrayList<Playlist> library_playlists = new ArrayList<>();

    /*
     * 'Handled' : handles are 'objects' that are not in the library, but that we need in RAM
     * For example, songs inside of playlists that are not in the library, or when
     * doing a web search
     */
    private static HashMap<String, Artist> handled_artists = new HashMap<>();
    private static HashMap<String, Album> handled_albums = new HashMap<>();
    private static HashMap<String, Song> handled_songs = new HashMap<>();

    /* Those are *sorted* lists, generated by sorting the content of HashMaps */
    private static ArrayList<Artist> artists_list = new ArrayList<>();
    private static ArrayList<Album> albums_list = new ArrayList<>();
    private static ArrayList<Song> songs_list = new ArrayList<>();

    public static List<Artist> getArtists()
    {
        return artists_list;
    }

    public static List<Album> getAlbums()
    {
        return albums_list;
    }

    public static List<Song> getSongs()
    {
        return songs_list;
    }

    public static List<Playlist> getPlaylists()
    {
        return library_playlists;
    }

    public static synchronized Song addSong(String title, String album, String[] artists, Source source, Object sourceId,
                                            String[] albumArtists, String albumMiniatureURL, int track_number, String[] artistMiniaturesUrl,
                                            String[] albumArtistsMiniatureUrl, String albumImageURL, int albumImageLevel)
    {
        /* obtain song artists and album artists */
        Artist[] sartists = new Artist[artists.length];
        for(int i = 0; i < sartists.length; i++)
        {
            Artist current = library_artists.get(artists[i].toLowerCase());

            if(current == null)
            {
                current = new Artist(artists[i], artistMiniaturesUrl[i]);
                library_artists.put(current.name.toLowerCase(), current);
            }

            sartists[i] = current;
        }

        Artist[] saartists = new Artist[albumArtists.length];
        for(int i = 0; i < saartists.length; i++)
        {
            Artist current = library_artists.get(albumArtists[i].toLowerCase());

            if(current == null)
            {
                current = new Artist(albumArtists[i], albumArtistsMiniatureUrl[i]);
                library_artists.put(current.name.toLowerCase(), current);
            }

            saartists[i] = current;
        }

        /* obtain song album */
        //noinspection ConstantConditions
        Album salbum = library_albums.get(((albumArtists == null || albumArtists[0] == null) ? "null" : albumArtists[0].toLowerCase()) + ":" + album.toLowerCase());
        if(salbum == null)
        {
            salbum = new Album(album, saartists, albumMiniatureURL, albumImageURL, albumImageLevel);
            //noinspection ConstantConditions
            library_albums.put(((albumArtists == null || albumArtists[0] == null) ? "null" : albumArtists[0].toLowerCase()) + ":" + album.toLowerCase(), salbum);
            for(Artist a : saartists) a.addAlbum(salbum);
        }
        else
        {
            //Add image if image level inferior
            salbum.setImage(albumMiniatureURL, albumImageURL, albumImageLevel);
        }
        for(Artist a : sartists)
            if(!a.albums.contains(salbum))
                a.addAlbum(salbum); //NOTE: this adds albums to artists even if only a featuring

        /* obtain song */
        Song s = library_songs.get(artists[0].toLowerCase() + ":" + album.toLowerCase() + ":" + title.toLowerCase());
        if(s == null)
        {
            s = new Song(title, salbum, sartists, track_number);
            library_songs.put(artists[0].toLowerCase() + ":" + album.toLowerCase() + ":" + title.toLowerCase(), s);
            for(Artist a : sartists) a.track_count++;
            salbum.addSong(s);
        }

        /* update song source information */
        boolean alreadyContainsSource = false;
        for(SourceInformation si : s.getSources())
        {
            if(si.source == source)
            {
                alreadyContainsSource = true;
                break;
            }
        }
        if(!alreadyContainsSource)
            s.addSource(source, sourceId, false);

        return s;
    }

    public static synchronized Song addSongHandle(String title, String album, String[] artists, Source source, Object sourceId,
                                                  String[] albumArtists, String albumMiniatureURL, int track_number, String[] artistMiniaturesUrl,
                                                  String[] albumArtistsMiniatureUrl, String albumImageURL, int albumImageLevel)
    {
        /* obtain song artists and album artists */
        Artist[] sartists = new Artist[artists.length];
        for(int i = 0; i < sartists.length; i++)
        {
            Artist current = library_artists.get(artists[i].toLowerCase());
            if(current == null) current = handled_artists.get(artists[i].toLowerCase());

            if(current == null)
            {
                current = new Artist(artists[i], artistMiniaturesUrl[i]);
                handled_artists.put(current.name.toLowerCase(), current);
            }

            sartists[i] = current;
        }

        Artist[] saartists = new Artist[albumArtists.length];
        for(int i = 0; i < saartists.length; i++)
        {
            Artist current = library_artists.get(albumArtists[i].toLowerCase());

            if(current == null) current = handled_artists.get(albumArtists[i].toLowerCase());

            if(current == null)
            {
                current = new Artist(albumArtists[i], albumArtistsMiniatureUrl[i]);
                handled_artists.put(current.name.toLowerCase(), current);
            }

            saartists[i] = current;
        }

        /* obtain song album */
        //noinspection ConstantConditions
        Album salbum = library_albums.get(((albumArtists == null || albumArtists[0] == null) ? "null" : albumArtists[0].toLowerCase()) + ":" + album.toLowerCase());
        if(salbum == null)
            //noinspection ConstantConditions
            salbum = handled_albums.get(((albumArtists == null || albumArtists[0] == null) ? "null" : albumArtists[0].toLowerCase()) + ":" + album.toLowerCase());
        else
        {
            //Add image if image level inferior
            salbum.setImage(albumMiniatureURL, albumImageURL, albumImageLevel);
        }
        if(salbum == null)
        {
            salbum = new Album(album, saartists, albumMiniatureURL, albumImageURL, albumImageLevel);
            //noinspection ConstantConditions
            handled_albums.put(((albumArtists == null || albumArtists[0] == null) ? "null" : albumArtists[0].toLowerCase()) + ":" + album.toLowerCase(), salbum);
        }

        /* obtain song */
        Song s = library_songs.get(artists[0].toLowerCase() + ":" + album.toLowerCase() + ":" + title.toLowerCase());
        if(s == null)
            s = handled_songs.get(artists[0].toLowerCase() + ":" + album.toLowerCase() + ":" + title.toLowerCase());
        if(s == null)
        {
            s = new Song(title, salbum, sartists, track_number);
            handled_songs.put(artists[0].toLowerCase() + ":" + album.toLowerCase() + ":" + title.toLowerCase(), s);
        }

        /* update song source information */
        boolean alreadyContainsSource = false;
        for(SourceInformation si : s.getSources())
        {
            if(si.source == source)
            {
                alreadyContainsSource = true;
                break;
            }
        }
        if(!alreadyContainsSource)
            s.addSource(source, sourceId, true);

        return s;
    }

    public static void addSongFromHandle(Song song)
    {
        /* obtain song artists and album artists */
        Artist[] sartists = new Artist[song.artists.length];
        for(int i = 0; i < sartists.length; i++)
        {
            Artist current = library_artists.get(song.artists[i].name.toLowerCase());

            if(current == null)
            {
                current = new Artist(song.artists[i].name, song.artists[i].imageStr);
                library_artists.put(current.name.toLowerCase(), current);
            }

            sartists[i] = current;
        }

        Artist[] saartists = new Artist[song.album.artists.length];
        for(int i = 0; i < saartists.length; i++)
        {
            Artist current = library_artists.get(song.album.artists[i].name.toLowerCase());

            if(current == null)
            {
                current = new Artist(song.album.artists[i].name, song.album.artists[i].imageStr);
                library_artists.put(current.name.toLowerCase(), current);
            }

            saartists[i] = current;
        }

        /* obtain song album */
        Album salbum = library_albums.get(((song.album.artists == null || song.album.artists[0] == null) ? "null" : song.album.artists[0].name.toLowerCase()) + ":" + song.album.getName().toLowerCase());
        if(salbum == null)
        {
            salbum = new Album(song.album.name, saartists, song.album.imageStr, song.album.imageBigStr, song.album.imageLevel);
            library_albums.put(((song.album.artists == null || song.album.artists[0] == null) ? "null" : song.album.artists[0].name.toLowerCase()) + ":" + song.album.getName().toLowerCase(), salbum);
            for(Artist a : saartists) a.addAlbum(salbum);
        }
        for(Artist a : sartists)
            if(!a.albums.contains(salbum))
                a.addAlbum(salbum); //NOTE: this adds albums to artists even if only a featuring

        /* obtain song */
        Song s = library_songs.get(song.artists[0].name.toLowerCase() + ":" + song.album.getName().toLowerCase() + ":" + song.getName().toLowerCase());
        if(s == null)
        {
            library_songs.put(song.artists[0].name.toLowerCase() + ":" + song.album.getName().toLowerCase() + ":" + song.getName().toLowerCase(), song);
            for(Artist a : sartists) a.track_count++;
            salbum.addSong(song);
        }
    }

    public static synchronized void removeSong(Song song)
    {
        //Handle artist
        for(Artist a : song.getArtists())
        {
            a.track_count--;
            if(a.track_count == 0)
                library_artists.remove(a.getName().toLowerCase());
        }

        //Handle album
        song.getAlbum().getSongs().remove(song);
        if(song.getAlbum().getSongs().isEmpty())
        {
            for(Artist a : song.getAlbum().getArtists())
            {
                a.getAlbums().remove(song.getAlbum());
                if(a.getAlbums().isEmpty())
                    library_artists.remove(a.getName().toLowerCase());
            }
            library_albums.remove(song.getAlbum().getArtists()[0].getName().toLowerCase() + ":" + song.getAlbum().getName().toLowerCase());
        }

        //Handle song
        library_songs.remove(song.getArtists()[0].getName().toLowerCase() + ":" + song.getAlbum().getName().toLowerCase() + ":" + song.getName().toLowerCase());
    }

    public static synchronized Playlist addPlaylist(String title, List<Song> songList, String imageMiniatureUrl, String subtitle, Source source, Object id)
    {
        Playlist playlist = new Playlist(title, songList, imageMiniatureUrl, subtitle, new SourceInformation(source, id, false));
        library_playlists.add(playlist);
        return playlist;
    }

    public static synchronized void removePlaylist(Playlist list)
    {
        library_playlists.remove(list);
    }

    /**
     * Reset the playlist, to be used before a library synchronization
     */
    public static void reset()
    {
        library_artists = new HashMap<>();
        library_albums = new HashMap<>();
        library_songs = new HashMap<>();
        library_playlists = new ArrayList<>();

        handled_artists = new HashMap<>();
        handled_albums = new HashMap<>();
        handled_songs = new HashMap<>();
    }

    /**
     * Generate artists, albums, and songs lists from library HashMaps
     */
    @SuppressWarnings("ComparatorCombinators")
    //NOTE : can be replaced with Comparator.comparing, but needs Android N
    public static void generateLists()
    {
        //re-gen lists from hashmaps
        artists_list = new ArrayList<>(library_artists.values());
        Collections.sort(artists_list, (artist, t1) -> artist.getName().toLowerCase().compareTo(t1.getName().toLowerCase()));
        albums_list = new ArrayList<>(library_albums.values());
        Collections.sort(albums_list, (album, t1) -> album.getName().toLowerCase().compareTo(t1.getName().toLowerCase()));
        songs_list = new ArrayList<>(library_songs.values());
        Collections.sort(songs_list, (song, t1) -> song.getName().toLowerCase().compareTo(t1.getName().toLowerCase()));

        //sort playlists alphabetically
        Collections.sort(library_playlists, (playlist, t1) -> playlist.getName().toLowerCase().compareTo(t1.getName().toLowerCase()));

        //NotifyDatasetChanged for mainListView actualization
        if(LibraryFragment.instance != null && LibraryFragment.instance.getActivity() != null)
            LibraryFragment.instance.requireActivity().runOnUiThread(() ->
                    LibraryFragment.instance.updateContent(LibraryFragment.instance.getTitle(), null, LibraryFragment.CURRENT_TYPE.LIBRARY, null));

        for(Album album : albums_list)
            Collections.sort(album.songList, (o1, o2) -> o1.track_number - o2.track_number);
    }

    /**
     * Saves the whole library to library json file
     */
    public static void save()
    {
        //Generate JSON Object from library
        Gson gson = new Gson();

        JsonObject libraryObject = new JsonObject();
        libraryObject.addProperty("version", LIBRARY_CACHE_VERSION);

        //Save library songs
        JsonArray library = new JsonArray();
        for(Song s : songs_list)
        {
            JsonObject songJson = songJson(s, gson);
            library.add(songJson);
        }
        libraryObject.add("library", library);

        //Save library playlists
        JsonArray playlists = new JsonArray();
        for(Playlist playlist : library_playlists)
        {
            JsonObject playlistJson = new JsonObject();

            playlistJson.addProperty("name", playlist.getName());
            playlistJson.addProperty("art", playlist.imageStr);
            playlistJson.addProperty("subtitle", playlist.getSubtitle());

            JsonArray playlistSongs = new JsonArray();
            for(Song s : playlist.getSongs()) playlistSongs.add(songJson(s, gson));
            playlistJson.add("songs", playlistSongs);

            playlistJson.addProperty("source", playlist.getSource().source.getIndex());
            playlistJson.add("id", gson.toJsonTree(playlist.getSource().id));

            playlists.add(playlistJson);
        }
        libraryObject.add("playlists", playlists);

        //Write JSON Object to file
        File libraryFile = new File(BladeApplication.appContext.getFilesDir().getAbsolutePath() + LIBRARY_FILE);
        try
        {
            //noinspection ResultOfMethodCallIgnored
            libraryFile.delete();
            if(!libraryFile.createNewFile())
            {
                System.err.println("Could not save library : could not create file");
                return;
            }

            BufferedWriter writer = new BufferedWriter(new FileWriter(libraryFile));
            writer.write(gson.toJson(libraryObject));
            writer.close();
        }
        catch(IOException ignored)
        {
        }
    }

    private static JsonObject songJson(Song s, Gson gson)
    {
        JsonObject songJson = new JsonObject();
        songJson.addProperty("name", s.getName());
        songJson.addProperty("track_number", s.getTrackNumber());
        songJson.addProperty("album", s.getAlbum().getName());

        songJson.addProperty("album_art", s.getAlbum().imageStr);
        songJson.addProperty("album_art_big", s.getAlbum().imageBigStr);
        JsonArray aartists = new JsonArray();
        for(Artist a : s.getAlbum().getArtists()) aartists.add(a.getName());
        songJson.add("album_artists", aartists);
        //TODO : save album_art, album_art_big, album artists only on first album encounter

        //TODO : handle artists imgs
        JsonArray artists = new JsonArray();
        for(Artist a : s.getArtists()) artists.add(a.getName());
        songJson.add("artists", artists);

        JsonArray sources = new JsonArray();
        for(SourceInformation si : s.getSources())
        {
            JsonObject sourceJson = new JsonObject();
            sourceJson.addProperty("source", si.source.getIndex());
            sourceJson.add("id", gson.toJsonTree(si.id));
            sources.add(sourceJson);
        }
        songJson.add("sources", sources);

        return songJson;
    }

    /**
     * Loads the whole library from cache json file
     */
    public static void loadFromCache()
    {
        File libraryFile = new File(BladeApplication.appContext.getFilesDir().getAbsolutePath() + LIBRARY_FILE);
        try
        {
            //read file
            BufferedReader reader = new BufferedReader(new FileReader(libraryFile));
            StringBuilder js = new StringBuilder();
            while(reader.ready()) js.append(reader.readLine()).append("\n");
            reader.close();

            //obtain from JSON
            JSONObject root = new JSONObject(js.toString());
            int cache_version = root.getInt("version");
            if(cache_version > LIBRARY_CACHE_VERSION)
            {
                System.err.println("Library cache more recent than current version; ignoring");
                return;
            }

            //Restore library songs
            JSONArray library = root.getJSONArray("library");
            for(int i = 0; i < library.length(); i++)
            {
                JSONObject s = library.getJSONObject(i);
                jsonSong(s, false);
            }

            //Restore playlists
            JSONArray playlists = root.getJSONArray("playlists");
            for(int i = 0; i < playlists.length(); i++)
            {
                JSONObject p = playlists.getJSONObject(i);

                ArrayList<Song> songList = new ArrayList<>();
                JSONArray songArray = p.getJSONArray("songs");
                for(int j = 0; j < songArray.length(); j++)
                    songList.add(jsonSong(songArray.getJSONObject(j), true));

                String art;
                try
                {
                    art = p.getString("art");
                }
                catch(JSONException noArt)
                {
                    art = null;
                }
                String subtitle;
                try
                {
                    subtitle = p.getString("subtitle");
                }
                catch(JSONException noSubtitle)
                {
                    subtitle = "";
                }

                addPlaylist(p.getString("name"), songList, art, subtitle,
                        Source.SOURCES.get(p.getInt("source")),
                        p.get("id"));
            }

            Library.generateLists();
        }
        catch(IOException | JSONException e)
        {
            e.printStackTrace();
        }
    }

    private static Song jsonSong(JSONObject s, boolean handled) throws JSONException
    {
        JSONArray artistsJson = s.getJSONArray("artists");
        String[] artists = new String[artistsJson.length()];
        String[] artistsImages = new String[artistsJson.length()];
        for(int j = 0; j < artistsJson.length(); j++) artists[j] = artistsJson.getString(j);

        JSONArray aartistsJson = s.getJSONArray("album_artists");
        String[] aartists = new String[aartistsJson.length()];
        String[] aartistsImages = new String[aartistsJson.length()];
        for(int j = 0; j < aartistsJson.length(); j++)
            aartists[j] = aartistsJson.getString(j);

        JSONArray sourcesJson = s.getJSONArray("sources");
        JSONObject source0Json = sourcesJson.getJSONObject(0);
        Source source0 = null;
        try
        {
            source0 = Source.SOURCES.get(source0Json.getInt("source"));
        }
        catch(IndexOutOfBoundsException exception)
        {
            System.out.println("BLADE: Song saved with a source that does not exist, skipping this source");
        }

        String art;
        String bigArt;
        try
        {
            art = s.getString("album_art");
            bigArt = s.getString("album_art_big");
        }
        catch(JSONException noArt)
        {
            art = null;
            bigArt = null;
        }

        Song song;
        if(handled)
            song = addSongHandle(s.getString("name"), s.getString("album"), artists, source0,
                    source0Json.get("id"), aartists, art, s.getInt("track_number"),
                    artistsImages, aartistsImages, bigArt, 1);
        else
            song = addSong(s.getString("name"), s.getString("album"), artists, source0,
                    source0Json.get("id"), aartists, art, s.getInt("track_number"),
                    artistsImages, aartistsImages, bigArt, 1);

        //Add all other sources to song
        for(int j = 1; j < sourcesJson.length(); j++)
        {
            JSONObject sourceJson = sourcesJson.getJSONObject(j);
            Source source = Source.SOURCES.get(sourceJson.getInt("source"));
            song.addSource(source, sourceJson.get("id"), handled);
        }

        return song;
    }

    public static List<LibraryObject> search(String query)
    {
        //TODO : improve search, for now we just do "contains", cringe
        ArrayList<LibraryObject> result = new ArrayList<>();

        //Add separator
        result.add(new Separator(BladeApplication.appContext.getString(R.string.songs)));

        //Add songs from library and handles
        for(Song s : songs_list)
            if(s.getName().toLowerCase().contains(query.toLowerCase()))
                result.add(s);

        for(Song s : handled_songs.values())
            if(s.getName().toLowerCase().contains(query.toLowerCase()) && !result.contains(s))
                result.add(s);

        //Add separator
        result.add(new Separator(BladeApplication.appContext.getString(R.string.albums)));

        //Add albums
        for(Album a : albums_list)
            if(a.getName().toLowerCase().contains(query.toLowerCase()))
                result.add(a);

        //Add separator
        result.add(new Separator(BladeApplication.appContext.getString(R.string.artists)));

        //Add artists
        for(Artist a : artists_list)
            if(a.getName().toLowerCase().contains(query.toLowerCase()))
                result.add(a);

        //Add separator
        result.add(new Separator(BladeApplication.appContext.getString(R.string.playlists)));

        //Add playlists
        for(Playlist p : library_playlists)
            if(p.getName().toLowerCase().contains(query.toLowerCase()))
                result.add(p);

        return result;
    }
}
