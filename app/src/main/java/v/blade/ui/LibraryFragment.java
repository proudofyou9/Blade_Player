package v.blade.ui;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.support.v4.media.session.MediaControllerCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import v.blade.BladeApplication;
import v.blade.R;
import v.blade.databinding.FragmentLibraryBinding;
import v.blade.library.Album;
import v.blade.library.Artist;
import v.blade.library.Library;
import v.blade.library.LibraryObject;
import v.blade.library.Playlist;
import v.blade.library.Song;
import v.blade.player.MediaBrowserService;
import v.blade.sources.Source;
import v.blade.sources.SourceInformation;

public class LibraryFragment extends Fragment
{
    //We keep a global instance of LibraryFragment to be able to update lists on Library update (launch, sync)
    public static LibraryFragment instance;

    public enum CURRENT_TYPE
    {
        LIBRARY, // We are inside a library item
        PLAYLIST, // We are inside a playlist
        SEARCH // We are in a search result
    }

    protected FragmentLibraryBinding binding;
    private List<? extends LibraryObject> current;
    private CURRENT_TYPE currentType;
    private LibraryObject currentObject;

    private static class BackInformation
    {
        private final String title;
        private final List<? extends LibraryObject> list;
        private final CURRENT_TYPE type;
        private final LibraryObject object;

        private BackInformation(String title, List<? extends LibraryObject> list, CURRENT_TYPE type, LibraryObject object)
        {
            this.title = title;
            this.list = list;
            this.type = type;
            this.object = object;
        }
    }

    private Stack<BackInformation> backStack = new Stack<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState)
    {
        binding = FragmentLibraryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setSmoothScrollbarEnabled(true);
        binding.mainListview.setLayoutManager(linearLayoutManager);
        updateContent(getTitle(), null, CURRENT_TYPE.LIBRARY, null);

        instance = this;

        return root;
    }

    public String getTitle()
    {
        return ((MainActivity) requireActivity()).binding == null ? getString(R.string.artists) : ((MainActivity) requireActivity()).binding.appBarMain.toolbar.getTitle().toString();
    }

    /*
     * Update content to list 'replacing', or to root directory
     * If we are updating because going back, we should not push to back : shouldPushToBack is false
     */
    private void updateContent(String title, List<? extends LibraryObject> replacing, CURRENT_TYPE type, LibraryObject object, boolean shouldPushToBack)
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

            currentType = CURRENT_TYPE.LIBRARY;
            currentObject = null;
            //Reset backstack
            backStack = new Stack<>();
        }
        else
        {
            //Push previous state to backStack
            if(shouldPushToBack)
                backStack.push(new BackInformation(getTitle(), current, currentType, currentObject));

            current = replacing;
            currentType = type;
            currentObject = object;
        }

        LibraryObjectAdapter adapter = new LibraryObjectAdapter(current, this::onMoreClicked, this::onViewClicked);
        binding.mainListview.setAdapter(adapter);
        if(((MainActivity) requireActivity()).binding != null)
            ((MainActivity) requireActivity()).binding.appBarMain.toolbar.setTitle(title);
    }

    public void updateContent(String title, List<? extends LibraryObject> replacing, CURRENT_TYPE type, LibraryObject currentObject)
    {
        updateContent(title, replacing, type, currentObject, true);
    }

    private void updateContent(BackInformation backInformation)
    {
        updateContent(backInformation.title, backInformation.list, backInformation.type, backInformation.object, false);
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
            updateContent(element.getName(), ((Artist) element).getAlbums(), CURRENT_TYPE.LIBRARY, element);
        else if(element instanceof Album)
            updateContent(element.getName(), ((Album) element).getSongs(), CURRENT_TYPE.LIBRARY, element);
        else if(element instanceof Playlist)
            updateContent(element.getName(), ((Playlist) element).getSongs(), CURRENT_TYPE.PLAYLIST, element);
        else if(element instanceof Song)
        {
            //noinspection unchecked
            MediaBrowserService.getInstance().setPlaylist(new ArrayList<>((List<Song>) current));
            MediaBrowserService.getInstance().setIndex(position);
            MediaControllerCompat.getMediaController(requireActivity()).getTransportControls().play();
        }
    }

    //TODO : maybe fix that ? switch on something else ?
    @SuppressLint("NonConstantResourceId")
    private void onMoreClicked(View view)
    {
        //Obtain object and menu
        LibraryObject element = (LibraryObject) view.getTag();
        PopupMenu popupMenu = new PopupMenu(requireContext(), view);
        popupMenu.inflate(R.menu.item_more);

        //Set element visibility depending on context
        if(element instanceof Song)
        {
            popupMenu.getMenu().getItem(3).setVisible(true);
            popupMenu.getMenu().getItem(4).setVisible(true);
            if(currentType == CURRENT_TYPE.PLAYLIST)
                popupMenu.getMenu().getItem(5).setVisible(true);
        }
        else if(element instanceof Playlist)
        {
            popupMenu.getMenu().getItem(6).setVisible(true);
        }

        //Set actions
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
                case R.id.action_add_to_list:
                    assert element instanceof Song;
                    openAddToPlaylistDialog((Song) element);
                    return true;
                case R.id.action_remove_from_library:
                    assert element instanceof Playlist;
                    openDeletePlaylistDialog((Playlist) element);
                    return true;
                case R.id.action_manage_libraries:
                    assert element instanceof Song;
                    openManageLibrariesDialog((Song) element);
                    return true;
                case R.id.action_remove_from_list:
                    assert element instanceof Song;
                    openRemoveFromPlaylistDialog((Song) element, (Playlist) currentObject);
                    return true;
            }
            return false;
        });
        popupMenu.show();
    }

    @Override
    public void onDestroyView()
    {
        super.onDestroyView();
        binding = null;
    }

    public void onBackPressed()
    {
        if(backStack.isEmpty())
            requireActivity().finish();
        else
            updateContent(backStack.pop());
    }

    protected void onSearch(String query)
    {
        updateContent(getString(R.string.search), Library.search(query), CURRENT_TYPE.SEARCH, null);
    }

    private void openAddToPlaylistDialog(Song toAdd)
    {
        //Build the lists of playlists suitable to receive toAdd
        ArrayList<Playlist> playlists = new ArrayList<>();

        //Create the "new playlist" option
        Playlist dummyNew = new Playlist(getString(R.string.new_playlist), null, null, null);
        dummyNew.setImageRequest(Picasso.get().load(R.drawable.ic_playlist_add));
        playlists.add(dummyNew);

        for(Playlist playlist : Library.getPlaylists())
        {
            for(SourceInformation s : toAdd.getSources())
            {
                if(s.source == playlist.getSource().source)
                {
                    playlists.add(playlist);
                    break;
                }
            }
        }

        //Build adapter and dialog with clickListener
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(playlists, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.add_to_playlist))
                .setAdapter(adapter, null);
        final AlertDialog dialog = builder.create();

        adapter.setClickListener(view ->
        {
            int position = dialog.getListView().getPositionForView(view);

            if(position == 0)
            {
                openCreatePlaylistDialog(toAdd);
                dialog.dismiss();
                return;
            }

            Playlist current = (Playlist) adapter.getItem(position);
            Source source = current.getSource().source;

            source.addSongToPlaylist(toAdd, current, () -> requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), getString(R.string.song_added_to_list, toAdd.getName(), current.getName()),
                                    Toast.LENGTH_SHORT).show()),
                    () -> requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), getString(R.string.song_added_to_list_error, toAdd.getName(), current.getName()),
                                    Toast.LENGTH_SHORT).show()));

            dialog.dismiss();
        });

        dialog.show();
    }

    private void openCreatePlaylistDialog(Song first)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.new_playlist)
                .setView(R.layout.dialog_create_playlist);

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface ->
        {
            Spinner sourceList = dialog.findViewById(R.id.playlist_source);
            assert sourceList != null;
            sourceList.setAdapter(new SpinnerAdapter()
            {
                class ViewHolder
                {
                    ImageView image;
                    TextView title;
                }

                @Override
                public View getDropDownView(int position, View convertView, ViewGroup parent)
                {
                    return getView(position, convertView, parent);
                }

                @Override
                public void registerDataSetObserver(DataSetObserver observer)
                {

                }

                @Override
                public void unregisterDataSetObserver(DataSetObserver observer)
                {

                }

                @Override
                public int getCount()
                {
                    return first.getSources().size();
                }

                @Override
                public Source getItem(int position)
                {
                    return first.getSources().get(position).source;
                }

                @Override
                public long getItemId(int position)
                {
                    return position;
                }

                @Override
                public boolean hasStableIds()
                {
                    return true;
                }

                @Override
                public View getView(int position, View convertView, ViewGroup parent)
                {
                    ViewHolder viewHolder;

                    if(convertView == null)
                    {
                        viewHolder = new ViewHolder();
                        convertView = LayoutInflater.from(requireContext())
                                .inflate(R.layout.item_simple_layout, parent, false);

                        viewHolder.title = convertView.findViewById(R.id.item_element_title);
                        viewHolder.image = convertView.findViewById(R.id.item_element_image);

                        convertView.setTag(viewHolder);
                    }
                    else viewHolder = (ViewHolder) convertView.getTag();

                    Source current = getItem(position);
                    viewHolder.title.setText(current.getName());
                    viewHolder.image.setImageResource(current.getImageResource());

                    return convertView;
                }

                @Override
                public int getItemViewType(int position)
                {
                    return 0;
                }

                @Override
                public int getViewTypeCount()
                {
                    return 1;
                }

                @Override
                public boolean isEmpty()
                {
                    return false;
                }
            });
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.string.ok), (dialogInterface, which) ->
        {
            Spinner spinner = dialog.findViewById(R.id.playlist_source);
            assert spinner != null;
            Source current = (Source) spinner.getSelectedItem();
            EditText name = dialog.findViewById(R.id.playlist_name);
            assert name != null;

            current.createPlaylist(name.getText().toString(), new BladeApplication.Callback<Playlist>()
            {
                @Override
                public void run(Playlist playlist)
                {
                    current.addSongToPlaylist(first, playlist, () -> requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), getString(R.string.song_added_to_list, first.getName(), playlist.getName()),
                                            Toast.LENGTH_SHORT).show()),
                            () -> requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), getString(R.string.song_added_to_list_error, first.getName(), playlist.getName()),
                                            Toast.LENGTH_SHORT).show()));
                }
            }, () -> requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), getString(R.string.could_not_create_playlist, name.getText().toString()), Toast.LENGTH_SHORT)));

            dialog.dismiss();
        });
        dialog.show();
    }

    private void openDeletePlaylistDialog(Playlist playlist)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_playlist)
                .setMessage(getString(R.string.are_you_sure_delete_playlist, playlist.getName()))
                .setPositiveButton(R.string.yes, (dialog, which) ->
                {
                    playlist.getSource().source.removePlaylist(playlist, () ->
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), getString(R.string.playlist_removed, playlist.getName()), Toast.LENGTH_SHORT).show()),
                            () -> requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), getString(R.string.playlist_could_not_remove, playlist.getName()), Toast.LENGTH_SHORT).show()));

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openManageLibrariesDialog(Song song)
    {
        BaseAdapter adapter = new BaseAdapter()
        {
            class ViewHolder
            {
                ImageView imageView;
                SwitchCompat switchView;
            }

            @Override
            public int getCount()
            {
                return song.getSources().size();
            }

            @Override
            public Source getItem(int position)
            {
                return song.getSources().get(position).source;
            }

            @Override
            public long getItemId(int position)
            {
                return position;
            }

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                ViewHolder viewHolder;
                if(convertView == null)
                {
                    convertView = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_switch, parent, false);
                    viewHolder = new ViewHolder();
                    viewHolder.imageView = convertView.findViewById(R.id.item_element_image);
                    viewHolder.switchView = convertView.findViewById(R.id.item_element_switch);
                    convertView.setTag(viewHolder);
                }
                else viewHolder = (ViewHolder) convertView.getTag();

                Source current = getItem(position);
                viewHolder.imageView.setImageResource(current.getImageResource());
                viewHolder.switchView.setText(current.getName());

                //Enabled :
                //For each source, we are either 'handled' or 'in library'
                viewHolder.switchView.setChecked(!song.getSources().get(position).handled);

                //TODO : disable switch for sources that does not support adding ?

                viewHolder.switchView.setOnClickListener(view ->
                {
                    //Check status and either add or remove
                    if(song.getSources().get(position).handled)
                    {
                        song.getSources().get(position).source.addToLibrary(song, () ->
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(view.getContext(), getString(R.string.song_added_to_library, song.getName()), Toast.LENGTH_SHORT).show()),
                                () -> requireActivity().runOnUiThread(() ->
                                {
                                    Toast.makeText(view.getContext(), getString(R.string.song_added_to_library_error, song.getName()), Toast.LENGTH_SHORT).show();
                                    viewHolder.switchView.setChecked(false);
                                }));
                    }
                    else
                    {
                        song.getSources().get(position).source.removeFromLibrary(song, () ->
                                        requireActivity().runOnUiThread(() ->
                                                Toast.makeText(view.getContext(), getString(R.string.song_removed_from_library, song.getName()), Toast.LENGTH_SHORT).show()),
                                () -> requireActivity().runOnUiThread(() ->
                                {
                                    Toast.makeText(view.getContext(), getString(R.string.song_removed_from_library_error, song.getName()), Toast.LENGTH_SHORT).show();
                                    viewHolder.switchView.setChecked(true);
                                }));
                    }
                });

                return convertView;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.manage_libraries)
                .setAdapter(adapter, ((dialog, which) ->
                {
                }));
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void openRemoveFromPlaylistDialog(Song song, Playlist playlist)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.remove_from_list)
                .setMessage(getString(R.string.are_you_sure_remove_from_list, song.getName(), playlist.getName()))
                .setPositiveButton(R.string.yes, (dialog, which) ->
                {
                    playlist.getSource().source.removeFromPlaylist(song, playlist, () ->
                                    requireActivity().runOnUiThread(() ->
                                            Toast.makeText(requireContext(), getString(R.string.song_removed_from_list, song.getName(), playlist.getName()), Toast.LENGTH_SHORT).show()),
                            () -> requireActivity().runOnUiThread(() ->
                                    Toast.makeText(requireContext(), getString(R.string.song_removed_from_list_error, song.getName(), playlist.getName()), Toast.LENGTH_SHORT).show()));

                    dialog.dismiss();
                })
                .setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}