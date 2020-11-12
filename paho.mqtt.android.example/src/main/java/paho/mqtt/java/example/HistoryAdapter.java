/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 */
package paho.mqtt.java.example;

import android.annotation.SuppressLint;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import paho.mqtt.java.example.utils.TimeUtils;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private ArrayList<String> history;

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mTextView;

        public ViewHolder(View v) {
            super(v);
            mTextView = (TextView) v.findViewById(R.id.row_text);
        }
    }

    public HistoryAdapter(ArrayList<String> dataSet) {
        history = dataSet;
    }

    @Override
    public HistoryAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Create View
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_row, parent, false);
        return new ViewHolder(v);
    }

    public void clear() {
        history.clear();
        this.notifyDataSetChanged();
    }

    public void add(String data) {
        history.add(data);
        this.notifyDataSetChanged();
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mTextView.setText(TimeUtils.formatTime() + history.get(position));
    }

    @Override
    public int getItemCount() {
        return history.size();
    }
}
