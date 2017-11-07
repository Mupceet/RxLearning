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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

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
import io.reactivex.schedulers.Schedulers;

public class CombiningFragment extends Fragment {
    public static final String TAG = "CombiningFragment";

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


    public CombiningFragment() {
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
        inflater.inflate(R.menu.menu_combining_observables, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_merge) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsMerge(mStoredAppsList);
            return true;
        } else if (id == R.id.menu_zip) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsZip(mStoredAppsList);
            return true;
        } else if (id == R.id.menu_join) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsJoin(mStoredAppsList);
            return true;
        } else if (id == R.id.menu_combineLatest) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsCombineLatest(mStoredAppsList);
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


    // Example 1: merge

    private void loadAppsMerge(List<AppInfo> apps) {
        List<AppInfo> reverseApps = new ArrayList<>();
        reverseApps.addAll(apps);
        Collections.reverse(reverseApps);
        Observable<AppInfo> observable = Observable.fromIterable(apps);
        Observable<AppInfo> observableReverse = Observable.fromIterable(reverseApps);

        Observable.merge(observable, observableReverse)
                .subscribe(mObserver);
    }

    // Example 2: zip

    private void loadAppsZip(List<AppInfo> apps) {
        Observable<AppInfo> observable = Observable.fromIterable(apps);
        Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS);

        Observable.zip(observable, interval, new BiFunction<AppInfo, Long, AppInfo>() {
            @Override
            public AppInfo apply(AppInfo appInfo, Long aLong) throws Exception {
                return updateAppName(appInfo, aLong);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .take(10)
                .subscribe(mObserver);
    }

    // Example 3: join
    // 这里需要画图来说明，就是一个排列组合关系 但是如果发生了重叠，就有点不一样了，要探究一下

    private void loadAppsJoin(final List<AppInfo> apps) {
        Observable<AppInfo> appsSequence = Observable.interval(1, TimeUnit.SECONDS)
                .map(new Function<Long, AppInfo>() {
                    @Override
                    public AppInfo apply(Long aLong) throws Exception {
                        return apps.get(aLong.intValue());
                    }
                });

        Observable<Long> interval = Observable.interval(1, TimeUnit.SECONDS);

        appsSequence.join(interval, new Function<AppInfo, ObservableSource<Long>>() {
            @Override
            public ObservableSource<Long> apply(AppInfo appInfo) throws Exception {
                return Observable.timer(1900, TimeUnit.MILLISECONDS);
            }
        }, new Function<Long, ObservableSource<Long>>() {
            @Override
            public ObservableSource<Long> apply(Long aLong) throws Exception {
                return Observable.timer(900, TimeUnit.MILLISECONDS);
            }
        }, new BiFunction<AppInfo, Long, AppInfo>() {
            @Override
            public AppInfo apply(AppInfo appInfo, Long aLong) throws Exception {
                return updateAppName(appInfo, aLong);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .take(10)
                .subscribe(mObserver);
    }

    // Example 4: combineLatest
    // 这里需要画图来说明，就是一个最近关系的zip 但是如果发生了重叠，就有点不一样了，要探究一下

    private void loadAppsCombineLatest(final List<AppInfo> apps) {
        Observable<AppInfo> appsSequence = Observable.interval(1, TimeUnit.SECONDS)
                .map(new Function<Long, AppInfo>() {
                    @Override
                    public AppInfo apply(Long aLong) throws Exception {
                        return apps.get(aLong.intValue());
                    }
                });

        Observable<Long> interval = Observable.interval(1400, TimeUnit.MILLISECONDS);

        Observable.combineLatest(appsSequence, interval, new BiFunction<AppInfo, Long, AppInfo>() {
            @Override
            public AppInfo apply(AppInfo appInfo, Long aLong) throws Exception {
                return updateAppName(appInfo, aLong);
            }
        }).observeOn(AndroidSchedulers.mainThread())
                .take(10)
                .subscribe(mObserver);
    }

    // Example 5: and them when

    private void loadAppsAndThenWhen(final List<AppInfo> apps) {
        // nothing
        
    }

    // Example 6: switchOnNext
    // 给出一个发射多个Observables序列的源Observable， switch() 订阅到源
    // Observable然后开始发射由第一个发射的Observable发射的一样的数据。当源
    // Observable发射一个新的Observable时， switch() 立即取消订阅前一个发射数
    // 据的Observable（ 因此打断了从它那里发射的数据流） 然后订阅一个新的
    // Observable，并开始发射它的数据。.

    //=====================================================================================

    private AppInfo updateAppName(AppInfo appInfo, Long aLong) {
        appInfo.setName(aLong + " " + appInfo.getName());
        return appInfo;
    }

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
