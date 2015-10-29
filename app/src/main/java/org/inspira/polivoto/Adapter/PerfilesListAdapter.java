package org.inspira.polivoto.Adapter;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;

import org.inspira.polivotoserver.R;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by jcapiz on 28/10/15.
 */
public class PerfilesListAdapter extends BaseAdapter {

    LinkedList<String> texts;
    Activity activity;

    public PerfilesListAdapter(Activity activity, LinkedList<String> textos){
        this.activity = activity;
        texts = textos;
    }

    public  void addElement(String newEditText){
        texts.add(newEditText);
    }

    public String removeElement(int position){
        return texts.remove(position);
    }

    public boolean removeElement(Object position){
        return texts.remove(position);
    }

    public String removeLastElement(){
        return texts.removeLast();
    }

    public LinkedList<String> getTexts(){
        return texts;
    }

    @Override
    public int getCount() {
        return texts.size();
    }

    @Override
    public Object getItem(int position) {
        return texts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        EditText text = null;
        LayoutInflater inflater = (LayoutInflater)activity.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
        if(convertView == null){
            text = (EditText)inflater.inflate(R.layout.custom_edit_text,parent,false);
        }else text = (EditText)convertView;
        text.setText(texts.get(position));
        return text;
    }
}