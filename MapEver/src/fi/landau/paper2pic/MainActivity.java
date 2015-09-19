package fi.landau.paper2pic;

import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.getbase.floatingactionbutton.FloatingActionButton;
import de.hu_berlin.informatik.spws2014.mapever.FileUtils;
import de.hu_berlin.informatik.spws2014.mapever.MapEverApp;
import de.hu_berlin.informatik.spws2014.mapever.R;
import de.hu_berlin.informatik.spws2014.mapever.Start;
import de.hu_berlin.informatik.spws2014.mapever.camera.CornerDetectionCamera;
import de.hu_berlin.informatik.spws2014.mapever.entzerrung.Entzerren;
import org.opencv.android.OpenCVLoader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends ListActivity {
    private final String TAG = "paper2pic";
    private final int TAKE_PICTURE_REQUESTCODE = 1;
    private final int CROP_PICTURE_REQUESTCODE = 2;

    private ArrayList<String> fakeDb;

    static {
        if (!OpenCVLoader.initDebug()) {
            // Handle initialization error
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        fakeDb = new ArrayList<>();
        FloatingActionButton fromCameraButton = (FloatingActionButton) findViewById(R.id.addFromCamera);
        fromCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent photoIntent;
                photoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                File destFile = new File(MapEverApp.getAbsoluteFilePath("tmp.jpg"));
                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(destFile));
                startActivityForResult(photoIntent, TAKE_PICTURE_REQUESTCODE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TAKE_PICTURE_REQUESTCODE && resultCode == RESULT_OK) {
            Log.d(TAG, "Got back data");
            Intent EntzerrenActivity = new Intent(getApplicationContext(), Entzerren.class);
            EntzerrenActivity.putExtra(Start.INTENT_IMAGEPATH,  "tmp.jpg");
            startActivityForResult(EntzerrenActivity, CROP_PICTURE_REQUESTCODE);
        } else if (requestCode == CROP_PICTURE_REQUESTCODE && resultCode == RESULT_OK) {
            try {
                String savedFile = fakeDb.size() + ".jpg";
                fakeDb.add(savedFile);
                FileUtils.copyFileToFile(
                        new File(MapEverApp.getAbsoluteFilePath("tmp.jpg")),
                        new File(MapEverApp.getAbsoluteFilePath(savedFile)));
                ListView list = (ListView)findViewById(android.R.id.list);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.listitem, fakeDb);
                list.setAdapter(adapter);
            } catch (IOException e) {
                Log.e(TAG, "Couldn't copy to result.jpg");
            }
        } else {
            Log.d(TAG, "request: " + requestCode + ", result: " + resultCode);
        }
    }


        @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        File destFile = new File(MapEverApp.getAbsoluteFilePath(fakeDb.get(position)));
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(destFile));
        shareIntent.setType("image/jpeg");
        startActivity(Intent.createChooser(shareIntent, "Send the file"));
    }

}
