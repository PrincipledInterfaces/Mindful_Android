package com.example.myapplication.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class MultiSelectAdapter extends ArrayAdapter<String> {
    private final boolean[] selectedItems;

    public MultiSelectAdapter(@NonNull Context context, @NonNull List<String> objects, boolean[] selectedItems) {
        super(context, android.R.layout.simple_list_item_multiple_choice, objects);
        if (selectedItems == null || selectedItems.length != objects.size()) {
            throw new IllegalArgumentException("selectedItems array must be the same length as the list size.");
        }
        this.selectedItems = selectedItems;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
            holder = new ViewHolder();
            holder.checkedTextView = convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.checkedTextView.setText(getItem(position));
        holder.checkedTextView.setChecked(selectedItems[position]);
        updateTextColor(holder.checkedTextView, selectedItems[position]);

        return convertView;
    }

    public void updateTextColor(CheckedTextView view, boolean isSelected) {
        if (isSelected) {
            view.setTextColor(Color.BLUE);
        } else {
            view.setTextColor(Color.BLACK);
        }
    }

    private static class ViewHolder {
        CheckedTextView checkedTextView;
    }
}
