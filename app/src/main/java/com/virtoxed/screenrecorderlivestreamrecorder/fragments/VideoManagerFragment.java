package com.virtoxed.screenrecorderlivestreamrecorder.fragments;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.AsyncTaskLoader;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.virtoxed.screenrecorderlivestreamrecorder.R;
import com.virtoxed.screenrecorderlivestreamrecorder.data.database.VideoDatabase;
import com.virtoxed.screenrecorderlivestreamrecorder.data.entities.Video;
import com.virtoxed.screenrecorderlivestreamrecorder.SyncActivity;
import com.virtoxed.screenrecorderlivestreamrecorder.adapters.VideoAdapter;
import com.virtoxed.screenrecorderlivestreamrecorder.services.sync.SyncService;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.DialogHelper;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.FileHelper;
import com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils.DEBUG;
import static com.virtoxed.screenrecorderlivestreamrecorder.utils.MyUtils.showSnackBarNotification;


/**
 * A simple {@link Fragment} subclass.
 * Use the {@link VideoManagerFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class VideoManagerFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = VideoManagerFragment.class.getSimpleName();
    private View mViewRoot;
    private Menu mMenu;
    GridView mListviewVideos;
    TextView mTvEmpty;
    private VideoAdapter mAdapter;
    private Object mSync = new Object();

    public VideoManagerFragment() {
        // Required empty public constructor
    }

    public static VideoManagerFragment newInstance(String param1, String param2) {
        VideoManagerFragment fragment = new VideoManagerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public static VideoManagerFragment getInstance()    {
        return new VideoManagerFragment();
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);
        mViewRoot = inflater.inflate(R.layout.fragment_video_manager, container, false);

        return mViewRoot;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.video_setting_menu, menu);
        mMenu= menu;
        final MenuItem item = menu.findItem(R.id.action_select_multiple);
        item.getActionView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean isChecked = item.isChecked();
                item.setChecked(!isChecked);
                mAdapter.selectAll(!isChecked);
                mAdapter.showAllCheckboxes(true);
                if(DEBUG) Log.i(TAG, "onOptionsItemSelected: "+mAdapter.logSelection());
            }
        });
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        updateMenuItemsList();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){

            case R.id.action_scan:
                reloadData();

                break;


            case R.id.action_rename:
                DialogHelper.getInstance(mAdapter).showRenameDialog(mAdapter.getSingleSelectedVideo());
                break;

            case R.id.action_share:
                Intent sharingIntent = new Intent(Intent.ACTION_SEND);
                sharingIntent.setType("video/*");
                Uri uri = Uri.parse(mAdapter.getSingleSelectedVideo().getLocalPath());
                sharingIntent.putExtra(Intent.EXTRA_STREAM, uri);
                startActivity(Intent.createChooser(sharingIntent, "Share Video!"));
                break;

            case R.id.action_detail:
                DialogHelper.getInstance(mAdapter).showDetailDialog(mAdapter.getSingleSelectedVideo());
                break;

            case R.id.action_delete:
                requestConfirmDeletion("Delete videos?", "Are you want to delete selected videos?");
                break;

            case R.id.action_cancel:
                performCancel();
                break;

        }
        return false;
    }

    private void performCancel() {
        mAdapter.showAllCheckboxes(false);
        mAdapter.selectAll(false);
        setMenuItemVisibility(R.id.action_select_multiple, false);
    }

    private void reloadData() {
        getLoaderManager().restartLoader(0, null, mLoadVideosCallback);
        mAdapter.verifyData();
        updateUI();
        toggleSelectMultipleCheckbox(false);
        MyUtils.showSnackBarNotification(mViewRoot, "Scanned video!", Snackbar.LENGTH_LONG);
    }

    private void updateUI() {
        performCancel();
        if(mAdapter.getCount() == 0){
            mTvEmpty.setText("No Video Record");
        }
    }

    private void requestConfirmDeletion(String title, String message) {
        new AlertDialog.Builder(getContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    final Video[] videos = mAdapter.getSelectedVideo();

                    FileHelper.getInstance(mAdapter)
                            .deleteVideoFromDatabaseCallable(videos)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {

                                @Override
                                public void onSuccess(Void aVoid) {
                                    FileHelper
                                            .getInstance(mAdapter)
                                            .deleteFilesFromStorageCallable(videos)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    reloadData();

                                                }
                                            })

                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    Log.e(TAG, "delete Video from DB failed", e);
                                                }
                                            });;
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    reloadData();
                                    Log.e(TAG, "delete Video from SD failed", e);
                                }
                            });
                        updateUI();
                    MyUtils.showSnackBarNotification(mViewRoot, "Delete video completed", Snackbar.LENGTH_LONG);
                }
            })
            .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mAdapter.clearSelected();
                }
            })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
    }

    private void toggleSelectMultipleCheckbox(boolean b) {
        setMenuItemVisibility(R.id.action_select_multiple, b);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initViews();
    }

    @Override
    public void onResume() {
        super.onResume();
//        reloadData();
        if(mMenu!=null)
            toggleSelectMultipleCheckbox(false);
    }

    private void initViews() {
         mListviewVideos = (GridView) mViewRoot.findViewById(R.id.list_videos);
         mTvEmpty = mViewRoot.findViewById(R.id.tvEmpty);

        final SwipeRefreshLayout srl = (SwipeRefreshLayout) mViewRoot.findViewById(R.id.swipeLayout);
        srl.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                reloadData();
                srl.setRefreshing(false);
            }

        });


        mListviewVideos.setEmptyView(mTvEmpty);

        // Create a new {@link ArrayAdapter} of earthquakes: gắn cái datalist vào layout
        mAdapter = new VideoAdapter(
                getActivity(), new ArrayList<Video>());

        // Set the mAdapter on the {@link ListView}
        // so the list can be populated in the user interface
        mListviewVideos.setAdapter(mAdapter);

        handleListviewItemClickEvents();

        if(DEBUG) Log.i(TAG, "initLoader (activity create) called");
        getLoaderManager().initLoader(0, null, mLoadVideosCallback);

    }

    private void handleListviewItemClickEvents() {
        mListviewVideos.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if(!hasFocus)
                    mAdapter.showAllCheckboxes(false);
            }
        });

        mListviewVideos.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                Log.d("chienpm_log", "onCheckedChanged: "+mSelectedPositions.size());
                if(mAdapter.getSelectedPositionCount() == 0) {

                    Video video = mAdapter.getItem(position);
                    String path = video.getLocalPath();
                    File file = new File(path);
                    if (file.exists()) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(video.getLocalPath()));
                        intent.setDataAndType(Uri.parse(video.getLocalPath()), "video/mp4");
                        startActivity(intent);
                    }
                    else{
                        MyUtils.showSnackBarNotification(mTvEmpty, "This video is not available now!", Snackbar.LENGTH_LONG);
                        mAdapter.toggleSelectionAtPosition(position);
                        requestConfirmDeletion("This video is not available", "Are you want to delete this video?");
                    }

                }
                else {
                    mAdapter.toggleSelectionAtPosition(position);
                }
                if(DEBUG) Log.i("virtoxed_log", "selected: "+mAdapter.logSelection());
            }
        });

        mListviewVideos.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                mAdapter.showAllCheckboxes(true);
                mAdapter.toggleSelectionAtPosition(position);
                setMenuItemVisibility(R.id.action_select_multiple, true);
                return false;
            }
        });
    }

    private void updateMenuItemsList() {
        int selectedMode = mAdapter.getSelectedMode();
        showMenuItems(false); //hide
        switch (selectedMode){

            case MyUtils.SELECTED_MODE_EMPTY:
                //hide all
                break;

            case MyUtils.SELECTED_MODE_ALL:

                setMenuItemVisibility(R.id.action_delete, false);
                setMenuItemVisibility(R.id.action_cancel, true);

                break;

            case MyUtils.SELECTED_MODE_MULTIPLE:

                setMenuItemVisibility(R.id.action_delete, true);
                setMenuItemVisibility(R.id.action_cancel, true);

                mMenu.findItem(R.id.action_delete).setTitle("Delete selected");

                break;
            case MyUtils.SELECTED_MODE_SINGLE:
                showMenuItems(true);
                break;
        }
    }

    private void setMenuItemVisibility(int id, boolean value) {
        if(mMenu!=null) {
            MenuItem item = mMenu.findItem(id);
            if(item!=null){
                item.setVisible(value);
            }
        }
    }

    private void showMenuItems(boolean value) {
        for(int i = 2; i < mMenu.size(); i++){
            mMenu.getItem(i).setVisible(value);
        }
    }


//    #Load data to list view
    private LoaderManager.LoaderCallbacks<ArrayList<Video>> mLoadVideosCallback = new LoaderManager.LoaderCallbacks<ArrayList<Video>>() {
        @NonNull
        @Override
        public Loader<ArrayList<Video>> onCreateLoader(int i, @Nullable Bundle bundle) {
            return new VideoAsyncTaskLoader(getContext());
        }

        @Override
        public void onLoadFinished(@NonNull Loader<ArrayList<Video>> loader, ArrayList<Video> videos) {
            if(DEBUG) Log.i(TAG, "onLoadFinished: called");
            //clear the adapter of previous earthquake data
            mAdapter.clear();

            // If there is a valid list of {@link Earthquake}s, then add them to the adapter's
            // data set. This will trigger the ListView to notifySettingChanged.
            if(videos != null && !videos.isEmpty()) {
                mAdapter.addAll(videos);
            }
            else{
                mTvEmpty.setText("No Video Record");
                Log.i(TAG, "onLoadFinished: setted textempty ");
            }
        }

        @Override
        public void onLoaderReset(@NonNull Loader<ArrayList<Video>> loader) {
            if(DEBUG) Log.d(TAG, "onLoaderReset: called");
            mAdapter.clear();
        }
    };

    static class VideoAsyncTaskLoader extends AsyncTaskLoader<ArrayList<Video>> {
        WeakReference<Context> mWeakReference;
        public VideoAsyncTaskLoader(@NonNull Context context) {
            super(context);
            mWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Nullable
        @Override
        public ArrayList<Video> loadInBackground() {
            try {
                return (ArrayList<Video>) VideoDatabase.getInstance(mWeakReference.get()).getVideoDao().getAllVideo();
            }catch (Exception e){
                e.printStackTrace();
                if(DEBUG) Log.i(TAG, "loadInBackground: "+e.getMessage());
                return null;
            }
        }

        @Override
        protected void onReset() {
            super.onReset();
        }
    }

}
