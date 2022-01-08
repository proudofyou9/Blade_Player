package v.blade.sources.spotify;

import com.google.gson.annotations.SerializedName;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * The Retrofit service allowing Spotify Web API access
 */
public interface SpotifyService
{
    class AlbumObject
    {
        String album_type;
        ArtistObject[] artists;
        String[] available_markets;
        CopyrightObject[] copyrights;
        ExternalIdObject externalId;
        ExternalUrlObject external_urls;
        String[] genres;
        String href;
        String id;
        ImageObject[] images;
        String label;
        String name;
        int popularity;
        String release_date;
        String release_date_precision;
        AlbumRestrictionObject restrictions;
        PagingObject<SimplifiedTrackObject> tracks;
        String type;
        String uri;
    }

    class AlbumRestrictionObject
    {
        String reason;
    }

    class ArtistObject
    {
        ExternalUrlObject external_urls;
        FollowersObject followers;
        String[] genres;
        String href;
        String id;
        ImageObject[] images;
        String name;
        int popularity;
        String type;
        String uri;
    }

    class CopyrightObject
    {
        String text;
        String type;
    }

    class ErrorObject
    {
        String message;
        int status;
    }

    class ExplicitContentSettingsObject
    {
        boolean filter_enabled;
        boolean filter_locked;
    }

    class ExternalIdObject
    {
        String ean;
        String isrc;
        String upc;
    }

    class ExternalUrlObject
    {
        String spotify;
    }

    class FollowersObject
    {
        String href;
        int total;
    }

    class ImageObject
    {
        int height;
        String url;
        int width;
    }

    class PagingObject<T>
    {
        String href;
        T[] items;
        int limit;
        String next;
        int offset;
        String previous;
        int total;
    }

    class PlaylistObject
    {
        boolean collaborative;
        String description;
        ExternalUrlObject external_urls;
        FollowersObject followers;
        String href;
        String id;
        ImageObject[] images;
        String name;
        PublicUserObject owner;
        @SerializedName("public")
        boolean is_public;
        String snapshot_id;
        PlaylistTrackObject[] tracks;
        String type;
        String uri;
    }

    class PlaylistTrackObject
    {
        String added_at; //Timestamp ?
        PublicUserObject added_by;
        boolean is_local;
        TrackObject track;
    }

    class PlaylistTracksRefObject
    {
        String href;
        int total;
    }

    class PrivateUserObject
    {
        String country;
        String display_name;
        String email;
        ExplicitContentSettingsObject explicit_content;
        ExternalUrlObject external_urls;
        FollowersObject followers;
        String href;
        String id;
        ImageObject[] images;
        String product;
        String type;
        String uri;
    }

    class PublicUserObject
    {
        String display_name;
        ExternalUrlObject external_urls;
        FollowersObject followers;
        String href;
        String id;
        ImageObject[] images;
        String type;
        String uri;
    }

    class SavedAlbumObject
    {
        String added_at; //Timestamp
        AlbumObject album;
    }

    class SavedTrackObject
    {
        String added_at; //Timestamp
        TrackObject track;
    }

    class SimplifiedAlbumObject
    {
        String album_group;
        String album_type;
        SimplifiedArtistObject[] artists;
        String[] available_markets;
        ExternalUrlObject external_urls;
        String href;
        String id;
        ImageObject[] images;
        String name;
        String release_date;
        String release_date_precision;
        AlbumRestrictionObject restrictions;
        String type;
        String uri;
    }

    class SimplifiedArtistObject
    {
        ExternalUrlObject external_urls;
        String href;
        String id;
        String name;
        String type;
        String uri;
    }

    class SimplifiedPlaylistObject
    {
        boolean collaborative;
        String description;
        ExternalUrlObject external_urls;
        String href;
        String id;
        ImageObject[] images;
        String name;
        PublicUserObject owner;
        @SerializedName("public")
        boolean is_public;
        String snapshot_id;
        PlaylistTracksRefObject tracks;
        String type;
        String uri;
    }

    class SimplifiedTrackObject
    {
        SimplifiedArtistObject[] artists;
        String[] available_markets;
        int disc_number;
        int duration_ms;
        boolean explicit;
        ExternalUrlObject external_urls;
        String href;
        String id;
        boolean is_local;
        boolean is_playable;
        //linked_from
        String name;
        String preview_url;
        TrackRestrictionObject restrictions;
        int track_number;
        String type;
        String uri;
    }

    class TrackObject
    {
        SimplifiedAlbumObject album;
        ArtistObject[] artists;
        String[] available_markets;
        int disc_number;
        int duration_ms;
        boolean explicit;
        ExternalIdObject external_ids;
        ExternalUrlObject external_urls;
        String href;
        String id;
        boolean is_local;
        boolean is_playable;
        //linked_from
        String name;
        int popularity;
        String preview_url;
        TrackRestrictionObject restrictions;
        int track_number;
        String type;
        String uri;
    }

    class TrackRestrictionObject
    {
        String reason;
    }

    class UserInformationObject
    {
        class ExplicitContent
        {
            boolean filter_enabled;
            boolean filter_locked;
        }

        class Followers
        {
            String href;
            int total;
        }

        String country;
        String display_name;
        String email;
        ExplicitContent explicit_content;
        ExternalUrlObject external_urls;
        Followers followers;
        String href;
        ImageObject[] images;
        String product;
        String type;
        String uri;
    }

    /**
     * max limit is 50
     */
    @GET("me/playlists")
    Call<PagingObject<SimplifiedPlaylistObject>> getListOfCurrentUserPlaylists(@Header("Authorization") String token, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * max limit is 100
     */
    @GET("playlists/{playlist_id}/tracks")
    Call<PagingObject<PlaylistTrackObject>> getPlaylistItems(@Header("Authorization") String token, @Path("playlist_id") String playlist_id, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * max limit is 50
     */
    @GET("me/tracks")
    Call<PagingObject<SavedTrackObject>> getUserSavedTracks(@Header("Authorization") String token, @Query("limit") int limit, @Query("offset") int offset);

    /**
     * max limit is 50
     */
    @GET("me/albums")
    Call<PagingObject<SavedAlbumObject>> getUserSavedAlbums(@Header("Authorization") String token, @Query("limit") int limit, @Query("offset") int offset);

    @GET("me")
    Call<UserInformationObject> getUser(@Header("Authorization") String token);
}