package com.example.lin.littleweather;

import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.CardView;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.GenericTransitionOptions;
import com.bumptech.glide.Glide;
import com.example.lin.littleweather.db.City;
import com.example.lin.littleweather.db.County;
import com.example.lin.littleweather.db.Province;
import com.example.lin.littleweather.util.HttpUtil;
import com.example.lin.littleweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseFragment extends Fragment{
    public static  final int Level_Province=0;
    public static  final int Level_City=1;
    public static  final int Level_County=2;
    private List<String> datalist=new ArrayList<>();
    private ProgressDialog progressDialog;
    private TextView title;
    private Button back;
    private RecyclerView recyclerView;
    private int currentLevel;
    private List<Province> provinceList;
    private List<City> cityList;
    private List<County> countyList;
    private ChooseRecyclerViewAdapter recyclerViewAdapter;
    private Province selectedProvince;
    private City selectedCity;
    private ImageView pic;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_choose,container,false);
       title=view.findViewById(R.id.title_text_choose);
        back=view.findViewById(R.id.back_btn_choose);
        pic=view.findViewById(R.id.pic_choose);
        recyclerView=view.findViewById(R.id.recycleview_choose);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        recyclerViewAdapter=new ChooseRecyclerViewAdapter(datalist);
        recyclerView.setAdapter(recyclerViewAdapter);
        //DividerItemDecoration divider = new DividerItemDecoration(getActivity(),DividerItemDecoration.VERTICAL);
        //divider.setDrawable(ContextCompat.getDrawable(getActivity(),R.drawable.custom_divider));
       // recyclerView.addItemDecoration(divider);
        return  view;
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel==Level_County){
                    queryCities();
                }else if (currentLevel==Level_City){
                    queryProvinces();
                }
            }
        });
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(getActivity());
        String weatherPic=prefs.getString("bing_pic",null);
        if(weatherPic==null){
            showProgressDialog();
            loadBingPic();
        }else {
            Glide.with(getActivity()).load(weatherPic).into(pic);
        }
        queryProvinces();
    }


    //以下是RecyclerViewAdapter的实现
    public class ChooseRecyclerViewAdapter extends RecyclerView.Adapter<ChooseRecyclerViewAdapter.ViewHolder> {

        private  List<String> mdatalist;

        public ChooseRecyclerViewAdapter(List<String> datalist) {
            mdatalist=datalist;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recycleview_baisc, parent, false);
            final ViewHolder holder=new ViewHolder(view);
            holder.ExView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(currentLevel==Level_Province){
                        int position=holder.getAdapterPosition();
                        selectedProvince=provinceList.get(position);
                        queryCities();
                    }else if(currentLevel==Level_City){
                        int position=holder.getAdapterPosition();
                        selectedCity=cityList.get(position);
                        queryCounties();
                    }else if(currentLevel==Level_County){
                        int position=holder.getAdapterPosition();
                        String weatherId=countyList.get(position).getWeatherId();
                        if(getActivity() instanceof MainActivity) {
                            Intent intent = new Intent(getActivity(), WeatherActivity.class);
                            intent.putExtra("weather_id", weatherId);
                            startActivity(intent);
                            getActivity().finish();
                        }else if(getActivity() instanceof WeatherActivity){
                            WeatherActivity activity= (WeatherActivity) getActivity();
                            activity.drawerLayout.closeDrawers();
                            activity.swipeRefresh.setRefreshing(true);
                            activity.requestWeather(weatherId);
                        }
                    }

                }
            });
            return holder;
        }

        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.textView.setText(mdatalist.get(position));
        }


        @Override
        public int getItemCount() {
            return mdatalist.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            View ExView;
            TextView textView;
            ViewHolder(View view) {
                super(view);
                ExView=view;
                textView=view.findViewById(R.id.DataText_basic);
            }

        }
    }
    //RecyclerViewAdapter的实现结束

    //查询全国所有的省，优先从数据库查询，如果没有则再去服务器上查询
    private void queryProvinces(){
        title.setText("中国");
        back.setVisibility(View.GONE);
        provinceList= DataSupport.findAll(Province.class);
        if(provinceList.size()>0){
            datalist.clear();
            for (Province province:provinceList){
                datalist.add(province.getProvinceName());
            }
            recyclerViewAdapter.notifyDataSetChanged();
            currentLevel=Level_Province;
        }else {
            String address="http://guolin.tech/api/china";
            queryFromSever(address,"province");
        }
    }

    //查询选中省的所有的市，优先从数据库查询，如果没有则再去服务器上查询
    private void queryCities(){
        title.setText(selectedProvince.getProvinceName());
        back.setVisibility(View.VISIBLE);
       cityList=DataSupport.where("ProvinceId=?",String.valueOf(selectedProvince.getProvinceId())).find(City.class);
        if(cityList.size()>0){
            datalist.clear();
            for (City city:cityList){
                datalist.add(city.getCityName());
            }
            recyclerViewAdapter.notifyDataSetChanged();
            currentLevel=Level_City;
        }else {
            int ProvinceId=selectedProvince.getProvinceId();
            String address="http://guolin.tech/api/china/"+ProvinceId;
            queryFromSever(address,"city");
        }
    }

    //查询选中市的所有的县，优先从数据库查询，如果没有则再去服务器上查询
    private void queryCounties(){
        title.setText(selectedCity.getCityName());
        back.setVisibility(View.VISIBLE);
        countyList= DataSupport.where("CityId=?",String.valueOf(selectedCity.getCityId())).find(County.class);
        if(countyList.size()>0){
            datalist.clear();
            for (County county:countyList){
                datalist.add(county.getCountyName());
            }
            recyclerViewAdapter.notifyDataSetChanged();
            currentLevel=Level_County;
        }else {
            int ProvinceId=selectedProvince.getProvinceId();
            int CityId=selectedCity.getCityId();
            String address="http://guolin.tech/api/china/"+ProvinceId+"/"+CityId;
            queryFromSever(address,"county");
        }
    }

    //根据传入的地址和服务类型从服务器上查询省市县的数据
    private void queryFromSever(String address, final String type){
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText=response.body().string();
                boolean result=false;
                if("province".equals(type)){
                    result= Utility.handleProvinceResponse(responseText);
                }else if("city".equals(type)){
                    result= Utility.handleCityResponse(responseText,selectedProvince.getProvinceId());
                }else if("county".equals(type)){
                    result= Utility.handleCountyResponse(responseText,selectedCity.getCityId());
                }
                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }
        });
    }

    //显示进度对话框
    private void showProgressDialog(){
        if(progressDialog==null){
            progressDialog=new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    //关闭进度对话框
    private void closeProgressDialog(){
        if(progressDialog!=null){
            progressDialog.dismiss();
        }
    }

    //加载必应每日一图
    public void loadBingPic(){
        final String requestpic="http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestpic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
               final String pic2=response.body().string();
                SharedPreferences.Editor editor=  PreferenceManager.getDefaultSharedPreferences(getActivity()).edit();
                editor.putString("bing_pic",pic2);
                editor.apply();
               getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(getActivity()).load(pic2).into(pic);
                        closeProgressDialog();
                    }
                });

            }
        });
    }

}