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

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import io.reactivex.MaybeObserver;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * 知识点：
 * filter
 * take
 * takeLast
 * distinct
 * first
 * last
 * skip skipLast
 * elementAt
 * <p>
 * 省略：
 * sample throttleFirst throttleLast
 * timeout
 * debounce
 */
public class FilteringFragment extends Fragment {
    public static final String TAG = "FilteringFragment";

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


    public FilteringFragment() {
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
        inflater.inflate(R.menu.menu_filtering_observables, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_filter) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            loadAppsFilter(mStoredAppsList);
            return true;
        } else if (id == R.id.menu_take) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() > 3) {
                loadAppsTake(mStoredAppsList);
            }
            return true;
        } else if (id == R.id.menu_take_last) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() > 3) {
                loadAppsTakeLast(mStoredAppsList);
            }
            return true;
        } else if (id == R.id.menu_distinct) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() > 3) {
                loadAppsDistinct(mStoredAppsList);
            }
            return true;
        } else if (id == R.id.menu_first) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() > 3) {
                loadAppsFirst(mStoredAppsList);
            }
        } else if (id == R.id.menu_skip) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() > 3) {
                loadAppsSkip(mStoredAppsList);
            }
        } else if (id == R.id.menu_element_at) {
            mSwipeRefreshLayout.setEnabled(false);
            mSwipeRefreshLayout.setRefreshing(true);
            mStoredAppsList = ApplicationsList.getInstance().getList();
            if (mStoredAppsList.size() > 3) {
                loadAppsElementAt(mStoredAppsList);
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


    // Example 1: Filter

    private void loadAppsFilter(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .filter(new Predicate<AppInfo>() {
                    @Override
                    public boolean test(AppInfo appInfo) throws Exception {
                        return appInfo.getName().startsWith("C");
                    }
                }).subscribe(mObserver);
    }

    // Example 2: take

    private void loadAppsTake(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .take(3)
                .subscribe(mObserver);
    }

    // Example 3: takeLast

    private void loadAppsTakeLast(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .takeLast(2)
                .subscribe(mObserver);
    }

    // Example 4: distinct
    // 缺少了：DistinctUntilsChanged 书中举了一个例子，就是温度计不停发送温度，只有温度发生变化时才真正地发送

    private void loadAppsDistinct(List<AppInfo> apps) {
        Observable<AppInfo> fullOfDuplicate = Observable.fromIterable(apps).take(3).repeat(3);
        fullOfDuplicate.distinct()
                .subscribe(mObserver);
    }

    // Example 5:
    // first(AppInfo defaultInfo)
    // last(AppInfo defaultInfo)

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

    private void loadAppsFirst(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .firstElement()
                .subscribe(mMaybeObserver);
    }


    // Example 6: skip skipLast
    // ..

    private void loadAppsSkip(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .skip(3)
                .subscribe(mObserver);
    }


    // Example 7: elementAt 两个参数，count defaultItem
    // ..

    private void loadAppsElementAt(List<AppInfo> apps) {
        Observable.fromIterable(apps)
                .elementAt(3)
                .subscribe(mMaybeObserver);
    }

    // Example 8:
    // sample(fullOfDuplicate, true)
    // sample(3, TimeUnit.DAYS) 每间隔时间发送最近的最后一个条目
    // 如果要发送最近的第一个条目： throttleFirst
    // 其中 throttleLast 就是使用 sample 实现的。
    // 补充：throttleWithTimeout 是使用 debounce 实现的

    // Example 9:
    // timeout 每隔n秒至少发射一个, 如果没能及时发射，则会触发 onError
    // ..

    // Example 10:
    // debounce() 函数过滤掉由Observable发射的速率过快的数据；如果在一个指定
    // 的时间间隔过去了仍旧没有发射一个，那么它将发射最后的那个

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

}
