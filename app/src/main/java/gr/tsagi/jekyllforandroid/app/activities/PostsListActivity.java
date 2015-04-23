package gr.tsagi.jekyllforandroid.app.activities;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.ArrayList;

import gr.tsagi.jekyllforandroid.app.R;
import gr.tsagi.jekyllforandroid.app.adapters.NavDrawerListAdapter;
import gr.tsagi.jekyllforandroid.app.data.PostsDbHelper;
import gr.tsagi.jekyllforandroid.app.fragments.MarkdownPreviewFragment;
import gr.tsagi.jekyllforandroid.app.fragments.PostsListFragment;
import gr.tsagi.jekyllforandroid.app.fragments.PrefsFragment;
import gr.tsagi.jekyllforandroid.app.utils.FetchPostsTask;
import gr.tsagi.jekyllforandroid.app.utils.NavDrawerItem;

/**
 * Created by tsagi on 9/9/13.
 */

public class PostsListActivity extends BaseActivity implements PostsListFragment.Callback {

    private static final String LOG_TAG = PostsListActivity.class.getSimpleName();

    public static boolean mTwoPane;

    public static final String POST_STATUS = "post_status";

    String mUsername;
    String mToken;
    String mRepo;
    Boolean mFirstTime;
    SharedPreferences settings;

    FetchPostsTask fetchPostsTask;

    private String[] mNavTitles;

    private DrawerLayout mDrawerLayout;
    private ActionBarDrawerToggle mDrawerToggle;
    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    ImageButton create;

    private ListView mDrawerList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setActionBarIcon(R.drawable.ic_ab_drawer);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            // create our manager instance after the content view is set
            SystemBarTintManager tintManager = new SystemBarTintManager(this);
            // enable status bar tint
            tintManager.setStatusBarTintEnabled(true);
            // Set color
            tintManager.setTintColor(getResources().getColor(R.color.primary));
        }

        restorePreferences();
        DrawerSetup();

        if (mToken.equals("") || mFirstTime) {
            login();
        } else {
            updateList();
            selectItem(0);
            if (findViewById(R.id.markdown_preview_container) != null) {
                // The preview container view will be present only in the large-screen layouts
                // (res/layout-sw600dp). If this view is present, then the activity should be
                // in two-pane mode.
                mTwoPane = true;
                // In two-pane mode, show the detail view in this activity by
                // adding or replacing the detail fragment using a
                // fragment transaction.
                if (savedInstanceState == null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.markdown_preview_container, new MarkdownPreviewFragment())
                            .commit();
                }
            } else {
                mTwoPane = false;
            }
        }

        if (mRepo.equals("") && !mToken.equals("")) {
            Toast.makeText(PostsListActivity.this,
                    "There is something wrong with your jekyll repo",
                    Toast.LENGTH_LONG).show();
        }

        create = (ImageButton) findViewById(R.id.fab);
        create.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                newPost();
            }
        });

    }

    @Override protected int getLayoutResource() {
        return R.layout.activity_posts_list;
    }

    private void updateList() {
        fetchPostsTask = new FetchPostsTask(this);
        fetchPostsTask.execute();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar
        // if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        // Just for the logout
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item))
            return true;
        switch (item.getItemId()) {
            case R.id.action_logout:
                logoutDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /* Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // If the nav drawer is open,
        // hide action items related to the content view
        return super.onPrepareOptionsMenu(menu);
    }

    private void DrawerSetup() {
        mNavTitles = getResources().getStringArray(R.array.nav_array);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) findViewById(R.id.left_drawer);

        mDrawerTitle = getResources().getString(R.string.app_name);

        final TypedArray navMenuIcons = getResources()
                .obtainTypedArray(R.array.nav_drawer_icons_dark);
        ArrayList<NavDrawerItem> navDrawerItems;
        NavDrawerListAdapter adapter;

        navDrawerItems = new ArrayList<NavDrawerItem>();

        navDrawerItems.add(new NavDrawerItem(mNavTitles[0],
                navMenuIcons.getResourceId(0, -1)));
        navDrawerItems.add(new NavDrawerItem(mNavTitles[1],
                navMenuIcons.getResourceId(1, -1)));
        navDrawerItems.add(new NavDrawerItem(mNavTitles[2],
                navMenuIcons.getResourceId(2, -1)));

        navMenuIcons.recycle();

        adapter = new NavDrawerListAdapter(getApplicationContext(),
                navDrawerItems);
        mDrawerList.setAdapter(adapter);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        // Set the list's click listener
        mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                toolbar, R.string.drawer_open,
                R.string.drawer_close) {

            /** Called when a drawer has settled in a completely closed state.*/
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                getSupportActionBar().setTitle(mTitle);
                supportInvalidateOptionsMenu(); // creates call onPrepareOptionsMenu()
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                getSupportActionBar().setTitle(mDrawerTitle);
                supportInvalidateOptionsMenu(); // creates call onPrepareOptionsMenu()
            }
        };

        // Set the drawer toggle as the DrawerListener
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // just styling option add shadow the right edge of the drawer
        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow,
                GravityCompat.START);
    }

    @Override
    public void onItemSelected(String postId, String content, int postStatus) {
        if (mTwoPane) {
            // In two-pane mode, show the detail view in this activity by
            // adding or replacing the detail fragment using a
            // fragment transaction.
            Bundle args = new Bundle();
            args.putString(PreviewMarkdownActivity.POST_CONTENT, content);

            MarkdownPreviewFragment fragment = new MarkdownPreviewFragment();
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.markdown_preview_container, fragment)
                    .commit();
        } else {
            editPost(postId, postStatus);
        }
    }

    @Override
    public void onItemEditSelected(String postId, String content, int postStatus) {
        editPost(postId, postStatus);
    }

    @Override
    public void onItemDeleteSelected(String postId, String content, int postStatus) {

    }

    private class DrawerItemClickListener implements
            ListView.OnItemClickListener {
        @Override
        public void onItemClick(AdapterView parent, View view,
                                int position, long id) {
            selectItem(position);
        }
    }

    /**
     * Swaps fragments in the main content view
     */
    private void selectItem(int position) {

        Fragment fragment;
        Bundle data = new Bundle();
        data.putInt(PostsListActivity.POST_STATUS, position);

        switch (position) {
            case 0:
                try {
                    fragment = new PostsListFragment();
                    // Insert the fragment by replacing any existing fragment
                    fragment.setArguments(data);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, fragment)
                            .commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 1:
                try {
                    fragment = new PostsListFragment();
                    // Insert the fragment by replacing any existing fragment
                    fragment.setArguments(data);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, fragment)
                            .commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case 2:
                try {
                    fragment = new PrefsFragment();
                    // Insert the fragment by replacing any existing fragment
                    fragment.setArguments(data);
                    getFragmentManager().beginTransaction()
                            .replace(R.id.content_frame, fragment)
                            .commit();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

        // Highlight the selected item, update the title, and close the drawer
        mDrawerList.setItemChecked(position, true);
        setTitle(mNavTitles[position]);
        mDrawerLayout.closeDrawer(mDrawerList);

    }

    @Override
    public void setTitle(CharSequence title) {
        mTitle = title;
        getSupportActionBar().setTitle(mTitle);
    }

    /**
     * Start new post or continue working on your draft
     */
    public void newPost() {
        Intent myIntent = new Intent(PostsListActivity.this,
                EditPostActivity.class);
        startActivity(myIntent);
    }

    public void editPost(String postId, int postStatus) {
        Intent intent = new Intent(this, EditPostActivity.class)
                .putExtra(EditPostActivity.POST_ID, postId)
                .putExtra(EditPostActivity.POST_STATUS, postStatus);
        startActivity(intent);
    }

    /**
     * Logout and clear settings
     */
    public void logoutDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Shared preferences and Intent settings
        // before logout ask user and remind him any draft posts

        final SharedPreferences sharedPreferences = getSharedPreferences(
                "gr.tsagi.jekyllforandroid", Context.MODE_PRIVATE);

        if (sharedPreferences.getString("draft_content", "").equals(""))
            builder.setMessage(R.string.dialog_logout_nodraft);
        else
            builder.setMessage(R.string.dialog_logout_draft);

        // Add the buttons
        builder.setPositiveButton(R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User clicked OK button
                        // Clear credentials and Drafts
                        login();
                    }
                }
        );
        builder.setNegativeButton(R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        // User cancelled the dialog
                    }
                }
        );

        // Create the AlertDialog
        AlertDialog dialog = builder.create();

        // Show it
        dialog.show();

    }

    private void restorePreferences() {
        settings = getSharedPreferences(
                "gr.tsagi.jekyllforandroid", Context.MODE_PRIVATE);
        mUsername = settings.getString("user_login", "");
        mToken = settings.getString("user_status", "");
        mRepo = settings.getString("user_repo", "");
        //Todo: resolve GitHub auth/login issues
        mFirstTime = settings.getBoolean("first_time", true);
//        mFirstTime = true;
    }

    private void login() {
        Intent PostListIntent = new Intent(PostsListActivity.this,
                LoginActivity.class);
        SharedPreferences sharedPreferences = getSharedPreferences(
                "gr.tsagi.jekyllforandroid", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();

        editor.clear();
        editor.commit();

        PostsDbHelper db = new PostsDbHelper(this);
        db.dropTables();
        db.close();

        startActivity(PostListIntent);
        this.finish();

    }
}

