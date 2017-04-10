package study.charco.demo.mapdemo;

import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.UiSettings;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;

import java.util.ArrayList;
import java.util.List;

import study.charco.demo.mapdemo.utils.LocationTask;
import study.charco.demo.mapdemo.utils.OnLocationGetListener;
import study.charco.demo.mapdemo.utils.PositionEntity;

import static com.amap.api.maps.AMapOptions.LOGO_POSITION_BOTTOM_RIGHT;
import static com.amap.api.maps.AMapOptions.ZOOM_POSITION_RIGHT_CENTER;
import static study.charco.demo.mapdemo.R.id.map;

public class MainActivity extends AppCompatActivity implements AMap.OnMapLoadedListener, AMap.OnCameraChangeListener, AMap.OnMapClickListener, OnLocationGetListener, PoiSearch.OnPoiSearchListener, GeocodeSearch.OnGeocodeSearchListener {

    public static final String TAG = "AMAP";

    private MapView mMapView;
    private AMap mAmap;
    private Marker mPositionMark;
    private UiSettings mUiSettings;
    private LocationTask mLocationTask;
    private LatLng mStartPosition , mMyPosition;
    private int currentPage;
    private PoiSearch.Query query;
    private PoiSearch poiSearch;
    private GeocodeSearch mGeocodeSearch;

    private RecyclerView mRecyclerView;
    private MyAdapter myAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initMapView(savedInstanceState);
        initRecyclerView();
    }

    private void initRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        myAdapter = new MyAdapter(getApplicationContext());
        mRecyclerView.setAdapter(myAdapter);
    }

    private void initMapView(Bundle savedInstanceState) {

        mLocationTask = LocationTask.getInstance(getApplicationContext());
        mLocationTask.setOnLocationGetListener(this);
        mMapView = (MapView) findViewById(map);
        mMapView.onCreate(savedInstanceState);
        mAmap = mMapView.getMap();
        mAmap.getUiSettings().setZoomControlsEnabled(false);
        mAmap.setOnMapLoadedListener(this);
        mAmap.setOnCameraChangeListener(this);
        mAmap.setMapType(AMap.MAP_TYPE_NORMAL);// 矢量地图模式
        mAmap.moveCamera(CameraUpdateFactory.zoomTo(15));//缩放级别

        //mAmap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
        // 设置定位的类型为定位模式：定位（AMap.LOCATION_TYPE_LOCATE）、跟随（AMap.LOCATION_TYPE_MAP_FOLLOW）
        // 地图根据面向方向旋转（AMap.LOCATION_TYPE_MAP_ROTATE）三种模式
        //mAmap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
        mAmap.setOnMapClickListener(this);// 点击地图监听

        mUiSettings = mAmap.getUiSettings();//实例化UiSettings类
        mUiSettings.setMyLocationButtonEnabled(false);// 设置默认定位按钮是否显示
        mUiSettings.setZoomControlsEnabled(true);//显示缩放按钮
        mUiSettings.setZoomPosition(ZOOM_POSITION_RIGHT_CENTER);//缩放按钮  右边界中部：ZOOM_POSITION_RIGHT_CENTER 右下：ZOOM_POSITION_RIGHT_BUTTOM。
        mUiSettings.setLogoPosition(LOGO_POSITION_BOTTOM_RIGHT);//Logo的位置 左下：LOGO_POSITION_BOTTOM_LEFT 底部居中：LOGO_POSITION_BOTTOM_CENTER 右下：LOGO_POSITION_BOTTOM_RIGHT
        //mUiSettings.setCompassEnabled(true);//指南针
        mUiSettings.setZoomGesturesEnabled(true);//手势缩放
        mUiSettings.setScaleControlsEnabled(true);//比例尺

        mGeocodeSearch = new GeocodeSearch(this);
        mGeocodeSearch.setOnGeocodeSearchListener(this);

    }

    /**
     * 返回到我的位置
     */
    public void backToMyLocation(View view){
        CameraUpdate cameraUpate = CameraUpdateFactory.newLatLngZoom(
                mMyPosition, mAmap.getCameraPosition().zoom);
        mAmap.animateCamera(cameraUpate);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mMapView.onSaveInstanceState(outState);
    }

    /**
     * 方法必须重写
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        mLocationTask.onDestroy();
    }

    /**
     *地图加载完成之后
     * 固定地图中心marker
     */
    @Override
    public void onMapLoaded() {

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.setFlat(true);
        markerOptions.anchor(0.5f, 0.5f);
        markerOptions.position(new LatLng(0, 0));
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(BitmapFactory
                        .decodeResource(getResources(), R.drawable.icon_loaction_start)));
        mPositionMark = mAmap.addMarker(markerOptions);

        mPositionMark.setPositionByPixels(mMapView.getWidth() / 2,
                mMapView.getHeight() / 2);

        mLocationTask.startSingleLocate();

    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    /**
     *操作地图之后，重新定位完成回调，根据坐标查询附近建筑
     */
    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        mStartPosition = cameraPosition.target;
        RegeocodeQuery regecodeQuery = new RegeocodeQuery(new LatLonPoint(
                mStartPosition.latitude, mStartPosition.longitude), 50, GeocodeSearch.AMAP);
        mGeocodeSearch.getFromLocationAsyn(regecodeQuery);
    }

    @Override
    public void onMapClick(LatLng latLng) {
        Log.d(TAG,"点击了地图 经度 = "+latLng.longitude + " 纬度 " +latLng.latitude);
    }

    /**
     * 单次定位回调，将位置显示在地图上
     * @param entity
     */
    @Override
    public void onLocationGet(PositionEntity entity) {
        mStartPosition = new LatLng(entity.latitue, entity.longitude);
        mMyPosition = mStartPosition;
        //将地图移动到当前位置
        CameraUpdate cameraUpate = CameraUpdateFactory.newLatLngZoom(
                mStartPosition, mAmap.getCameraPosition().zoom);
        mAmap.animateCamera(cameraUpate);

        BitmapDescriptor bitmapDescriptor=BitmapDescriptorFactory
                .fromResource(R.drawable.mylocation);
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.setFlat(true);
        markerOptions.anchor(0.5f, 0.5f);
        markerOptions.icon(bitmapDescriptor);
        markerOptions.position(new LatLng(mStartPosition.latitude, mStartPosition.longitude));
        mAmap.addMarker(markerOptions);

        PoiItem poiItem = new PoiItem("", new LatLonPoint(entity.latitue, entity.longitude), "", "");
        poiItem.setAdName(entity.address);
        myAdapter.setMyLocation(poiItem);
    }

    @Override
    public void onRegecodeGet(PositionEntity entity) {

    }

    protected void doSearchQuery(String name, String type, String city) {
        currentPage = 0;
        query = new PoiSearch.Query(name, type, city);// 第一个参数表示搜索字符串，第二个参数表示poi搜索类型，第三个参数表示poi搜索　区域（空字符串代表全国）
        query.setPageSize(20);// 设置每页最多返回多少条poiitem
        query.setPageNum(currentPage);// 设置查第一页

        poiSearch = new PoiSearch(this, query);
        poiSearch.setOnPoiSearchListener(this);
        poiSearch.setBound(new PoiSearch.SearchBound(
                new LatLonPoint(mStartPosition.longitude,mStartPosition.latitude), 2000, true));//
        poiSearch.searchPOIAsyn();
    }

    @Override
    public void onPoiSearched(PoiResult poiResult, int rCode) {
        Log.d(TAG,"onPoiSearched poiResult = "+poiResult.toString());
        ArrayList<PoiItem> pois = poiResult.getPois();
        ArrayList<String> address=new ArrayList<String>();
        for(int i=0;i<pois.size();i++){
            address.add(pois.get(i).toString());
        }
        myAdapter.changeLocations(pois);
        Log.d(TAG,"address = "+address.toString());
    }

    @Override
    public void onPoiItemSearched(PoiItem poiItem, int i) {
        Log.d(TAG,"onPoiItemSearched poiItem = "+poiItem.toString());
    }


    @Override
    public void onRegeocodeSearched(RegeocodeResult regeocodeReult, int resultCode) {
        if (resultCode == AMapException.CODE_AMAP_SUCCESS) {
            if (regeocodeReult != null
                    && regeocodeReult.getRegeocodeAddress() != null ) {
                doSearchQuery("","120000",regeocodeReult.getRegeocodeAddress().getCity());
            }
        }
    }

    @Override
    public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

    }

    class MyAdapter extends RecyclerView.Adapter{

        private List<PoiItem> locastions = new ArrayList<>();
        private Context mContext;
        private PoiItem myLocation;

        public MyAdapter(Context context){
            mContext = context;
        }

        public void setMyLocation(PoiItem myLocation){
            this.myLocation = myLocation;
            notifyItemChanged(0);
        }

        public void addLocations(List<PoiItem> data){
            locastions.addAll(data);
            notifyDataSetChanged();
        }

        public void changeLocations(List<PoiItem> data){
            locastions.clear();
            if (myLocation!=null)
            locastions.add(myLocation);
            locastions.addAll(data);
            notifyDataSetChanged();
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(mContext).inflate(R.layout.item,null);
            return new MyHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MyHolder myHolder = (MyHolder) holder;
            String address = position == 0?"我的位置-> "+locastions.get(position).getAdName():locastions.get(position).getTitle();
            myHolder.textview.setText(address);
        }

        @Override
        public int getItemCount() {
            return locastions.size();
        }
    }

    class MyHolder extends RecyclerView.ViewHolder{

        public TextView textview;

        public MyHolder(View itemView) {
            super(itemView);
            textview = (TextView) itemView.findViewById(R.id.texview);
        }
    }

}
