package com.mupceet.rxlearning.apps;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.mupceet.rxlearning.R;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;


public class ApplicationAdapter extends RecyclerView.Adapter<ApplicationAdapter.ViewHolder> {

    private List<AppInfo> mApplications;

    private int mRowLayout;

    public ApplicationAdapter(List<AppInfo> applications, int rowLayout) {
        mApplications = applications;
        mRowLayout = rowLayout;
    }

    @Override
    public ViewHolder onCreateViewHolder(final ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(mRowLayout, viewGroup, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder viewHolder, int i) {
        final AppInfo appInfo = mApplications.get(i);
        Log.i("lgz", "onBindViewHolder: " + appInfo.toString());
        viewHolder.name.setText(appInfo.getName());
        getBitmap(appInfo.getIcon())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Bitmap>() {
                    @Override
                    public void accept(Bitmap bitmap) throws Exception {
                        viewHolder.image.setImageBitmap(bitmap);
                    }
                });
    }

    @Override
    public int getItemCount() {
        return mApplications == null ? 0 : mApplications.size();
    }

    private Observable<Bitmap> getBitmap(final String icon) {
        Log.i("lgz", "getBitmap: " + icon);
        return Observable.create(new ObservableOnSubscribe<Bitmap>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<Bitmap> subscriber) throws Exception {
                subscriber.onNext(BitmapFactory.decodeFile(icon));
                subscriber.onComplete();
            }
        });
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView name;

        public ImageView image;

        public ViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.name);
            image = (ImageView) itemView.findViewById(R.id.image);
        }
    }
}
