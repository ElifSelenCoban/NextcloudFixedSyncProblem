package project.android.nextcloud;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.owncloud.android.lib.resources.files.model.RemoteFile;

public class FilesArrayAdapter extends ArrayAdapter<RemoteFile> {
    private LayoutInflater userInflater;

    public FilesArrayAdapter(Context context, int resource, Activity activity) {
        super(context, resource);
        userInflater = (LayoutInflater) activity.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View lineView;
        lineView = userInflater.inflate(R.layout.file_in_list, null);
        TextView textViewUserName = (TextView) lineView.findViewById(R.id.textViewUserName);
        textViewUserName.setText(getItem(position).getRemotePath());
        ImageView imageViewUserPicture = (ImageView) lineView.findViewById(R.id.imageViewUserPicture);
        if (getItem(position).getMimeType() == "DIR") {
            imageViewUserPicture.setImageResource(R.drawable.folder);
        }
        else if (getItem(position).getMimeType().contains("png"))
        {

            imageViewUserPicture.setImageResource(R.drawable.png);
        }
        else if (getItem(position).getMimeType().contains("jpg"))
        {

            imageViewUserPicture.setImageResource(R.drawable.jpg);
        }
        else if (getItem(position).getMimeType().contains("doc")||getItem(position).getMimeType().contains("docx") )
        {

            imageViewUserPicture.setImageResource(R.drawable.doc);
        }  else if (getItem(position).getMimeType().contains("zip"))
        {

            imageViewUserPicture.setImageResource(R.drawable.zip);
        }  else if (getItem(position).getMimeType().contains("txt"))
        {

            imageViewUserPicture.setImageResource(R.drawable.txt);
        }else if (getItem(position).getMimeType().contains("ppt"))
        {

            imageViewUserPicture.setImageResource(R.drawable.ppt);
        }else if (getItem(position).getMimeType().contains("pdf"))
        {

            imageViewUserPicture.setImageResource(R.drawable.pdf);
        }
        else
        {
            imageViewUserPicture.setImageResource(R.drawable.file);
        }
        return lineView;
    }
}