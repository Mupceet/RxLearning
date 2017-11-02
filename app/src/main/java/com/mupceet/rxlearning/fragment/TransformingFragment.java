package com.mupceet.rxlearning.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mupceet.rxlearning.App;
import com.mupceet.rxlearning.R;
import com.mupceet.rxlearning.apps.AppInfo;
import com.mupceet.rxlearning.apps.AppInfoRich;
import com.mupceet.rxlearning.apps.ApplicationAdapter;
import com.mupceet.rxlearning.apps.ApplicationsList;
import com.mupceet.rxlearning.utils.Utils;

import java.io.File;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.observables.GroupedObservable;
import io.reactivex.schedulers.Schedulers;

public class TransformingFragment extends Fragment {
    public static final String TAG = "TransformingFragment";

    @BindView(R.id.fragment_creating_list)
    RecyclerView mRecyclerView;

    @BindView(R.id.fragment_creating_swipe_container)
    SwipeRefreshLayout mSwipeRefreshLayout;
    Unbinder unbinder;

    private ApplicationAdapter mAdapter;
    private List<AppInfo> mApps = new ArrayList<>();
    private List<AppInfo> mStoredAppsList = new ArrayList<>();
    private File mFilesDir;
    private Disposable mDisposable;


    public TransformingFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView: ");
        View view = inflater.inflate(R.layout.fragment_creating, container, false);
        unbinder = ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        Log.i(TAG, "onViewCreated: ");
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        mAdapter = new ApplicationAdapter(mApps, R.layout.applications_list_item);
        mRecyclerView.setAdapter(mAdapter);

        mSwipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorPrimary, null));
        mSwipeRefreshLayout.setProgressViewOffset(false, 0,
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics()));

        // Progress
        mSwipeRefreshLayout.setEnabled(false);
        mSwipeRefreshLayout.setRefreshing(true);

        getFileDir()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        mFilesDir = file;
                        refreshTheList();
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_transforming_observables, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_map) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsMap(mStoredAppsList);
            return true;
        } else if (id == R.id.menu_scan) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsScan(mStoredAppsList);
            return true;
        } else if (id == R.id.menu_group_by) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsGroupBy(mStoredAppsList);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
        if (mDisposable != null && !mDisposable.isDisposed()) {
            mDisposable.dispose();
        }
    }


    // Example 1: map
    // 还有一个特殊版本 cast 它的作用是转换类型

    private void loadAppsMap(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .map(new Function<AppInfo, AppInfo>() {
                    @Override
                    public AppInfo apply(AppInfo appInfo) throws Exception {
                        String currentName = appInfo.getName();
                        String lowercaseName = currentName.toLowerCase();
                        appInfo.setName(lowercaseName);
                        return appInfo;
                    }
                }).subscribe(mObserver);
    }

    // Example 2: flatMap
    // 在复杂的场景中，我们有一个这样的Observable：它发射一个数据序列，这些数据
    // 本身也可以发射Observable。RxJava的 flatMap() 函数提供一种铺平序列的方
    // 式，然后合并这些Observables发射的数据，最后将合并后的结果作为最终的
    // Observable
    // 要注意的是：关于合并部分：它允许交叉。正如上图所示，这意味
    // 着 flatMap() 不能够保证在最终生成的Observable中源Observables确切的发射
    // 顺序..

//    private void loadAppsFlatMap(List<AppInfo> apps) {
//        Observable.fromIterable(apps)
//                .flatMap(new Function<AppInfo, ObservableSource<List<AppInfo>>>() {
//                    @Override
//                    public ObservableSource<List<AppInfo>> apply(AppInfo appInfo) throws Exception {
//                        return null;
//                    }
//                })
//    }

    // Example 3: concatMap
    // 解决了以上说的问题，它的作用是把发射的值连续在一起，不合并他们所以不交叉

    // Example 4： flatMapIterable
    // 不是太明白字这个操作符的内容

    // Example 5： switchMap
    // 切换的含义。switchMap() 和 flatMap() 很像，除了一点：每当源Observable发射一个新的数据项（ Observable）
    // 时，它将取消订阅并停止监视之前那个数据项产生的Observable，并开始监视当前发射的这一个。

    // Example 6: scan
    // 将输出的继续扫描下去

    private void loadAppsScan(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .scan(new BiFunction<AppInfo, AppInfo, AppInfo>() {
                    @Override
                    public AppInfo apply(AppInfo appInfo, AppInfo appInfo2) throws Exception {
                        if (appInfo.getName().length() > appInfo2.getName().length()) {
                            return appInfo;
                        } else {
                            return appInfo2;
                        }
                    }
                })
                .distinct()
                .subscribe(mObserver);
    }

    // Example 7: groupBy

    private void loadAppsGroupBy(List<AppInfo> apps) {
        Observable<GroupedObservable<String, AppInfo>> groupedItems = Observable.fromIterable(apps)
                .groupBy(new Function<AppInfo, String>() {
                    @Override
                    public String apply(AppInfo appInfo) throws Exception {
                        SimpleDateFormat formatter = new SimpleDateFormat("MM/yyyy");
                        return formatter.format(new Date(appInfo.getLastUpdateTime()));
                    }
                });

        Observable
                .concat(groupedItems)
                .subscribe(mObserver);
    }

    // Example 8: buffer

    // Example 9: window

    //=====================================================================================

    private Observable<File> getFileDir() {
        return Observable.create(new ObservableOnSubscribe<File>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<File> e) throws Exception {
                e.onNext(getContext().getFilesDir());
                e.onComplete();
            }
        });
    }

    private Observable<AppInfo> getApps() {
        return Observable.create(new ObservableOnSubscribe<AppInfo>() {
            @Override
            public void subscribe(@NonNull ObservableEmitter<AppInfo> subscriber) throws Exception {
                List<AppInfoRich> apps = new ArrayList<>();

                final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
                mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

                List<ResolveInfo> infos = getActivity().getPackageManager().queryIntentActivities(mainIntent, 0);
                for (ResolveInfo info : infos) {
                    apps.add(new AppInfoRich(getActivity(), info));
                }
                //apps.addAll(infos.stream().map(info -> new AppInfoRich(getActivity(), info)).collect(Collectors.toList()));


                for (AppInfoRich appInfo : apps) {
                    Bitmap icon = Utils.drawableToBitmap(appInfo.getIcon());
                    String name = appInfo.getName();
                    String iconPath = mFilesDir + "/" + name;
                    Utils.storeBitmap(App.getContext(), icon, name);

                    // 检查观察者的 isDisposed 状态，以便在没有观察者的时候，让你的 Observable 停止发射数据或者做昂贵的运算。
                    if (subscriber.isDisposed()) {
                        return;
                    }
                    subscriber.onNext(new AppInfo(name, iconPath, appInfo.getLastUpdateTime()));
                }
                if (!subscriber.isDisposed()) {
                    subscriber.onComplete();
                }
            }
        });
    }

    private void refreshTheList() {
        getApps()
                // 经过转换，得到一个List的Single对象
                // Single 的观察者是一个 SingleObserver，只需要实现 成功与失败的回调
                .toSortedList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<AppInfo>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        Log.d("lgz", "onSubscribe: ");
                        mDisposable = d;
                    }

                    @Override
                    public void onSuccess(@NonNull List<AppInfo> appInfos) {
                        Toast.makeText(getActivity(), "App list have updated", Toast.LENGTH_SHORT).show();
                        mApps.clear();
                        mApps.addAll(appInfos);
                        mAdapter.notifyDataSetChanged();
                        mSwipeRefreshLayout.setRefreshing(false);
                        // store for other examples
                        storeList(appInfos);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                });

    }

    private void storeList(final List<AppInfo> appInfos) {
        ApplicationsList.getInstance().setList(appInfos);

        Schedulers.io().createWorker().schedule(new Runnable() {
            @Override
            public void run() {
                SharedPreferences sharedPref = getActivity().getPreferences(Context.MODE_PRIVATE);
                Type appInfoType = new TypeToken<List<AppInfo>>() {
                }.getType();
                sharedPref.edit().putString("APPS", new Gson().toJson(appInfos, appInfoType)).apply();
            }
        });
    }


    Observer<AppInfo> mObserver = new Observer<AppInfo>() {
        @Override
        public void onSubscribe(@NonNull Disposable d) {
            Log.i(TAG, "onSubscribe: ");
            mDisposable = d;
            mApps.clear();
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onNext(AppInfo appInfo) {
            Log.i(TAG, "onNext: " + appInfo.toString());
            mApps.add(appInfo);
            mAdapter.notifyItemInserted(mApps.size() - 1);
        }

        @Override
        public void onError(Throwable t) {
            Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onComplete() {
            mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_SHORT).show();
        }
    };

    private MaybeObserver<AppInfo> mMaybeObserver = new MaybeObserver<AppInfo>() {
        @Override
        public void onSubscribe(@NonNull Disposable d) {
            Log.i(TAG, "onSubscribe: ");
            mDisposable = d;
            mApps.clear();
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onSuccess(AppInfo appInfo) {
            Log.i(TAG, "onSuccess: " + appInfo.toString());
            mApps.add(appInfo);
            mAdapter.notifyItemInserted(mApps.size() - 1);
            // Maybe 正常情况下会success，所以在这里设置状态
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onError(Throwable t) {
            Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
            mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        public void onComplete() {
            // Maybe 不发射数据时回调这里
            mSwipeRefreshLayout.setRefreshing(false);
            Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_SHORT).show();
        }
    };

}
