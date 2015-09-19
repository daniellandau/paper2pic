package fi.landau.paper2pic;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import de.hu_berlin.informatik.spws2014.mapever.R;

import java.util.List;

public class ListItemAdapter extends ArrayAdapter<ScannedItem> {

    private final int resource;
    private final List<ScannedItem> objects;

    public ListItemAdapter(Context context, int resource, List<ScannedItem> objects) {
        super(context, resource, objects);
        this.resource = resource;
        this.objects = objects;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext()).inflate(resource, parent, false);

        ImageView im = (ImageView) v.findViewById(R.id.paperImage);
        TextView text = (TextView) v.findViewById(R.id.paperName);

        im.setImageURI(Uri.fromFile(objects.get(position).thumb));
        text.setText(objects.get(position).name);
        return v;
    }

}
