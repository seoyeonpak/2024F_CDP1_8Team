package com.dashboard.obd.driving;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.dashboard.obd.R;




public class BottomFragment extends Fragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		try {
			return inflater.inflate(R.layout.fragment_driving_bottom, container, false);
		} catch (Exception e) {
			Log.e("BottomFragment","oncreate view");
			return null;
		}
	}
	
	@Override
	public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
		try {
			super.onViewCreated(view, savedInstanceState);
		}catch(Exception e){
			Log.e("BottomFragment", "onViewCreated");
		}
	}
}
