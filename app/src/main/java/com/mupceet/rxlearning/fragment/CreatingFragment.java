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
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * 主要内容：
 * 1. create
 * 2. from
 * 3. just
 * 4. repeat
 * 5. range
 * 6. timer
 * 7. interval
 * <p>
 * 缺少：
 * 3. defer
 * 4. Empty/Never/Throw
 */

public class CreatingFragment extends Fragment {
    public static final String TAG = "CreatingFragment";

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


    public CreatingFragment() {
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

        getFileDir()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<File>() {
                    @Override
                    public void accept(File file) throws Exception {
                        mFilesDir = file;
                    }
                });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_creating_observables, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_create) {
            mSwipeRefreshLayout.setEnabled(true);
            mSwipeRefreshLayout.setRefreshing(true);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    refreshTheList();
                }
            });
            refreshTheList();
            return true;
        } else if (id == R.id.menu_from) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadList(mStoredAppsList);
            return true;
        } else if (id == R.id.menu_just) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() >= 5) {
                loadApps(mStoredAppsList.get(0), mStoredAppsList.get(2), mStoredAppsList.get(5));
            }
        } else if (id == R.id.menu_repeat) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() >= 5) {
                loadAppsRepeat(mStoredAppsList.get(0),
                        mStoredAppsList.get(2),
                        mStoredAppsList.get(5));
            }
        } else if (id == R.id.menu_range) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() >= 5) {
                loadAppsRange(mStoredAppsList);
            }
        } else if (id == R.id.menu_timer) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() >= 5) {
                loadAppsTimer(mStoredAppsList.get(0));
            }
        } else if (id == R.id.menu_interval) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() >= 5) {
                loadAppsInterval(mStoredAppsList.get(0));
            }
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

    //
    // Example 1: create
    //
    // 知识点：create

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

    //
    // Example 2: from
    //
    // 知识点：fromIterable
    // 扩展：
    // fromArray
    // fromFuture
    // fromCallable
    // fromPublisher。

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

    private void loadList(List<AppInfo> apps) {
        if (apps == null || apps.size() == 0) {
            Toast.makeText(getActivity(), "Have not loaded list yet!", Toast.LENGTH_SHORT).show();
            mSwipeRefreshLayout.setRefreshing(false);
            return;
        }
        Observable.fromIterable(apps)
                .subscribe(mObserver);

    }

    //
    // Example 3: just
    //
    // 知识点：just 最多只能 just 10个，但实际上使用的是 fromArray 的方式。

    private void loadApps(AppInfo app1, AppInfo app2, AppInfo app3) {
        Observable.just(app1, app2, app3)
                .subscribe(mObserver);
    }

    //
    // Example 4: repeat
    //
    // 知识点：repeat 重复发射数据

    private void loadAppsRepeat(AppInfo app1, AppInfo app2, AppInfo app3) {
        Observable.just(app1, app2, app3)
                .repeat(3)
                .subscribe(mObserver);
    }

    //
    // Example 5: range
    //
    // 知识点：range 从某个数开始，连续发射几个数

    private void loadAppsRange(final List<AppInfo> apps) {
        Observable.range(2, 3)
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mDisposable = d;
                        mApps.clear();
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onNext(@NonNull Integer integer) {
                        AppInfo appInfo = apps.get(integer);
                        Log.i(TAG, "onNext: " + appInfo.toString());
                        mApps.add(appInfo);
                        mAdapter.notifyItemInserted(mApps.size() - 1);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onComplete() {
                        mSwipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_SHORT).show();
                    }
                });


    }

    //
    // Example 6: timer
    //
    // 知识点：延时

    private void loadAppsTimer(final AppInfo app) {
        mApps.clear();
        mAdapter.notifyDataSetChanged();

        Observable.timer(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        mApps.add(app);
                        mAdapter.notifyItemInserted(mApps.size() - 1);
                        mSwipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_SHORT).show();

                    }
                });
    }

    //
    // Example 7: interval
    //
    // 知识点：轮询

    private void loadAppsInterval(final AppInfo app) {
        Observable.interval(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Long>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        mDisposable = d;
                        mApps.clear();
                        mAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onNext(@NonNull Long aLong) {
                        Log.i(TAG, "onNext: " + aLong);
                        mApps.add(app);
                        mAdapter.notifyItemInserted(mApps.size() - 1);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        Toast.makeText(getActivity(), "Something went wrong!", Toast.LENGTH_SHORT).show();
                        mSwipeRefreshLayout.setRefreshing(false);
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG, "onComplete: ");
                        mSwipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(getActivity(), "Here is the list!", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
