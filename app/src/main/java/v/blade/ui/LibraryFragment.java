package v.blade.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import v.blade.R;
import v.blade.databinding.FragmentLibraryBinding;
import v.blade.library.Album;
import v.blade.library.Artist;
import v.blade.library.Library;
import v.blade.library.LibraryObject;
import v.blade.library.Playlist;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;

public class LibraryFragment extends Fragment
{
    //We keep a global instance of LibraryFragment to be able to update lists on Library update (launch, sync)
    public static LibraryFragment instance;

    private FragmentLibraryBinding binding;
    public List<? extends LibraryObject> current;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        binding.mainListview.setLayoutManager(new LinearLayoutManager(getActivity()));
        updateContent(getTitle(), null);

        instance = this;

        return root;
    }

    public String getTitle()
    {
        return ((MainActivity) requireActivity()).binding == null ? getString(R.string.artists) : ((MainActivity) requireActivity()).binding.appBarMain.toolbar.getTitle().toString();
    }

    /*
     * Update content to list 'replacing', or to root directory
     */
    public void updateContent(String title, List<? extends LibraryObject> replacing)
    {
        if(replacing == null)
        {
            /* we are going back to top directory : artists, albums, songs, playlists */
            if(title.equals(getString(R.string.artists)))
                current = Library.getArtists();
            else if(title.equals(getString(R.string.albums)))
                current = Library.getAlbums();
            else if(title.equals(getString(R.string.songs)))
                current = Library.getSongs();
            else if(title.equals(getString(R.string.playlists)))
                current = Library.getPlaylists();
            else return;
        }
        else
        {
            current = replacing;
        }

        LibraryObjectAdapter adapter = new LibraryObjectAdapter(current, this::onMoreClicked, this::onViewClicked);
        binding.mainListview.setAdapter(adapter);
        if(((MainActivity) requireActivity()).binding != null)
            ((MainActivity) requireActivity()).binding.appBarMain.toolbar.setTitle(title);
    }

    private void onViewClicked(View view)
    {
        int position = binding.mainListview.getChildLayoutPosition(view);
        LibraryObject clicked = current.get(position);
        onElementClicked(clicked, position);
    }

    private void onElementClicked(LibraryObject element, int position)
    {
        if(element instanceof Artist)
            updateContent(element.getName(), ((Artist) element).getAlbums());
        else if(element instanceof Album)
            updateContent(element.getName(), ((Album) element).getSongs());
        else if(element instanceof Playlist)
            updateContent(element.getName(), ((Playlist) element).getSongs());
        else if(element instanceof Song)
        {
            //noinspection unchecked
            MediaBrowserService.getInstance().setPlaylist((List<Song>) current);
            MediaBrowserService.getInstance().setIndex(position);
            MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().play();
        }
    }

    //TODO : maybe fix that ? switch on something else ?
    @SuppressLint("NonConstantResourceId")
    private void onMoreClicked(View view)
    {
        LibraryObject element = (LibraryObject) view.getTag();
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.setOnMenuItemClickListener(item ->
        {
            switch(item.getItemId())
            {
                case R.id.action_play:
                    ArrayList<Song> playlist = new ArrayList<>();
                    if(element instanceof Song) playlist.add((Song) element);
                    else if(element instanceof Album) playlist.addAll(((Album) element).getSongs());
                    else if(element instanceof Artist)
                        for(Album a : ((Artist) element).getAlbums()) playlist.addAll(a.getSongs());
                    else if(element instanceof Playlist)
                        playlist.addAll(((Playlist) element).getSongs());
                    MediaBrowserService.getInstance().setPlaylist(playlist);
                    MediaBrowserService.getInstance().setIndex(0);
                    LibraryFragment.this.requireActivity().getMediaController().getTransportControls().play();
                    return true;
                case R.id.action_play_next:
                    ArrayList<Song> playlistAddNext = new ArrayList<>();
                    if(element instanceof Song) playlistAddNext.add((Song) element);
                    else if(element instanceof Album)
                        playlistAddNext.addAll(((Album) element).getSongs());
                    else if(element instanceof Artist) for(Album a : ((Artist) element).getAlbums())
                        playlistAddNext.addAll(a.getSongs());
                    else if(element instanceof Playlist)
                        playlistAddNext.addAll(((Playlist) element).getSongs());
                    if(MediaBrowserService.getInstance().getPlaylist() != null && !MediaBrowserService.getInstance().getPlaylist().isEmpty())
                        MediaBrowserService.getInstance().getPlaylist().addAll(MediaBrowserService.getInstance().getIndex() + 1, playlistAddNext);
                    else
                    {
                        MediaBrowserService.getInstance().setPlaylist(playlistAddNext);
                        MediaBrowserService.getInstance().setIndex(0);
                        LibraryFragment.this.requireActivity().getMediaController().getTransportControls().play();
                    }
                    return true;
                case R.id.action_add_to_playlist:
                    ArrayList<Song> playlistAdd = new ArrayList<>();
                    if(element instanceof Song) playlistAdd.add((Song) element);
                    else if(element instanceof Album)
                        playlistAdd.addAll(((Album) element).getSongs());
                    else if(element instanceof Artist) for(Album a : ((Artist) element).getAlbums())
                        playlistAdd.addAll(a.getSongs());
                    else if(element instanceof Playlist)
                        playlistAdd.addAll(((Playlist) element).getSongs());
                    if(MediaBrowserService.getInstance().getPlaylist() != null && !MediaBrowserService.getInstance().getPlaylist().isEmpty())
                        MediaBrowserService.getInstance().getPlaylist().addAll(playlistAdd);
                    else
                    {
                        MediaBrowserService.getInstance().setPlaylist(playlistAdd);
                        MediaBrowserService.getInstance().setIndex(0);
                        LibraryFragment.this.requireActivity().getMediaController().getTransportControls().play();
                    }
                    return true;
            }
            return false;
        });
        popupMenu.inflate(R.menu.item_more);
        popupMenu.show();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }
}